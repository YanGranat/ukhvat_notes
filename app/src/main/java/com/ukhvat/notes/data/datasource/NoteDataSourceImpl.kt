package com.ukhvat.notes.data.datasource

import androidx.room.Transaction
import com.ukhvat.notes.data.database.NoteMetadataDao
import com.ukhvat.notes.data.database.NoteContentDao
import com.ukhvat.notes.data.database.toDomainWithContent
import com.ukhvat.notes.data.database.toMetadataEntity
import com.ukhvat.notes.data.database.toMetadataEntityForSave
import com.ukhvat.notes.data.database.toContentEntity
import com.ukhvat.notes.domain.datasource.NoteDataSource
import com.ukhvat.notes.domain.model.Note
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.koin.core.component.inject
import org.koin.core.component.KoinComponent
import org.koin.core.qualifier.named

/**
 * РЕАЛИЗАЦИЯ DATASOURCE ДЛЯ ОСНОВНЫХ ОПЕРАЦИЙ С ЗАМЕТКАМИ
 * 
 * Threading optimizations:
 * - Room operations: не используем withContext (Room уже настроен с правильным executor)
 * - Batch operations: используем computation_dispatcher для CPU-intensive обработки
 * - Простые операции: выполняются прямо без context switching
 * 
 * PERFORMANCE IMPROVEMENTS:
 * - Устранен excessive context switching для single operations  
 * - Специализированный dispatcher для batch processing
 * - Three-table architecture preserved for optimal performance
 */
class NoteDataSourceImpl(
    private val metadataDao: NoteMetadataDao,
    private val contentDao: NoteContentDao
) : NoteDataSource, KoinComponent {
    
    // Инжектим specialized dispatcher для тяжелых batch операций
    private val computationDispatcher: CoroutineDispatcher by inject(named("computation_dispatcher"))
    
    // ============ ОСНОВНЫЕ CRUD ОПЕРАЦИИ ============
    
    @Transaction
    override suspend fun insertNote(note: Note): Long {
        val currentTime = System.currentTimeMillis()
        
        // Создаем метаданные через extension с правильной логикой timestamps
        val metadata = note.toMetadataEntityForSave(currentTime)
        
        return if (note.id != 0L) {
            // Предопределенный ID - используем метод без автогенерации
            metadataDao.insertMetadataWithId(metadata)
            
            // Создаем содержимое с предопределенным ID
            val content = note.toContentEntity(note.id)
            contentDao.insertContent(content)
            
            note.id
        } else {
            // Автогенерация ID как обычно
            val noteId = metadataDao.insertMetadata(metadata)
            
            // Создаем содержимое через extension
            val content = note.toContentEntity(noteId)
            contentDao.insertContent(content)
            
            noteId
        }
    }
    
    override suspend fun getNoteById(id: Long): Note? {
        val metadata = metadataDao.getMetadataById(id) ?: return null
        val content = contentDao.getContentById(id)?.content ?: ""
        
        return metadata.toDomainWithContent(content)
    }
    
    @Transaction
    override suspend fun updateNote(note: Note) {
        val currentTime = System.currentTimeMillis()
        
        // Обновляем метаданные через extension с правильной логикой timestamps
        val metadata = note.toMetadataEntityForSave(currentTime)
        metadataDao.updateMetadata(metadata)
        
        // Обновляем содержимое через extension
        val content = note.toContentEntity(note.id)
        contentDao.updateContent(content)
    }
    
    override suspend fun deleteNote(note: Note) {
        deleteNoteById(note.id)
    }
    
    override suspend fun deleteNoteById(id: Long) {
        val currentTime = System.currentTimeMillis()
        metadataDao.moveToTrash(id, currentTime)
        // Содержимое остается в БД, заметка помечается как удаленная (soft delete)
    }
    
    // ============ СПИСКИ ЗАМЕТОК ============
    
    override fun getAllNotes(): Flow<List<Note>> {
        return metadataDao.getAllMetadata().map { metadataList ->
            // Используем заголовки из метаданных
            metadataList.map { metadata ->
                Note(
                    id = metadata.id,
                    content = "", // Пустое содержимое для списка
                    cachedTitle = metadata.title,  // Заголовок из метаданных
                    createdAt = metadata.createdAt,
                    updatedAt = metadata.updatedAt,
                    isFavorite = metadata.isFavorite,
                    isDeleted = metadata.isDeleted,
                    deletedAt = metadata.deletedAt,
                    isArchived = metadata.isArchived,
                    archivedAt = metadata.archivedAt
                )
            }
        }
    }
    
    override fun getAllNotesSync(): List<Note> {
        val metadataList = metadataDao.getAllMetadataSync()
        return metadataList.map { metadata ->
            Note(
                id = metadata.id,
                content = "", // Пустое содержимое для batch операций
                cachedTitle = metadata.title,
                createdAt = metadata.createdAt,
                updatedAt = metadata.updatedAt,
                isFavorite = metadata.isFavorite,
                isDeleted = metadata.isDeleted,
                deletedAt = metadata.deletedAt,
                isArchived = metadata.isArchived,
                archivedAt = metadata.archivedAt
            )
        }
    }
    
    // ============ BATCH ОПЕРАЦИИ ============
    
    @Transaction
    override suspend fun insertNotes(notes: List<Note>): List<Long> {
        if (notes.isEmpty()) return emptyList()
        
        // Optimization: use computation_dispatcher for CPU-intensive batch processing
        return withContext(computationDispatcher) {
            val currentTime = System.currentTimeMillis()
            
            // Подготавливаем metadata для batch операции
            val metadataEntities = notes.map { note ->
                note.toMetadataEntityForSave(currentTime)
            }
            
            // Batch insert metadata - одна SQL операция вместо N
            val noteIds = metadataDao.insertMetadataBatch(metadataEntities)
            
            // Подготавливаем content entities с полученными IDs
            val contentEntities = notes.zip(noteIds) { note, noteId ->
                note.toContentEntity(noteId)
            }
            
            // Batch insert содержимого - одна SQL операция вместо N
            contentDao.insertContentBatch(contentEntities)
            
            noteIds
        }
    }
    
    override suspend fun getNotesWithContentByIds(ids: List<Long>): List<Note> {
        if (ids.isEmpty()) return emptyList()
        
        // Optimization: batch queries instead of multiple getNoteById
        val metadataMap = metadataDao.getMetadataByIds(ids).associateBy { it.id }
        val contentMap = contentDao.getContentByIds(ids).associateBy { it.noteId }
        
        // Собираем заметки без множественных запросов к БД
        return ids.mapNotNull { id ->
            val metadata = metadataMap[id]
            val content = contentMap[id]?.content ?: ""
            
            if (metadata != null) {
                metadata.toDomainWithContent(content)
            } else null
        }.sortedByDescending { it.updatedAt }
    }
    
    override suspend fun deleteNotesByIds(ids: List<Long>) {
        if (ids.isEmpty()) return
        
        val currentTime = System.currentTimeMillis()
        // Optimization: removed withContext - Room already configured with proper executor
        metadataDao.moveMultipleToTrash(ids, currentTime)
    }
    
    @Transaction
    override suspend fun updateNotes(notes: List<Note>) {
        if (notes.isEmpty()) return
        
        val currentTime = System.currentTimeMillis()
        
        // BATCH OPERATION: создаем списки для batch обновления
        val metadataEntities = notes.map { note ->
            note.toMetadataEntityForSave(currentTime)
        }
        val contentEntities = notes.map { note ->
            note.toContentEntity(note.id)
        }
        
        // Выполняем batch update операции
        metadataDao.updateMetadataBatch(metadataEntities)
        contentDao.updateContentBatch(contentEntities)
    }
    
    override suspend fun increaseMaxVersions(noteId: Long) {
        metadataDao.increaseMaxVersions(noteId)
    }

    override suspend fun setNoteFavorite(id: Long, isFavorite: Boolean) {
        metadataDao.setFavorite(id, isFavorite)
    }

    override suspend fun setNotesFavorite(ids: List<Long>, isFavorite: Boolean) {
        if (ids.isEmpty()) return
        metadataDao.setFavoriteForIds(ids, isFavorite)
    }
}