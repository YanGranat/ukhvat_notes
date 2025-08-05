package com.ukhvat.notes.ui.theme

/**
 * Application theme preferences
 */
enum class ThemePreference(val displayName: String) {
    LIGHT("Light"),
    DARK("Dark"), 
    SYSTEM("System");
    
    companion object {
        fun fromOrdinal(ordinal: Int): ThemePreference {
            return values().getOrElse(ordinal) { SYSTEM }
        }
    }
} 