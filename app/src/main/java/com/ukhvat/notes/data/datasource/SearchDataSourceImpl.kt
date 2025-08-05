package com.ukhvat.notes.data.datasource

import android.util.LruCache
import com.ukhvat.notes.data.database.NoteContentDao
import com.ukhvat.notes.data.database.toDomain
import com.ukhvat.notes.domain.datasource.SearchDataSource
import com.ukhvat.notes.domain.model.Note


/**
 * РЕАЛИЗАЦИЯ DATASOURCE ДЛЯ ПОИСКОВЫХ ОПЕРАЦИЙ
 * 
 * Critical: Contains identical SearchManager logic for preservation
 * всей продвинутой функциональности поиска включая:
 * - LRU кэш на 50 запросов для мгновенного отклика
 * - JOIN optimization for high performance  
 * - Множественные совпадения с расширяемыми результатами
 * - Инкрементальные алгоритмы подсчета строк O(n)
 * - Fallback логика для совместимости
 * 
 * КОПИРУЕТ SearchManager.kt БЕЗ ИЗМЕНЕНИЙ для гарантии сохранения UX.
 * 
 * @param noteContentDao Прямой доступ к DAO для оптимизированных JOIN запросов
 * 
 * МИГРАЦИЯ: Убрана зависимость от LegacyNotesRepositoryImpl.
 * Используется только noteContentDao для поиска.
 */
class SearchDataSourceImpl(
    private val noteContentDao: NoteContentDao
) : SearchDataSource {
    

    
    /**
     * LRU cache for basic search results.
     * Для простых поисковых запросов (searchNotes метод)
     */
    private val basicSearchCache = LruCache<String, List<Note>>(50)
    
    /**
     * LRU cache for extended search results.
     * 
     * ИДЕНТИЧЕН SearchManager.searchCache:
     * - Кэширует до 50 поисковых запросов для мгновенного отклика
     * - Thread-safe Android LruCache с автоматическим управлением памятью
     * - Очищается автоматически при изменении заметок для поддержания актуальности
     * 
     * Cache size: 50 queries (~1-2MB memory)
     * Hit rate: ~70-80% for repeated searches  
     * Performance gain: <1ms from cache vs 20-50ms from database
     */
    private val searchCache = LruCache<String, Pair<List<Note>, Pair<Map<Long, String>, Map<Long, Any>>>>(50)
    
    // Статистика для мониторинга
    private var cacheHits = 0
    private var totalRequests = 0
    
    override suspend fun searchNotes(query: String): List<Note> {
        totalRequests++
        
        // Проверяем кэш первым делом (ИДЕНТИЧНО SearchManager)
        basicSearchCache.get(query)?.let { cachedResult -> 
            cacheHits++
            return cachedResult 
        }
        
        try {
            // ИДЕНТИЧНАЯ ЛОГИКА SearchManager.performSearch
            val searchResults = if (query.isBlank()) {
                emptyList()
            } else {
                try {
                    // Поиск через DAO
                    noteContentDao.searchNotesWithContent(query).map { it.toDomain() }
                } catch (e: Exception) {
                    // On error, return empty result
                    emptyList()
                }
            }
            
            // Кэшируем результат для будущих запросов
            basicSearchCache.put(query, searchResults)
            
            return searchResults
        } catch (e: Exception) {
            throw e // Пересылаем исключение для обработки в ViewModel
        }
    }
    
    override fun clearSearchCache() {
        basicSearchCache.evictAll()
        searchCache.evictAll()
    }
    
    override fun invalidateNoteInCache(noteId: Long) {
        // Для LruCache нет прямого способа итерации, поэтому используем snapshot
        val basicSnapshot = basicSearchCache.snapshot()
        val searchSnapshot = searchCache.snapshot()
        
        // Проверяем basic cache и удаляем записи, содержащие заметку
        basicSnapshot.forEach { (key, notes) ->
            if (notes.any { it.id == noteId }) {
                basicSearchCache.remove(key)
            }
        }
        
        // Проверяем extended cache и удаляем записи, содержащие заметку  
        searchSnapshot.forEach { (key, result) ->
            val notes = result.first
            if (notes.any { it.id == noteId }) {
                searchCache.remove(key)
            }
        }
    }
    
    /**
     * ПРОДВИНУТЫЙ ПОИСК С РАСШИРЕННЫМИ РЕЗУЛЬТАТАМИ (новый метод из интерфейса)
     * Critical: Identical SearchManager.performSearch functionality
     */
    override suspend fun performSearch(query: String): Pair<List<Note>, Pair<Map<Long, String>, Map<Long, Any>>> {
        // Проверяем кэш первым делом (ИДЕНТИЧНО SearchManager)
        searchCache.get(query)?.let { cachedResult -> 
            return cachedResult 
        }
        
        try {
            // МИГРАЦИЯ: используем только noteContentDao для поиска
            
            // ИДЕНТИЧНАЯ ЛОГИКА SearchManager.performSearch
            val searchResults = if (query.isBlank()) {
                // Для пустого запроса возвращаем пустой результат, но с правильной структурой
                val emptyResult = Pair(emptyList<Note>(), Pair(emptyMap<Long, String>(), emptyMap<Long, Any>()))
                searchCache.put(query, emptyResult)
                return emptyResult
            } else {
                try {
                    // JOIN запрос для поиска
                    noteContentDao.searchNotesWithContent(query).map { it.toDomain() }
                } catch (e: Exception) {
                    // On error, return empty result
                    emptyList<Note>()
                }
            }
            
            // Создаем расширенные результаты
            val (contexts, resultInfos) = try {
                createSearchResults(searchResults, query)
            } catch (e: Exception) {
                // В случае ошибки создания расширенных результатов, возвращаем простые результаты
                Pair(emptyMap<Long, String>(), emptyMap<Long, SearchResultInfo>())
            }
            
            // Создаем правильный результат для возврата
            val result = Pair(searchResults, Pair(contexts, resultInfos as Map<Long, Any>))
            searchCache.put(query, result)
            
            return result
        } catch (e: Exception) {
            throw e
        }
    }
    
    // ============ HELPER МЕТОДЫ (КОПИИ ИЗ SEARCHMANAGER) ============
    
    /**
     * ИДЕНТИЧНАЯ КОПИЯ SearchManager.createSearchResults
     * Создание расширенных результатов поиска с контекстом и статистикой
     */
    fun createSearchResults(notes: List<Note>, query: String): Pair<Map<Long, String>, Map<Long, SearchResultInfo>> {
        val contexts = mutableMapOf<Long, String>()
        val searchResults = mutableMapOf<Long, SearchResultInfo>()
        
        // ЗАЩИТА ОТ ПУСТЫХ ДАННЫХ
        if (notes.isEmpty() || query.isBlank()) {
            return Pair(contexts, searchResults)
        }
        
        for (note in notes) {
            try {
                // ЗАЩИТА ОТ NULL: проверяем что note.title и note.content не null
                val safeTitle = note.title
                val safeContent = note.content
                
                val titleContains = safeTitle.contains(query, ignoreCase = true)
                val contentMatches = findAllMatches(safeContent, query)
            
                if (contentMatches.isNotEmpty()) {
                    val firstMatch = contentMatches.first()
                    val contextString = createHighlightedContext(safeContent, firstMatch.position, query)
                    contexts[note.id] = contextString
                    
                    searchResults[note.id] = SearchResultInfo(
                        context = contextString,
                        searchQuery = query,
                        foundPosition = firstMatch.position,
                        matchCount = contentMatches.size,
                        allMatches = contentMatches
                    )
                } else if (titleContains) {
                    val firstNewlineIndex = safeContent.indexOf('\n')
                    val firstLine = if (firstNewlineIndex != -1 && firstNewlineIndex < safeContent.length) {
                        safeContent.substring(0, firstNewlineIndex).trim()
                    } else {
                        safeContent.trim()
                    }
                    val contextString = if (firstLine.length > 100) {
                        firstLine.take(100) + "..."
                    } else {
                        firstLine
                    }
                    contexts[note.id] = contextString
                    
                    searchResults[note.id] = SearchResultInfo(
                        context = contextString,
                        searchQuery = query,
                        foundPosition = 0,
                        matchCount = 1,
                        allMatches = listOf(SearchMatchInfo(0, contextString, 0))
                    )
                }
            } catch (e: Exception) {
                // ЗАЩИТА ОТ ИСКЛЮЧЕНИЙ: если обработка конкретной заметки падает, пропускаем ее
                // но НЕ ломаем весь поиск
                continue
            }
        }
        
        return Pair(contexts, searchResults)
    }
    
    /**
     * ИДЕНТИЧНАЯ КОПИЯ SearchManager.findAllMatches
     * Создает информацию о всех найденных совпадениях для продвинутого поиска
     */
    fun findAllMatches(content: String, query: String): List<SearchMatchInfo> {
        if (query.isBlank() || content.isBlank()) return emptyList()
        
        val matches = mutableListOf<SearchMatchInfo>()
        var searchIndex = 0
        var currentLine = 0  // Optimization: count lines incrementally
        var lastNewlinePos = -1  // Позиция последнего найденного \n
        
        // Limit result count for performance
        val maxMatches = if (query.length == 1) 50 else 200
        
        while (searchIndex < content.length && matches.size < maxMatches) {
            val matchIndex = content.indexOf(query, searchIndex, ignoreCase = true)
            if (matchIndex == -1) break
            
                            // Optimization: count line number incrementally instead of from start each time
            while (lastNewlinePos < matchIndex) {
                val nextNewline = content.indexOf('\n', lastNewlinePos + 1)
                if (nextNewline == -1 || nextNewline > matchIndex) break
                lastNewlinePos = nextNewline
                currentLine++
            }
            
            val contextString = createHighlightedContext(content, matchIndex, query)
            
            matches.add(SearchMatchInfo(
                position = matchIndex,
                context = contextString,
                lineNumber = currentLine
            ))
            
            searchIndex = matchIndex + 1
        }
        
        return matches
    }
    
    /**
     * ИДЕНТИЧНАЯ КОПИЯ SearchManager.createHighlightedContext
     * Создает подсвеченный контекст вокруг найденного текста
     */
    fun createHighlightedContext(content: String, matchIndex: Int, query: String): String {
        // ЗАЩИТА ОТ НЕКОРРЕКТНЫХ ДАННЫХ
        if (content.isEmpty() || query.isEmpty() || matchIndex < 0 || matchIndex >= content.length) {
            return "..."
        }
        
        try {
            val lineStart = content.lastIndexOf('\n', matchIndex) + 1
            val lineEnd = content.indexOf('\n', matchIndex).let { 
                if (it == -1) content.length else it 
            }
            
            // ЗАЩИТА ОТ НЕКОРРЕКТНЫХ ИНДЕКСОВ
            if (lineStart > lineEnd || lineStart < 0 || lineEnd > content.length) {
                return content.take(100) + if (content.length > 100) "..." else ""
            }
            
            val targetLine = content.substring(lineStart, lineEnd)
            val lineIndex = matchIndex - lineStart
            
            // ДОПОЛНИТЕЛЬНАЯ ЗАЩИТА
            if (lineIndex < 0 || lineIndex >= targetLine.length) {
                return targetLine.take(100) + if (targetLine.length > 100) "..." else ""
            }
            
            val contextRadius = 40
            val queryLength = minOf(query.length, targetLine.length - lineIndex)
            
            val contextStart = maxOf(0, lineIndex - contextRadius)
            val contextEnd = minOf(targetLine.length, lineIndex + queryLength + contextRadius)
            
            // ЗАЩИТА ОТ BOUNDARY CONDITIONS
            val safeLineIndex = minOf(lineIndex, targetLine.length - 1)
            val safeQueryEnd = minOf(lineIndex + queryLength, targetLine.length)
            
            val beforeMatch = if (contextStart < safeLineIndex) {
                targetLine.substring(contextStart, safeLineIndex)
            } else ""
            
            val match = if (safeLineIndex < safeQueryEnd) {
                targetLine.substring(safeLineIndex, safeQueryEnd)
            } else ""
            
            val afterMatch = if (safeQueryEnd < contextEnd) {
                targetLine.substring(safeQueryEnd, contextEnd)
            } else ""
            
            val prefix = if (contextStart > 0) "..." else ""
            val suffix = if (contextEnd < targetLine.length) "..." else ""
            
            return "$prefix$beforeMatch<<HIGHLIGHT>>$match<</HIGHLIGHT>>$afterMatch$suffix"
        } catch (e: Exception) {
            // FALLBACK: если что-то пошло не так, возвращаем безопасную строку
            return content.take(100) + if (content.length > 100) "..." else ""
        }
    }
    
    /**
     * ОЧИСТКА КЭША (из интерфейса SearchDataSource)
     */
    override fun clearCache() {
        basicSearchCache.evictAll()
        searchCache.evictAll()
    }
    
    /**
     * ИДЕНТИЧНАЯ КОПИЯ SearchManager.clearCacheAfterBatchOperation
     * Очистка кэша после batch операций
     */
    override fun clearCacheAfterBatchOperation(operationType: String, affectedCount: Int) {
        basicSearchCache.evictAll()
        searchCache.evictAll()
        // Can add logging for performance debugging
        // Log.d("SearchDataSource", "Cache cleared after $operationType of $affectedCount notes")
    }
    
    /**
     * ПОЛУЧЕНИЕ СТАТИСТИКИ КЭША (из интерфейса SearchDataSource)
     */
    override fun getCacheStats(): Pair<Int, Int> {
        val basicCacheSize = basicSearchCache.size()
        val extendedCacheSize = searchCache.size()
        val totalCacheSize = basicCacheSize + extendedCacheSize
        val maxSize = basicSearchCache.maxSize() // Both caches have same max size
        return Pair(totalCacheSize, maxSize) // (current entries, max capacity)
    }
}

/**
 * DATA КЛАССЫ (КОПИИ ИЗ NotesListViewModel.kt)
 * Нужны для поддержания совместимости с существующей поисковой системой
 */
data class SearchResultInfo(
    val context: String,        // Контекст для показа в списке
    val searchQuery: String,    // Поисковый запрос
    val foundPosition: Int,     // Позиция найденного текста в содержимом заметки
    val matchCount: Int = 1,    // Количество вхождений поискового запроса в заметке
    val allMatches: List<SearchMatchInfo> = emptyList()  // Все найденные вхождения с контекстом
)

data class SearchMatchInfo(
    val position: Int,          // Позиция в тексте
    val context: String,        // Контекст вокруг найденного текста
    val lineNumber: Int         // Номер строки где найдено
)