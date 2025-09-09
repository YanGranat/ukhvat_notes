package com.ukhvat.notes.domain.datasource

import com.ukhvat.notes.domain.model.NoteVersion
import kotlinx.coroutines.flow.Flow

/**
 * DataSource for note versioning
 * 
 * Manages note version history with intelligent cleanup.
 * Extracted from NotesRepositoryImpl for better architecture.
 * 
 * Responsibilities:
 * - Creating versions when notes change
 * - Getting version history for note
 * - Intelligent cleanup of old versions
 * - Operations with specific versions (view, restore, delete)
 * 
 * Versioning logic must remain IDENTICAL to existing.
 */
interface VersionDataSource {
    
    // ============ VERSION CREATION ============
    
    /**
     * Create note version
     * Called on significant note content changes
     * 
     * @param noteId Note ID for which version is created
     * @param content Version content  
     * @param customName User-defined version name (optional)
     * @param isForcedSave Forced save flag (protects from auto-cleanup)
     */
    suspend fun createVersion(noteId: Long, content: String, customName: String? = null, isForcedSave: Boolean = false, diffOpsJson: String? = null)
    
    // ============ VERSION RETRIEVAL ============
    
    /**
     * Get all versions for note
     * @param noteId Note ID
     * @return Flow<List<NoteVersion>> Reactive version list, sorted by creation date (newest first)
     */
    fun getVersionsForNote(noteId: Long): Flow<List<NoteVersion>>
    
    /**
     * Get all versions for note as List (for export and batch operations)
     * @param noteId Note ID
     * @return List<NoteVersion> Version list, sorted by creation date (newest first)
     */
    suspend fun getVersionsForNoteList(noteId: Long): List<NoteVersion>
    
    /**
     * Get specific version by ID
     * @param versionId Version ID
     * @return NoteVersion? Version or null if not found
     */
    suspend fun getVersionById(versionId: Long): NoteVersion?
    
    /**
     * Get last version for note
     * Used in shouldCreateVersion for time interval analysis
     * @param noteId Note ID
     * @return NoteVersion? Last version or null if no versions exist
     */
    suspend fun getLatestVersionForNote(noteId: Long): NoteVersion?
    
    // ============ VERSION OPERATIONS ============
    
    /**
     * Delete version
     * @param versionId Version ID for deletion
     * @return Boolean true if version was deleted
     */
    suspend fun deleteVersion(versionId: Long): Boolean
    
    /**
     * Update user-defined version name
     * @param versionId Version ID
     * @param customName New name
     */
    suspend fun updateVersionName(versionId: Long, customName: String)
    /**
     * Update AI meta fields for a version (provider, model, durationMs).
     * Does not change user-defined custom name.
     */
    suspend fun updateVersionAiMeta(versionId: Long, provider: String?, model: String?, durationMs: Long?)

    /** Attach AI-generated hashtags to the latest version (optional). */
    suspend fun updateVersionAiHashtags(versionId: Long, hashtags: String?)
    
    // ============ VERSION CLEANUP ============
    
    /**
     * Intelligent cleanup of old versions for note
     * Keeps last N versions, deletes older ones
     * 
     * @param noteId Note ID for cleanup
     * @param maxVersions Maximum number of versions to keep (default 10)
     */
    suspend fun cleanupOldVersions(noteId: Long, maxVersions: Int = 10)
    
    /**
     * Global version cleanup for all notes  
     * Called periodically to maintain performance
     * 
     * @param maxVersionsPerNote Maximum number of versions per note
     */
    suspend fun globalVersionCleanup(maxVersionsPerNote: Int = 10)
    
    /**
     * Batch operation: mass version creation
     * 
     * Used when importing notes with rich version history.
     * Critical for performance - creating versions for 100 notes in ~200ms.
     * 
     * @param versions List of (noteId, content) pairs for version creation
     * @param changeDescription Single description for all created versions
     * @return List of created version IDs
     */
    suspend fun createVersionsInBatch(versions: List<Pair<Long, String>>, changeDescription: String?): List<Long>
}