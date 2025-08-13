package com.ukhvat.notes.data.util

import android.content.Context
import android.widget.Toast
import android.widget.TextView
import android.graphics.drawable.GradientDrawable
import android.graphics.Color
import android.util.TypedValue
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
        showCustomToast(context.getString(textRes))
    }
    
    override suspend fun toast(text: String) = withContext(Dispatchers.Main) {
        showCustomToast(text)
    }
    
    override suspend fun toast(@StringRes textRes: Int, vararg formatArgs: Any) = withContext(Dispatchers.Main) {
        val formattedText = context.getString(textRes, *formatArgs)
        showCustomToast(formattedText)
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

    private fun showCustomToast(message: String) {
        val density = context.resources.displayMetrics.density
        val paddingH = (16 * density).toInt()
        val paddingV = (10 * density).toInt()
        val radius = 8 * density

        val isNight = (context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
        val bgColor = if (isNight) Color.parseColor("#424242") else Color.parseColor("#323232")
        val textColor = Color.WHITE

        val bgDrawable = GradientDrawable().apply {
            cornerRadius = radius
            setColor(bgColor)
        }

        val tv = TextView(context).apply {
            text = message
            setTextColor(textColor)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setPadding(paddingH, paddingV, paddingH, paddingV)
            background = bgDrawable
        }

        val toast = Toast(context)
        toast.duration = Toast.LENGTH_SHORT
        toast.view = tv
        toast.show()
    }
}