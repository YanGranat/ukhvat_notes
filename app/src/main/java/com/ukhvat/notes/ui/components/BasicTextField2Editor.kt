package com.ukhvat.notes.ui.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldBuffer
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.MaterialTheme
import com.ukhvat.notes.ui.screens.SearchMatch
import com.ukhvat.notes.ui.theme.rememberGlobalColors
import kotlinx.coroutines.delay
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController

/**
 * CLEAN TEXT EDITOR WITHOUT RACE CONDITIONS
 * 
 * New architecture eliminates all problems of the old system:
 * 
 * ELIMINATED PROBLEMS:
  * Removed: TextFieldStateAdapter with setTextAndPlaceCursorAtEnd
 * Fixed: Triple cursor management - now only TextController
 * Fixed: Synchronization cycles through LaunchedEffect
 * Fixed: Direct textFieldState.edit calls - replaced with TextController methods
 * Fixed: Conflicts between autosave and search - separated jobs
 * 
 * NEW ARCHITECTURE:
 * Single Source of Truth - only TextController
 * Cursor Preservation - position preserved on all updates
 * Unidirectional Data Flow - clear data flow without cycles
 * No Race Conditions - separated responsibility areas
 * 
 * DATA FLOW:
 * User Input → InputTransformation → onValueChange → ViewModel (preserves cursor)
 * ViewModel → TextController.updateTextSmart → UI (cursor preserved)
 * Search → TextController.setSelection → Highlight (direct control)
 */
@Composable
fun BasicTextField2Editor(
    textController: TextController,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    searchMatches: List<SearchMatch> = emptyList(),
    currentMatchIndex: Int = -1,
    searchQuery: String = "",
    isSearchMode: Boolean = false,


) {
    val focusRequester = remember { FocusRequester() }
    
    // PERFORMANCE CRITICAL: Use cached colors instead of expensive color function calls
    // Solution: colors.text from GlobalColorBundle cached once per theme change
    // Solution: rememberGlobalColors() caches all colors and updates only on theme change
    val colors = rememberGlobalColors()
    val finalTextColor = colors.text
    
    val brush = SolidColor(value = finalTextColor)
    
    // ScrollState for programmatic scrolling
    val scrollState = rememberScrollState()
    
    // State for storing TextLayoutResult
    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

    // Clean solution: InputTransformation without side effects
    // Notify ViewModel of changes, but DON'T interfere with cursor control
    val userChangeTransformation = remember(onValueChange) {
        InputTransformation {
            // Notify ViewModel ONLY about user changes
            val newText = this.asCharSequence().toString()
            onValueChange(newText)
            // DON'T block changes - just notify ViewModel
        }
    }

    // Faster autofocus: request focus immediately and explicitly show keyboard
    val keyboardController = LocalSoftwareKeyboardController.current
    LaunchedEffect(Unit) {
        // No autofocus in search mode - user is browsing, not editing
        if (isSearchMode && searchQuery.isNotEmpty()) return@LaunchedEffect
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    // Ensure the last visual line (cursor at end) stays above the keyboard on first open
    // Trigger only when IME becomes visible and we are not in search mode
    val density = LocalDensity.current
    val imeBottomPx = WindowInsets.ime.getBottom(density)
    var didImeAdjust by remember { mutableStateOf(false) }
    LaunchedEffect(imeBottomPx, isSearchMode) {
        if (isSearchMode) return@LaunchedEffect
        if (imeBottomPx > 0 && !didImeAdjust) {
            // If cursor is at the end or near it, scroll to bottom so last line is above IME
            val selEnd = textController.textFieldState.selection.end
            val totalLen = textController.textFieldState.text.length
            if (totalLen == 0 || selEnd >= totalLen - 1) {
                // Small delay to allow layout to include imePadding in measurements
                delay(30)
                scrollState.scrollTo(scrollState.maxValue)
                didImeAdjust = true
            }
        }
        if (imeBottomPx == 0) {
            didImeAdjust = false
        }
    }

            // Handle search matches 

    LaunchedEffect(searchQuery, currentMatchIndex, searchMatches, textLayoutResult, isSearchMode) {
        // Protection: search logic only works in search mode
        if (!isSearchMode) return@LaunchedEffect
        
        if (searchQuery.isEmpty()) {
            // Clear selection via TextController
            textController.clearSelection()
            return@LaunchedEffect
        }
        
        if (currentMatchIndex >= 0 && currentMatchIndex < searchMatches.size) {
            val match = searchMatches[currentMatchIndex]
            
            // Set selection through TextController (safe)
            textController.setSelection(match.startIndex, match.endIndex)
            
            // Optimization: Use current TextLayoutResult without waiting cycles
            // Before: 20×25ms = 500ms delay cycle. Now: instantly
            val layoutResult = textLayoutResult
            
            if (layoutResult != null) {
                
                val lineIndex = layoutResult.getLineForOffset(match.startIndex)
                val lineTop = layoutResult.getLineTop(lineIndex)
                
                // Correct positioning without keyboard compensation
                //
                        // Positioning without keyboard compensation
        // imePadding() automatically raises content
                //
                        // LOGIC:
        // 1. Position: lineTop - topMargin (200dp top margin)
        // 2. Keyboard appears: imePadding() raises content automatically
        // 3. Result: text visible in correct position
        val topMargin = 200 // Top margin for visibility
                val targetScrollY = lineTop.toInt() - topMargin
                
                // Instant scroll without animation to prevent jumps
                scrollState.scrollTo(maxOf(0, targetScrollY))
            }
        } else if (searchMatches.isEmpty() && searchQuery.isNotEmpty()) {
            // Clear selection when no matches via TextController
            textController.clearSelection()
        }
    }

    BasicTextField(
        state = textController.textFieldState,
        inputTransformation = userChangeTransformation,
        textStyle = TextStyle(
            fontSize = MaterialTheme.typography.bodyLarge.fontSize,
            color = finalTextColor
        ),
        cursorBrush = brush,
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Sentences
        ),
        scrollState = scrollState,
        onTextLayout = { getResult ->
            // Save TextLayoutResult for search usage
            textLayoutResult = getResult()
        },
        modifier = modifier
            .fillMaxSize()
            .focusRequester(focusRequester)
    )
} 