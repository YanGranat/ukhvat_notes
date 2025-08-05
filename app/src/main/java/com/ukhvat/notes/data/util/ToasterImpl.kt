package com.ukhvat.notes.data.util

import android.content.Context
import android.widget.Toast
import androidx.annotation.StringRes
import com.ukhvat.notes.domain.util.Toaster
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
// Removed Hilt imports for Koin migration

/**
 * TOAST NOTIFICATION IMPLEMENTATION BASED ON ANDROID TOAST
 * 
 * Concrete implementation of Toaster interface using standard Android Toast.
 * Ensures safe work with main UI thread via coroutines.
 * 
 * Architectural decisions:
 * - @Singleton: single instance for entire app
 * - @ApplicationContext: safe context, not tied to Activity
 * - withContext(Dispatchers.Main): guarantee execution on UI thread
 * - Toast.LENGTH_SHORT: brief notifications for better UX
 * 
 * Koin DI integration:
 * - Automatic injection via Koin DI
 * - Interface binding via KoinModule
 * 
 * Usage:
 * - All Toast operations automatically switch to Main thread
 * - Supports both string resources and direct strings
 * - Conditional logic via toastIf() for validation
 */
/**
 * MIGRATED FROM HILT TO KOIN
 * 
 * Dependencies injected via Koin instead of @Inject constructor:
 * - Context: for Toast creation
 */
class ToasterImpl(
    private val context: Context
) : Toaster {
    
    override suspend fun toast(@StringRes textRes: Int) = withContext(Dispatchers.Main) {
        Toast.makeText(context, context.getString(textRes), Toast.LENGTH_SHORT).show()
    }
    
    override suspend fun toast(text: String) = withContext(Dispatchers.Main) {
        Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
    }
    
    override suspend fun toast(@StringRes textRes: Int, vararg formatArgs: Any) = withContext(Dispatchers.Main) {
        val formattedText = context.getString(textRes, *formatArgs)
        Toast.makeText(context, formattedText, Toast.LENGTH_SHORT).show()
    }
    
    override suspend fun toastIf(
        condition: Boolean,
        @StringRes textRes: Int,
        block: () -> Unit
    ) = if (condition) toast(textRes) else block()
    
    override suspend fun toastIf(
        condition: Boolean,
        text: String,
        block: () -> Unit
    ) = if (condition) toast(text) else block()
}