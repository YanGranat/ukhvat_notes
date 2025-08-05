package com.ukhvat.notes.domain.datasource

import com.ukhvat.notes.domain.model.Note

/**
 * DataSource for search operations
 * 
 * Critical: This interface preserves advanced search system functionality
 * with LRU caching, JOIN optimization and multiple matches.
 * 
 * Responsibilities:
 * - Full-text search by note content
 * - Search result caching (LRU cache)
 * - Optimized JOIN queries to DB
 * - Advanced search support with multiple matches
 * 
 * Important: Implementation must preserve ALL SearchManager capabilities:
 * - LRU cache for 50 queries  
 * - JOIN optimization (one query instead of two)
 * - Fallback logic for compatibility
 * - Incremental line counting algorithms O(n)
 */
interface SearchDataSource {
    
    /**
     * MAIN NOTE SEARCH
     * 
     * Performs full-text search by note content.
     * MUST use same algorithms as SearchManager to preserve functionality.
     * 
     * @param query Search query (not empty)
     * @return List<Note> List of found notes with full content, sorted by relevance
     */
    suspend fun searchNotes(query: String): List<Note>
    
    /**
     * Advanced search with extended results (from SearchManager)
     * 
     * Returns notes + detailed match information for UI.
     * Critical: Identical SearchManager.performSearch signature for compatibility.
     * 
     * @param query Search query
     * @return Pair<List<Note>, Pair<Map<Long, String>, Map<Long, Any>>>
     *         where second element contains (contexts, resultInfos as SearchResultInfo)
     */
    suspend fun performSearch(query: String): Pair<List<Note>, Pair<Map<Long, String>, Map<Long, Any>>>
    
    /**
     * Search cache cleanup
     * 
     * Called when notes change (create/update/delete)
     * to maintain cache consistency.
     */
    fun clearCache()
    
    /**
     * Selective cache invalidation
     * 
     * Removes from cache only entries containing specified note.
     * Used instead of full cleanup to preserve performance.
     * 
     * @param noteId Note ID for invalidation
     */
    fun invalidateNoteInCache(noteId: Long)
    
    /**
     * Cache cleanup after batch operations
     * 
     * Special method for batch operations (import/export/update).
     * Critical: Identical SearchManager.clearCacheAfterBatchOperation signature.
     * 
     * @param operationType Operation type for logging
     * @param affectedCount Number of affected notes
     */
    fun clearCacheAfterBatchOperation(operationType: String, affectedCount: Int)
    
    /**
     * Legacy method for compatibility
     * Alias for clearCache() for old code
     */
    fun clearSearchCache() = clearCache()
    
    /**
     * Getting cache statistics
     * 
     * For monitoring and debugging search performance.
     * @return Pair<Int, Int> (cache hits, total requests)
     */
    fun getCacheStats(): Pair<Int, Int>
}