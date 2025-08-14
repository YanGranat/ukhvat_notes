package com.ukhvat.notes.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * DAO FOR NOTE VERSION HISTORY
 * 
 * Управляет версионированием заметок с полными снапшотами контента.
 * Обеспечивает возможность отката к предыдущим версиям и отслеживание изменений.
 * 
 * Batch operations for optimization:
 * Critical for version operation performance:
 * 
 * 1. **Batch удаление версий** - deleteVersionsByIds()
 *    - ДО: N отдельных DELETE запросов
 *    - ПОСЛЕ: 1 batch DELETE запрос (20x быстрее)
 *    - Использование: умная очистка старых версий в performSmartVersionCleanup()
 * 
 * 2. **Batch создание версий** - insertVersionsBatch()  
 *    - Critical for importing notes with version history
 *    - Атомарность: все версии создаются в одной транзакции
 * 
 * 3. **Массовая очистка по времени** - cleanupOldVersionsForNotes()
 *    - Efficient version cleanup for multiple notes simultaneously
 *    - Performance maintenance with large collections
 * 
 * Алгоритмы оптимизации:
 * - Умная группировка версий по времени (10-минутные окна)
 * - Сохранение важных версий (с ключевыми словами в описании)
 * - Автоматическая очистка при превышении лимитов
 * 
 * @see NotesRepositoryImpl.performSmartVersionCleanup() Алгоритм умной очистки версий
 * @see NoteVersionEntity Entity с полными снапшотами контента
 */
@Dao
interface NoteVersionDao {
    
    @Query("SELECT * FROM note_versions WHERE noteId = :noteId ORDER BY timestamp DESC")
    fun getVersionsForNote(noteId: Long): Flow<List<NoteVersionEntity>>
    
    @Query("SELECT * FROM note_versions WHERE noteId = :noteId ORDER BY timestamp DESC")
    suspend fun getVersionsForNoteList(noteId: Long): List<NoteVersionEntity>
    
    @Insert
    suspend fun insertVersion(version: NoteVersionEntity): Long
    
    @Query("SELECT * FROM note_versions WHERE id = :versionId")
    suspend fun getVersionById(versionId: Long): NoteVersionEntity?
    
    @Query("SELECT * FROM note_versions WHERE noteId = :noteId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestVersionForNote(noteId: Long): NoteVersionEntity?
    
    @Query("DELETE FROM note_versions WHERE id = :versionId")
    suspend fun deleteVersionById(versionId: Long)
    
    /**
     * Batch operation: bulk version deletion by ID
     * Эффективно для очистки множественных версий за раз
     */
    @Query("DELETE FROM note_versions WHERE id IN (:versionIds)")
    suspend fun deleteVersionsByIds(versionIds: List<Long>)
    
    @Query("DELETE FROM note_versions WHERE noteId = :noteId")
    suspend fun deleteVersionsForNote(noteId: Long)
    
    @Query("""
        DELETE FROM note_versions 
        WHERE noteId = :noteId 
        AND id NOT IN (
            SELECT id FROM note_versions 
            WHERE noteId = :noteId 
            ORDER BY timestamp DESC 
            LIMIT :keepCount
        )
    """)
    suspend fun cleanupOldVersions(noteId: Long, keepCount: Int)

    /**
     * Keep latest [keepCount] versions overall and preserve ALL forced versions.
     * Deletes only non-forced (isForcedSave = 0) versions that are older than the latest [keepCount].
     */
    @Query("""
        DELETE FROM note_versions
        WHERE noteId = :noteId
        AND isForcedSave = 0
        AND id NOT IN (
            SELECT id FROM note_versions
            WHERE noteId = :noteId
            ORDER BY timestamp DESC
            LIMIT :keepCount
        )
    """)
    suspend fun cleanupNonForcedVersionsKeepLatest(noteId: Long, keepCount: Int)

    /**
     * Batch operation: bulk cleanup of old versions for multiple notes
     * Оптимизирует процесс очистки при обработке больших объемов данных
     */
    @Query("""
        DELETE FROM note_versions 
        WHERE noteId IN (:noteIds) 
        AND timestamp < :beforeTimestamp
    """)
    suspend fun cleanupOldVersionsForNotes(noteIds: List<Long>, beforeTimestamp: Long)

    /**
     * Batch operation: bulk version creation
     * Используется при импорте заметок с историей версий
     */
    @Insert
    suspend fun insertVersionsBatch(versions: List<NoteVersionEntity>): List<Long>

    /**
     * Синхронные методы для импорта
     */
    @Insert
    fun insertVersionSync(version: NoteVersionEntity): Long
    
    /**
     * Получить все версии синхронно (для импорта базы данных)
     */
    @Query("SELECT * FROM note_versions ORDER BY timestamp DESC")
    fun getAllVersionsSync(): List<NoteVersionEntity>
    
    /**
     * Batch operation: synchronous bulk version creation
     * Для импорта в синхронном контексте
     */
    @Insert
    fun insertVersionsBatchSync(versions: List<NoteVersionEntity>): List<Long>

    /**
     * Обновить пользовательское название версии
     */
    @Query("UPDATE note_versions SET customName = :customName WHERE id = :versionId")
    suspend fun updateVersionCustomName(versionId: Long, customName: String?)

    @Query("UPDATE note_versions SET aiProvider = :provider, aiModel = :model, aiDurationMs = :durationMs WHERE id = :versionId")
    suspend fun updateVersionAiMeta(versionId: Long, provider: String?, model: String?, durationMs: Long?)

    @Query("UPDATE note_versions SET aiHashtags = :hashtags WHERE id = :versionId")
    suspend fun updateVersionAiHashtags(versionId: Long, hashtags: String?)

    /**
     * Получить количество принудительно созданных версий для заметки
     * Используется для корректировки maxVersions при удалении
     */
    @Query("SELECT COUNT(*) FROM note_versions WHERE noteId = :noteId AND isForcedSave = 1")
    suspend fun getForcedVersionsCount(noteId: Long): Int

    /**
     * Получить общее количество версий (для database preloading)
     */
    @Query("SELECT COUNT(*) FROM note_versions")
    suspend fun getVersionsCount(): Int
} 