package com.ukhvat.notes.domain.model

import androidx.annotation.StringRes
import com.ukhvat.notes.R

/**
 * Supported application languages
 */
enum class Language(
    val code: String,
    @StringRes val displayNameRes: Int
) {
    RUSSIAN("ru", R.string.language_russian),
    ENGLISH("en", R.string.language_english);
    
    companion object {
        /**
         * Get language by code, defaults to Russian
         */
        fun from(code: String): Language {
            return values().find { it.code == code } ?: RUSSIAN
        }
        
        /**
         * Default language
         */
        val DEFAULT = RUSSIAN
    }
}