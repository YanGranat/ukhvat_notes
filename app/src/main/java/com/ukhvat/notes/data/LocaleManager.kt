package com.ukhvat.notes.data

import android.app.LocaleManager
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.os.LocaleList
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.ukhvat.notes.domain.model.Language
/**
 * Manager for app localization control
 * 
 * Uses modern AppCompatDelegate.setApplicationLocales() API
 * for Android 13+ with fallback for older versions
 */
class LocaleManager(
    private val context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME, Context.MODE_PRIVATE
    )
    
    /**
     * Get current app language
     */
    fun getCurrentLanguage(): Language {
        // First try to get language from system API
        val systemLanguage = getSystemAppLanguage()
        if (systemLanguage != null) {
            return systemLanguage
        }
        
        // Fallback to saved language in SharedPreferences
        val savedCode = prefs.getString(KEY_LANGUAGE, null)
        return if (savedCode != null) {
            Language.from(savedCode)
        } else {
            Language.DEFAULT
        }
    }
    
    /**
     * Set app language using Android 2025 best practices
     */
    fun setLanguage(language: Language) {
        // Save in SharedPreferences for compatibility
        prefs.edit()
            .putString(KEY_LANGUAGE, language.code)
            .apply()
        
        // Use correct modern API depending on Android version
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ - use LocaleManager (recommended approach 2025)
            val localeManager = context.getSystemService(LocaleManager::class.java)
            localeManager.applicationLocales = LocaleList.forLanguageTags(language.code)
        } else {
            // Android 12 and below - use AppCompatDelegate
            val localeList = LocaleListCompat.forLanguageTags(language.code)
            AppCompatDelegate.setApplicationLocales(localeList)
        }
    }
    
    /**
     * Check if language is supported
     */
    fun isLanguageSupported(languageCode: String): Boolean {
        return Language.values().any { it.code == languageCode }
    }
    
    /**
     * Get all supported languages
     */
    fun getSupportedLanguages(): List<Language> {
        return Language.values().toList()
    }
    
    /**
     * Get app language from system settings (best practices 2025)
     */
    private fun getSystemAppLanguage(): Language? {
        return try {
            val locale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // Android 13+ - use LocaleManager
                context.getSystemService(LocaleManager::class.java)
                    ?.applicationLocales
                    ?.get(0)
            } else {
                // Android 12 and below - use AppCompatDelegate
                AppCompatDelegate.getApplicationLocales().get(0)
            }
            
            locale?.let { Language.from(it.language) }
        } catch (e: Exception) {
            null
        }
    }
    
    companion object {
        private const val PREFS_NAME = "language_prefs"
        private const val KEY_LANGUAGE = "selected_language"
    }
}