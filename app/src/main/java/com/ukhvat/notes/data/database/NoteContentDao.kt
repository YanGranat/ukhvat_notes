package com.ukhvat.notes.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * 📦 DAO ДЛЯ СОДЕРЖИМОГО ЗАМЕТОК
 * 
 * Управляет тяжелым контентом заметок отдельно от метаданных.
 * Контент загружается только при необходимости (открытие заметки, поиск).
 * 
 * Batch operations and optimizations:
 * 1. **Batch CRUD операции** - массовое создание/обновление контента
 * 2. **JOIN optimization** - searchNotesWithContent() combines metadata and content in single query (2x faster)
 * 3. **Batch получение** - getContentByIds() для эффективного экспорта множества заметок
 * 
 * Архитектурные решения:
 * - Foreign Key CASCADE для автоматического удаления контента при удалении заметки
 * - Отдельная таблица для оптимизации памяти при загрузке списков
 * - Подготовка к FTS (Full-Text Search) индексам для поиска в больших коллекциях
 * 
 * @see NoteMetadataDao Парный DAO для легких метаданных  
 * @see NoteWithContentEntity JOIN entity для оптимизированного поиска
 */
@Dao
interface NoteContentDao {
    
    /**
     * Получить содержимое конкретной заметки
     * Загружается только при открытии заметки
     */
    @Query("SELECT * FROM note_content WHERE noteId = :noteId")
    suspend fun getContentById(noteId: Long): NoteContentEntity?

    /**
     * Получить содержимое как Flow для реактивных обновлений
     */
    @Query("SELECT * FROM note_content WHERE noteId = :noteId")
    fun getContentByIdFlow(noteId: Long): Flow<NoteContentEntity?>

    /**
     * Поиск по содержимому заметок (legacy метод)
     * 
     * Important: Excludes deleted notes (isDeleted = 0) for proper UX.
     * Использует JOIN с metadata для фильтрации активных заметок.
     * 
     * Будет заменен на FTS индексы в будущем
     */
    @Query("""
        SELECT nc.* FROM note_content nc
        INNER JOIN note_metadata m ON nc.noteId = m.id
        WHERE nc.content LIKE '%' || :query || '%'
        AND m.isDeleted = 0
    """)
    suspend fun searchByContent(query: String): List<NoteContentEntity>

    /**
     * Optimized search with JOIN
     * 
     * Объединяет метаданные и содержимое в одном запросе вместо двух отдельных.
     * Это в 2 раза быстрее чем текущий подход с searchByContent + getMetadataByIds.
     * 
     * Important: Excludes deleted notes (isDeleted = 0) for proper UX.
     * 
     * @param query Поисковый запрос для LIKE поиска
     * @return Список АКТИВНЫХ заметок с полными данными, отсортированный по времени обновления
     */
    @Query("""
        SELECT m.id, m.title, m.createdAt, m.updatedAt, m.characterCount, m.isFavorite, c.content
        FROM note_metadata m 
        INNER JOIN note_content c ON m.id = c.noteId 
        WHERE c.content LIKE '%' || :query || '%' 
        AND m.isDeleted = 0 AND m.isArchived = 0
        ORDER BY m.updatedAt DESC
    """)
    suspend fun searchNotesWithContent(query: String): List<NoteWithContentEntity>

    /**
     * Получить содержимое множества заметок (для экспорта/поиска)
     * Фильтрация по isDeleted происходит на уровне getMetadataByIds()
     */
    @Query("SELECT * FROM note_content WHERE noteId IN (:noteIds)")
    suspend fun getContentByIds(noteIds: List<Long>): List<NoteContentEntity>

    /**
     * Получить содержимое заметок БЕЗ фильтрации по корзине (для TrashDataSource)
     * 
     * Используется специально для загрузки содержимого удаленных заметок в корзине.
     * Это оригинальная версия getContentByIds() без JOIN с isDeleted фильтром.
     */
    @Query("SELECT * FROM note_content WHERE noteId IN (:noteIds)")
    suspend fun getContentByIdsIncludingDeleted(noteIds: List<Long>): List<NoteContentEntity>

    /**
     * Создать содержимое для новой заметки
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContent(content: NoteContentEntity)

    /**
     * Batch operation: bulk content creation
     * Используется при импорте больших объемов заметок
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContentBatch(content: List<NoteContentEntity>)

    /**
     * Обновить содержимое заметки
     */
    @Update
    suspend fun updateContent(content: NoteContentEntity)

    /**
     * Batch operation: bulk content update
     * Эффективно для синхронизации множественных изменений
     */
    @Update
    suspend fun updateContentBatch(content: List<NoteContentEntity>)

    /**
     * Удалить содержимое заметки
     */
    @Query("DELETE FROM note_content WHERE noteId = :noteId")
    suspend fun deleteContentById(noteId: Long)

    /**
     * Получить количество записей содержимого (для database preloading)
     */
    @Query("SELECT COUNT(*) FROM note_content")
    suspend fun getContentCount(): Int

    /**
     * Синхронные методы для импорта данных
     */
    @Query("SELECT * FROM note_content")
    fun getAllContentSync(): List<NoteContentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertContentSync(content: NoteContentEntity)


}