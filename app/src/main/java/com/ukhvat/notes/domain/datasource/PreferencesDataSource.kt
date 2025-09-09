package com.ukhvat.notes.domain.datasource

import com.ukhvat.notes.ui.theme.ThemePreference

/**
 * DataSource for app preferences
 * 
 * Manages user preferences via SharedPreferences.
 * Extracted from NotesRepositoryImpl for better separation of concerns.
 * 
 * Responsibilities:
 * - Saving and loading app theme
 * - Other user preferences (future)
 * - Preferences migration between app versions
 */
interface PreferencesDataSource {
    
    // ============ Theme settings ============
    
        /**
     * Get current app theme
     * @return ThemePreference Current theme (SYSTEM, LIGHT, DARK)
     */
    suspend fun getThemePreference(): ThemePreference
    
    /**
     * Save selected app theme
     * @param theme New theme to save
     */
    suspend fun saveThemePreference(theme: ThemePreference)
    
    // ============ Additional settings (for future expansion) ============
    
    /**
     * Get autosave setting
     * @return Boolean true if autosave is enabled
     */
    suspend fun getAutoSaveEnabled(): Boolean
    
    /**
     * Save autosave setting  
     * @param enabled true to enable autosave
     */
    suspend fun setAutoSaveEnabled(enabled: Boolean)
    
    /**
     * Get autosave interval in milliseconds
     * @return Long Autosave interval (default 500ms)
     */
    suspend fun getAutoSaveInterval(): Long
    
    /**
     * Save autosave interval
     * @param intervalMs Interval in milliseconds
     */
    suspend fun setAutoSaveInterval(intervalMs: Long)

    // ============ Versioning settings ============

    /** Auto-create versions on timer and on exit */
    suspend fun getVersioningAutoEnabled(): Boolean
    suspend fun setVersioningAutoEnabled(enabled: Boolean)

    /** Versioning check interval (ms) */
    suspend fun getVersioningIntervalMs(): Long
    suspend fun setVersioningIntervalMs(intervalMs: Long)

    /** Minimum characters changed to create version */
    suspend fun getVersioningMinChangeChars(): Int
    suspend fun setVersioningMinChangeChars(chars: Int)

    /** Max regular (non-forced) versions to keep per note */
    suspend fun getVersioningMaxRegularVersions(): Int
    suspend fun setVersioningMaxRegularVersions(max: Int)

    // ============ AI API keys ============
    /** Get saved OpenAI API key (or null if not set) */
    suspend fun getOpenAiApiKey(): String?
    /** Save OpenAI API key (empty string clears) */
    suspend fun setOpenAiApiKey(key: String)

    /** Get saved Google Gemini API key (or null if not set) */
    suspend fun getGeminiApiKey(): String?
    /** Save Google Gemini API key (empty string clears) */
    suspend fun setGeminiApiKey(key: String)

    /** Get saved Anthropic Claude API key (or null if not set) */
    suspend fun getAnthropicApiKey(): String?
    /** Save Anthropic Claude API key (empty string clears) */
    suspend fun setAnthropicApiKey(key: String)

    // ============ AI preferences ============
    /** Preferred provider (OPENAI/GEMINI/ANTHROPIC) */
    suspend fun getPreferredAiProvider(): com.ukhvat.notes.domain.model.AiProvider?
    suspend fun setPreferredAiProvider(provider: com.ukhvat.notes.domain.model.AiProvider)

    /** Preferred model per provider */
    suspend fun getOpenAiModel(): String?
    suspend fun setOpenAiModel(model: String)
    suspend fun getGeminiModel(): String?
    suspend fun setGeminiModel(model: String)
    suspend fun getAnthropicModel(): String?
    suspend fun setAnthropicModel(model: String)

    // OpenRouter
    suspend fun getOpenRouterApiKey(): String?
    suspend fun setOpenRouterApiKey(key: String)
    suspend fun getOpenRouterModel(): String?
    suspend fun setOpenRouterModel(model: String)
}