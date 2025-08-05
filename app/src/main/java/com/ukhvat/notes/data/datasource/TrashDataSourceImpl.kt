package com.ukhvat.notes.data.datasource

import androidx.room.Transaction
import com.ukhvat.notes.data.database.NoteMetadataDao
import com.ukhvat.notes.data.database.NoteContentDao
import com.ukhvat.notes.data.database.NoteVersionDao
import com.ukhvat.notes.data.database.toDomainWithContent
import com.ukhvat.notes.domain.datasource.TrashDataSource
import com.ukhvat.notes.domain.model.Note
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext

/**
 * DATASOURCE IMPLEMENTATION FOR TRASH MANAGEMENT
 * 
 * Threading optimizations:
 * - Simple operations: removed redundant withContext (Room already optimized)
 * - Batch operations: preserved withContext only where really needed
 * - Complex delete operations: minimal context switching
 * 
 * PERFORMANCE IMPROVEMENTS:
 * - Eliminated excessive context switching for single operations
 * - Preserved withContext only for multi-step operations
 * - Batch operations optimized for minimal overhead
 */
class TrashDataSourceImpl(
    private val metadataDao: NoteMetadataDao,
    private val contentDao: NoteContentDao,
    private val versionDao: NoteVersionDao
) : TrashDataSource {
    
    // ============ BASIC TRASH OPERATIONS ============
    
    override suspend fun moveToTrash(noteId: Long) {
        val currentTime = System.currentTimeMillis()
        metadataDao.moveToTrash(noteId, currentTime)
    }
    
    override suspend fun moveMultipleToTrash(noteIds: List<Long>) {
        if (noteIds.isEmpty()) return
        
        val currentTime = System.currentTimeMillis()
        // Batch operation Room
        metadataDao.moveMultipleToTrash(noteIds, currentTime)
    }
    
    override suspend fun restoreFromTrash(noteId: Long) {
        // Simple Room operation
        metadataDao.restoreFromTrash(noteId)
    }
    
    override suspend fun restoreMultipleFromTrash(noteIds: List<Long>) {
        if (noteIds.isEmpty()) return
        
        // Optimization: keep withContext only for multi-step operations
        withContext(Dispatchers.IO) {
            for (noteId in noteIds) {
                metadataDao.restoreFromTrash(noteId)
            }
        }
    }
    
    @Transaction
    override suspend fun permanentDelete(noteId: Long) {
        withContext(Dispatchers.IO) {
            // IDENTICAL LOGIC from NotesRepositoryImpl
            // First delete all note versions
            versionDao.deleteVersionsForNote(noteId)
            
            // Then delete content
            contentDao.deleteContentById(noteId)
            
            // Then metadata
            metadataDao.deleteMetadataById(noteId)
        }
    }
    
    @Transaction
    override suspend fun permanentDeleteMultiple(noteIds: List<Long>) {
        if (noteIds.isEmpty()) return
        
        withContext(Dispatchers.IO) {
            for (noteId in noteIds) {
                // Use same logic as for single deletion
                versionDao.deleteVersionsForNote(noteId)
                contentDao.deleteContentById(noteId)
                metadataDao.deleteMetadataById(noteId)
            }
        }
    }
    
    // ============ GETTING NOTES IN TRASH ============
    
    /**
     * Get all deleted notes with full content
     * 
     * Technical fix: loading note content for trash display.
     * Previous implementation returned empty content (content = ""), 
     * что делало заметки неразборчивыми в интерфейсе корзины.
     * 
     * Architecture solution:
     * - Batch query contentDao.getContentByIds() for optimal performance
     * - Объединение метаданных с содержимым через associateBy()
     * - Идентичная логика с LegacyNotesRepositoryImpl.getDeletedNotes()
     */
    override fun getTrashedNotes(): Flow<List<Note>> = flow {
        metadataDao.getDeletedMetadata().collect { metadataList ->
            // Получаем ID всех удаленных заметок
            val noteIds = metadataList.map { it.id }
            
            val notes = if (noteIds.isEmpty()) {
                emptyList()
            } else {
                // Batch-запрос для получения содержимого всех удаленных заметок
                val contentMap = contentDao.getContentByIdsIncludingDeleted(noteIds).associateBy { it.noteId }
                
                // Объединяем метаданные с содержимым
                metadataList.mapNotNull { metadata ->
                    val content = contentMap[metadata.id]?.content ?: ""
                    metadata.toDomainWithContent(content)
                }
            }
            
            emit(notes)
        }
    }
    
    override suspend fun getTrashCount(): Int {
        return metadataDao.getTrashCount()
    }
    
    // ============ АВТОМАТИЧЕСКАЯ ОЧИСТКА ============
    
    override suspend fun autoCleanupTrash(daysOld: Int) {
        // IDENTICAL LOGIC from NotesRepositoryImpl.autoCleanupTrash
        val cutoffTime = System.currentTimeMillis() - (daysOld * 24 * 60 * 60 * 1000L)
        metadataDao.autoCleanupTrash(cutoffTime)
    }
    
    override suspend fun emptyTrash() {
        // IDENTICAL LOGIC from NotesRepositoryImpl.clearTrash()
        metadataDao.clearTrash()
    }
}