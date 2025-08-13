package com.ukhvat.notes.ui.screens

import android.content.Context
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ukhvat.notes.R
import com.ukhvat.notes.domain.model.Note
import com.ukhvat.notes.domain.repository.NotesRepository
import com.ukhvat.notes.domain.util.Toaster

import com.ukhvat.notes.ui.theme.Animations.DEBOUNCE_DELAY_MS
import com.ukhvat.notes.domain.util.VersioningConstants.VERSION_CHECK_INTERVAL_MS
import com.ukhvat.notes.ui.theme.UiConstants
import androidx.compose.ui.text.input.TextFieldValue
import com.ukhvat.notes.ui.theme.LoadingState
import androidx.compose.ui.text.TextRange
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.abs
// Removed Hilt imports for Koin migration

/**
 * MIGRATED FROM HILT TO KOIN
 * 
 * Dependencies injected via Koin instead of @Inject constructor:
 * - NotesRepository: for note operations and search
 * - Toaster: for user notifications
 * 
 * CRITICAL: This ViewModel contains complex search logic that must be preserved exactly
 */
class NoteEditViewModel(
    private val repository: NotesRepository,
    private val toaster: Toaster,
    private val context: Context,
    private val aiDataSource: com.ukhvat.notes.domain.datasource.AiDataSource
) : ViewModel() {

    private val _uiState = MutableStateFlow(NoteEditUiState())
    val uiState: StateFlow<NoteEditUiState> = _uiState.asStateFlow()

        private var autoSaveJob: Job? = null         // ONLY for autosave
    private var searchJob: Job? = null           // ONLY for search
    private var versionCheckJob: Job? = null     // For periodic versioning check
    private var currentNoteId: Long = 0L
    private var isSearchFromNavigation = false   // Flag to protect search from auto-reset
    private var wasSearchClearedByUser = false   // Flag to track user search reset
    
    // Prevent repeated search application
    /**
          * Flag to prevent repeated application of search parameters.
     * 
     * Architectural feature: when returning from version history, LaunchedEffect(noteId)
     * doesn't trigger (noteId unchanged), but ViewModel preserves state,
     * including active search. Flag ensures one-time application
     * of search parameters for each note.
     */
    private var searchParamsProcessedForNoteId: Long = -1L

    /**
     * Note loading with search context support
     * 
     * Universal method for loading notes in various scenarios:
     * 1. Regular editing (without search parameters)
     * 2. Navigation from note list search (with search parameters)
     * 3. Creating new note (noteId = NEW_NOTE_ID)
     * 
     * Handling repeated search application:
     * - Search parameters are applied only ONCE for each note
     * - When returning from version history, search is NOT activated again
     * - searchParamsProcessedForNoteId tracks processed notes
     * - wasSearchClearedByUser prevents re-activation after reset
     * - Flags are automatically reset when switching to other notes
     * 
     * Technical search architecture:
     * - isWaitingForSearchPositioning flag prevents screen jumps
     * - initializeSearchFromNavigationSync() executes synchronously without delays
     * - UI is shown only after search positioning completion
     * 
     * Autofocus logic:
     * - All notes: autofocus on input field when opening
     * - Existing notes: standard autofocus handling
     * 
     * @param noteId Note ID
     * @param searchQuery Search query from navigation (optional)
     * @param searchPosition Position of found text (optional)
     */
    fun loadNote(noteId: Long, searchQuery: String? = null, searchPosition: Int? = null) {
        // Determine note type for UI handling

        
        // Reset flags when switching to another note  
        if (currentNoteId != noteId) {
            wasSearchClearedByUser = false  // Reset search reset flag when changing note
            searchParamsProcessedForNoteId = -1L  // Reset processed parameters flag
        }
        
        currentNoteId = noteId
        
        // Search parameters are applied only once for each note
        // And NOT applied if user already reset search for CURRENT note
        val hasSearchParams = !searchQuery.isNullOrBlank() && searchPosition != null && 
                             searchParamsProcessedForNoteId != noteId &&
                             !wasSearchClearedByUser  // Don't activate search if user reset it
        
        // Mark that search parameters will be processed for this note
        if (hasSearchParams) {
            searchParamsProcessedForNoteId = noteId
        }
        
        viewModelScope.launch {
            try {
                // Load note from DB (always real note with ID)
                repository.getNoteById(noteId)?.let { note ->
                        // UX optimization: cursor at end for convenient note editing
                        val initialContent = if (hasSearchParams) {
                            // Search navigation - position will be set by search logic
                            TextFieldValue(note.content) 
                        } else {
                            // Regular editing - cursor to end for convenient writing
                            TextFieldValue(note.content, TextRange(note.content.length))
                        }
                        
                        if (hasSearchParams) {
                            // UI display logic: first position search, then show content
                            _uiState.value = _uiState.value.copy(
                                content = initialContent,
                                isFavorite = note.isFavorite,
                                loadingState = LoadingState.WaitingForSearchPositioning // First wait for positioning
                            )
                            
                            // Synchronous search initialization from navigation
                            initializeSearchFromNavigationSync(searchQuery!!, searchPosition!!)
                        } else {
                            // Regular loading without search context
                            _uiState.value = _uiState.value.copy(
                                content = initialContent,
                                isFavorite = note.isFavorite,
                                loadingState = LoadingState.Idle
                            )
                        }
                            } ?: run {
            // Note not found: show error
                        _uiState.value = _uiState.value.copy(
                            error = context.getString(R.string.note_not_found_error),
                            loadingState = LoadingState.Idle
                        )
                    }
                    } catch (e: Exception) {
            // Handle loading errors
                _uiState.value = _uiState.value.copy(
                    error = context.getString(R.string.note_load_error, e.localizedMessage ?: ""),
                    loadingState = LoadingState.Idle
                )
            }
        }
        
        // Start versioning timer for all notes (new and existing)
        startVersionCheckTimer()
    }

    fun onEvent(event: NoteEditEvent) {
        when (event) {
            is NoteEditEvent.ContentChanged -> updateContent(event.content)
            is NoteEditEvent.SaveNote -> saveNote()
            is NoteEditEvent.ForceSave -> forceSave()
            is NoteEditEvent.DeleteNote -> deleteNote()
            is NoteEditEvent.ExportNote -> exportNote()
            is NoteEditEvent.ToggleFavorite -> toggleFavorite()
            is NoteEditEvent.MoveToArchive -> moveToArchive()
            is NoteEditEvent.ShowNoteInfo -> showNoteInfo()
            is NoteEditEvent.ShowVersionHistory -> showVersionHistory()
            is NoteEditEvent.StartSearch -> startSearch()
            is NoteEditEvent.ClearSearch -> clearSearch()
            is NoteEditEvent.SearchQueryChanged -> updateSearchQuery(event.query)
            is NoteEditEvent.NextSearchMatch -> navigateToNextMatch()
            is NoteEditEvent.PreviousSearchMatch -> navigateToPreviousMatch()
            is NoteEditEvent.NavigateBack -> handleNavigateBack()
            is NoteEditEvent.AiFixErrors -> aiFixErrors()
        }
    }



    // Content change handling with cursor preservation
    private fun updateContent(content: String) {

        
        // Search protection: mark that user started editing
        if (_uiState.value.isSearchMode && isSearchFromNavigation) {
            wasSearchClearedByUser = true
        }
        
        // Auto-clear search when user edits
        // BUT NOT reset if search came from navigation and wasn't processed yet
        if (_uiState.value.isSearchMode && !isSearchFromNavigation) {
            clearSearch()
        }
        
        // Reset navigation flag on first edit
        if (isSearchFromNavigation) {
            isSearchFromNavigation = false
        }
        
        // Don't try to save cursor position in ViewModel
        // TextController manages cursor position based on real state
        val validContent = TextFieldValue(
            text = content,
            selection = TextRange.Zero  // TextController will determine correct position
        )
        
        _uiState.value = _uiState.value.copy(
            content = validContent,
            hasUnsavedChanges = true,
            
        )
        
        /**
         * Adaptive autosave with note creation speed optimization
         * 
         * Architectural feature: fixed 500ms debounce slowed down appearance
         * of new notes in main menu. User pressed "+", started typing,
         * but note appeared in list only 500ms after stopping typing.
         * 
         * Approach: adaptive debounce logic
         * - New notes (NEW_NOTE_ID): 0ms - instant save and appearance in list
         * - Existing notes: 500ms - protection from frequent DB operations when editing
         * 
         * Technical justification:
         * - getAllNotes() Flow instantly updates main menu on INSERT operation
         * - New notes are created rarely, protection from frequent operations not critical
         * - Existing notes are edited intensively, debounce prevents
         *   dozens of UPDATE operations during continuous typing
         * 
         * Titles are handled in handleNavigateBack() - forced save before exit
         */
        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch {
                            delay(DEBOUNCE_DELAY_MS)  // Standard 500ms debounce for all notes
            saveNote()
        }
    }

    /**
     * Automatic note saving WITHOUT versioning
     * 
     * New autosave architecture:
     * - Called with 500ms debounce delay after each text change
     * - Saves content WITHOUT creating versions
              * - Versions are created separately: once per minute + on note exit
         * 
         * Save process:
         * 1. Check for empty content (ignored)
         * 2. Create new note (first save) with mandatory version
         * 3. Update existing note WITHOUT versioning
         * 4. Update metadata (title, updatedAt, characterCount)
     * 
              * Error handling:
         * - All exceptions are caught and displayed in UI
         * - Saving doesn't block interface (asynchronous execution)
     */
    private fun saveNote() {
        val currentUiState = _uiState.value
        val content = currentUiState.content.text
        val noteId = currentNoteId
        
        // Filter empty content
        if (content.isBlank()) {
            return
        }
        
        viewModelScope.launch {
            try {
                // Simple note update (no more NEW_NOTE_ID handling)
                val updatedNote = Note(
                    id = noteId,
                    content = content,
                    updatedAt = System.currentTimeMillis(),
                    createdAt = 0L, // Not used during update
                    isFavorite = currentUiState.isFavorite
                )
                
                // Simple save WITHOUT versioning (versions created separately)
                repository.updateNote(updatedNote)
                
                        // Reset hasUnsavedChanges flag after successful save
        // This prevents duplicate save in handleNavigateBack() on fast input and exit
                _uiState.value = _uiState.value.copy(
                    hasUnsavedChanges = false
                )
            } catch (e: Exception) {
                // Pass error to UI for user display
                _uiState.value = _uiState.value.copy(
                    error = context.resources.getString(R.string.save_error, e.localizedMessage ?: "")
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    /**
     * Start periodic versioning check once per minute
     * 
     * New versioning system:
     * - Checks version creation necessity every minute
     * - Creates version only for changes >140 characters
     * - Works independently from autosave
     */
    private fun startVersionCheckTimer() {
        versionCheckJob?.cancel()
        versionCheckJob = viewModelScope.launch {
            while (true) {
                delay(VERSION_CHECK_INTERVAL_MS) // 1 minute
                checkAndCreateVersionOnTimer()
            }
        }
    }

    /**
     * Check and create version by timer (once per minute)
     * For existing notes: creates version for >140 character changes
     */
    private suspend fun checkAndCreateVersionOnTimer() {
        val currentUiState = _uiState.value
        val content = currentUiState.content.text
        val noteId = currentNoteId
        
        // Check only for notes with content
        if (content.isBlank()) {
            return
        }
        
        try {
                    // FIX RACE CONDITION: Use shouldCreateVersion for check
        // instead of separate hasVersions + shouldCreateVersion calls
            val needsVersion = repository.shouldCreateVersion(noteId, content)
            
            if (needsVersion) {
                // Check if versions exist to choose correct description
                val hasVersions = repository.getVersionsForNoteList(noteId).isNotEmpty()
                
                if (!hasVersions && content.length > 140) {
                    // New note: create version only if >140 characters
                    repository.createVersion(noteId, content, context.resources.getString(R.string.version_creation))
                } else if (hasVersions) {
                    // Existing note: create version with autosave
                    repository.createVersion(noteId, content, context.resources.getString(R.string.version_autosave))
                }
            }
        } catch (e: Exception) {
            // Versioning errors should not affect user experience
        }
    }

    /**
     * Check and create version on note exit
     * 
     * FIXED: according to user requirements
     * - For new notes: version created ONLY for >140 characters OR on note exit (any amount)
     * - For existing notes: version created for >140 character changes
     */
    private suspend fun checkAndCreateVersionOnExit() {
        val currentUiState = _uiState.value
        val content = currentUiState.content.text
        val noteId = currentNoteId
        
        // Check only for notes with content
        if (content.isBlank()) {
            return
        }
        
        try {
            // Check if note has versions (distinguish new from existing)
            val hasVersions = repository.getVersionsForNoteList(noteId).isNotEmpty()
            
            if (!hasVersions) {
                // New note: create version on exit only if >=3 characters
                if (content.length >= 3) {
                    repository.createVersion(noteId, content, context.resources.getString(R.string.version_creation))
                }
            } else {
                // Existing note: check changes >140 characters
                val needsVersion = repository.shouldCreateVersion(noteId, content)
                if (needsVersion) {
                    repository.createVersion(noteId, content, context.resources.getString(R.string.version_autosave))
                }
            }
        } catch (e: Exception) {
            // Versioning errors should not affect user experience
        }
    }

    private fun showVersionHistory() {
        viewModelScope.launch {
            // Check if note has text
            val contentToSave = _uiState.value.content.text.trim()
            if (contentToSave.isEmpty()) {
                _uiState.value = _uiState.value.copy(
                    error = context.getString(R.string.add_text_first)
                )
                return@launch
            }
            
            // Navigate to version history with real ID
            _uiState.value = _uiState.value.copy(
                navigateToVersionHistory = true,
                versionHistoryNoteId = currentNoteId
            )
        }
    }

    fun onVersionHistoryNavigated() {
        _uiState.value = _uiState.value.copy(
            navigateToVersionHistory = false,
            versionHistoryNoteId = 0L
        )
    }



    /**
     * Forced note version save (Ctrl+S)
     * 
     * Forced save features:
     * - Creates version regardless of shouldCreateVersion conditions
     * - Increases maximum version limit for note by 1
     * - Marked with isForcedSave flag for auto-cleanup protection
     * - Shows success notification to user
     * 
     * State handling:
     * - New notes: await creation with infinite attempt protection
     * - Existing notes: immediate version creation
     * - Errors: display in UI with detailed description
     * 
     * @param retryCount Attempt counter for new notes (loop protection)
     */
    private fun forceSave(retryCount: Int = 0) {
        // Note always has real ID (created immediately on navigation)
        
        viewModelScope.launch {
            try {
                val contentToSave = _uiState.value.content.text.trim()
                
                // Unconditional version creation with limit increase
                repository.createVersionForced(
                    noteId = currentNoteId,
                    content = contentToSave,
                    changeDescription = context.resources.getString(R.string.version_force_save)
                )
                
                // Optimization: create Note object without extra DB query
                val updatedNote = Note(
                    id = currentNoteId,
                    content = contentToSave,
                    updatedAt = System.currentTimeMillis(),
                    createdAt = 0L, // Not used during update
                    isFavorite = _uiState.value.isFavorite
                )
                repository.updateNote(updatedNote)
                
                // User success notification
                toaster.toast(R.string.note_saved)
                _uiState.value = _uiState.value.copy(
                    hasUnsavedChanges = false,
                    error = null
                )
            } catch (e: Exception) {
                // Catch and display save errors
                _uiState.value = _uiState.value.copy(
                    error = context.resources.getString(R.string.save_error, e.localizedMessage ?: "")
                )
            }
        }
    }

    private fun startSearch() {
        _uiState.value = _uiState.value.copy(
            isSearchMode = true,
            searchQuery = "",
            searchMatches = emptyList(),
            currentMatchIndex = -1
        )
    }

    private fun clearSearch() {
        _uiState.value = _uiState.value.copy(
            isSearchMode = false,
            searchQuery = "",
            searchMatches = emptyList(),
            currentMatchIndex = -1,
            showSearchNavigation = false  // Reset search navigation display
        )
                    isSearchFromNavigation = false // Reset flag
                    wasSearchClearedByUser = true // Set search reset flag
        
        // ADDITIONAL PROTECTION: reset processed search parameters flag
        // so search can work normally when switching to other notes
        searchParamsProcessedForNoteId = -1L
        
        // wasSearchClearedByUser flag logic:
        // In deleteNote() and handleNavigateBack() if wasSearchClearedByUser = true,
        // then shouldClearMainSearch = true → search is cleared in main menu
        // This ensures correct navigation: search → note → clear search → delete → main screen
    }

    private fun updateSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        
        // Use separate searchJob, not autoSaveJob
        searchJob?.cancel()
        
        if (query.isBlank()) {
            _uiState.value = _uiState.value.copy(
                searchMatches = emptyList(),
                currentMatchIndex = -1
            )
            return
        }
        
                    // Add debounce for performance optimization
        searchJob = viewModelScope.launch {
            delay(50) // Maximum fast debounce for instant search response
            
            val content = _uiState.value.content.text
            val matches = findMatches(content, query)
            
            _uiState.value = _uiState.value.copy(
                searchMatches = matches,
                currentMatchIndex = if (matches.isNotEmpty()) 0 else -1
            )
        }
    }

    private fun findMatches(content: String, query: String): List<SearchMatch> {
        if (query.isBlank() || content.isBlank()) return emptyList()
        
        val matches = mutableListOf<SearchMatch>()
        var globalIndex = 0
        var currentLine = 0
        var lastNewlineIndex = -1
        
        // Optimization: count line numbers in single pass instead of expensive substring().count
        while (globalIndex < content.length) {
            val matchIndex = content.indexOf(query, globalIndex, ignoreCase = true)
            if (matchIndex == -1) break
            
            // Determine line number efficiently - count \n between last position and current
            while (lastNewlineIndex < matchIndex) {
                val nextNewline = content.indexOf('\n', lastNewlineIndex + 1)
                if (nextNewline == -1 || nextNewline >= matchIndex) break
                lastNewlineIndex = nextNewline
                currentLine++
            }
            
            matches.add(
                SearchMatch(
                    startIndex = matchIndex,
                    endIndex = matchIndex + query.length,
                    line = currentLine
                )
            )
            
            globalIndex = matchIndex + 1
        }
        
        return matches
    }
    
            // Add navigation between search results
    fun navigateToNextMatch() {
        val matches = _uiState.value.searchMatches
        if (matches.isEmpty()) return
        
        val currentIndex = _uiState.value.currentMatchIndex
        val nextIndex = if (currentIndex < matches.size - 1) currentIndex + 1 else 0
        
        _uiState.value = _uiState.value.copy(currentMatchIndex = nextIndex)
    }
    
    fun navigateToPreviousMatch() {
        val matches = _uiState.value.searchMatches
        if (matches.isEmpty()) return
        
        val currentIndex = _uiState.value.currentMatchIndex
        val prevIndex = if (currentIndex > 0) currentIndex - 1 else matches.size - 1
        
        _uiState.value = _uiState.value.copy(currentMatchIndex = prevIndex)
    }
    
    /**
     * Synchronous search initialization on navigation WITHOUT DELAYS
     * 
     * Screen jump handling:
     * 
     * Synchronous search initialization from navigation prevents
     * visual artifacts when transitioning from list search to in-note search.
     * ═══════════════════════════════════════════════════════════════
     * 
     * Issue: Search transition from notes list to in-note search caused jumps:
     * 1. Note was shown from top
          * 2. Then "jumped" to search result
     * 3. Another shift from keyboard appearance
     * 
     * Approach: Synchronous positioning without showing intermediate states
     * • isWaitingForSearchPositioning hides content until positioning complete
     * • Atomic update of all search states IMMEDIATELY
     * • User sees note ALREADY in correct position
     * 
     * RESULT: Perfectly smooth transition without visual artifacts
     * 
     * @param searchQuery Search query for highlighting
     * @param searchPosition Position in text for navigation
     */
    private fun initializeSearchFromNavigationSync(searchQuery: String, searchPosition: Int) {
        val content = _uiState.value.content.text
        
        // Synchronous logic without delays to prevent screen jumps
        if (content.isNotEmpty()) {
            val matches = findMatches(content, searchQuery)
            val targetMatchIndex = if (matches.isNotEmpty()) findClosestMatchIndex(matches, searchPosition) else 0
            
            // Atomic update: set all search states simultaneously
            _uiState.value = _uiState.value.copy(
                searchQuery = searchQuery,
                isSearchMode = true,
                showSearchNavigation = true,
                searchMatches = matches,
                currentMatchIndex = targetMatchIndex,
                loadingState = LoadingState.Idle // Show UI, positioning completed
            )
        } else {
            // If content is empty - show without search
            _uiState.value = _uiState.value.copy(
                loadingState = LoadingState.Idle
            )
        }
    }
    
    /**
     * Initialize search on navigation from main menu
     * DEPRECATED VERSION with delays - kept for compatibility
     */
    fun initializeSearchFromNavigation(searchQuery: String, searchPosition: Int) {
        viewModelScope.launch {
                                // Optimization: reduced delay since cursor already positioned correctly
            delay(50)
            
            val content = _uiState.value.content.text
            
            // Simplified content check: if empty content, likely loading error
            if (content.isEmpty()) {
                delay(25)  // Short retry attempt
                val retryContent = _uiState.value.content.text  
                if (retryContent.isEmpty()) {
                    // If content is still empty, don't initialize search
                    return@launch
                }
            }
            
            val finalContent = _uiState.value.content.text
            val matches = if (finalContent.isNotEmpty()) findMatches(finalContent, searchQuery) else emptyList()
            val targetMatchIndex = if (matches.isNotEmpty()) findClosestMatchIndex(matches, searchPosition) else 0
            
            // Single combined UI state update for speed
            _uiState.value = _uiState.value.copy(
                searchQuery = searchQuery,
                isSearchMode = true,
                showSearchNavigation = true,
                searchMatches = matches,
                currentMatchIndex = targetMatchIndex
            )
        }
    }
    
    /**
     * Find match index closest to specified position
     */
    private fun findClosestMatchIndex(matches: List<SearchMatch>, targetPosition: Int): Int {
        var closestIndex = 0
        var minDistance = Int.MAX_VALUE
        
        matches.forEachIndexed { index, match ->
            val distance = abs(match.startIndex - targetPosition)
            if (distance < minDistance) {
                minDistance = distance
                closestIndex = index
            }
        }
        
        return closestIndex
    }

    /**
     * Note deletion with correct navigation
     * 
          * UX NAVIGATION LOGIC:
     * If user accessed note through main menu search, but then cleared
     * search IN NOTE and deleted note - they should go to main screen
     * (main menu search should be cleared), not back to search results.
     * 
     * TECHNICAL IMPLEMENTATION:
     * shouldClearMainSearch = wasSearchClearedByUser ensures search reset
     * in main menu if user cleared search in note.
     */
    private fun deleteNote() {

        

        
        viewModelScope.launch {
            try {
                repository.deleteNoteById(currentNoteId)
                toaster.toast(R.string.note_deleted)
                _uiState.value = _uiState.value.copy(
                    shouldNavigateBack = true,
                    shouldClearMainSearch = wasSearchClearedByUser // Clear main search if user cleared search in note
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = context.getString(R.string.deletion_error, e.message ?: "")
                )
            }
        }
    }

    fun onNavigationHandled() {
        _uiState.value = _uiState.value.copy(
            shouldNavigateBack = false, 
            shouldClearMainSearch = false,
            shouldScrollToTop = false
        )
    }

    private fun exportNote() {
        
        viewModelScope.launch {
            try {
                val currentNote = repository.getNoteById(currentNoteId)
                if (currentNote != null) {
                    _uiState.value = _uiState.value.copy(
                        exportContent = currentNote.content
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                                            error = context.getString(R.string.export_error, e.message ?: "")
                )
            }
        }
    }

    fun clearExport() {
        _uiState.value = _uiState.value.copy(exportContent = null)
    }

    /**
     * AI: Fix errors in current note content
     */
    private fun aiFixErrors() {
        val currentContent = _uiState.value.content.text
        if (currentContent.isBlank()) {
            _uiState.value = _uiState.value.copy(
                error = context.getString(R.string.add_text_first)
            )
            return
        }
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isAiBusy = true)
                val startedAt = System.currentTimeMillis()
                // Create version BEFORE AI correction (non-blocking for UX)
                try {
                    repository.createVersion(
                        noteId = currentNoteId,
                        content = currentContent,
                        changeDescription = context.resources.getString(R.string.version_ai_before_fix)
                    )
                } catch (_: Exception) {
                    // Versioning errors should not affect user experience
                }
                val aiResult = aiDataSource.correctText(currentContent)
                val corrected = aiResult.text
                // Replace note text and mark as changed; cursor to end
                _uiState.value = _uiState.value.copy(
                    content = TextFieldValue(corrected, TextRange(corrected.length)),
                    hasUnsavedChanges = true,
                    isAiBusy = false
                )
                // Create version AFTER AI correction (non-blocking for UX)
                try {
                    repository.createVersion(
                        noteId = currentNoteId,
                        content = corrected,
                        changeDescription = context.resources.getString(R.string.version_ai_after_fix)
                    )
                    // Attach AI metadata to the latest version via dedicated fields (not customName)
                    try {
                        val latest = repository.getVersionsForNoteList(currentNoteId).firstOrNull()
                        latest?.let { 
                            repository.updateVersionAiMeta(
                                versionId = it.id,
                                provider = aiResult.provider.name,
                                model = aiResult.model,
                                durationMs = System.currentTimeMillis() - startedAt
                            )
                        }
                    } catch (_: Exception) {
                        // Ignore meta attachment failures
                    }
                } catch (_: Exception) {
                    // Versioning errors should not affect user experience
                }
                // Show toast with elapsed time
                val elapsedMs = System.currentTimeMillis() - startedAt
                val human = formatElapsed(elapsedMs)
                if (human.isNotEmpty()) {
                    toaster.toast(R.string.ai_fixed_with_time, human)
                } else {
                    toaster.toast(R.string.ai_fixed_short)
                }
                // Store last AI operation meta for VersionInfo dialog
                lastAiMeta = AiMeta(provider = aiResult.provider, model = aiResult.model, elapsedMs = elapsedMs)
                // Trigger save on next debounce cycle
                autoSaveJob?.cancel()
                autoSaveJob = viewModelScope.launch {
                    delay(DEBOUNCE_DELAY_MS)
                    saveNote()
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = context.getString(R.string.update_error, e.localizedMessage ?: ""),
                    isAiBusy = false
                )
            }
        }
    }

    // AI meta info for latest correction
    private var lastAiMeta: AiMeta? = null
    data class AiMeta(
        val provider: com.ukhvat.notes.domain.model.AiProvider,
        val model: String,
        val elapsedMs: Long
    )

    private fun formatElapsed(ms: Long): String {
        val seconds = ms / 1000
        return when {
            seconds < 60 -> "${seconds} с."
            seconds < 3600 -> {
                val m = seconds / 60
                val s = seconds % 60
                if (s == 0L) "${m} мин." else "${m} мин. ${s} с."
            }
            else -> {
                val h = seconds / 3600
                val m = (seconds % 3600) / 60
                if (m == 0L) "${h} ч." else "${h} ч. ${m} мин."
            }
        }
    }

    private fun showNoteInfo() {

        
        viewModelScope.launch {
            try {
                val currentNote = repository.getNoteById(currentNoteId)
                if (currentNote != null) {
                    val content = currentNote.content
                    val noteInfo = NoteInfo(
                        createdAt = currentNote.createdAt,
                        updatedAt = currentNote.updatedAt,
                        characterCount = content.length,
                        wordCount = if (content.isBlank()) 0 else content.trim().split("\\s+".toRegex()).size
                    )
                    
                    _uiState.value = _uiState.value.copy(
                        showNoteInfo = true,
                        noteInfo = noteInfo
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = context.getString(R.string.info_error, e.message ?: "")
                )
            }
        }
    }

    fun clearNoteInfo() {
        _uiState.value = _uiState.value.copy(
            showNoteInfo = false,
            noteInfo = null
        )
    }

    /**
     * Toggle favorite flag for current note
     */
    private fun toggleFavorite() {
        val noteId = currentNoteId
        val newValue = !_uiState.value.isFavorite
        viewModelScope.launch {
            try {
                repository.setNoteFavorite(noteId, newValue)
                _uiState.value = _uiState.value.copy(isFavorite = newValue)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = context.getString(R.string.update_error, e.localizedMessage ?: "")
                )
            }
        }
    }

    /** Move current note to archive and navigate back */
    private fun moveToArchive() {
        val noteId = currentNoteId
        viewModelScope.launch {
            try {
                repository.moveToArchive(noteId)
                toaster.toast(R.string.archived)
                _uiState.value = _uiState.value.copy(
                    shouldNavigateBack = true,
                    shouldScrollToTop = true
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = context.getString(R.string.update_error, e.localizedMessage ?: "")
                )
            }
        }
    }



    
    /**
     * NOTE EXIT HANDLING WITH RACE CONDITION PREVENTION
     * 
     * Issue:
     * On fast exit from new note only first character was saved.
     * 
     * REASON:
     * 1. First character → autosave 0ms → created in DB
     * 2. Rest of text → autosave 500ms debounce (in progress...)
     * 3. Fast exit → if (wasOriginallyNewNote) return → not saved
     * 
     * Approach:
     * - Forced save before exit for all note types
     * - autoSaveJob?.cancel() to cancel debounce
     * - Save current _uiState.value.content.text
     * - Unified logic for all note types
     * 
     * Positioning handling:
     * Changed shouldScrollToTop logic from wasOriginallyNewNote to true.
          * Now exit from any note (new or existing) leads to scrolling
     * to list beginning, ensuring consistent UX behavior.
     */
    private fun handleNavigateBack() {
        // Unified behavior - always to list beginning
        val shouldScrollToTop = true
        
        viewModelScope.launch {
            try {
                // Handle race condition: Cancel autosave before final save
                autoSaveJob?.cancel()
                
                val currentNote = repository.getNoteById(currentNoteId)
                val currentContent = _uiState.value.content.text
                
                if (currentNote != null) {
                    if (currentContent.isBlank()) {
                                // SMART EMPTY NOTE DELETION LOGIC:
        // Check if note has versions (was it with content before)
                        val hasVersions = repository.getVersionsForNoteList(currentNoteId).isNotEmpty()
                        
                        if (hasVersions) {
                                        // Note with versions → to trash
            // Logic: previously had content, user might want to restore
                            repository.deleteNote(currentNote)
                        } else {
                                        // New note without versions → permanent deletion
            // Logic: just empty note, no point cluttering trash
                            repository.permanentlyDeleteNote(currentNoteId)
                        }
                    } else {
                        // FORCED SAVE of note with content
                        val updatedNote = currentNote.copy(
                            content = currentContent,  // Save current content
                            updatedAt = System.currentTimeMillis(),
                            isFavorite = _uiState.value.isFavorite
                            // Title automatically recalculated in toMetadataEntityForSave()
                        )
                        repository.updateNote(updatedNote)
                        
                        // Check need for version creation on note exit
                        checkAndCreateVersionOnExit()
                    }
                } else {
                            // RACE CONDITION: Note hasn't been created in DB yet, but user already exited
        // If content is empty - do nothing (background creation should be cancelled)
        // If there's content - create note forcefully
                    if (currentContent.isNotBlank()) {
                        val newNote = Note(
                            id = currentNoteId,
                            content = currentContent,
                            createdAt = currentNoteId,
                            updatedAt = System.currentTimeMillis()
                        )
                        repository.insertNote(newNote)
                    }
                }
                _uiState.value = _uiState.value.copy(
                    shouldNavigateBack = true, 
                    shouldClearMainSearch = wasSearchClearedByUser,
                    shouldScrollToTop = shouldScrollToTop // Scroll to list beginning on exit from any note
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = context.getString(R.string.exit_error, e.message ?: ""),
                    shouldNavigateBack = true,
                    shouldClearMainSearch = wasSearchClearedByUser,
                    shouldScrollToTop = shouldScrollToTop // Scroll to list beginning on exit from any note
                )
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        autoSaveJob?.cancel()
        searchJob?.cancel()
        versionCheckJob?.cancel()
    }
}

/**
      * REFACTORED NOTE EDIT UI STATE WITH LOADING STATE
     * 
     * Replaced boolean flag isLoading + isWaitingForSearchPositioning
     * with type-safe LoadingState sealed class
     * 
     * Result: clear transitions between loading states
 */
data class NoteEditUiState(
    val content: TextFieldValue = TextFieldValue(""),
    
    // REFACTORED LOADING STATE
    val loadingState: LoadingState = LoadingState.Loading, // Replaces isLoading + isWaitingForSearchPositioning
    
    val hasUnsavedChanges: Boolean = false,
    val error: String? = null,
    val shouldNavigateBack: Boolean = false,
    val shouldClearMainSearch: Boolean = false,
    val shouldScrollToTop: Boolean = false, // Flag to scroll to list beginning on new note exit
    val exportContent: String? = null,
    val showNoteInfo: Boolean = false,
    val noteInfo: NoteInfo? = null,
    val navigateToVersionHistory: Boolean = false,
    val versionHistoryNoteId: Long = 0L,
    val isFavorite: Boolean = false,

    val isSearchMode: Boolean = false,
    val searchQuery: String = "",
    val searchMatches: List<SearchMatch> = emptyList(),
    val currentMatchIndex: Int = -1,
    val showSearchNavigation: Boolean = false,

    // AI busy indicator for UI (e.g., greyed icon)
    val isAiBusy: Boolean = false,

) {
    /**
     * COMPUTED PROPERTIES for backward compatibility
     * 
     * Gradual transition from booleans to sealed classes
     * without simultaneous UI code changes
     */
    val isLoading: Boolean get() = loadingState is LoadingState.Loading
    val isWaitingForSearchPositioning: Boolean get() = loadingState is LoadingState.WaitingForSearchPositioning
}

data class NoteInfo(
    val createdAt: Long,
    val updatedAt: Long,
    val characterCount: Int,
    val wordCount: Int
)

data class SearchMatch(
    val startIndex: Int,
    val endIndex: Int,
    val line: Int
)



sealed class NoteEditEvent {
    data class ContentChanged(val content: String) : NoteEditEvent()
    object SaveNote : NoteEditEvent()
    object ForceSave : NoteEditEvent()
    object DeleteNote : NoteEditEvent()
    object ExportNote : NoteEditEvent()
    object ToggleFavorite : NoteEditEvent()
    object MoveToArchive : NoteEditEvent()
    object ShowNoteInfo : NoteEditEvent()
    object ShowVersionHistory : NoteEditEvent()
    object StartSearch : NoteEditEvent()
    object ClearSearch : NoteEditEvent()
    data class SearchQueryChanged(val query: String) : NoteEditEvent()
    object NextSearchMatch : NoteEditEvent()
    object PreviousSearchMatch : NoteEditEvent()
    object NavigateBack : NoteEditEvent()
    object AiFixErrors : NoteEditEvent()
} 