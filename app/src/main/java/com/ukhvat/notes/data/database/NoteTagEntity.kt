package com.ukhvat.notes.data.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * Normalized hashtag storage for notes.
 *
 * Schema:
 * - Composite primary key (noteId, tag) to prevent duplicates
 * - Foreign key to note_metadata(id) with CASCADE delete
 * - Index on tag for future tag-based queries
 */
@Entity(
    tableName = "note_tags",
    primaryKeys = ["noteId", "tag"],
    foreignKeys = [
        ForeignKey(
            entity = NoteMetadataEntity::class,
            parentColumns = ["id"],
            childColumns = ["noteId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["tag"], name = "index_note_tags_tag")
    ]
)
data class NoteTagEntity(
    val noteId: Long,
    val tag: String
)


