package com.ukhvat.notes.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.ukhvat.notes.ui.screens.NotesListViewModel

/**
 * Component for application theme management
 * Supports three variants: light, dark, system
 */
@Composable
fun ThemedUkhvat(
    viewModel: NotesListViewModel,
    content: @Composable (Boolean) -> Unit
) {
    val isSystemInDarkTheme = isSystemInDarkTheme()
    
    // Use ViewModel state directly for reactivity
    val uiState by viewModel.uiState.collectAsState()
    val themePreference = uiState.currentTheme
    
    // Determine final theme based on user settings
    val isDarkTheme = when (themePreference) {
        ThemePreference.LIGHT -> false
        ThemePreference.DARK -> true
        ThemePreference.SYSTEM -> isSystemInDarkTheme
    }
    
    // Update theme in ColorManager on change
    LaunchedEffect(isDarkTheme, themePreference, isSystemInDarkTheme) {
        ColorManager.setCurrentTheme(isDarkTheme)
    }
    
            UkhvatTheme(darkTheme = isDarkTheme) {
        content(isDarkTheme)
    }
} 