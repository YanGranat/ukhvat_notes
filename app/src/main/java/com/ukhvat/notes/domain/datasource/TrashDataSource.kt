package com.ukhvat.notes.domain.datasource

import com.ukhvat.notes.domain.model.Note
import kotlinx.coroutines.flow.Flow

/**
 * DataSource for trash management
 * 
 * Manages deleted notes with restore capability.
 * Provides soft delete logic and automatic cleanup.
 * 
  * Responsibilities:
 * - Moving notes to trash (soft delete)
 * - Restoring notes from trash
 * - Permanent note deletion
 * - Automatic cleanup of old deleted notes
 * - Getting list of notes in trash
 */
interface TrashDataSource {
    
    // ============ MAIN TRASH OPERATIONS ============
    
    /**
     * Move note to trash (soft delete)
     * @param noteId Note ID for moving to trash
     */
    suspend fun moveToTrash(noteId: Long)
    
    /**
     * Move multiple notes to trash (batch operation)
     * @param noteIds List of note IDs for moving to trash
     */
    suspend fun moveMultipleToTrash(noteIds: List<Long>)
    
    /**
     * Restore note from trash
     * @param noteId Note ID for restoration
     */
    suspend fun restoreFromTrash(noteId: Long)
    
    /**
     * Restore multiple notes from trash (batch operation)
     * @param noteIds List of note IDs for restoration
     */
    suspend fun restoreMultipleFromTrash(noteIds: List<Long>)
    
    /**
     * Permanently delete note from trash (hard delete)
     * Deletes note, all its versions and content from DB
     * @param noteId Note ID for permanent deletion
     */
    suspend fun permanentDelete(noteId: Long)
    
    /**
     * Permanently delete multiple notes (batch hard delete)
     * @param noteIds List of note IDs for permanent deletion
     */
    suspend fun permanentDeleteMultiple(noteIds: List<Long>)
    
    // ============ GETTING NOTES IN TRASH ============
    
    /**
     * Get all notes in trash
     * @return Flow<List<Note>> Reactive list of deleted notes without content
     */
    fun getTrashedNotes(): Flow<List<Note>>
    
    /**
     * Get number of notes in trash
     * @return Int Number of notes in trash
     */
    suspend fun getTrashCount(): Int
    
    // ============ AUTOMATIC CLEANUP ============
    
    /**
     * Automatic trash cleanup
     * Permanently deletes notes older than specified number of days
     * 
     * @param daysOld Number of days (default 30)
     */
    suspend fun autoCleanupTrash(daysOld: Int = 30)
    
    /**
     * Clear entire trash (permanently delete all notes)
     */
    suspend fun emptyTrash()
}