package com.ukhvat.notes.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index
import com.ukhvat.notes.domain.model.Note

/**
 * 📦 СОДЕРЖИМОЕ ЗАМЕТКИ (ТЯЖЕЛЫЕ ДАННЫЕ)
 * 
 * Содержит полный текст заметки
 */
@Entity(
    tableName = "note_content",
    foreignKeys = [
        ForeignKey(
            entity = NoteMetadataEntity::class,
            parentColumns = ["id"],
            childColumns = ["noteId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["noteId"])]
)
data class NoteContentEntity(
    @PrimaryKey 
    val noteId: Long,        // Связь с метаданными (1:1)
    val content: String      // Полный текст заметки
)

/**
 * Entity for JOIN queries
 * 
 * Используется в searchNotesWithContent() для объединения
 * метаданных и контента в одном оптимизированном запросе
 */
data class NoteWithContentEntity(
    val id: Long,
    val title: String,
    val createdAt: Long,
    val updatedAt: Long,
    val characterCount: Int,
    val content: String
)

/**
 * Преобразование JOIN результата в доменную модель
 */
fun NoteWithContentEntity.toDomain(): Note = Note(
    id = id,
    content = content,
    cachedTitle = title,
    createdAt = createdAt,
    updatedAt = updatedAt
)


/**
 * Extension функция для создания NoteContentEntity с привязкой к конкретной заметке.
 * 
 * Architecture purpose:
 * Используется после создания NoteMetadataEntity когда известен сгенерированный noteId.
 * Часть двухтабличной архитектуры: метаданные отдельно, контент отдельно.
 * 
 * ПРИМЕНЕНИЕ:
 * val noteId = metadataDao.insertMetadata(note.toMetadataEntityForSave())
 * val content = note.toContentEntity(noteId)
 * contentDao.insertContent(content)
 * 
 * @param noteId ID заметки полученный после вставки метаданных
 * @return NoteContentEntity готовый для сохранения в БД
 */
fun Note.toContentEntity(noteId: Long): NoteContentEntity = NoteContentEntity(
    noteId = noteId,
    content = content
)