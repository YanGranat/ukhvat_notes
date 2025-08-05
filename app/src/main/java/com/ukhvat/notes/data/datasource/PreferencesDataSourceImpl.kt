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
}