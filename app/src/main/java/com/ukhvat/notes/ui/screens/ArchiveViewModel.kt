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

class ArchiveViewModel(
    private val repository: NotesRepository,
    private val toaster: Toaster
) : ViewModel() {

    private val _uiState = MutableStateFlow(ArchiveUiState())
    val uiState: StateFlow<ArchiveUiState> = _uiState.asStateFlow()

    init {
        loadArchivedNotes()
    }

    private fun loadArchivedNotes() {
        viewModelScope.launch {
            repository.getArchivedNotes().collect { notes ->
                _uiState.value = _uiState.value.copy(archivedNotes = notes)
            }
        }
    }

    fun onEvent(event: ArchiveEvent) {
        when (event) {
            is ArchiveEvent.RestoreNote -> restore(event.noteId)
            is ArchiveEvent.DeleteToTrash -> deleteToTrash(event.noteId)
            is ArchiveEvent.RestoreAll -> restoreAll()
            is ArchiveEvent.DeleteAll -> deleteAll()
            is ArchiveEvent.ShowDeleteAllDialog -> _uiState.value = _uiState.value.copy(showDeleteAllDialog = true)
            is ArchiveEvent.DismissDeleteAllDialog -> _uiState.value = _uiState.value.copy(showDeleteAllDialog = false)
        }
    }

    private fun restore(noteId: Long) {
        // optimistic UI
        val current = _uiState.value.archivedNotes
        _uiState.value = _uiState.value.copy(archivedNotes = current.filterNot { it.id == noteId })
        viewModelScope.launch {
            runCatching { repository.restoreFromArchive(noteId) }
                .onSuccess { toaster.toast(R.string.note_restored) }
                .onFailure { _uiState.value = _uiState.value.copy(archivedNotes = current) }
        }
    }

    private fun deleteToTrash(noteId: Long) {
        // optimistic UI
        val current = _uiState.value.archivedNotes
        _uiState.value = _uiState.value.copy(archivedNotes = current.filterNot { it.id == noteId })
        viewModelScope.launch {
            runCatching { repository.deleteFromArchive(noteId) }
                .onSuccess { toaster.toast(R.string.note_deleted) }
                .onFailure { _uiState.value = _uiState.value.copy(archivedNotes = current) }
        }
    }

    private fun restoreAll() {
        val notes = _uiState.value.archivedNotes
        if (notes.isEmpty()) return
        _uiState.value = _uiState.value.copy(isLoading = true)
        viewModelScope.launch {
            try {
                notes.forEach { repository.restoreFromArchive(it.id) }
                _uiState.value = _uiState.value.copy(archivedNotes = emptyList(), isLoading = false)
                toaster.toast(R.string.restored_all)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    private fun deleteAll() {
        val notes = _uiState.value.archivedNotes
        if (notes.isEmpty()) {
            _uiState.value = _uiState.value.copy(showDeleteAllDialog = false)
            return
        }
        _uiState.value = _uiState.value.copy(isLoading = true, showDeleteAllDialog = false)
        viewModelScope.launch {
            try {
                notes.forEach { repository.deleteFromArchive(it.id) }
                _uiState.value = _uiState.value.copy(archivedNotes = emptyList(), isLoading = false)
                toaster.toast(R.string.moved_all_to_trash)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }
}

data class ArchiveUiState(
    val archivedNotes: List<Note> = emptyList(),
    val isLoading: Boolean = false,
    val showDeleteAllDialog: Boolean = false
)

sealed class ArchiveEvent {
    data class RestoreNote(val noteId: Long) : ArchiveEvent()
    data class DeleteToTrash(val noteId: Long) : ArchiveEvent()
    data object RestoreAll : ArchiveEvent()
    data object DeleteAll : ArchiveEvent()
    data object ShowDeleteAllDialog : ArchiveEvent()
    data object DismissDeleteAllDialog : ArchiveEvent()
}


