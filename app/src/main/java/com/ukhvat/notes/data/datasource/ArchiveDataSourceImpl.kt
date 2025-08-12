package com.ukhvat.notes.data.datasource

import com.ukhvat.notes.data.database.NoteContentDao
import com.ukhvat.notes.data.database.NoteMetadataDao
import com.ukhvat.notes.data.database.toDomainWithContent
import com.ukhvat.notes.domain.datasource.ArchiveDataSource
import com.ukhvat.notes.domain.model.Note
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class ArchiveDataSourceImpl(
    private val metadataDao: NoteMetadataDao,
    private val contentDao: NoteContentDao
) : ArchiveDataSource {

    override suspend fun moveToArchive(noteId: Long) {
        val ts = System.currentTimeMillis()
        metadataDao.moveToArchive(noteId, ts)
    }

    override suspend fun moveMultipleToArchive(noteIds: List<Long>) {
        if (noteIds.isEmpty()) return
        val ts = System.currentTimeMillis()
        metadataDao.moveMultipleToArchive(noteIds, ts)
    }

    override suspend fun restoreFromArchive(noteId: Long) {
        metadataDao.restoreFromArchive(noteId)
    }

    override suspend fun moveArchivedToTrash(noteId: Long) {
        val ts = System.currentTimeMillis()
        metadataDao.moveArchivedToTrash(noteId, ts)
    }

    override fun getArchivedNotes(): Flow<List<Note>> = flow {
        metadataDao.getArchivedMetadata().collect { metadataList ->
            val ids = metadataList.map { it.id }
            val notes = if (ids.isEmpty()) emptyList() else {
                val contentMap = contentDao.getContentByIdsIncludingDeleted(ids).associateBy { it.noteId }
                metadataList.map { m ->
                    val content = contentMap[m.id]?.content ?: ""
                    m.toDomainWithContent(content)
                }
            }
            emit(notes)
        }
    }
}


