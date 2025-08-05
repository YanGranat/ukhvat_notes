package com.ukhvat.notes.domain.datasource

import com.ukhvat.notes.domain.model.Note
import kotlinx.coroutines.flow.Flow

/**
 * Main DataSource for notes
 * 
 * Interface for basic CRUD operations with notes.
 * Extracted from NotesRepositoryImpl to improve architecture and testability.
 * 
 * Responsibilities:
 * - Creating, reading, updating, deleting notes
 * - Getting note list (optimized for UI)
 * - Batch operations for mass actions
 * 
  * Does NOT contain:
 * - Search logic (SearchDataSource)
 * - Versioning (VersionDataSource)
 * - Trash (TrashDataSource)
 * - Theme settings (PreferencesDataSource)
 */
interface NoteDataSource {
    
    // ============ MAIN CRUD OPERATIONS ============
    
    /**
     * Create new note
     * @param note Note to create (id will be ignored)
     * @return Long ID of created note
     */
    suspend fun insertNote(note: Note): Long
    
    /**
     * Get note by ID with full content
     * @param id Note ID
     * @return Note? Note with full content or null if not found
     */
    suspend fun getNoteById(id: Long): Note?
    
    /**
     * Update note (metadata + content)
     * @param note Note with updated data
     */
    suspend fun updateNote(note: Note)
    
    /**
     * Delete note (soft delete to trash)
     * @param note Note to delete
     */
    suspend fun deleteNote(note: Note)
    
    /**
          * Delete note by ID (soft delete to trash)
     * @param id ID of note to delete
     */
    suspend fun deleteNoteById(id: Long)
    
    // ============ NOTE LISTS ============
    
    /**
     * Get all active notes (NOT in trash)
     * OPTIMIZED: returns only metadata for fast list display
     * @return Flow<List<Note>> Reactive note list without content
     */
    fun getAllNotes(): Flow<List<Note>>
    
    /**
     * Get all notes synchronously (for batch operations)
     * @return List<Note> List of all notes without content
     */
    fun getAllNotesSync(): List<Note>
    
    // ============ BATCH OPERATIONS ============
    
    /**
     * Create multiple notes at once (for import)
     * @param notes List of notes to create
     * @return List<Long> List of created note IDs
     */
    suspend fun insertNotes(notes: List<Note>): List<Long>
    
    /**
     * Get notes with full content by ID list
     * OPTIMIZED: batch query instead of multiple getNoteById
     * @param ids List of note IDs
     * @return List<Note> Notes with full content
     */
    suspend fun getNotesWithContentByIds(ids: List<Long>): List<Note>
    
    /**
     * Update multiple notes at once (batch update)
     * @param notes List of notes to update
     */
    suspend fun updateNotes(notes: List<Note>)
    
    /**
     * Delete multiple notes by ID (batch soft delete)
     * @param ids List of IDs to delete
     */
    suspend fun deleteNotesByIds(ids: List<Long>)
    
    /**
     * Increase maximum version limit for note
     * Used for forced version saving (Ctrl+S)
     * @param noteId ID of note for which limit is increased
     */
    suspend fun increaseMaxVersions(noteId: Long)
}