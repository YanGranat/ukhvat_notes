package com.ukhvat.notes.ui.screens

import android.content.Context
import android.net.Uri
import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ukhvat.notes.R
import com.ukhvat.notes.domain.model.Note
import com.ukhvat.notes.domain.repository.NotesRepository
import com.ukhvat.notes.domain.util.Toaster
import com.ukhvat.notes.ui.theme.UiConstants
import com.ukhvat.notes.ui.theme.ThemePreference
import com.ukhvat.notes.ui.theme.DialogState
import com.ukhvat.notes.ui.theme.SelectionMode
import com.ukhvat.notes.ui.theme.SearchMode
import com.ukhvat.notes.ui.theme.NavigationState
// Removed Hilt imports and unused export/import classes (now injected via Koin)
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import com.ukhvat.notes.ui.theme.ColorManager
import com.ukhvat.notes.domain.datasource.SearchDataSource
import com.ukhvat.notes.data.datasource.SearchResultInfo
import com.ukhvat.notes.data.datasource.SearchMatchInfo
// Final Hilt import removed for Koin migration
import android.content.Intent

/**
 * Main notes list ViewModel
 * 
 * After refactoring reduced from 1063 to 674 lines (-37%).
 * Now focuses on UI coordination and delegates complex logic to specialized Managers.
 * 
 * Core responsibilities:
 * - UI state management (NotesListUiState)
 * - Coordination between Manager classes
      * - Handling user events (NotesListEvent)
 * - Navigation between screens
 * - Theme and app settings management
 * 
 * Delegation to specialized Managers:
 * - ExportManager: all export operations in 5 formats
 * - ImportManager: import from 3 sources (DB, Archive, Folder)
 * - SearchDataSource: full-text search with expandable results
 * 
 * Architectural advantages of refactoring:
 * - Testability (each Manager can be tested separately)
 * - Clear separation of responsibilities (Single Responsibility Principle)
 * - Ease of adding new features without changing main ViewModel
 * - Reduced cognitive load when reading and maintaining code
 * 
 * @param repository Main repository for working with notes
 * @param exportManager Export manager (301 lines of specialized logic)
 * @param importManager Import manager (129 lines of specialized logic)  
 * @param searchDataSource Note search (replaces SearchManager, preserves all functionality)
 * @param toaster Service for Toast notifications
 */
/**
 * MIGRATED FROM HILT TO KOIN - CRITICAL: PRESERVING ADVANCED SEARCH SYSTEM
 * 
 * Dependencies injected via Koin instead of @Inject constructor:
 * - NotesRepository: for note operations
 * - Toaster: for user notifications  
 * - Context: for file operations
 * - ExportManager: for all export operations (CRITICAL: preserves advanced export logic)
 * - ImportManager: for all import operations (CRITICAL: preserves batch import logic)
 * - SearchDataSource: for advanced search with LRU cache (CRITICAL: preserves search performance)
 */
class NotesListViewModel(
    private val repository: NotesRepository,
    private val toaster: Toaster,
    private val context: Context,
    private val exportManager: ExportManager,
    private val importManager: ImportManager,
    private val searchDataSource: SearchDataSource,
    private val localeManager: com.ukhvat.notes.data.LocaleManager
) : ViewModel() {
    

    
    // MANAGERS NOW INJECTED VIA KOIN DI - NO MORE LAZY INITIALIZATION NEEDED
    
            // Lazy theme - loads only on first theme access
    private var _themeLoaded = false
    private fun loadThemeIfNeeded() {
        if (!_themeLoaded) {
            viewModelScope.launch {
                val theme = repository.getThemePreference()
                _uiState.value = _uiState.value.copy(currentTheme = theme)
                _themeLoaded = true
            }
        }
    }
    
    private val _uiState = MutableStateFlow(NotesListUiState())
    val uiState: StateFlow<NotesListUiState> = _uiState.asStateFlow()

    // Search job for preventing race conditions during search operations
    private var searchJob: Job? = null
    private var displayJob: Job? = null

    init {
        // STARTUP PERFORMANCE OPTIMIZATION:
        // Removed synchronous loadDataSimple() from init block
        // Now loading happens only after ViewModel creation
        // which occurs asynchronously in OptimizedMainApp
        loadDataSimple()
        
        // Load saved theme preference at startup
        loadThemeIfNeeded()
    }
    
    /**
     * Data initialization via reactive Flow
     * 
     * Technical implementation:
     * - repository.getAllNotes() returns Flow<List<Note>>
     * - Automatic allNotes updates when DB changes
     * - Ensures data accuracy without manual loadNotes() calls
     * - Supports architecture without flashing when switching modes
     */
    private fun loadDataSimple() {
        viewModelScope.launch {
            repository.getAllNotes().collect { notes ->
                // Defensive UI filtering to exclude trash/archive even if DB query temporarily out-of-sync
                val visibleNotes = notes.filter { !it.isDeleted && !it.isArchived }
                _uiState.value = _uiState.value.copy(
                    allNotes = visibleNotes,
                    isLoading = false
                )
            }
        }
    }
    




    fun onEvent(event: NotesListEvent) {
        when (event) {
            is NotesListEvent.CreateNewNote -> createNewNote()
            is NotesListEvent.LoadMoreNotes -> loadMoreNotes()
            is NotesListEvent.SearchQueryChanged -> updateSearchQuery(event.query)
            is NotesListEvent.ClearSearch -> clearSearch()
            is NotesListEvent.StartSearch -> startSearch()
            is NotesListEvent.NavigateToNote -> navigateToNote(event.noteId, event.searchInfo)
            is NotesListEvent.ToggleNoteSelection -> toggleNoteSelection(event.noteId)
            is NotesListEvent.StartSelectionMode -> startSelectionMode(event.noteId)
            is NotesListEvent.SelectAllNotes -> selectAllNotes()
            is NotesListEvent.ClearSelection -> clearSelection()
            is NotesListEvent.DeleteSelectedNotes -> deleteSelectedNotes()
            is NotesListEvent.MoveSelectedToArchive -> moveSelectedToArchive()
            is NotesListEvent.ToggleFavoriteForSelected -> toggleFavoriteForSelected()
            is NotesListEvent.ShowExportOptions -> showExportOptions()
            is NotesListEvent.ShowExportOptionsForSelected -> showExportOptionsForSelected()
            is NotesListEvent.DismissExportOptions -> dismissExportOptions()
            is NotesListEvent.ShowAboutDialog -> showAboutDialog()
            is NotesListEvent.DismissAboutDialog -> dismissAboutDialog()
            is NotesListEvent.ShowLanguageDialog -> showLanguageDialog()
            is NotesListEvent.DismissLanguageDialog -> dismissLanguageDialog()
            is NotesListEvent.ChangeLanguage -> changeLanguage(event.language)
            is NotesListEvent.ExportAsText -> exportNotes("text", event.context)
            is NotesListEvent.ExportAsMarkdown -> exportNotes("markdown", event.context)
            is NotesListEvent.ExportAsArchive -> exportNotes("archive", event.context)
            is NotesListEvent.ExportAsDatabase -> exportNotes("database", event.context)
            is NotesListEvent.ExportAsIndividualMarkdown -> exportNotes("individual_markdown", event.context)
            is NotesListEvent.ExportDatabaseFile -> exportDatabaseFile(event.context)
            is NotesListEvent.ImportDatabaseFile -> importDatabase(event.context, event.fileUri)
            is NotesListEvent.ImportArchiveFile -> importArchive(event.context, event.fileUri)
            is NotesListEvent.ImportFromFolderUri -> importFromFolder(event.context, event.folderUri)
            is NotesListEvent.ExportMarkdownFileWithSelection -> exportMarkdownFileWithSelection(event.context, event.fileUri)
            is NotesListEvent.ExportArchiveFileWithSelection -> exportArchiveFileWithSelection(event.context, event.fileUri)
            is NotesListEvent.ExportIndividualMarkdownFolderWithSelection -> exportIndividualMarkdownFolderWithSelection(event.context, event.folderUri)
            is NotesListEvent.ToggleNoteExpansion -> toggleNoteExpansion(event.noteId)
            is NotesListEvent.DismissImportOptions -> dismissImportOptions()
            is NotesListEvent.ShowImportOptions -> showImportOptions()
            is NotesListEvent.SelectTheme -> selectTheme(event.theme)
            is NotesListEvent.DismissExportDialog -> dismissExportDialog()
            is NotesListEvent.ShowExportDialog -> showExportDialog()
            is NotesListEvent.ShowImportDialog -> showImportDialog()
            is NotesListEvent.HideExportDialog -> hideExportDialog()
            is NotesListEvent.HideImportDialog -> hideImportDialog()
            is NotesListEvent.RefreshNotesList -> refreshNotesList()
            is NotesListEvent.ShowSettingsDialog -> showSettings()
            is NotesListEvent.DismissSettingsDialog -> dismissSettings()
            is NotesListEvent.ShowApiKeysDialog -> openApiKeys()
            is NotesListEvent.DismissApiKeysDialog -> dismissApiKeys()
            is NotesListEvent.SaveApiKeys -> saveApiKeys(event.openAi, event.gemini, event.anthropic, event.openRouter)
            is NotesListEvent.ShowModelSelectionDialog -> showModelSelection()
            is NotesListEvent.DismissModelSelectionDialog -> dismissModelSelection()
            is NotesListEvent.SaveModelSelection -> saveModelSelection(event.provider, event.openAiModel, event.geminiModel, event.anthropicModel, event.openRouterModel)
        }
    }

            // ============ BATCH OPERATIONS AND INTEGRATION ============

    /**
     * Batch notes update with automatic search cache clearing
     * Used during synchronization or mass changes
     */
    fun updateNotesInBatch(notes: List<Note>) {
        viewModelScope.launch {
            try {
                repository.updateNotesInBatch(notes)
                searchDataSource.clearCacheAfterBatchOperation("update", notes.size)
                toaster.toast(context.getString(R.string.notes_updated, notes.size))
            } catch (e: Exception) {
                toaster.toast(context.getString(R.string.update_error, e.localizedMessage ?: ""))
            }
        }
    }

    /**
     * Mass cleanup of old versions with performance optimization
     * Useful for maintaining performance with large collections
     */
    fun cleanupOldVersionsForAllNotes(olderThanDays: Int = 30) {
        viewModelScope.launch {
            try {
                val allNotes = repository.getAllNotesSync()
                val noteIds = allNotes.map { it.id }
                
                if (noteIds.isNotEmpty()) {
                    repository.cleanupOldVersionsBatch(noteIds, olderThanDays)
                    toaster.toast(context.getString(R.string.old_versions_cleared, noteIds.size))
                }
            } catch (e: Exception) {
                toaster.toast(context.getString(R.string.cleanup_versions_error, e.localizedMessage ?: ""))
            }
        }
    }

            // ============ IMPORT WITH BATCH OPTIMIZATIONS ============

    /**
     * Forced notes update
     * 
     * Used after import/export/delete operations for forced
     * UI updates. Uses synchronous version to bypass Flow issues after batch operations.
     */
    private fun loadNotes() {
        viewModelScope.launch {
            try {
                        // Use synchronous version for forced update
        // Flow might not trigger after batch import
                val notes = withContext(Dispatchers.IO) {
                    repository.getAllNotesSync()
                }
                
                if (!_uiState.value.isSearchMode) {
                    _uiState.value = _uiState.value.copy(
                        allNotes = notes,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                // Handle error silently - collect will continue working
            }
        }
    }
    
    /**
     * Loading notes with synchronized scrolling
     * 
     * Race condition fix: Scroll happens after data loading.
     * This prevents scrolling to old list (search results).
     */
    private fun loadNotesWithScrollToTop() {
        viewModelScope.launch {
            try {
                // Load notes synchronously
                val notes = withContext(Dispatchers.IO) {
                    repository.getAllNotesSync().filter { !it.isDeleted && !it.isArchived }
                }
                
                if (!_uiState.value.isSearchMode) {
                    // Update state with notes AND scroll flag SIMULTANEOUSLY
                    _uiState.value = _uiState.value.copy(
                        allNotes = notes,
                        isLoading = false,
                        navigationState = NavigationState.ScrollToTop  // Set after data loading
                    )
                }
            } catch (e: Exception) {
                // Handle error silently - collect will continue working
            }
        }
    }

    private fun toggleNoteExpansion(noteId: Long) {
        val currentExpanded = _uiState.value.expandedNotes
        val newExpanded = if (noteId in currentExpanded) {
            currentExpanded - noteId
        } else {
            currentExpanded + noteId
        }
        
        _uiState.value = _uiState.value.copy(expandedNotes = newExpanded)
    }

    /**
     * Instant note creation
     * 
     * Uses timestamp with large offset as predefined ID.
     * Eliminates main screen flashing when creating note.
     * 
     * Logic:
     * 1. Generate unique ID based on timestamp + offset
     * 2. Instant navigation to edit_note/{predictableId}
     * 3. Create note in background with same ID
     * 
     * ID security:
     * - Offset 1000000000000L guarantees no conflicts with auto-generated IDs
     * - Timestamp uniqueness prevents conflicts between new notes
     * 
     * Advantages:
     * - Instant navigation without delays
     * - No flashing or main screen updates
     * - Predictable URL from the start
     * - ID security without conflicts
     */
    private var pendingNoteCreationJob: Job? = null
    
    private fun createNewNote() {
        // Use timestamp + offset for unique ID without conflicts
        val newNoteId = System.currentTimeMillis() + 1000000000000L
        
        // Instant navigation to note
        _uiState.value = _uiState.value.copy(
            navigationState = NavigationState.NavigateToNote(newNoteId)
        )
        
        // Create note in DB in background with predefined ID
        pendingNoteCreationJob = viewModelScope.launch {
            try {
                // Check that coroutine was not cancelled
                ensureActive()
                
                val newNote = Note(
                    id = newNoteId,
                    content = "",
                    createdAt = newNoteId,
                    updatedAt = newNoteId
                )
                
                repository.insertNote(newNote)
            } catch (e: Exception) {
                // On creation error, show error to user (only if not cancelled)
                if (isActive) {
                    toaster.toast("Note creation error: ${e.localizedMessage ?: ""}")
                }
            }
        }
    }
    
    fun cancelPendingNoteCreation() {
        pendingNoteCreationJob?.cancel()
        pendingNoteCreationJob = null
    }

    private fun deleteNote(note: Note) {
        // Instantly remove note from corresponding list
        val currentState = _uiState.value
        if (currentState.isSearchMode) {
            // In search mode, update searchResultNotes
            val updatedSearchResults = currentState.searchResultNotes.filterNot { it.id == note.id }
            _uiState.value = currentState.copy(searchResultNotes = updatedSearchResults)
        } else {
            // In normal mode, update allNotes
            val updatedAllNotes = currentState.allNotes.filterNot { it.id == note.id }
            _uiState.value = currentState.copy(allNotes = updatedAllNotes)
        }
        
        // Delete from DB in background
        viewModelScope.launch {
            repository.deleteNoteById(note.id)
            toaster.toast(R.string.note_deleted)
            
            // Clear search cache after note deletion for up-to-date results
            searchDataSource.clearCache()
        }
    }

    /**
     * Export notes through ExportManager
     * 
     * Delegates export to ExportManager with result handling via callback
     */
    private fun exportNotes(type: String, context: Context) {
        viewModelScope.launch {
            // Get notes with full content for export
            val selectedIds = _uiState.value.selectedNotes
            val notes = if (selectedIds.isNotEmpty()) {
                // Export selected notes
                repository.getNotesWithContentByIds(selectedIds.toList())
            } else {
                // Export all notes
                val allMetadata = repository.getAllNotes().first()
                val allIds = allMetadata.map { it.id }
                repository.getNotesWithContentByIds(allIds)
            }
            
            exportManager.exportNotes(
                type = type,
                context = context,
                notes = notes
            ) { success, content, exportType, error ->
                if (success) {
                    if (content != null && exportType != null) {
                        // Show dialog for markdown, archive, database, individual_markdown
                        _uiState.value = _uiState.value.copy(
                            dialogState = DialogState.Export(content, exportType),
                            error = null
                        )
                    } else {
                        // For text export just close dialog
                        _uiState.value = _uiState.value.copy(
                            dialogState = DialogState.None,
                            error = null
                        )
                    }
                } else {
                    _uiState.value = _uiState.value.copy(
                        error = error,
                        dialogState = DialogState.None
                    )
                }
            }
        }
    }

    fun dismissExportDialog() {
        _uiState.value = _uiState.value.copy(
            dialogState = DialogState.None
        )
    }

    /**
     * Database export via ExportManager
     */
    private fun exportDatabaseFile(context: Context) {
        viewModelScope.launch {
            exportManager.exportDatabaseFile(context) { success, error ->
                if (success) {
                    _uiState.value = _uiState.value.copy(
                        dialogState = DialogState.None,
                        error = null
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        error = error,
                        dialogState = DialogState.None
                    )
                }
            }
        }
    }

    private fun exportIndividualMarkdownToFolder(context: Context, folderUri: Uri, notes: List<Note>) {
        viewModelScope.launch {
            exportManager.exportIndividualMarkdownToFolder(context, folderUri, notes) { success, successCount, error ->
                if (success) {
                    _uiState.value = _uiState.value.copy(
                        dialogState = DialogState.None,
                        error = null
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        error = error,
                        dialogState = DialogState.None
                    )
                }
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    // Refactored navigation methods - Sealed Classes
    
    fun onNavigationHandled() {
        _uiState.value = _uiState.value.copy(navigationState = NavigationState.None)
    }

    // Refactored selection methods - Sealed Classes
    
    private fun toggleNoteSelection(noteId: Long) {
        val currentSelection = _uiState.value.selectionMode
        val newSelected = when (currentSelection) {
            is SelectionMode.None -> setOf(noteId)
            is SelectionMode.Active -> {
                if (currentSelection.selectedNotes.contains(noteId)) {
                    currentSelection.selectedNotes - noteId
                } else {
                    currentSelection.selectedNotes + noteId
                }
            }
        }
        
        _uiState.value = _uiState.value.copy(
            selectionMode = if (newSelected.isEmpty()) SelectionMode.None else SelectionMode.Active(newSelected)
        )
    }

    private fun startSelectionMode(noteId: Long) {
        _uiState.value = _uiState.value.copy(
            selectionMode = SelectionMode.Active(setOf(noteId))
        )
    }

    private fun clearSelection() {
        _uiState.value = _uiState.value.copy(
            selectionMode = SelectionMode.None
        )
    }

    /**
     * Simplified: Select all loaded notes without complex logic
     */
    private fun selectAllNotes() {
        val allNoteIds = _uiState.value.notes.map { it.id }.toSet()
        _uiState.value = _uiState.value.copy(
            selectionMode = SelectionMode.Active(allNoteIds)
        )
    }

    private fun deleteSelectedNotes() {
        val selectedIds = _uiState.value.selectedNotes
        
        showToastIf(selectedIds.isEmpty(), R.string.no_notes_selected_delete) {
            // Instantly remove notes from corresponding list
            val currentState = _uiState.value
            if (currentState.isSearchMode) {
                // In search mode, update searchResultNotes
                val updatedSearchResults = currentState.searchResultNotes.filterNot { it.id in selectedIds }
                _uiState.value = currentState.copy(searchResultNotes = updatedSearchResults)
            } else {
                // In normal mode, update allNotes
                val updatedAllNotes = currentState.allNotes.filterNot { it.id in selectedIds }
                _uiState.value = currentState.copy(allNotes = updatedAllNotes)
            }
            clearSelection()
            
            // Delete from DB in background with single batch query
            viewModelScope.launch {
                repository.deleteNotesByIds(selectedIds.toList())
                
                // Show success deletion notification
                val toastRes = if (selectedIds.size == 1) {
                    R.string.note_deleted
                } else {
                    R.string.notes_deleted
                }
                toaster.toast(toastRes)
                
                // Clear search cache after mass note deletion
                searchDataSource.clearCache()
            }
        }
    }

    private fun moveSelectedToArchive() {
        val selectedIds = _uiState.value.selectedNotes
        showToastIf(selectedIds.isEmpty(), R.string.no_notes_selected_delete) {
            val currentState = _uiState.value
            if (currentState.isSearchMode) {
                val updatedSearchResults = currentState.searchResultNotes.filterNot { it.id in selectedIds }
                _uiState.value = currentState.copy(searchResultNotes = updatedSearchResults)
            } else {
                val updatedAllNotes = currentState.allNotes.filterNot { it.id in selectedIds }
                _uiState.value = currentState.copy(allNotes = updatedAllNotes)
            }
            clearSelection()
            viewModelScope.launch {
                repository.moveNotesToArchive(selectedIds.toList())
                toaster.toast(R.string.archived)
            }
        }
    }

    private fun toggleFavoriteForSelected() {
        val selectedIds = _uiState.value.selectedNotes
        if (selectedIds.isEmpty()) return
        viewModelScope.launch {
            try {
                // Determine target value: if any selected is not favorite, set favorite; else remove favorite
                val currentNotes = _uiState.value.notes.filter { it.id in selectedIds }
                val shouldSetFavorite = currentNotes.any { !it.isFavorite }
                repository.setNotesFavorite(selectedIds.toList(), shouldSetFavorite)
                // Update UI model quickly to reflect color change
                val updatedAll = _uiState.value.allNotes.map { n ->
                    if (n.id in selectedIds) n.copy(isFavorite = shouldSetFavorite) else n
                }
                val updatedSearch = _uiState.value.searchResultNotes.map { n ->
                    if (n.id in selectedIds) n.copy(isFavorite = shouldSetFavorite) else n
                }
                _uiState.value = _uiState.value.copy(
                    allNotes = updatedAll,
                    searchResultNotes = updatedSearch
                )
            } catch (e: Exception) {
                toaster.toast(context.getString(R.string.update_error, e.localizedMessage ?: ""))
            }
        }
    }

    /**
     * Search reset with instant switch to main list
     * 
          * Technical implementation:
     * - isSearchMode = false → computed property automatically switches UI to allNotes
     * - allNotes always up-to-date via Flow, DB reload not required
     * - No race conditions between data loading and scrolling
     * - Prevents interface flashing when switching states
     */
    fun clearSearch() {
        searchJob?.cancel()
        
        _uiState.value = _uiState.value.copy(
            searchMode = SearchMode.None,   // Switch to allNotes via computed property
            searchResultNotes = emptyList(), // Clear search results cache
            searchContexts = emptyMap(),
            searchResults = emptyMap(),
            expandedNotes = emptySet(),
            navigationState = NavigationState.ScrollToTop // Scroll to top without delays
        )
    }

    /**
     * Optimized search
     * 
     * Uses SearchDataSource with LRU caching and adaptive debounce.
     * Supports multiple matches and expandable results.
     * Use new fast searchNotes method from repository
     */
    private fun updateSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(
            searchMode = SearchMode.Active(query),
            expandedNotes = emptySet()  // Clear expanded results on new search
        )
        
        // Cancel previous search to prevent race condition  
        searchJob?.cancel()
        
        if (query.isBlank()) {
            // Technical solution: preserve active search state with empty query
            // isSearchMode remains true - search bar doesn't close
            // Only search results are cleared, not search mode
            _uiState.value = _uiState.value.copy(
                searchResultNotes = emptyList(),  // Empty results list
                searchContexts = emptyMap(),      // Clear match contexts
                searchResults = emptyMap()        // Clear search metadata
            )
            return
        }
        
        // Simple search with adaptive debounce
        searchJob = viewModelScope.launch {
            // Adaptive delay: no delay for short queries
            val debounceDelay = when {
                query.length <= 2 -> 0L      // Instant for 1-2 characters
                query.length <= 4 -> 25L     // Fast for short queries  
                else -> 50L                  // Minimal delay for long queries
            }
            
            if (debounceDelay > 0) {
                delay(debounceDelay)
            }
            
            // Additional race condition protection
            if (_uiState.value.searchQuery == query) {
                performSearch(query)
            }
        }
    }
    
    /**
     * Search execution via SearchDataSource with result caching
     * 
     * Technical implementation:
     * - searchDataSource.performSearch() uses LRU cache for optimization
     * - Results are saved in searchResultNotes for instant switching
     * - Race condition protection via query relevance check
     * - UI automatically displays results via computed property
     */
    private suspend fun performSearch(query: String) {
        try {
            val (searchResults, searchData) = searchDataSource.performSearch(query)
            val (contexts, resultInfos) = searchData
            
            // Race condition protection: check query relevance
            if (_uiState.value.searchQuery == query) {
                _uiState.value = _uiState.value.copy(
                    searchResultNotes = searchResults,    // Cache search results
                    searchContexts = contexts,  
                    searchResults = resultInfos as Map<Long, SearchResultInfo>,
                    isLoading = false
                )
            }
        } catch (e: Exception) {
            if (_uiState.value.searchQuery == query) {
                _uiState.value = _uiState.value.copy(
                    error = context.getString(R.string.search_error, e.localizedMessage ?: ""),
                    isLoading = false
                )
            }
        }
    }
    

    
    private fun toggleTheme() {
        loadThemeIfNeeded() // Load theme on first access
        viewModelScope.launch {
            val currentTheme = repository.getThemePreference()
            val newTheme = when (currentTheme) {
                ThemePreference.SYSTEM -> ThemePreference.LIGHT
                ThemePreference.LIGHT -> ThemePreference.DARK
                ThemePreference.DARK -> ThemePreference.SYSTEM
            }
            
            selectTheme(newTheme)
        }
    }
    
    private fun selectTheme(theme: ThemePreference) {
        loadThemeIfNeeded() // Load theme on first access
        viewModelScope.launch {
            repository.saveThemePreference(theme)
            
            _uiState.value = _uiState.value.copy(
                currentTheme = theme,
                themeChanged = System.currentTimeMillis()
            )
        }
    }
    
    suspend fun getThemePreference(): ThemePreference {
        loadThemeIfNeeded() // Load theme on first access
        return repository.getThemePreference()
    }
    
            // Lazy auto-cleanup - called only when user enters trash
    fun scheduleTrashCleanupIfNeeded() {
        viewModelScope.launch {
            delay(5_000) // 5 second delay for UX
            repository.autoCleanupTrash(30) // 30 days
        }
    }
    
    fun getSearchInfoForNote(noteId: Long): SearchResultInfo? {
        return _uiState.value.searchResults[noteId]
    }
    
    suspend fun getSelectedNotes(): List<Note> {
        val selectedIds = _uiState.value.selectedNotes
        
        return if (selectedIds.isEmpty()) {
            // If nothing selected, get all notes with full content
            val allMetadata = repository.getAllNotes().first()
            val allIds = allMetadata.map { it.id }
            repository.getNotesWithContentByIds(allIds)
        } else {
            // If notes selected, load them with full content (batch query)
            repository.getNotesWithContentByIds(selectedIds.toList())
        }
    }
    
            // ============ Note import functions (remain unchanged) ============
    
    private fun showExportOptions() {
        _uiState.value = _uiState.value.copy(dialogState = DialogState.ExportOptions)
    }
    
    private fun showExportOptionsForSelected() {
        _uiState.value = _uiState.value.copy(dialogState = DialogState.ExportOptions)
    }
    
    private fun dismissExportOptions() {
        _uiState.value = _uiState.value.copy(dialogState = DialogState.None)
    }
    
    private fun showImportOptions() {
        _uiState.value = _uiState.value.copy(dialogState = DialogState.ImportOptions)
    }
    
    private fun dismissImportOptions() {
        _uiState.value = _uiState.value.copy(dialogState = DialogState.None)
    }
    
    private fun importDatabase(context: Context, fileUri: Uri) {
        viewModelScope.launch {
            importManager.importDatabaseFile(
                uri = fileUri,
                onSuccess = { importedCount ->
                    // Clear search cache after database import  
                    searchDataSource.clearCacheAfterBatchOperation("import", importedCount)
                    
                    hideImportDialog()
                    // Force reload in case Flow doesn't emit after batch import
                    loadNotesWithScrollToTop()
                },
                onError = { error ->
                    _uiState.value = _uiState.value.copy(
                        error = error
                    )
                }
            )
        }
    }

    private fun importArchive(context: Context, fileUri: Uri) {
        viewModelScope.launch {
            importManager.importArchiveFile(
                uri = fileUri,
                onSuccess = { importedCount ->
                    // Clear search cache after ZIP archive import
                    searchDataSource.clearCacheAfterBatchOperation("import", importedCount)
                    // Reset search state to avoid showing stale searchResultNotes after data changes
                    _uiState.value = _uiState.value.copy(
                        searchMode = SearchMode.None,
                        searchResultNotes = emptyList(),
                        searchContexts = emptyMap(),
                        searchResults = emptyMap(),
                        expandedNotes = emptySet()
                    )
                    // Force reload to avoid race conditions with Flow emissions
                    hideImportDialog()
                    loadNotesWithScrollToTop()
                },
                onError = { error ->
                    _uiState.value = _uiState.value.copy(
                        error = error
                    )
                }
            )
        }
    }

    private fun importFromFolder(context: Context, folderUri: Uri) {
        viewModelScope.launch {
            importManager.importFromFolderUri(
                uri = folderUri,
                onSuccess = { importedCount ->
                    // Clear search cache after folder import
                    searchDataSource.clearCacheAfterBatchOperation("import", importedCount)
                    // Reset search state to avoid showing stale searchResultNotes after data changes
                    _uiState.value = _uiState.value.copy(
                        searchMode = SearchMode.None,
                        searchResultNotes = emptyList(),
                        searchContexts = emptyMap(),
                        searchResults = emptyMap(),
                        expandedNotes = emptySet()
                    )
                    // Force reload to avoid race conditions with Flow emissions
                    hideImportDialog()
                    loadNotesWithScrollToTop()
                },
                onError = { error ->
                    _uiState.value = _uiState.value.copy(
                        error = error
                    )
                }
            )
        }
    }
    
            // ============ File export functions with correct note selection ============
    
    private fun exportMarkdownFileWithSelection(context: Context, fileUri: Uri) {
        viewModelScope.launch {
            // Use same logic as in exportNotes()
            val selectedIds = _uiState.value.selectedNotes
            val notes = if (selectedIds.isNotEmpty()) {
                // Export selected notes
                repository.getNotesWithContentByIds(selectedIds.toList())
            } else {
                // Export all notes
                val allMetadata = repository.getAllNotes().first()
                val allIds = allMetadata.map { it.id }
                repository.getNotesWithContentByIds(allIds)
            }
            
            exportManager.exportMarkdownFileWithSelection(context, fileUri, notes) { success, error ->
                if (success) {
                    _uiState.value = _uiState.value.copy(
                        dialogState = DialogState.None,
                        error = null
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        error = error,
                        dialogState = DialogState.None
                    )
                }
            }
        }
    }
    
    private fun exportArchiveFileWithSelection(context: Context, fileUri: Uri) {
        viewModelScope.launch {
            // Use same logic as in exportNotes()
            val selectedIds = _uiState.value.selectedNotes
            val notes = if (selectedIds.isNotEmpty()) {
                // Export selected notes
                repository.getNotesWithContentByIds(selectedIds.toList())
            } else {
                // Export all notes
                val allMetadata = repository.getAllNotes().first()
                val allIds = allMetadata.map { it.id }
                repository.getNotesWithContentByIds(allIds)
            }
            
            exportManager.exportArchiveFileWithSelection(context, fileUri, notes) { success, error ->
                if (success) {
                    _uiState.value = _uiState.value.copy(
                        dialogState = DialogState.None,
                        error = null
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        error = error,
                        dialogState = DialogState.None
                    )
                }
            }
        }
    }
    
    private fun exportIndividualMarkdownFolderWithSelection(context: Context, folderUri: Uri) {
        viewModelScope.launch {
            // Use same logic as in exportNotes()
            val selectedIds = _uiState.value.selectedNotes
            val notes = if (selectedIds.isNotEmpty()) {
                // Export selected notes
                repository.getNotesWithContentByIds(selectedIds.toList())
            } else {
                // Export all notes
                val allMetadata = repository.getAllNotes().first()
                val allIds = allMetadata.map { it.id }
                repository.getNotesWithContentByIds(allIds)
            }
            
            exportManager.exportIndividualMarkdownToFolder(context, folderUri, notes) { success, successCount, error ->
                if (success) {
                    _uiState.value = _uiState.value.copy(
                        dialogState = DialogState.None,
                        error = null
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        error = error,
                        dialogState = DialogState.None
                    )
                }
            }
        }
    }
    
    // ============ Missing functions ============
    
    private fun loadMoreNotes() {
        // Pagination no longer used in new architecture
        // Function left for compatibility but does nothing
    }
    
    private fun navigateToNote(noteId: Long, searchInfo: SearchResultInfo?) {
        _uiState.value = _uiState.value.copy(
            navigationState = NavigationState.NavigateToNote(noteId)
        )
    }
    
    // Refactored search methods - Sealed Classes
    
    private fun startSearch() {
        _uiState.value = _uiState.value.copy(
            searchMode = SearchMode.Active(""),
            expandedNotes = emptySet()  // Clear expanded results when starting search
        )
    }
    
    // Refactored dialog methods - Sealed Classes
    
    private fun showExportDialog() {
        _uiState.value = _uiState.value.copy(dialogState = DialogState.Export("", ""))
    }

    private fun hideExportDialog() {
        _uiState.value = _uiState.value.copy(dialogState = DialogState.None)
    }

    private fun showImportDialog() {
        _uiState.value = _uiState.value.copy(dialogState = DialogState.ImportOptions)
    }

    private fun hideImportDialog() {
        _uiState.value = _uiState.value.copy(dialogState = DialogState.None)
    }
    
    private fun showAboutDialog() {
        _uiState.value = _uiState.value.copy(dialogState = DialogState.About)
    }
    
    private fun dismissAboutDialog() {
        _uiState.value = _uiState.value.copy(dialogState = DialogState.None)
    }
    
    private fun showLanguageDialog() {
        _uiState.value = _uiState.value.copy(dialogState = DialogState.Language)
    }
    
    private fun dismissLanguageDialog() {
        _uiState.value = _uiState.value.copy(dialogState = DialogState.None)
    }

    private fun showSettings() {
        _uiState.value = _uiState.value.copy(dialogState = DialogState.Settings)
    }

    private fun dismissSettings() {
        _uiState.value = _uiState.value.copy(dialogState = DialogState.None)
    }

    private fun openApiKeys() {
        viewModelScope.launch {
            try {
                val open = repository.getOpenAiApiKey() ?: ""
                val gem = repository.getGeminiApiKey() ?: ""
                val ant = repository.getAnthropicApiKey() ?: ""
                val router = repository.getOpenRouterApiKey() ?: ""
                _uiState.value = _uiState.value.copy(
                    dialogState = DialogState.ApiKeys,
                    openAiKeyDraft = open,
                    geminiKeyDraft = gem,
                    anthropicKeyDraft = ant,
                    openRouterKeyDraft = router
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = context.getString(R.string.info_error, e.localizedMessage ?: "")
                )
            }
        }
    }

    private fun dismissApiKeys() {
        _uiState.value = _uiState.value.copy(dialogState = DialogState.None)
    }

    private fun saveApiKeys(openAi: String, gemini: String, anthropic: String, openRouter: String) {
        viewModelScope.launch {
            try {
                repository.setOpenAiApiKey(openAi)
                repository.setGeminiApiKey(gemini)
                repository.setAnthropicApiKey(anthropic)
                repository.setOpenRouterApiKey(openRouter)
                _uiState.value = _uiState.value.copy(dialogState = DialogState.None)
                toaster.toast(R.string.saved)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = context.getString(R.string.save_error, e.localizedMessage ?: "")
                )
            }
        }
    }

    private fun showModelSelection() {
        viewModelScope.launch {
            try {
                val provider = repository.getPreferredAiProvider() ?: com.ukhvat.notes.domain.model.AiProvider.OPENAI
                val openAi = repository.getOpenAiModel() ?: "gpt-5-2025-08-07"
                val gemini = repository.getGeminiModel() ?: "gemini-2.5-flash"
                val anthropic = repository.getAnthropicModel() ?: "claude-3-7-sonnet-thinking"
                val openrouter = repository.getOpenRouterModel() ?: "deepseek/deepseek-chat-v3-0324:free"
                _uiState.value = _uiState.value.copy(
                    dialogState = DialogState.ModelSelection,
                    aiProviderDraft = provider,
                    openAiModelDraft = openAi,
                    geminiModelDraft = gemini,
                    anthropicModelDraft = anthropic,
                    openRouterModelDraft = openrouter
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    dialogState = DialogState.ModelSelection
                )
            }
        }
    }

    private fun dismissModelSelection() {
        _uiState.value = _uiState.value.copy(dialogState = DialogState.None)
    }

    private fun saveModelSelection(
        provider: com.ukhvat.notes.domain.model.AiProvider,
        openAiModel: String?,
        geminiModel: String?,
        anthropicModel: String?,
        openRouterModel: String?
    ) {
        viewModelScope.launch {
            try {
                repository.setPreferredAiProvider(provider)
                openAiModel?.let { repository.setOpenAiModel(it) }
                geminiModel?.let { repository.setGeminiModel(it) }
                anthropicModel?.let { repository.setAnthropicModel(it) }
                openRouterModel?.let { repository.setOpenRouterModel(it) }
                _uiState.value = _uiState.value.copy(
                    dialogState = DialogState.None,
                    aiProviderDraft = provider,
                    openAiModelDraft = openAiModel ?: _uiState.value.openAiModelDraft,
                    geminiModelDraft = geminiModel ?: _uiState.value.geminiModelDraft,
                    anthropicModelDraft = anthropicModel ?: _uiState.value.anthropicModelDraft,
                    openRouterModelDraft = openRouterModel ?: _uiState.value.openRouterModelDraft
                )
                toaster.toast(R.string.saved)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = context.getString(R.string.save_error, e.localizedMessage ?: "")
                )
            }
        }
    }
    
    private fun changeLanguage(language: com.ukhvat.notes.domain.model.Language) {
        viewModelScope.launch {
            try {
                // Close dialog first for smoother transition
                _uiState.value = _uiState.value.copy(
                    dialogState = DialogState.None
                )
                
                // Small delay for smoother visual transition
                kotlinx.coroutines.delay(100)
                
                // Use modern 2025 approach with proper APIs
                localeManager.setLanguage(language)
                
                // Show language change notification after transition
                showToast(R.string.language_changed)
                
                // Activity will update automatically via configChanges
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = context.getString(R.string.language_change_error, e.message ?: "")
                )
            }
        }
    }
    
    /**
     * Get current app language
     */
    fun getCurrentLanguage(): com.ukhvat.notes.domain.model.Language {
        return localeManager.getCurrentLanguage()
    }

    private fun refreshNotesList() {
        // No longer needed - Flow automatically updates allNotes
        // Can add force refresh if needed, but usually not required
    }
    
    /**
     * Reset scroll flag after UI processing
     * 
     * Called from NotesListScreen after executing scroll to top.
     * Ensures single scroll execution.
     */
    fun onScrollToTopHandled() {
        _uiState.value = _uiState.value.copy(navigationState = NavigationState.None)
    }


    /**
     * Helper methods for showing notifications
     */
    private fun showToast(@StringRes textRes: Int) = viewModelScope.launch {
        toaster.toast(textRes)
    }
    
    private fun showToast(text: String) = viewModelScope.launch {
        toaster.toast(text)
    }
    
    private fun showToastIf(
        condition: Boolean,
        @StringRes textRes: Int,
        block: () -> Unit = {}
    ) = viewModelScope.launch {
        toaster.toastIf(condition, textRes, block)
    }
}



/**
 * UI state with separate data storage to prevent interface flashing
 * 
 * Architectural principles:
 * - allNotes: constantly updated via Flow, contains full note list
 * - searchResultNotes: search results cache, updated only during search execution
 * - computed property notes: automatic switching between lists without DB reload
 */
/**
 * Refactored UI State with Sealed Classes
 * 
 * Replaced boolean flags with type-safe sealed classes:
 * - 5 dialog flags → DialogState (mutually exclusive states)
 * - selection flags → SelectionMode (type-safe selection)
 * - search flags → SearchMode (logically connected states)
 * - navigation flags → NavigationState (clear transitions)
 * 
 * Result: 256 boolean combinations → 4 clear states
 */
data class NotesListUiState(
    val allNotes: List<Note> = emptyList(),              // Full note list (Flow)
    val searchResultNotes: List<Note> = emptyList(),     // Search results cache
    val isLoading: Boolean = true,
    val error: String? = null,
    
    // Refactored states
    val dialogState: DialogState = DialogState.None,     // Replaces 5 dialog booleans + export data
    val selectionMode: SelectionMode = SelectionMode.None, // Replaces isSelectionMode + selectedNotes
    val searchMode: SearchMode = SearchMode.None,        // Replaces isSearchMode + searchQuery
    val navigationState: NavigationState = NavigationState.None, // Replaces shouldScrollToTop + navigateToNote
    
    // Remaining fields (not refactored yet)
    val searchContexts: Map<Long, String> = emptyMap(),
    val searchResults: Map<Long, SearchResultInfo> = emptyMap(),
    val expandedNotes: Set<Long> = emptySet(),
    val themeChanged: Long = 0L,
    val currentTheme: ThemePreference = ThemePreference.SYSTEM,
    // Drafts for API keys editing
    val openAiKeyDraft: String = "",
    val geminiKeyDraft: String = "",
        val anthropicKeyDraft: String = "",
        val openRouterKeyDraft: String = "",
    // AI model/provider drafts for ModelSelectionDialog
    val aiProviderDraft: com.ukhvat.notes.domain.model.AiProvider = com.ukhvat.notes.domain.model.AiProvider.OPENAI,
    val openAiModelDraft: String = "gpt-5-2025-08-07",
    val geminiModelDraft: String = "gemini-2.5-flash",
    val anthropicModelDraft: String = "claude-3-7-sonnet-thinking"
    ,
    val openRouterModelDraft: String = "deepseek/deepseek-chat-v3-0324:free"
) {
    /**
     * Computed properties for backward compatibility
     * 
     * Allow gradual transition from booleans to sealed classes
     * without simultaneous UI code changes
     */
    
    // Dialog compatibility properties
    val showExportOptions: Boolean get() = dialogState is DialogState.ExportOptions
    val showImportOptions: Boolean get() = dialogState is DialogState.ImportOptions
    val showAboutDialog: Boolean get() = dialogState is DialogState.About
    val showLanguageDialog: Boolean get() = dialogState is DialogState.Language
    val showSettingsDialog: Boolean get() = dialogState is DialogState.Settings
    val showApiKeysDialog: Boolean get() = dialogState is DialogState.ApiKeys
    val showModelSelectionDialog: Boolean get() = dialogState is DialogState.ModelSelection
    val showExportDialog: Boolean get() = dialogState is DialogState.Export
    val exportContent: String? get() = (dialogState as? DialogState.Export)?.content
    val exportType: String get() = (dialogState as? DialogState.Export)?.type ?: ""
    
    // Selection compatibility properties
    val isSelectionMode: Boolean get() = selectionMode is SelectionMode.Active
    val selectedNotes: Set<Long> get() = (selectionMode as? SelectionMode.Active)?.selectedNotes ?: emptySet()
    
    // Search compatibility properties
    val isSearchMode: Boolean get() = searchMode is SearchMode.Active
    val searchQuery: String get() = (searchMode as? SearchMode.Active)?.query ?: ""
    
    // Navigation compatibility properties
    val shouldScrollToTop: Boolean get() = navigationState is NavigationState.ScrollToTop
    val navigateToNote: Long? get() = (navigationState as? NavigationState.NavigateToNote)?.noteId
    
    /**
     * Computed property for automatic switching between note lists
     * Technical implementation to prevent interface flashing on search reset
     */
    val notes: List<Note> get() = if (isSearchMode) searchResultNotes else allNotes
}

sealed class NotesListEvent {
    data object CreateNewNote : NotesListEvent()
    data object LoadMoreNotes : NotesListEvent()
    data class SearchQueryChanged(val query: String) : NotesListEvent()
    data object ClearSearch : NotesListEvent()
    data object StartSearch : NotesListEvent()
    data class NavigateToNote(val noteId: Long, val searchInfo: SearchResultInfo? = null) : NotesListEvent()
    data class ToggleNoteSelection(val noteId: Long) : NotesListEvent()
    data class StartSelectionMode(val noteId: Long) : NotesListEvent()
    data object SelectAllNotes : NotesListEvent()
    data object ClearSelection : NotesListEvent() 
    data object DeleteSelectedNotes : NotesListEvent()
    data object MoveSelectedToArchive : NotesListEvent()
    data object ToggleFavoriteForSelected : NotesListEvent()
    data object ShowExportOptions : NotesListEvent()
    data object ShowExportOptionsForSelected : NotesListEvent()
    data object DismissExportOptions : NotesListEvent()
    data object ShowAboutDialog : NotesListEvent()
    data object DismissAboutDialog : NotesListEvent()
    data object ShowLanguageDialog : NotesListEvent()
    data object DismissLanguageDialog : NotesListEvent()
    data class ChangeLanguage(val language: com.ukhvat.notes.domain.model.Language) : NotesListEvent()
    data class ExportAsText(val context: Context) : NotesListEvent()
    data class ExportAsMarkdown(val context: Context) : NotesListEvent()
    data class ExportAsArchive(val context: Context) : NotesListEvent()
    data class ExportAsDatabase(val context: Context) : NotesListEvent()
    data class ExportAsIndividualMarkdown(val context: Context) : NotesListEvent()
    data class ExportDatabaseFile(val context: Context) : NotesListEvent()
    data class ImportDatabaseFile(val context: Context, val fileUri: Uri) : NotesListEvent()
    data class ImportArchiveFile(val context: Context, val fileUri: Uri) : NotesListEvent()
    data class ImportFromFolderUri(val context: Context, val folderUri: Uri) : NotesListEvent()
    data class ExportMarkdownFileWithSelection(val context: Context, val fileUri: Uri) : NotesListEvent()
    data class ExportArchiveFileWithSelection(val context: Context, val fileUri: Uri) : NotesListEvent()
    data class ExportIndividualMarkdownFolderWithSelection(val context: Context, val folderUri: Uri) : NotesListEvent()
    data class ToggleNoteExpansion(val noteId: Long) : NotesListEvent()
    data object DismissImportOptions : NotesListEvent()
    data object ShowImportOptions : NotesListEvent()
    data class SelectTheme(val theme: ThemePreference) : NotesListEvent()
    data object DismissExportDialog : NotesListEvent()
    data object ShowExportDialog : NotesListEvent()
    data object HideExportDialog : NotesListEvent()
    data object ShowImportDialog : NotesListEvent()
    data object HideImportDialog : NotesListEvent()
    data object RefreshNotesList : NotesListEvent()
    data object ShowSettingsDialog : NotesListEvent()
    data object DismissSettingsDialog : NotesListEvent()
    data object ShowApiKeysDialog : NotesListEvent()
    data object DismissApiKeysDialog : NotesListEvent()
    data class SaveApiKeys(val openAi: String, val gemini: String, val anthropic: String, val openRouter: String) : NotesListEvent()
    data object ShowModelSelectionDialog : NotesListEvent()
    data object DismissModelSelectionDialog : NotesListEvent()
    data class SaveModelSelection(
        val provider: com.ukhvat.notes.domain.model.AiProvider,
        val openAiModel: String? = null,
        val geminiModel: String? = null,
        val anthropicModel: String? = null,
        val openRouterModel: String? = null
    ) : NotesListEvent()
} 