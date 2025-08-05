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
}