package com.ukhvat.notes.ui.components

import androidx.compose.foundation.text.input.TextFieldState
// Removed problematic setTextAndSelection import
import androidx.compose.runtime.*
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Text management controller for search system
 * 
 * Provides cursor management API (setCursor, setSelection) required for in-note search.
 * Maintains single source of truth with TextFieldState for thread-safe operations.
 * 
 * Key features:
 * - Cursor preservation during text updates
 * - Thread-safe operations for background search
 * - Unified API preventing cursor management conflicts
 * 1. Missing API for setCursor() and setSelection()
 * 2. Search system requires programmatic cursor control
 * 3. Thread-safety needed for search from background threads
 * 4. SearchManager integrated with TextController methods
 */
@Stable
class TextController(
    initialText: String = "",
    initialSelection: TextRange = TextRange.Zero
) {
    
    // Single source of truth for text and cursor
    val textFieldState = TextFieldState(
        initialText = initialText,
        initialSelection = initialSelection
    )
    
    // Reactive state for subscribers
    private val _textValue = MutableStateFlow(TextFieldValue(initialText, initialSelection))
    val textValue: StateFlow<TextFieldValue> = _textValue.asStateFlow()
    
    // Edit journal for building precise diffs between versions
    fun snapshotAndClearEditJournal(): String? = null // Deprecated: use DiffJournalStore

    // Current cursor position (read-only)
    val currentCursorPosition: Int
        get() = textFieldState.selection.start
    
    /**
     * SMART TEXT UPDATE WITH CURSOR PRESERVATION
     * 
     * Solves setTextAndPlaceCursorAtEnd problem:
     * - If there's selection in newValue - use it
     * - If selection is empty - preserve current cursor position
     * - If text changed - check position safety
     */
    fun updateTextSmart(newValue: TextFieldValue) {
        // Cursor handling: Smart logic for cursor position determination
        // 
            // Issue: ViewModel creates TextFieldValue with outdated cursor position
            // Approach: Use current cursor position if ViewModel passed position 0
        val targetSelection = if (newValue.selection == TextRange.Zero) {
            // ViewModel doesn't know real cursor position - preserve current
            val currentPos = textFieldState.selection.start
            val safePos = currentPos.coerceIn(0, newValue.text.length)
            TextRange(safePos)
        } else {
            // ViewModel explicitly specified position (e.g., for search) - use it
            val safeStart = newValue.selection.start.coerceIn(0, newValue.text.length)
            val safeEnd = newValue.selection.end.coerceIn(safeStart, newValue.text.length)
            TextRange(safeStart, safeEnd)
        }
        
        // Update handling: Avoid replace(0, length) which resets cursor
        // 
            // Issue: replace(0, length, newText) replaces entire text and may reset cursor
            // Approach: Update only if text actually changed
        
        val currentText = textFieldState.text.toString()
        if (currentText != newValue.text) {
            // Text actually changed - need to update
            textFieldState.edit {
                replace(0, length, newValue.text)
                selection = targetSelection
            }
        } else {
            // Text didn't change - update only cursor if needed
            if (textFieldState.selection != targetSelection) {
                textFieldState.edit {
                    selection = targetSelection
                }
            }
        }
        
        // Update reactive state
        _textValue.value = TextFieldValue(newValue.text, targetSelection)
    }
    
    /**
     * FORCED CURSOR SETTING
     * Used for search and navigation
     */
    fun setCursor(position: Int) {
        val safePosition = position.coerceIn(0, textFieldState.text.length)
        val newSelection = TextRange(safePosition)
        
        textFieldState.edit {
            selection = newSelection
        }
        
        _textValue.value = TextFieldValue(
            text = textFieldState.text.toString(),
            selection = newSelection
        )
    }
    
    /**
     * TEXT SELECTION SETTING
     * Used for highlighting search results
     */
    fun setSelection(start: Int, end: Int) {
        val textLength = textFieldState.text.length
        val safeStart = start.coerceIn(0, textLength)
        val safeEnd = end.coerceIn(safeStart, textLength)
        val newSelection = TextRange(safeStart, safeEnd)
        
        textFieldState.edit {
            selection = newSelection
        }
        
        _textValue.value = TextFieldValue(
            text = textFieldState.text.toString(),
            selection = newSelection
        )
    }
    
    /**
     * GET CURRENT TextFieldValue
     * For compatibility with existing code
     */
    fun getCurrentTextFieldValue(): TextFieldValue {
        return TextFieldValue(
            text = textFieldState.text.toString(),
            selection = textFieldState.selection
        )
    }
    
    /**
     * SAFE SELECTION CLEARING
     * Used when exiting search mode
     */
    fun clearSelection() {
        val currentPos = textFieldState.selection.start
        setCursor(currentPos)
    }
}

/**
      * Cursor handling architecture
 * 
 * Creates TextController with proper state synchronization.
 * 
      * Issue (handled):
 * Previous remember(initialText, initialSelection) implementation recreated
 * TextController on every initialText change, which happened on every
 * user input and caused cursor reset to TextRange.Zero.
 * 
      * Architecture solution:
 * - TextController created ONCE
 * - LaunchedEffect synchronizes external changes (note switching, search)
 * - Smart logic distinguishes user input from external changes
 */
@Composable
fun rememberTextController(
    initialText: String = "",
    initialSelection: TextRange = TextRange.Zero
): TextController {
            // Architecture solution: Create TextController only once
    // 
            // Issue: remember(initialText, initialSelection) recreated TextController 
    // on every text change from user, resetting cursor to TextRange.Zero
    // 
    // Approach: Create only once, then synchronize via updateTextSmart()
    val textController = remember {
        TextController(initialText, initialSelection)
    }
    
    // Synchronize external changes (note switching, search) WITHOUT recreation
    LaunchedEffect(initialText, initialSelection) {
        val currentText = textController.textFieldState.text.toString()
        val currentSelection = textController.textFieldState.selection
        
        // Input handling: Ignore user input changes
        // 
        // Issue: InputTransformation calls updateContent() with TextRange.Zero during text input,
        // which leads to false LaunchedEffect triggering and cursor reset
        // 
        // Approach: Synchronize only real external changes:
        // • Note switching (text dramatically different)
        // • Search navigation (setting specific cursor position)
        // • DON'T synchronize if it's user input with TextRange.Zero
        
        val isUserInput = currentText == initialText && initialSelection == TextRange.Zero
        val isExternalChange = currentText != initialText || 
                              (initialSelection != TextRange.Zero && currentSelection != initialSelection)
                              
        if (isExternalChange && !isUserInput) {
            textController.updateTextSmart(TextFieldValue(initialText, initialSelection))
        }
    }
    
    return textController
}