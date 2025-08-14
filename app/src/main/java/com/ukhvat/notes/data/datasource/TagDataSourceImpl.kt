package com.ukhvat.notes.data.datasource

import com.ukhvat.notes.data.database.NoteTagDao
import com.ukhvat.notes.domain.datasource.TagDataSource

class TagDataSourceImpl(
    private val tagDao: NoteTagDao
) : TagDataSource {
    override suspend fun getTags(noteId: Long): List<String> = tagDao.getTagsForNote(noteId)
    override suspend fun replaceTags(noteId: Long, tags: List<String>) = tagDao.replaceTags(noteId, tags)
}


