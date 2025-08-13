package com.ukhvat.notes.domain.datasource

/**
 * AI DataSource for text transformations
 *
 * Provides LLM-backed operations such as proofreading/correction.
 */
interface AiDataSource {
    /**
     * Corrects errors in Russian text with minimal style changes.
     * Returns fully corrected text only. Throws on network or configuration errors.
     */
    suspend fun correctText(original: String): String
}


