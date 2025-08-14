package com.ukhvat.notes.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface NoteTagDao {

    @Query("SELECT tag FROM note_tags WHERE noteId = :noteId ORDER BY tag COLLATE NOCASE ASC")
    suspend fun getTagsForNote(noteId: Long): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTags(entities: List<NoteTagEntity>)

    @Query("DELETE FROM note_tags WHERE noteId = :noteId")
    suspend fun deleteTagsForNote(noteId: Long)

    @Transaction
    suspend fun replaceTags(noteId: Long, tags: List<String>) {
        deleteTagsForNote(noteId)
        if (tags.isNotEmpty()) {
            insertTags(tags.map { NoteTagEntity(noteId = noteId, tag = it) })
        }
    }
}


