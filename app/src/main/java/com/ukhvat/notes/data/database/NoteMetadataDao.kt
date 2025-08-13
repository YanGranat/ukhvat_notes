package com.ukhvat.notes.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * DAO for note metadata operations.
 * 
 * Manages lightweight note metadata (titles, timestamps, counters) optimized
 * for fast list loading without heavy content. Includes batch operations
 * for improved performance with large datasets.
 * 
 * @see NoteContentDao Парный DAO для управления тяжелым контентом
 * @see NotesRepositoryImpl Uses batch methods for optimal performance
 */
@Dao
interface NoteMetadataDao {
    
    /**
     * Получить все метаданные АКТИВНЫХ заметок для отображения списка
     * Очень быстрая операция - только легкие данные
     * Исключает удаленные заметки (isDeleted = true)
     */
    @Query("SELECT * FROM note_metadata WHERE isDeleted = 0 AND isArchived = 0 ORDER BY updatedAt DESC, id DESC")
    fun getAllMetadata(): Flow<List<NoteMetadataEntity>>

    /**
     * Получить метаданные конкретной заметки
     */
    @Query("SELECT * FROM note_metadata WHERE id = :id")
    suspend fun getMetadataById(id: Long): NoteMetadataEntity?

    /**
     * Получить метаданные для списка ID (для оптимизации поиска)
     * 
     * Important: Returns only ACTIVE notes (isDeleted = 0).
     * Исключает заметки из корзины для правильного UX поиска.  
     */
    @Query("SELECT * FROM note_metadata WHERE id IN (:ids) AND isDeleted = 0 AND isArchived = 0")
    suspend fun getMetadataByIds(ids: List<Long>): List<NoteMetadataEntity>

    /**
     * Создать новую заметку (только метаданные)
     */
    @Insert
    suspend fun insertMetadata(metadata: NoteMetadataEntity): Long
    
    /**
     * Создать новую заметку с предопределенным ID (для мгновенной навигации)
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMetadataWithId(metadata: NoteMetadataEntity)

    /**
     * Batch operation: bulk metadata creation
     * Используется при импорте больших объемов заметок
     */
    @Insert
    suspend fun insertMetadataBatch(metadata: List<NoteMetadataEntity>): List<Long>

    /**
     * Обновить метаданные заметки (заголовок, время обновления и т.д.)
     */
    @Update
    suspend fun updateMetadata(metadata: NoteMetadataEntity)

    /**
     * Batch operation: bulk metadata update  
     * Эффективно для синхронизации множественных изменений
     */
    @Update
    suspend fun updateMetadataBatch(metadata: List<NoteMetadataEntity>)

    /**
     * Удалить метаданные заметки
     */
    @Query("DELETE FROM note_metadata WHERE id = :id")
    suspend fun deleteMetadataById(id: Long)
    
    @Query("DELETE FROM note_metadata WHERE id IN (:ids)")
    suspend fun deleteMetadataByIds(ids: List<Long>)

    /**
     * Получить количество АКТИВНЫХ заметок (исключая корзину)
     */
    @Query("SELECT COUNT(*) FROM note_metadata WHERE isDeleted = 0")
    suspend fun getNotesCount(): Int

    /**
     * Увеличить максимальное количество версий для заметки на 1
     * Используется при принудительном сохранении
     */
    @Query("UPDATE note_metadata SET maxVersions = maxVersions + 1 WHERE id = :noteId")
    suspend fun increaseMaxVersions(noteId: Long)

    /**
     * Получить максимальное количество версий для заметки
     */
    @Query("SELECT maxVersions FROM note_metadata WHERE id = :noteId")
    suspend fun getMaxVersions(noteId: Long): Int?

    /**
     * Уменьшить максимальное количество версий для заметки на 1
     * Используется при удалении принудительно созданных версий
     * Минимальный лимит остается 100
     */
    @Query("UPDATE note_metadata SET maxVersions = CASE WHEN maxVersions > 100 THEN maxVersions - 1 ELSE 100 END WHERE id = :noteId")
    suspend fun decreaseMaxVersions(noteId: Long)

    /**
     * Синхронные методы для импорта данных
     */
    @Query("SELECT * FROM note_metadata WHERE isDeleted = 0 AND isArchived = 0 ORDER BY updatedAt DESC, id DESC")
    fun getAllMetadataSync(): List<NoteMetadataEntity>

    /**
     * Получить все метаданные БЕЗ фильтрации (для полного импорта БД)
     * Включает активные, удаленные (корзина) и архивные заметки.
     */
    @Query("SELECT * FROM note_metadata ORDER BY updatedAt DESC, id DESC")
    fun getAllMetadataSyncAll(): List<NoteMetadataEntity>

    /**
     * Normalize flags after data import
     * - If deletedAt is not null, ensure isDeleted = 1
     * - If archivedAt is not null, ensure isArchived = 1
     */
    @Query("UPDATE note_metadata SET isDeleted = 1 WHERE deletedAt IS NOT NULL AND isDeleted = 0")
    fun normalizeDeletedFlags()

    @Query("UPDATE note_metadata SET isArchived = 1 WHERE archivedAt IS NOT NULL AND isArchived = 0")
    fun normalizeArchivedFlags()

    /**
     * Safety rule: archived notes cannot be active in main list even if deletedAt is null
     */
    @Query("UPDATE note_metadata SET isArchived = 1 WHERE isArchived = 0 AND archivedAt IS NOT NULL")
    fun enforceArchivedFromTimestamps()

    @Insert
    fun insertMetadataSync(metadata: NoteMetadataEntity): Long

    /**
     * Toggle or set favorite flag for a note
     */
    @Query("UPDATE note_metadata SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun setFavorite(id: Long, isFavorite: Boolean)

    /**
     * Batch: set favorite flag for multiple notes
     */
    @Query("UPDATE note_metadata SET isFavorite = :isFavorite WHERE id IN (:ids)")
    suspend fun setFavoriteForIds(ids: List<Long>, isFavorite: Boolean)

    // ============ КОРЗИНА (TRASH) - МЕТОДЫ ДЛЯ SOFT DELETE ============
    
    /**
     * Получить все удаленные заметки (корзина)
     * Сортировка по времени удаления (новые первыми)
     */
    @Query("SELECT * FROM note_metadata WHERE isDeleted = 1 ORDER BY deletedAt DESC, id DESC")
    fun getDeletedMetadata(): Flow<List<NoteMetadataEntity>>
    
    /**
     * Переместить заметку в корзину (soft delete)
     * Устанавливает isDeleted = true и текущее время в deletedAt
     */
    @Query("UPDATE note_metadata SET isDeleted = 1, deletedAt = :deletedAt WHERE id = :id")
    suspend fun moveToTrash(id: Long, deletedAt: Long)
    
    /**
     * Batch optimization: Mass note deletion in single SQL query
     * Uses SQL IN operator for maximum performance
     */
    @Query("UPDATE note_metadata SET isDeleted = 1, deletedAt = :deletedAt WHERE id IN (:ids)")
    suspend fun moveMultipleToTrash(ids: List<Long>, deletedAt: Long)
    
    /**
     * Восстановить заметку из корзины
     * Сбрасывает isDeleted = false и очищает deletedAt
     */
    @Query("UPDATE note_metadata SET isDeleted = 0, deletedAt = null WHERE id = :id")
    suspend fun restoreFromTrash(id: Long)
    
    /**
     * Очистить всю корзину (permanent delete)
     * Полное удаление всех заметок в корзине
     */
    @Query("DELETE FROM note_metadata WHERE isDeleted = 1")
    suspend fun clearTrash()
    
    /**
     * Автоматическая очистка старых заметок из корзины
     * Удаляет заметки старше указанного времени (30 дней = 30 * 24 * 60 * 60 * 1000 мс)
     */
    @Query("DELETE FROM note_metadata WHERE isDeleted = 1 AND deletedAt < :olderThan")
    suspend fun autoCleanupTrash(olderThan: Long)
    
    /**
     * Получить количество заметок в корзине
     */
    @Query("SELECT COUNT(*) FROM note_metadata WHERE isDeleted = 1")
    suspend fun getTrashCount(): Int

    // ============ АРХИВ (ARCHIVE) ============
    /**
     * Получить все архивированные заметки
     */
    @Query("SELECT * FROM note_metadata WHERE isArchived = 1 ORDER BY archivedAt DESC, id DESC")
    fun getArchivedMetadata(): Flow<List<NoteMetadataEntity>>

    /**
     * Переместить заметку в архив
     */
    @Query("UPDATE note_metadata SET isArchived = 1, archivedAt = :archivedAt WHERE id = :id")
    suspend fun moveToArchive(id: Long, archivedAt: Long)

    /**
     * Batch: переместить несколько заметок в архив
     */
    @Query("UPDATE note_metadata SET isArchived = 1, archivedAt = :archivedAt WHERE id IN (:ids)")
    suspend fun moveMultipleToArchive(ids: List<Long>, archivedAt: Long)

    /**
     * Восстановить заметку из архива
     */
    @Query("UPDATE note_metadata SET isArchived = 0, archivedAt = NULL WHERE id = :id")
    suspend fun restoreFromArchive(id: Long)

    /**
     * Переместить заметку из архива в корзину (soft delete)
     */
    @Query("UPDATE note_metadata SET isArchived = 0, archivedAt = NULL, isDeleted = 1, deletedAt = :deletedAt WHERE id = :id")
    suspend fun moveArchivedToTrash(id: Long, deletedAt: Long)

}