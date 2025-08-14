package com.ukhvat.notes.domain.datasource

interface TagDataSource {
    suspend fun getTags(noteId: Long): List<String>
    suspend fun replaceTags(noteId: Long, tags: List<String>)
}


