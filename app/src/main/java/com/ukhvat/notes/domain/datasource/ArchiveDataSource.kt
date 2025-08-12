package com.ukhvat.notes.domain.datasource

import com.ukhvat.notes.domain.model.Note
import kotlinx.coroutines.flow.Flow

/**
 * DataSource for archive management.
 * Handles moving notes to archive, restoring from archive,
 * and soft deleting archived notes (to trash).
 */
interface ArchiveDataSource {
    suspend fun moveToArchive(noteId: Long)
    suspend fun moveMultipleToArchive(noteIds: List<Long>)
    suspend fun restoreFromArchive(noteId: Long)
    suspend fun moveArchivedToTrash(noteId: Long)

    fun getArchivedNotes(): Flow<List<Note>>
}


