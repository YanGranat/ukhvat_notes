package com.ukhvat.notes.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ukhvat.notes.domain.model.Note
import com.ukhvat.notes.domain.repository.NotesRepository
import com.ukhvat.notes.domain.util.Toaster
import com.ukhvat.notes.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
// Removed Hilt imports for Koin migration

/**
 * Trash ViewModel - Trash management
 * 
 * ViewModel for trash screen (TrashScreen). Provides management
 * of deleted notes: viewing, restoration, permanent deletion.
 * 
 * Core functionality:
 * - Loading deleted notes list via reactive Flow
 * - Restoring notes from trash to active notes
 * - Permanent deletion of individual notes from trash
 * - Complete trash clearing with confirmation
 * - Toast notifications for all operations
 * 
 * Architectural principles:
 * - MVVM pattern with StateFlow for reactivity
 * - Automatic subscription to trash changes
 * - Optimistic UI updates for fast responsiveness
 * - Koin DI for dependency injection
 * 
 * @param repository Repository for notes and trash operations
 * @param toaster Service for displaying Toast notifications
 * 
 * Addition of trash system
 */
/**
 * MIGRATED FROM HILT TO KOIN
 * 
 * Dependencies injected via Koin instead of @Inject constructor:
 * - NotesRepository: for note operations and trash management  
 * - Toaster: for user notifications
 */
class TrashViewModel(
    private val repository: NotesRepository,
    private val toaster: Toaster
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(TrashUiState())
    val uiState: StateFlow<TrashUiState> = _uiState.asStateFlow()

    init {
        loadDeletedNotes()
    }

    /**
     * Loading deleted notes
     * 
     * Subscribes to reactive Flow from repository for automatic
     * trash list updates when DB changes.
     * 
     * Reactivity:
     * - Automatic updates when restoring notes
     * - Automatic updates when permanently deleting
     * - Instant synchronization with changes from other screens
     */
    private fun loadDeletedNotes() {
        _uiState.value = _uiState.value.copy(isLoading = true)
        
        viewModelScope.launch {
            repository.getDeletedNotes().collect { deletedNotes ->
                _uiState.value = _uiState.value.copy(
                    deletedNotes = deletedNotes,
                    isLoading = false
                )
            }
        }
    }

    /**
     * Trash events handler
     * 
     * Central method for handling all user actions in trash.
     * Uses sealed class pattern for type-safe event handling.
     * 
     * @param event Event to handle (restore, delete, clear)
     */
    fun onEvent(event: TrashEvent) {
        when (event) {
            is TrashEvent.RestoreNote -> restoreNote(event.noteId)
            is TrashEvent.PermanentlyDeleteNote -> permanentlyDeleteNote(event.noteId) 
            is TrashEvent.ClearTrash -> clearTrash()
            is TrashEvent.ShowClearTrashDialog -> showClearTrashDialog()
            is TrashEvent.DismissClearTrashDialog -> dismissClearTrashDialog()
        }
    }

    /**
     * Restore note from trash
     * 
     * Moves note from trash back to active notes.
     * Uses optimistic UI update for fast responsiveness.
     * 
     * Behavior:
     * - Instant note removal from trash list in UI
     * - Asynchronous restoration in DB
     * - Toast notification about successful restoration
     * 
     * @param noteId ID of note to restore
     */
    private fun restoreNote(noteId: Long) {
        // Optimistic UI update - remove note from trash
        val currentNotes = _uiState.value.deletedNotes
        val updatedNotes = currentNotes.filterNot { it.id == noteId }
        _uiState.value = _uiState.value.copy(deletedNotes = updatedNotes)
        
                    // Restore in database in background
        viewModelScope.launch {
            try {
                repository.restoreNote(noteId)
                toaster.toast(R.string.note_restored)
            } catch (e: Exception) {
                // On error, return note to trash list
                _uiState.value = _uiState.value.copy(deletedNotes = currentNotes)
                toaster.toast(R.string.error_restoring_note)
            }
        }
    }

    /**
     * Permanently delete note from trash
     * 
     * Performs final note deletion from DB.
     * WARNING: Operation is irreversible!
     * 
     * Cascade deletion:
     * - Note metadata is deleted
     * - Note content is automatically deleted
     * - All note versions are automatically deleted
     * 
     * @param noteId ID of note for permanent deletion
     */
    private fun permanentlyDeleteNote(noteId: Long) {
        // Optimistic UI update - remove note from trash
        val currentNotes = _uiState.value.deletedNotes
        val updatedNotes = currentNotes.filterNot { it.id == noteId }
        _uiState.value = _uiState.value.copy(deletedNotes = updatedNotes)
        
                    // Delete from database in background
        viewModelScope.launch {
            try {
                repository.permanentlyDeleteNote(noteId)
                toaster.toast(R.string.note_permanently_deleted)
            } catch (e: Exception) {
                // On error, return note to trash list
                _uiState.value = _uiState.value.copy(deletedNotes = currentNotes)
                toaster.toast(R.string.error_deleting_note)
            }
        }
    }

    /**
     * Clear entire trash
     * 
     * Permanently deletes ALL notes from trash via repository.clearTrash().
     * WARNING: Operation is irreversible!
     * 
     * Architecture behavior:
     * - Optimistic UI: immediately hides confirmation dialog
     * - Loading state: shows indicator during operation
     * - Error handling: rollback UI on errors
     * - Toast feedback: informs user about result
     * 
     * Performance:
     * - Batch operation via repository.clearTrash()
     * - Reactive updates via StateFlow
     * - Automatic UI state cleanup
     */
    private fun clearTrash() {
        val notesCount = _uiState.value.deletedNotes.size
        if (notesCount == 0) {
            viewModelScope.launch {
                toaster.toast(R.string.trash_already_empty)
            }
            return
        }
        
        _uiState.value = _uiState.value.copy(
            isLoading = true,
            showClearTrashDialog = false
        )
        
        viewModelScope.launch {
            try {
                repository.clearTrash()
                _uiState.value = _uiState.value.copy(
                    deletedNotes = emptyList(),
                    isLoading = false
                )
                
                // Show toast with deleted notes count
                if (notesCount == 1) {
                    toaster.toast(R.string.trash_cleared_single)
                } else {
                    toaster.toast(R.string.trash_cleared_multiple, notesCount)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false)
                toaster.toast(R.string.error_clearing_trash)
            }
        }
    }

    /**
     * Show trash clearing confirmation dialog
     */
    private fun showClearTrashDialog() {
        _uiState.value = _uiState.value.copy(showClearTrashDialog = true)
    }

    /**
     * Close trash cleanup confirmation dialog
     */
    private fun dismissClearTrashDialog() {
        _uiState.value = _uiState.value.copy(showClearTrashDialog = false)
    }
}

/**
 * UI state for trash screen
 * 
 * Contains all data needed to display trash screen.
 * 
 * @param deletedNotes List of deleted notes to display
 * @param isLoading Loading flag (shows progress indicator)
 * @param showClearTrashDialog Whether to show trash clearing confirmation dialog
 */
data class TrashUiState(
    val deletedNotes: List<Note> = emptyList(),
    val isLoading: Boolean = false,
    val showClearTrashDialog: Boolean = false
)

/**
 * Trash events
 * 
 * Sealed class for type-safe handling of user actions in trash.
 * Each event contains necessary data to perform operation.
 */
sealed class TrashEvent {
    /**
     * Restore note from trash
     * @param noteId ID of note to restore
     */
    data class RestoreNote(val noteId: Long) : TrashEvent()
    
    /**
     * Permanently delete note from trash
     * @param noteId ID of note for permanent deletion
     */
    data class PermanentlyDeleteNote(val noteId: Long) : TrashEvent()
    
    /**
     * Clear entire trash (executed after confirmation)
     */
    data object ClearTrash : TrashEvent()
    
    /**
     * Show trash clearing confirmation dialog
     */
    data object ShowClearTrashDialog : TrashEvent()
    
    /**
     * Close trash clearing confirmation dialog
     */
    data object DismissClearTrashDialog : TrashEvent()
}
