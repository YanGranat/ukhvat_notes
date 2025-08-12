package com.ukhvat.notes.data.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.ukhvat.notes.domain.model.Note

/**
 * Легкая Entity для метаданных заметок
 * Используется для быстрого отображения списка заметок
 * Содержит только необходимую информацию без тяжелого контента
 * 
 * Performance optimization:
 * - Index на updatedAt для быстрой сортировки ORDER BY updatedAt DESC
 * - Index на isDeleted для быстрого разделения активных заметок и корзины
 * - Устраняет лаги прокрутки при запуске приложения на больших объемах данных
 * 
 * КОРЗИНА (SOFT DELETE):
 * - isDeleted: флаг удаления (false = активная, true = в корзине)
 * - deletedAt: время удаления для автоматической очистки через 30 дней
 */
@Entity(
    tableName = "note_metadata",
    indices = [
        Index(value = ["updatedAt"], name = "index_updatedAt"),
        Index(value = ["isDeleted"], name = "index_isDeleted"),
        Index(value = ["isFavorite"], name = "index_isFavorite")
    ]
)
data class NoteMetadataEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,           // Заголовок заметки
    val createdAt: Long,
    val updatedAt: Long,
    val characterCount: Int = 0, // Количество символов для отображения в списке
    val maxVersions: Int = 100,  // Максимальное количество версий для этой заметки
    val isFavorite: Boolean = false, // Флаг избранного
    val isDeleted: Boolean = false,     // Флаг удаления (soft delete)
    val deletedAt: Long? = null         // Время удаления для автоочистки через 30 дней
)

/**
 * Преобразование в доменную модель (требует отдельной загрузки содержимого)
 */
fun NoteMetadataEntity.toDomainWithContent(content: String): Note = Note(
    id = id,
    content = content,
    cachedTitle = title,  // Используем заголовок из метаданных
    createdAt = createdAt,
    updatedAt = updatedAt,
    isFavorite = isFavorite,
    isDeleted = isDeleted,
    deletedAt = deletedAt
)

/**
 * Преобразование из доменной модели в метаданные
 */
fun Note.toMetadataEntity(): NoteMetadataEntity = NoteMetadataEntity(
    id = id,
    title = title,  // Используем title property (с fallback логикой)
    createdAt = createdAt,
    updatedAt = updatedAt,
    characterCount = content.length,
    maxVersions = 100,  // Начальное значение для всех заметок
    isFavorite = isFavorite,
    isDeleted = isDeleted,
    deletedAt = deletedAt
)


/**
 * Extension функция для создания NoteMetadataEntity при сохранении заметки.
 * 
 * Architecture rationale:
 * Централизует логику маппинга Domain → Entity с правильной обработкой timestamps.
 * Применяется в NotesRepositoryImpl для устранения дублирования кода.
 * 
 * ИСПРАВЛЕНИЕ ЗАГОЛОВКОВ:
 * Реализована архитектура с пересчетом заголовков при каждом сохранении.
 * 
 * Issue:
 * - Заголовки кэшировались и не обновлялись при изменении содержимого
 * - 1000+ SQL запросов для списка заметок
 * 
 * Solution:
 * - title = content.substringBefore("\\n").trim() при каждом сохранении
 * - Список заметок загружает только метаданные (1 SQL запрос)
 * - Заголовки всегда актуальные в БД
 * 
 * ЛОГИКА TIMESTAMPS:
 * - createdAt: сохраняется существующий или устанавливается currentTime для новых заметок
 * - updatedAt: всегда currentTime (актуальное время сохранения)
 * 
 * @param currentTime Текущее время для установки timestamps (по умолчанию System.currentTimeMillis())
 * @return NoteMetadataEntity готовый для сохранения в БД
 */
fun Note.toMetadataEntityForSave(currentTime: Long = System.currentTimeMillis()): NoteMetadataEntity = NoteMetadataEntity(
    id = id,
    title = content.substringBefore("\n").trim(), // Всегда пересчитываем из содержимого
    createdAt = if (createdAt == 0L) currentTime else createdAt,
    updatedAt = currentTime,  // Всегда текущее время при сохранении
    characterCount = content.length,
    maxVersions = 100,
    isFavorite = isFavorite,
    isDeleted = isDeleted,
    deletedAt = deletedAt
)