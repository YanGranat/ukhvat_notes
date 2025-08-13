package com.ukhvat.notes.data.database

import android.content.Context
import androidx.room.Entity
import com.ukhvat.notes.R
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index
import com.ukhvat.notes.domain.model.NoteVersion
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Entity(
    tableName = "note_versions",
    foreignKeys = [
        ForeignKey(
            entity = NoteMetadataEntity::class,  // Ссылаемся на NoteMetadataEntity
            parentColumns = ["id"],
            childColumns = ["noteId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["noteId"]),
        Index(value = ["timestamp"])
    ]
)
data class NoteVersionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val noteId: Long,
    val content: String,
    val timestamp: Long,
    val changeDescription: String? = null,
    val customName: String? = null,    // Пользовательское название версии
    val isForcedSave: Boolean = false,  // Была ли версия создана принудительным сохранением
    val aiProvider: String? = null,
    val aiModel: String? = null,
    val aiDurationMs: Long? = null
)

// Extension functions для конвертации
fun NoteVersionEntity.toDomain(): NoteVersion = NoteVersion(
    id = id,
    noteId = noteId,
    content = content,
    timestamp = timestamp,
    changeDescription = changeDescription,
    customName = customName,
    isForcedSave = isForcedSave,
    aiProvider = aiProvider,
    aiModel = aiModel,
    aiDurationMs = aiDurationMs
)

fun NoteVersion.toEntity(): NoteVersionEntity = NoteVersionEntity(
    id = id,
    noteId = noteId,
    content = content,
    timestamp = timestamp,
    changeDescription = changeDescription,
    customName = customName,
    isForcedSave = isForcedSave,
    aiProvider = aiProvider,
    aiModel = aiModel,
    aiDurationMs = aiDurationMs
)


/**
 * Helper функция для создания NoteVersion с правильными параметрами по умолчанию.
 * 
 * Architecture purpose:
 * Упрощает создание версий заметок в NotesRepositoryImpl без дублирования кода.
 * Инкапсулирует логику создания domain модели NoteVersion для последующего
 * преобразования в NoteVersionEntity через .toEntity().
 * 
 * ПРИМЕНЕНИЕ В РЕПОЗИТОРИИ:
 * val version = createNoteVersion(noteId, content, "Автосохранение")
 * versionDao.insertVersion(version.toEntity())
 * 
 * @param noteId ID заметки для которой создается версия
 * @param content Содержимое заметки для сохранения в истории
 * @param changeDescription Описание изменения (опционально)
 * @param isForcedSave Флаг принудительного сохранения (по умолчанию false)
 * @param timestamp Время создания версии (по умолчанию текущее время)
 * @return NoteVersion готовый для конвертации в Entity и сохранения
 */
fun createNoteVersion(
    noteId: Long,
    content: String,
    changeDescription: String? = null,
    isForcedSave: Boolean = false,
    timestamp: Long = System.currentTimeMillis()
): NoteVersion = NoteVersion(
    id = 0, // Автогенерация ID
    noteId = noteId,
    content = content,
    timestamp = timestamp,
    changeDescription = changeDescription,
    customName = null,
    isForcedSave = isForcedSave,
    aiProvider = null,
    aiModel = null,
    aiDurationMs = null
) 