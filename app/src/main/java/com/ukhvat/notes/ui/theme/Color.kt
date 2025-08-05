package com.ukhvat.notes.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

/**
 * PERFORMANCE CRITICAL: Global Color Bundle System
 * 
 * Issue: Expensive color function calls (textColor(), backgroundColor() etc) 
 * caused UI freezes during recomposition in all UI components.
 * 
 * Solution: Global cached color system through GlobalColorBundle.
 * All colors computed once per theme change and cached.
 * 
 * Architecture:
 * - GlobalColorBundle: data class with all app colors
 * - rememberGlobalColors(): Composable function for cached color access
 * - ColorManager: preserved for backward compatibility
 */

/**
 * Global cached color bundle for entire application
 * Eliminates expensive colorResource() calls during recomposition
 */
data class GlobalColorBundle(
    // Primary colors
    val primary: androidx.compose.ui.graphics.Color,
    val background: androidx.compose.ui.graphics.Color, 
    val text: androidx.compose.ui.graphics.Color,
    val textSecondary: androidx.compose.ui.graphics.Color,
    
    // Dialogs
    val dialogBackground: androidx.compose.ui.graphics.Color,
    val dialogText: androidx.compose.ui.graphics.Color,
    
    // Menu
    val menuBackground: androidx.compose.ui.graphics.Color,
    val menuText: androidx.compose.ui.graphics.Color,
    
    // Buttons and accents
    val buttonAccent: androidx.compose.ui.graphics.Color,
    val cardBackground: androidx.compose.ui.graphics.Color,
    val surface: androidx.compose.ui.graphics.Color,
    
    // Search
    val searchBackground: androidx.compose.ui.graphics.Color,
    val searchIcon: androidx.compose.ui.graphics.Color,
    val searchFieldBackground: androidx.compose.ui.graphics.Color,
    val searchFieldText: androidx.compose.ui.graphics.Color,
    
    // Derived colors for optimization
    val highlight: androidx.compose.ui.graphics.Color,
    val highlightBackground: androidx.compose.ui.graphics.Color,
    val selection: androidx.compose.ui.graphics.Color
)

/**
 * Performance critical: cached colors for entire application
 * Computed once per theme change and reused everywhere
 */
@androidx.compose.runtime.Composable
fun rememberGlobalColors(): GlobalColorBundle {
    return androidx.compose.runtime.remember(ColorManager.isCurrentlyDarkTheme) {
        val isDark = ColorManager.isCurrentlyDarkTheme
        val primary = androidx.compose.ui.graphics.Color(0xFF1c75d3)
        
        GlobalColorBundle(
            // Primary colors - constants for maximum performance
            primary = primary,
            background = if (isDark) androidx.compose.ui.graphics.Color(0xFF323231) else androidx.compose.ui.graphics.Color(0xFFFFFFFF),
            text = if (isDark) androidx.compose.ui.graphics.Color.White else androidx.compose.ui.graphics.Color.Black,
            textSecondary = if (isDark) androidx.compose.ui.graphics.Color(0xFFBDBDBD) else androidx.compose.ui.graphics.Color(0xFF757575),
            
            // Dialogs
            dialogBackground = if (isDark) androidx.compose.ui.graphics.Color(0xFF0F0F0F) else androidx.compose.ui.graphics.Color(0xFFFFFFFF),
            dialogText = if (isDark) androidx.compose.ui.graphics.Color.White else androidx.compose.ui.graphics.Color.Black,
            
            // Menu
            menuBackground = if (isDark) androidx.compose.ui.graphics.Color(0xFF272726) else androidx.compose.ui.graphics.Color(0xFFF5F5F5),  
            menuText = if (isDark) androidx.compose.ui.graphics.Color.White else androidx.compose.ui.graphics.Color.Black,
            
            // Buttons
            buttonAccent = primary,
            cardBackground = if (isDark) androidx.compose.ui.graphics.Color(0xFF272726) else androidx.compose.ui.graphics.Color(0xFFFFFFFF),
            surface = if (isDark) androidx.compose.ui.graphics.Color(0xFF272726) else androidx.compose.ui.graphics.Color(0xFFFFFFFF),
            
            // Search
            searchBackground = androidx.compose.ui.graphics.Color(0xFF2483e2),  // Always blue
            searchIcon = androidx.compose.ui.graphics.Color.White,  // Always white on blue
            searchFieldBackground = if (isDark) androidx.compose.ui.graphics.Color(0xFF323231) else androidx.compose.ui.graphics.Color(0xFFFFFFFF),
            searchFieldText = if (isDark) androidx.compose.ui.graphics.Color.White else androidx.compose.ui.graphics.Color.Black,
            
            // Derived colors
            highlight = primary,
            highlightBackground = primary.copy(alpha = 0.2f),
            selection = primary.copy(alpha = 0.1f)
        )
    }
}

/**
 * Legacy ColorManager preserved for system component compatibility
 * Main color system migrated to rememberGlobalColors()
 */
object ColorManager {
    // Use mutableStateOf for reactivity
    var isCurrentlyDarkTheme by mutableStateOf(true)
    
    /**
     * Set current theme for color resolution
     */
    fun setCurrentTheme(isDark: Boolean) {
        isCurrentlyDarkTheme = isDark
    }
    
    /**
     * Return current theme state
     */
    fun isDarkTheme(): Boolean {
        return isCurrentlyDarkTheme
    }
}

// Legacy Composable functions removed - use rememberGlobalColors() for all color access 