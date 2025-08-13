package com.ukhvat.notes.data.datasource

import com.ukhvat.notes.domain.datasource.AiDataSource
import com.ukhvat.notes.domain.model.AiProvider
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
     * NOTE: Currently implements 'correctText' and 'generateTitle' actions.
 */
class AiDataSourceImpl(
    private val okHttpClient: OkHttpClient,
    private val networkDispatcher: CoroutineDispatcher,
    private val repository: NotesRepository
) : AiDataSource {

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    override suspend fun correctText(original: String): AiDataSource.AiResult = withContext(networkDispatcher) {
        // Resolve preferred provider and models STRICTLY from settings
        val preferred = repository.getPreferredAiProvider() ?: throw IllegalStateException("AI provider not set in Settings")
        val openaiKey = repository.getOpenAiApiKey()
        val geminiKey = repository.getGeminiApiKey()
        val anthropicKey = repository.getAnthropicApiKey()
        val openRouterKey = repository.getOpenRouterApiKey()
        val openaiModel = repository.getOpenAiModel()
        val geminiModel = repository.getGeminiModel()
        val anthropicModel = repository.getAnthropicModel()
        val openrouterModel = repository.getOpenRouterModel()

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

        // Use ONLY the selected provider/model; no fallbacks
        return@withContext when (preferred) {
            AiProvider.OPENAI -> {
                val key = openaiKey ?: throw IllegalStateException("OpenAI API key not set")
                val model = openaiModel ?: throw IllegalStateException("OpenAI model not selected")
                callOpenAi(key, model, systemPrompt, userPrompt)
            }
            AiProvider.GEMINI -> {
                val key = geminiKey ?: throw IllegalStateException("Gemini API key not set")
                val model = geminiModel ?: throw IllegalStateException("Gemini model not selected")
                callGemini(key, model, systemPrompt, userPrompt)
            }
            AiProvider.ANTHROPIC -> {
                val key = anthropicKey ?: throw IllegalStateException("Anthropic API key not set")
                val model = anthropicModel ?: throw IllegalStateException("Anthropic model not selected")
                callAnthropic(key, model, systemPrompt, userPrompt)
            }
            AiProvider.OPENROUTER -> {
                val key = openRouterKey ?: throw IllegalStateException("OpenRouter API key not set")
                val model = openrouterModel ?: throw IllegalStateException("OpenRouter model not selected")
                callOpenRouter(key, model, systemPrompt, userPrompt)
            }
        }
    }

    override suspend fun generateTitle(note: String): AiDataSource.AiResult = withContext(networkDispatcher) {
        // Resolve provider/model strictly from settings
        val preferred = repository.getPreferredAiProvider() ?: throw IllegalStateException("AI provider not set in Settings")
        val openaiKey = repository.getOpenAiApiKey()
        val geminiKey = repository.getGeminiApiKey()
        val anthropicKey = repository.getAnthropicApiKey()
        val openRouterKey = repository.getOpenRouterApiKey()
        val openaiModel = repository.getOpenAiModel()
        val geminiModel = repository.getGeminiModel()
        val anthropicModel = repository.getAnthropicModel()
        val openrouterModel = repository.getOpenRouterModel()

        // Build prompt
        val systemPrompt = """
            Ты — редактор. Твоя задача — придумать лаконичный заголовок к заметке.
            Требования:
            - Одна строка, до 50 символов.
            - Без дополнительных пояснений.
            - Сохраняй стиль и смысл автора.
            Выводи только заголовок.
        """.trimIndent()

        val userPrompt = """
            Сгенерируй заголовок для вот этой заметки.
            <ЗАМЕТКА>
            $note
            </ЗАМЕТКА>
        """.trimIndent()

        return@withContext when (preferred) {
            AiProvider.OPENAI -> {
                val key = openaiKey ?: throw IllegalStateException("OpenAI API key not set")
                val model = openaiModel ?: throw IllegalStateException("OpenAI model not selected")
                callOpenAi(key, model, systemPrompt, userPrompt)
            }
            AiProvider.GEMINI -> {
                val key = geminiKey ?: throw IllegalStateException("Gemini API key not set")
                val model = geminiModel ?: throw IllegalStateException("Gemini model not selected")
                callGemini(key, model, systemPrompt, userPrompt)
            }
            AiProvider.ANTHROPIC -> {
                val key = anthropicKey ?: throw IllegalStateException("Anthropic API key not set")
                val model = anthropicModel ?: throw IllegalStateException("Anthropic model not selected")
                callAnthropic(key, model, systemPrompt, userPrompt)
            }
            AiProvider.OPENROUTER -> {
                val key = openRouterKey ?: throw IllegalStateException("OpenRouter API key not set")
                val model = openrouterModel ?: throw IllegalStateException("OpenRouter model not selected")
                callOpenRouter(key, model, systemPrompt, userPrompt)
            }
        }
    }

    private fun callOpenAi(apiKey: String, model: String, systemPrompt: String, userPrompt: String): AiDataSource.AiResult {
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
            return AiDataSource.AiResult(text = content, provider = AiProvider.OPENAI, model = model)
        }
    }

    private fun callGemini(apiKey: String, model: String, systemPrompt: String, userPrompt: String): AiDataSource.AiResult {
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
            return AiDataSource.AiResult(text = text, provider = AiProvider.GEMINI, model = model)
        }
    }

    private fun callAnthropic(apiKey: String, model: String, systemPrompt: String, userPrompt: String): AiDataSource.AiResult {
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
            return AiDataSource.AiResult(text = text, provider = AiProvider.ANTHROPIC, model = model)
        }
    }

    private fun callOpenRouter(apiKey: String, model: String, systemPrompt: String, userPrompt: String): AiDataSource.AiResult {
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
            return AiDataSource.AiResult(text = content, provider = AiProvider.OPENROUTER, model = model)
        }
    }
}


