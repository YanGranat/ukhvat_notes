package com.ukhvat.notes.domain.datasource

import com.ukhvat.notes.domain.model.AiProvider

/**
 * AI DataSource for text transformations
 *
 * Provides LLM-backed operations such as proofreading/correction.
 */
interface AiDataSource {
    data class AiResult(
        val text: String,
        val provider: AiProvider,
        val model: String
    )

    /**
     * Corrects errors in Russian text with minimal style changes.
     * Returns corrected text with provider and model used. Throws on network or configuration errors.
     */
    suspend fun correctText(original: String): AiResult
}


 