package com.ukhvat.notes.data.datasource

import androidx.room.Transaction
import com.ukhvat.notes.data.database.NoteVersionDao
import com.ukhvat.notes.data.database.createNoteVersion
import com.ukhvat.notes.data.database.toEntity
import com.ukhvat.notes.data.database.toDomain
import com.ukhvat.notes.domain.datasource.VersionDataSource
import com.ukhvat.notes.domain.model.NoteVersion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlin.math.abs

/**
 * DATASOURCE IMPLEMENTATION FOR NOTE VERSIONING
 * 
 * Threading optimizations:
 * - Single operations: removed redundant withContext for simple Room calls
 * - Batch cleanup: preserved withContext only for multi-step operations  
 * - Version queries: using optimized Room executor
 * 
 * PERFORMANCE IMPROVEMENTS:
 * - Eliminated excessive context switching for single version operations
 * - Batch operations optimized for minimal threading overhead
 * - Preserved all versioning algorithms without logic changes
 */
class VersionDataSourceImpl(
    private val versionDao: NoteVersionDao
) : VersionDataSource {
    
    // ============ VERSION CREATION ============
    
    override suspend fun createVersion(noteId: Long, content: String, customName: String?, isForcedSave: Boolean) {
        val version = createNoteVersion(noteId, content, customName, isForcedSave)
        versionDao.insertVersion(version.toEntity())
        // Enforce retention policy: keep latest 100 non-forced versions; preserve all forced
        try {
            versionDao.cleanupNonForcedVersionsKeepLatest(noteId, 100)
        } catch (_: Exception) {
            // Best-effort cleanup; do not affect UX on failure
        }
    }
    
    // ============ VERSION RETRIEVAL ============
    
    override fun getVersionsForNote(noteId: Long): Flow<List<NoteVersion>> {
        return versionDao.getVersionsForNote(noteId).map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    override suspend fun getVersionsForNoteList(noteId: Long): List<NoteVersion> {
        return versionDao.getVersionsForNoteList(noteId).map { it.toDomain() }
    }
    
    override suspend fun getVersionById(versionId: Long): NoteVersion? {
        return versionDao.getVersionById(versionId)?.toDomain()
    }
    
    override suspend fun getLatestVersionForNote(noteId: Long): NoteVersion? {
        return versionDao.getLatestVersionForNote(noteId)?.toDomain()
    }
    
    // ============ VERSION OPERATIONS ============
    
    override suspend fun deleteVersion(versionId: Long): Boolean {
        versionDao.deleteVersionById(versionId)
        return true // DAO method void, return true if no exception
    }
    
    override suspend fun updateVersionName(versionId: Long, customName: String) {
        versionDao.updateVersionCustomName(versionId, customName)
    }

    override suspend fun updateVersionAiMeta(versionId: Long, provider: String?, model: String?, durationMs: Long?) {
        versionDao.updateVersionAiMeta(versionId, provider, model, durationMs)
    }

    override suspend fun updateVersionAiHashtags(versionId: Long, hashtags: String?) {
        versionDao.updateVersionAiHashtags(versionId, hashtags)
    }
    
    // ============ VERSION CLEANUP ============
    
    @Transaction
    override suspend fun cleanupOldVersions(noteId: Long, maxVersions: Int) {
        // Optimization: removed withContext - use Room batch operation
        // Room batch operations already run on correct executor
        val versions = versionDao.getVersionsForNoteList(noteId)
        if (versions.size > maxVersions) {
            val versionsToDelete = versions.drop(maxVersions)
            val versionIdsToDelete = versionsToDelete.map { it.id }
            // Batch delete - one SQL operation instead of N
            versionDao.deleteVersionsByIds(versionIdsToDelete)
        }
    }
    
    override suspend fun globalVersionCleanup(maxVersionsPerNote: Int) {
        // TODO: implement global cleanup by iterating notes if needed; keep existing behavior unchanged.
    }
    
    /**
     * Batch operation: mass version creation
     * 
     * FULL IMPLEMENTATION FROM LEGACY REPOSITORY - preserves 100% functionality.
     * Returns real IDs of created versions.
     * 
     * @param versions List of pairs (noteId, content)
     * @param changeDescription Description for all versions
     * @return List of created version IDs
     */
    @Transaction
    override suspend fun createVersionsInBatch(versions: List<Pair<Long, String>>, changeDescription: String?): List<Long> {
        if (versions.isEmpty()) return emptyList()
        
        val versionEntities = versions.map { (noteId, content) ->
            createNoteVersion(
                noteId = noteId,
                content = content,
                changeDescription = changeDescription,
                isForcedSave = false  // Batch versions not considered forced
            ).toEntity()
        }
        
        return versionDao.insertVersionsBatch(versionEntities)
    }
}