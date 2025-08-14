package com.ukhvat.notes.domain.util

import androidx.annotation.StringRes

/**
 * Centralized Toast notification system
 * 
 * Interface for displaying system notifications to user.
 * Follows clean architecture principles and dependency injection.
 * 
 * Features:
 * - Type safety via @StringRes annotations
 * - Async safety via suspend functions
 * - Conditional logic via toastIf() methods
 * - Internationalization via string resources
 * - Koin DI integration for singleton instance
 * 
 * Usage in ViewModels:
 * ```kotlin
 * class SomeViewModel(
 *     private val toaster: Toaster
 * ) { // injected via Koin
 *     private fun showSuccess() = viewModelScope.launch {
 *         toaster.toast(R.string.operation_success)
 *     }
 * }
 * ```
 * 
 * @see ToasterImpl Concrete implementation via Android Toast
 * @see com.ukhvat.notes.di.KoinModule appModule for dependency injection
 */
interface Toaster {
    suspend fun toast(@StringRes textRes: Int)
    suspend fun toast(text: String)
    
    /**
     * Toast with formatted string and parameters
     * Used for messages like "Imported: %d notes"
     */
    suspend fun toast(@StringRes textRes: Int, vararg formatArgs: Any)
    
    suspend fun toastIf(
        condition: Boolean,
        @StringRes textRes: Int,
        block: () -> Unit = {}
    )
    
    suspend fun toastIf(
        condition: Boolean,
        text: String,
        block: () -> Unit = {}
    )
}