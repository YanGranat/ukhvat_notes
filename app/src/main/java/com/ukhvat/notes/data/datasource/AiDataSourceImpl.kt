package com.ukhvat.notes.data.datasource

import com.ukhvat.notes.domain.datasource.AiDataSource
import com.ukhvat.notes.domain.repository.NotesRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

/**
 * Implementation of AiDataSource using HTTP APIs.
 *
 * Strategy:
 * - Prefer OpenAI-compatible API if OpenAI key is present (gpt-4o-mini style endpoint)
 * - Otherwise try Gemini (text-bison/flash where applicable via Generative Language API)
 * - Otherwise try Anthropic Claude
 *
 * NOTE: We keep implementation minimal for a single 'correctText' action.
 */
class AiDataSourceImpl(
    private val okHttpClient: OkHttpClient,
    private val networkDispatcher: CoroutineDispatcher,
    private val repository: NotesRepository
) : AiDataSource {

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    override suspend fun correctText(original: String): String = withContext(networkDispatcher) {
        // Resolve preferred provider and keys
        val preferred = repository.getPreferredAiProvider()
        val openaiKey = repository.getOpenAiApiKey()
        val geminiKey = repository.getGeminiApiKey()
        val anthropicKey = repository.getAnthropicApiKey()
        val openRouterKey = repository.getOpenRouterApiKey()
        val openaiModel = repository.getOpenAiModel() ?: "gpt-5-2025-08-07"
        val geminiModel = repository.getGeminiModel() ?: "gemini-2.5-flash"
        val anthropicModel = repository.getAnthropicModel() ?: "claude-3-7-sonnet-thinking"

        // Build prompt
        val systemPrompt = """
            Ты — профессиональный корректор-лингвист. Твоя задача — выявить и исправить ошибки, сохраняя оригинальный смысл и стиль автора.

            Инструкции:
            1. Типы ошибок для исправления:
            - Орфографические
            - Пунктуационные
            - Грамматические
            - Стилистические (минимальные правки, только очевидные ошибки, сохранение стиля)

            2. Принципы:
            - Минимальное вмешательство
            - Контекстная чувствительность
            - Стилистическая деликатность
            - Терминологическая точность

            Вывод: полностью исправленный текст без дополнительных комментариев, только текст.
        """.trimIndent()

        val userPrompt = """
            Вот текст, который нужно исправить.
            <ТЕКСТ КОТОРЫЙ НУЖНО ИСПРАВИТЬ>
            $original
            </ТЕКСТ КОТОРЫЙ НУЖНО ИСПРАВИТЬ>
        """.trimIndent()

        // Try preferred provider first, then fallbacks by priority
        val tryOrder = when (preferred) {
            com.ukhvat.notes.domain.model.AiProvider.OPENAI -> listOf("openai", "gemini", "anthropic", "openrouter")
            com.ukhvat.notes.domain.model.AiProvider.GEMINI -> listOf("gemini", "openai", "anthropic", "openrouter")
            com.ukhvat.notes.domain.model.AiProvider.ANTHROPIC -> listOf("anthropic", "openai", "gemini", "openrouter")
            com.ukhvat.notes.domain.model.AiProvider.OPENROUTER -> listOf("openrouter", "openai", "gemini", "anthropic")
            null -> listOf("openai", "gemini", "anthropic", "openrouter")
        }

        for (p in tryOrder) {
            when (p) {
                "openai" -> if (!openaiKey.isNullOrBlank()) return@withContext callOpenAi(openaiKey, openaiModel, systemPrompt, userPrompt)
                "gemini" -> if (!geminiKey.isNullOrBlank()) return@withContext callGemini(geminiKey, geminiModel, systemPrompt, userPrompt)
                "anthropic" -> if (!anthropicKey.isNullOrBlank()) return@withContext callAnthropic(anthropicKey, anthropicModel, systemPrompt, userPrompt)
                "openrouter" -> if (!openRouterKey.isNullOrBlank()) return@withContext callOpenRouter(openRouterKey, repository.getOpenRouterModel() ?: openaiModel, systemPrompt, userPrompt)
            }
        }
        throw IllegalStateException("No AI API key configured")
    }

    private fun callOpenAi(apiKey: String, model: String, systemPrompt: String, userPrompt: String): String {
        // OpenAI Chat Completions style (v1/chat/completions)
        val url = "https://api.openai.com/v1/chat/completions"
        val payload = JSONObject().apply {
            put("model", model)
            // Some models (e.g., gpt-5-2025-08-07) only support default temperature.
            // Omit temperature to use provider default.
            put("messages", org.json.JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", systemPrompt)
                })
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", userPrompt)
                })
            })
        }
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(payload.toString().toRequestBody(jsonMediaType))
            .build()

        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val err = response.body?.string()?.take(2000)
                val suffix = if (!err.isNullOrBlank()) " - $err" else ""
                throw IllegalStateException("OpenAI error: HTTP ${response.code}$suffix")
            }
            val body = response.body?.string() ?: throw IllegalStateException("OpenAI empty body")
            val json = JSONObject(body)
            val choices = json.optJSONArray("choices")
            if (choices == null || choices.length() == 0) {
                throw IllegalStateException("OpenAI: no choices")
            }
            val content = choices.getJSONObject(0)
                .getJSONObject("message")
                .optString("content")
            if (content.isNullOrBlank()) throw IllegalStateException("OpenAI: empty content")
            return content
        }
    }

    private fun callGemini(apiKey: String, model: String, systemPrompt: String, userPrompt: String): String {
        // Google Generative Language API for text-only input
        // Using models like gemini-1.5-flash with text prompt
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"
        val payload = JSONObject().apply {
            put("contents", org.json.JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", org.json.JSONArray().apply {
                        put(JSONObject().apply { put("text", systemPrompt) })
                        put(JSONObject().apply { put("text", userPrompt) })
                    })
                })
            })
        }
        val request = Request.Builder()
            .url(url)
            .addHeader("Content-Type", "application/json")
            .post(payload.toString().toRequestBody(jsonMediaType))
            .build()

        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val err = response.body?.string()?.take(2000)
                val suffix = if (!err.isNullOrBlank()) " - $err" else ""
                throw IllegalStateException("Gemini error: HTTP ${response.code}$suffix")
            }
            val body = response.body?.string() ?: throw IllegalStateException("Gemini empty body")
            val json = JSONObject(body)
            val candidates = json.optJSONArray("candidates")
            if (candidates == null || candidates.length() == 0) {
                throw IllegalStateException("Gemini: no candidates")
            }
            val parts = candidates.getJSONObject(0)
                .getJSONObject("content")
                .optJSONArray("parts")
            if (parts == null || parts.length() == 0) {
                throw IllegalStateException("Gemini: no parts")
            }
            val text = parts.getJSONObject(0).optString("text")
            if (text.isNullOrBlank()) throw IllegalStateException("Gemini: empty content")
            return text
        }
    }

    private fun callAnthropic(apiKey: String, model: String, systemPrompt: String, userPrompt: String): String {
        // Anthropic Messages API v1
        val url = "https://api.anthropic.com/v1/messages"
        val payload = JSONObject().apply {
            put("model", model)
            put("max_tokens", 4096)
            put("system", systemPrompt)
            put("messages", org.json.JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", userPrompt)
                })
            })
        }
        val request = Request.Builder()
            .url(url)
            .addHeader("x-api-key", apiKey)
            .addHeader("anthropic-version", "2023-06-01")
            .addHeader("Content-Type", "application/json")
            .post(payload.toString().toRequestBody(jsonMediaType))
            .build()

        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val err = response.body?.string()?.take(2000)
                val suffix = if (!err.isNullOrBlank()) " - $err" else ""
                throw IllegalStateException("Anthropic error: HTTP ${response.code}$suffix")
            }
            val body = response.body?.string() ?: throw IllegalStateException("Anthropic empty body")
            val json = JSONObject(body)
            val contentArr = json.optJSONArray("content")
            if (contentArr == null || contentArr.length() == 0) {
                throw IllegalStateException("Anthropic: no content")
            }
            val text = contentArr.getJSONObject(0).optString("text")
            if (text.isNullOrBlank()) throw IllegalStateException("Anthropic: empty content")
            return text
        }
    }

    private fun callOpenRouter(apiKey: String, model: String, systemPrompt: String, userPrompt: String): String {
        // OpenRouter: unified chat completions across providers
        val url = "https://openrouter.ai/api/v1/chat/completions"
        val payload = JSONObject().apply {
            put("model", model)
            put("messages", org.json.JSONArray().apply {
                put(JSONObject().apply { put("role", "system"); put("content", systemPrompt) })
                put(JSONObject().apply { put("role", "user"); put("content", userPrompt) })
            })
        }
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(payload.toString().toRequestBody(jsonMediaType))
            .build()

        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val err = response.body?.string()?.take(2000)
                val suffix = if (!err.isNullOrBlank()) " - $err" else ""
                throw IllegalStateException("OpenRouter error: HTTP ${response.code}$suffix")
            }
            val body = response.body?.string() ?: throw IllegalStateException("OpenRouter empty body")
            val json = JSONObject(body)
            val choices = json.optJSONArray("choices")
            if (choices == null || choices.length() == 0) {
                throw IllegalStateException("OpenRouter: no choices")
            }
            val content = choices.getJSONObject(0).getJSONObject("message").optString("content")
            if (content.isNullOrBlank()) throw IllegalStateException("OpenRouter: empty content")
            return content
        }
    }
}


