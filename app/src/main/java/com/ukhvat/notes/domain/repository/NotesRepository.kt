package com.ukhvat.notes.domain.repository

import com.ukhvat.notes.domain.model.Note
import com.ukhvat.notes.domain.model.NoteVersion
import com.ukhvat.notes.ui.theme.ThemePreference
import kotlinx.coroutines.flow.Flow

interface NotesRepository {
    
    // ============ Core note operations ============
    
    /**
     * Get all notes (metadata only for list display)
     * Very fast operation - content not loaded
     */
    fun getAllNotes(): Flow<List<Note>>

    /**
     * Get note with full content (for editing)
     */
    suspend fun getNoteById(id: Long): Note?

    /**
     * Get notes with full content by ID list (for export)
     */
    suspend fun getNotesWithContentByIds(ids: List<Long>): List<Note>

    /**
     * Create new note
     */
    suspend fun insertNote(note: Note): Long

    /**
     * Update note (metadata + content)
     */
    suspend fun updateNote(note: Note)

    /**
     * Delete note (SOFT DELETE - move to trash)
     */
    suspend fun deleteNote(note: Note)
    suspend fun deleteNoteById(id: Long)
    suspend fun deleteNotesByIds(ids: List<Long>)  // New method for batch deletion

    // ============ Batch operations for notes ============
    
    /**
     * Bulk note creation (async)
     * Optimized for importing large volumes of notes
     * @param notes List of notes to create
     * @return List of created note IDs
     */
    suspend fun insertNotesInBatch(notes: List<Note>): List<Long>
    
    /**
     * Bulk note updates (async)
     * Efficient for synchronizing multiple changes
     * @param notes List of notes to update
     */
    suspend fun updateNotesInBatch(notes: List<Note>)
    
    /**
     * Optimized note update with versioning necessity check
     * Combines updateNote + shouldCreateVersion into one optimized query
     * @param note Note to update
     * @param newContent New content for versioning check
     * @return true if version should be created, false otherwise
     */
    suspend fun updateNoteWithVersionCheck(note: Note, newContent: String): Boolean


    // ============ Search ============
    
    /**
     * Search notes by titles and content
     * Will be replaced with separate FTS methods in future
     */
    suspend fun searchNotes(query: String): List<Note>

    /**
     * Synchronous retrieval of all notes
     * Used for batch operations and version cleanup
     */
    fun getAllNotesSync(): List<Note>



    // ============ Theme settings ============
    
    suspend fun saveThemePreference(themePreference: ThemePreference)
    suspend fun getThemePreference(): ThemePreference

    // ============ Note version history ============
    
    fun getVersionsForNote(noteId: Long): Flow<List<NoteVersion>>
    suspend fun getVersionsForNoteList(noteId: Long): List<NoteVersion>
    /** Fast check: whether note has at least one version. */
    suspend fun hasAnyVersion(noteId: Long): Boolean
    suspend fun createVersion(noteId: Long, content: String, changeDescription: String? = null)
    suspend fun createVersionForced(noteId: Long, content: String, changeDescription: String? = null)
    suspend fun shouldCreateVersion(noteId: Long, newContent: String): Boolean
    suspend fun getVersionById(versionId: Long): NoteVersion?
    suspend fun deleteVersion(versionId: Long): Boolean
    suspend fun updateVersionCustomName(versionId: Long, customName: String?)
    suspend fun updateVersionAiMeta(versionId: Long, provider: String?, model: String?, durationMs: Long?)
    suspend fun updateVersionAiHashtags(versionId: Long, hashtags: String?)

    // ============ Batch operations for versions ============
    
    /**
     * Bulk version creation
     * Used when importing notes with version history
     * @param versions List of (noteId, content) pairs for version creation
     * @param changeDescription Change description for all versions
     * @return List of created version IDs
     */
    suspend fun createVersionsInBatch(versions: List<Pair<Long, String>>, changeDescription: String? = null): List<Long>
    
    /**
     * Bulk cleanup of old versions
     * Optimizes cleanup for multiple notes simultaneously
     * @param noteIds List of note IDs for version cleanup
     * @param olderThanDays Delete versions older than specified days
     */
    suspend fun cleanupOldVersionsBatch(noteIds: List<Long>, olderThanDays: Int = 30)

    // ============ TRASH - SOFT DELETE SYSTEM ============
    
    /**
     * Get all deleted notes (trash)
     * @return Flow with list of deleted notes, sorted by deletion date
     */
    fun getDeletedNotes(): Flow<List<Note>>
    
    /**
     * Restore note from trash
     * Moves note back to active, clears deletion flags
     * @param id Note ID for restoration
     */
    suspend fun restoreNote(id: Long)
    
    /**
     * Permanently delete note from trash
     * Complete deletion of note and all its versions from DB
     * @param id Note ID for final deletion
     */
    suspend fun permanentlyDeleteNote(id: Long)
    
    /**
     * Clear entire trash
     * Permanently deletes ALL notes from trash
     */
    suspend fun clearTrash()
    
    /**
     * Automatic trash cleanup from old notes
     * Removes notes that have been in trash longer than specified time
     * Default 30 days (2_592_000_000 ms)
     * @param daysOld Number of days after which notes are automatically deleted
     */
    suspend fun autoCleanupTrash(daysOld: Int = 30)
    
    /**
     * Get number of notes in trash
     * @return Number of deleted notes
     */
    suspend fun getTrashCount(): Int

    // ============ Favorites ============
    /**
     * Mark/unmark note as favorite
     */
    suspend fun setNoteFavorite(id: Long, isFavorite: Boolean)
    /**
     * Batch favorites update
     */
    suspend fun setNotesFavorite(ids: List<Long>, isFavorite: Boolean)

    // ============ ARCHIVE ============
    /** Получить все архивированные заметки */
    fun getArchivedNotes(): kotlinx.coroutines.flow.Flow<List<Note>>
    /** Переместить заметку в архив */
    suspend fun moveToArchive(id: Long)
    /** Переместить несколько заметок в архив */
    suspend fun moveNotesToArchive(ids: List<Long>)
    /** Восстановить из архива */
    suspend fun restoreFromArchive(id: Long)
    /** Удалить из архива (в корзину) */
    suspend fun deleteFromArchive(id: Long)

    // ============ TAGS / HASHTAGS ============
    /** Get normalized hashtags for a note (without leading #). */
    suspend fun getTags(noteId: Long): List<String>
    /** Replace hashtags for a note atomically. */
    suspend fun replaceTags(noteId: Long, tags: List<String>)

    // ============ AI Settings ============
    suspend fun getOpenAiApiKey(): String?
    suspend fun setOpenAiApiKey(key: String)
    suspend fun getGeminiApiKey(): String?
    suspend fun setGeminiApiKey(key: String)
    suspend fun getAnthropicApiKey(): String?
    suspend fun setAnthropicApiKey(key: String)

    // ============ AI Settings ============
    suspend fun getPreferredAiProvider(): com.ukhvat.notes.domain.model.AiProvider?
    suspend fun setPreferredAiProvider(provider: com.ukhvat.notes.domain.model.AiProvider)
    suspend fun getOpenAiModel(): String?
    suspend fun setOpenAiModel(model: String)
    suspend fun getGeminiModel(): String?
    suspend fun setGeminiModel(model: String)
    suspend fun getAnthropicModel(): String?
    suspend fun setAnthropicModel(model: String)

    // OpenRouter
    suspend fun getOpenRouterApiKey(): String?
    suspend fun setOpenRouterApiKey(key: String)
    suspend fun getOpenRouterModel(): String?
    suspend fun setOpenRouterModel(model: String)
} 