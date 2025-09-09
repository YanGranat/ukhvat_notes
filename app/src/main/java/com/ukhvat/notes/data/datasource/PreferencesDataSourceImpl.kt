package com.ukhvat.notes.data.datasource

import android.content.Context
import android.content.SharedPreferences
import com.ukhvat.notes.domain.datasource.PreferencesDataSource
import com.ukhvat.notes.ui.theme.ThemePreference
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.koin.core.component.inject
import org.koin.core.component.KoinComponent
import org.koin.core.qualifier.named

/**
 * DATASOURCE IMPLEMENTATION FOR APP SETTINGS
 * 
 * Threading optimizations:
 * - SharedPreferences operations: используем dedicated preferences_dispatcher
 * - Single-threaded access: предотвращаем race conditions в SharedPreferences
 * - Optimized I/O: избегаем блокировки других threads
 * 
 * PERFORMANCE IMPROVEMENTS:
 * - Dedicated thread pool для preferences (single thread для thread safety)
 * - Устранен context switching overhead с общим Dispatchers.IO
 * - Сохранена вся совместимость настроек
 */
class PreferencesDataSourceImpl(
    private val context: Context
) : PreferencesDataSource, KoinComponent {
    
    private val prefs: SharedPreferences = context.getSharedPreferences("note_app_prefs", Context.MODE_PRIVATE)
    
    // Инжектим specialized dispatcher для SharedPreferences операций
    private val preferencesDispatcher: CoroutineDispatcher by inject(named("preferences_dispatcher"))
    
    // ============ НАСТРОЙКИ ТЕМЫ ============
    
    override suspend fun getThemePreference(): ThemePreference {
        return withContext(preferencesDispatcher) {
            // Optimization: use dedicated single-threaded dispatcher for SharedPreferences
            val themeName = prefs.getString("theme_preference", ThemePreference.SYSTEM.name)
                ?: ThemePreference.SYSTEM.name
            
            try {
                ThemePreference.valueOf(themeName)
            } catch (e: IllegalArgumentException) {
                ThemePreference.SYSTEM // Fallback на системную тему
            }
        }
    }
    
    override suspend fun saveThemePreference(theme: ThemePreference) {
        withContext(preferencesDispatcher) {
            // Optimization: single-threaded access prevents race conditions
            prefs.edit()
                .putString("theme_preference", theme.name)
                .apply()
        }
    }
    
    // ============ ДОПОЛНИТЕЛЬНЫЕ НАСТРОЙКИ ============
    
    override suspend fun getAutoSaveEnabled(): Boolean {
        return withContext(preferencesDispatcher) {
            prefs.getBoolean("auto_save_enabled", true) // По умолчанию включено
        }
    }
    
    override suspend fun setAutoSaveEnabled(enabled: Boolean) {
        withContext(preferencesDispatcher) {
            prefs.edit()
                .putBoolean("auto_save_enabled", enabled)
                .apply()
        }
    }
    
    override suspend fun getAutoSaveInterval(): Long {
        return withContext(preferencesDispatcher) {
            prefs.getLong("auto_save_interval", 500L) // По умолчанию 500ms как в текущем коде
        }
    }
    
    override suspend fun setAutoSaveInterval(intervalMs: Long) {
        withContext(preferencesDispatcher) {
            prefs.edit()
                .putLong("auto_save_interval", intervalMs)
                .apply()
        }
    }

    // ============ VERSIONING SETTINGS ============
    override suspend fun getVersioningAutoEnabled(): Boolean {
        return withContext(preferencesDispatcher) {
            prefs.getBoolean("versioning_auto_enabled", true)
        }
    }

    override suspend fun setVersioningAutoEnabled(enabled: Boolean) {
        withContext(preferencesDispatcher) {
            prefs.edit().putBoolean("versioning_auto_enabled", enabled).apply()
        }
    }

    override suspend fun getVersioningIntervalMs(): Long {
        return withContext(preferencesDispatcher) {
            prefs.getLong("versioning_interval_ms", com.ukhvat.notes.domain.util.VersioningConstants.VERSION_CHECK_INTERVAL_MS)
        }
    }

    override suspend fun setVersioningIntervalMs(intervalMs: Long) {
        withContext(preferencesDispatcher) {
            prefs.edit().putLong("versioning_interval_ms", intervalMs).apply()
        }
    }

    override suspend fun getVersioningMinChangeChars(): Int {
        return withContext(preferencesDispatcher) {
            prefs.getInt("versioning_min_change_chars", com.ukhvat.notes.domain.util.VersioningConstants.MIN_CHANGE_FOR_VERSION)
        }
    }

    override suspend fun setVersioningMinChangeChars(chars: Int) {
        withContext(preferencesDispatcher) {
            prefs.edit().putInt("versioning_min_change_chars", chars).apply()
        }
    }

    override suspend fun getVersioningMaxRegularVersions(): Int {
        return withContext(preferencesDispatcher) {
            prefs.getInt("versioning_max_regular_versions", com.ukhvat.notes.domain.util.VersioningConstants.DEFAULT_MAX_VERSIONS)
        }
    }

    override suspend fun setVersioningMaxRegularVersions(max: Int) {
        withContext(preferencesDispatcher) {
            prefs.edit().putInt("versioning_max_regular_versions", max).apply()
        }
    }

    // ============ AI API keys ============
    override suspend fun getOpenAiApiKey(): String? {
        return withContext(preferencesDispatcher) {
            prefs.getString("openai_api_key", null)
        }
    }

    override suspend fun setOpenAiApiKey(key: String) {
        withContext(preferencesDispatcher) {
            prefs.edit()
                .putString("openai_api_key", key.ifBlank { null })
                .apply()
        }
    }

    override suspend fun getGeminiApiKey(): String? {
        return withContext(preferencesDispatcher) {
            prefs.getString("gemini_api_key", null)
        }
    }

    override suspend fun setGeminiApiKey(key: String) {
        withContext(preferencesDispatcher) {
            prefs.edit()
                .putString("gemini_api_key", key.ifBlank { null })
                .apply()
        }
    }

    override suspend fun getAnthropicApiKey(): String? {
        return withContext(preferencesDispatcher) {
            prefs.getString("anthropic_api_key", null)
        }
    }

    override suspend fun setAnthropicApiKey(key: String) {
        withContext(preferencesDispatcher) {
            prefs.edit()
                .putString("anthropic_api_key", key.ifBlank { null })
                .apply()
        }
    }

    // ============ AI preferences ============
    override suspend fun getPreferredAiProvider(): com.ukhvat.notes.domain.model.AiProvider? {
        return withContext(preferencesDispatcher) {
            prefs.getString("ai_provider", null)?.let {
                runCatching { com.ukhvat.notes.domain.model.AiProvider.valueOf(it) }.getOrNull()
            }
        }
    }

    override suspend fun setPreferredAiProvider(provider: com.ukhvat.notes.domain.model.AiProvider) {
        withContext(preferencesDispatcher) {
            prefs.edit().putString("ai_provider", provider.name).apply()
        }
    }

    override suspend fun getOpenAiModel(): String? = withContext(preferencesDispatcher) {
        prefs.getString("openai_model", null)
    }

    override suspend fun setOpenAiModel(model: String) {
        withContext(preferencesDispatcher) {
            prefs.edit().putString("openai_model", model.ifBlank { null }).apply()
        }
    }

    override suspend fun getGeminiModel(): String? = withContext(preferencesDispatcher) {
        prefs.getString("gemini_model", null)
    }

    override suspend fun setGeminiModel(model: String) {
        withContext(preferencesDispatcher) {
            prefs.edit().putString("gemini_model", model.ifBlank { null }).apply()
        }
    }

    override suspend fun getAnthropicModel(): String? = withContext(preferencesDispatcher) {
        prefs.getString("anthropic_model", null)
    }

    override suspend fun setAnthropicModel(model: String) {
        withContext(preferencesDispatcher) {
            prefs.edit().putString("anthropic_model", model.ifBlank { null }).apply()
        }
    }

    // ============ OpenRouter ============
    override suspend fun getOpenRouterApiKey(): String? = withContext(preferencesDispatcher) {
        prefs.getString("openrouter_api_key", null)
    }
    override suspend fun setOpenRouterApiKey(key: String) {
        withContext(preferencesDispatcher) {
            prefs.edit().putString("openrouter_api_key", key.ifBlank { null }).apply()
        }
    }
    override suspend fun getOpenRouterModel(): String? = withContext(preferencesDispatcher) {
        prefs.getString("openrouter_model", null)
    }
    override suspend fun setOpenRouterModel(model: String) {
        withContext(preferencesDispatcher) {
            prefs.edit().putString("openrouter_model", model.ifBlank { null }).apply()
        }
    }
}