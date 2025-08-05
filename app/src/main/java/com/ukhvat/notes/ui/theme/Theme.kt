package com.ukhvat.notes.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
            primary = Color(0xFF1c75d3),           // primary from DarkTheme
    onPrimary = Color.White,
            background = Color(0xFF323231),        // mainBackground from DarkTheme  
            onBackground = Color(0xFFFAFAFA),      // noteTextColor from DarkTheme
            surface = Color(0xFF272726),           // surface from DarkTheme
            onSurface = Color(0xFFFAFAFA),         // noteTextColor from DarkTheme
            onSurfaceVariant = Color(0xFFEEEEEE)   // noteTextVariant from DarkTheme
)

private val LightColorScheme = lightColorScheme(
            primary = Color(0xFF1c75d3),           // primary from LightTheme
    onPrimary = Color.White,
            background = Color(0xFFFFFFFF),        // mainBackground from LightTheme
            onBackground = Color(0xFF000000),      // noteTextColor from LightTheme
            surface = Color(0xFFF5F5F5),           // surface from LightTheme  
            onSurface = Color(0xFF000000),         // noteTextColor from LightTheme
            onSurfaceVariant = Color(0xFF606060)   // noteTextVariant from LightTheme
)

@Composable
fun UkhvatTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
} 