package com.ukhvat.notes.ui.theme

/**
 * Sealed classes for improved state management
 * 
 * Refactoring boolean flags in NotesListUiState and NoteEditUiState
 * to eliminate combinatorial state explosion.
 * 
 * Benefits:
 * - Mutually exclusive states instead of 256 boolean combinations
 * - Better type safety for state transitions
 * - Simplified testing with clear boundary states
 * - Eliminates invalid UI states
 */

/**
 * Dialog state for NotesListUiState
 * 
 * Replaces 5 boolean flags:
 * - showExportOptions, showImportOptions, showAboutDialog, 
 * - showLanguageDialog, showExportDialog
 * 
 * Ensures only one dialog can be open simultaneously
 */
sealed class DialogState {
    data object None : DialogState()
    data object ExportOptions : DialogState()
    data object ImportOptions : DialogState()
    data object About : DialogState()
    data object Language : DialogState()
    data object Settings : DialogState()
    data object ApiKeys : DialogState()
    data object ModelSelection : DialogState()
    data class Export(val content: String, val type: String) : DialogState()
}

/**
 * Loading state for NoteEditUiState
 * 
 * Replaces 2 boolean flags:
 * - isLoading, isWaitingForSearchPositioning
 * 
 * Clear transitions between loading states
 */
sealed class LoadingState {
    data object Idle : LoadingState()
    data object Loading : LoadingState()
    data object WaitingForSearchPositioning : LoadingState()
}

/**
 * Search mode for UI states
 * 
 * Replaces isSearchMode + related flags:
 * - isSearchMode, showSearchNavigation
 * 
 * Groups search states logically
 */
sealed class SearchMode {
    data object None : SearchMode()
    data class Active(
        val query: String,
        val showNavigation: Boolean = false
    ) : SearchMode()
}

/**
 * Selection mode for NotesListUiState
 * 
 * Replaces isSelectionMode + selectedNotes:
 * - isSelectionMode, selectedNotes
 * 
 * Type-safe note selection management
 */
sealed class SelectionMode {
    data object None : SelectionMode()
    data class Active(val selectedNotes: Set<Long>) : SelectionMode() {
        val isEmpty: Boolean get() = selectedNotes.isEmpty()
        val size: Int get() = selectedNotes.size
    }
}

/**
 * Navigation state for common navigation needs
 * 
 * Replaces navigation boolean flags:
 * - shouldScrollToTop, shouldNavigateBack
 * 
 * Clear navigation transition management
 */
sealed class NavigationState {
    data object None : NavigationState()
    data object ScrollToTop : NavigationState()
    data object NavigateBack : NavigationState()
    data class NavigateToNote(val noteId: Long) : NavigationState()
}

/**
 * Save indicator state for autosave visual feedback
 * 
 * Solves UX issue: users don't understand when note is saved
 * 
 * States:
 * - Idle: no save activity
 * - Saving: save process active
 * - Saved: note successfully saved (with timestamp for fade out)
 * - Error: save error occurred
 */
sealed class SaveIndicatorState {
    data object Idle : SaveIndicatorState()
    data object Saving : SaveIndicatorState()
    data class Saved(val timestamp: Long) : SaveIndicatorState()
    data class Error(val message: String) : SaveIndicatorState()
}