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

    /**
     * Generates a concise title for the given note content.
     * The model is instructed to output a single-line title up to 50 characters.
     * Returns the generated title text with provider and model used.
     */
    suspend fun generateTitle(note: String): AiResult

    /**
     * Generates relevant hashtags for the given note content.
     * Returns a single-line string with space-separated hashtags (each starting with '#').
     */
    suspend fun generateHashtags(note: String, existing: List<String> = emptyList()): AiResult

    enum class AiLanguage { RU, EN }

    /**
     * Translates the given text to the target language (RU/EN).
     * Returns translated text with provider and model used.
     */
    suspend fun translate(text: String, target: AiLanguage): AiResult
}


 