package com.ukhvat.notes.ui.theme

/**
 * Application constants for improved code readability and maintainability.
 * 
 * Contains global UI parameters, business rules, and configuration values
 * that are used across multiple components.
 */
object UiConstants {
    const val NEW_NOTE_ID = 0L
}

object TextLimits {
    const val TITLE_FROM_CONTENT_LENGTH = 50
}

object Animations {
    const val DEBOUNCE_DELAY_MS = 500L
}



object Dimensions {
    const val TOP_APP_BAR_HEIGHT_DP = 54  // Standard height 56dp, reduced by ~5%
}

object ThemePreferences {
    const val THEME_PREFERENCE_KEY = "theme_preference"
    const val THEME_DARK = "dark"
    const val THEME_LIGHT = "light"
} 