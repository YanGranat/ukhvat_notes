package com.ukhvat.notes.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.ukhvat.notes.R
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.material3.MaterialTheme
import com.ukhvat.notes.ui.theme.rememberGlobalColors

/**
 * Search bar with correct cursor positioning
 * 
      * Cursor issue handled:
 * ═══════════════════════════════════════════════
 * 
      * Issue: Cursor remained at line start when navigating between search screens
 * REASON: BasicTextField doesn't automatically position cursor on programmatic changes
 * Approach: Use TextFieldValue.selection for cursor position control
 * 
      * Technical solution:
 * - Stable BasicTextField (not experimental BasicTextField2)
 * - TextFieldValue with correct TextRange.selection for end positioning
 * - remember(query) recreates TextFieldValue with cursor at end on query change
 * - Full compatibility with all Compose versions
 * 
 * ARCHITECTURAL ADVANTAGES:
 * • No dependencies on experimental API (BasicTextField2, InputTransformation)
 * • Preserved all existing search architecture
 * • Works correctly in all contexts: NotesListScreen + NoteEditScreen
      * • Stable solution without instability risks
 */
@Composable
fun NoteSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClearSearch: () -> Unit,
    placeholder: String = "Search...", // Will be passed from stringResource
    matchCount: Int = 0,
    currentMatch: Int = 0,
    onPreviousMatch: () -> Unit = {},
    onNextMatch: () -> Unit = {},
    focusRequester: FocusRequester = FocusRequester()
) {
    // Cursor problem handling:
    // 
    // When navigating between search screens (main menu ↔ note editor) 
    // cursor should be at end of line, not at beginning.
    // User can move cursor during input
    //
    // TECHNICAL IMPLEMENTATION:
    // • Create local textFieldValue state for cursor control
    // • LaunchedEffect tracks external query changes and sets cursor to end
    // • User can freely move cursor within search bar
    // • On screen navigation cursor automatically moves to end
    //
    // ADVANTAGES:
                // • Cursor at end when navigating between screens (original issue handled)
    // • User can freely move cursor during editing
    // • Stable API without experimental dependencies
    var textFieldValue by remember { 
        mutableStateOf(TextFieldValue(text = query, selection = TextRange(query.length))) 
    }
    
    // External query synchronization: 
    // Update textFieldValue only when query changes externally (navigation between screens)
    // DON'T update on user changes (to not block cursor)
    LaunchedEffect(query) {
        if (textFieldValue.text != query) {
            // Query changed externally (navigation) → set cursor to end
            textFieldValue = TextFieldValue(
                text = query,
                selection = TextRange(query.length) // Cursor to end on navigation
            )
        }
    }
    
    // PERFORMANCE CRITICAL: Cached colors instead of expensive calls
    // Color caching for optimization
    val colors = rememberGlobalColors()

    // Auto-focus: request focus when component appears
    // Search bar autofocus
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = colors.searchBackground, // Cached color instead of expensive call
                shape = RoundedCornerShape(0.dp)
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // SEARCH FIELD: Main component for text input
        // Has own background inside blue area for better readability
        Box(
            modifier = Modifier
                .weight(1f)
                .background(
                    color = colors.searchFieldBackground, // Cached color instead of expensive call
                    shape = RoundedCornerShape(4.dp)
                )
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            // BasicTextField with stable TextFieldValue implementation
            //
            // Use stable BasicTextField instead of experimental BasicTextField2
            // BasicTextField2 is in experimental stage (Foundation 1.6.0-alpha)
            // and may contain bugs or change in future versions.
            //
            // UPDATE LOGIC:
            // 1. User types → onValueChange triggers
            // 2. If text changed → notify parent component via onQueryChange
            // 3. Parent component updates query
            // 4. remember(query) recreates TextFieldValue with cursor at end
            BasicTextField(
                value = textFieldValue,
                onValueChange = { newValue ->
                    // Update local state (allows free cursor movement)
                    textFieldValue = newValue
                    
                    // Notify parent component only about text changes
                    // Filter unnecessary calls to avoid infinite update loops
                    if (newValue.text != query) {
                        onQueryChange(newValue.text)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester), // Connect focus management
                textStyle = TextStyle(
                    color = colors.searchFieldText, // Cached text color
                    fontSize = 16.sp
                ),
                cursorBrush = SolidColor(colors.searchFieldText), // Cached cursor color
                singleLine = true, // Search bar always single line
                decorationBox = { innerTextField ->
                    // DECORATION BOX: Input field framing with placeholder
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        // Placeholder: Show hint when field is empty
                        if (textFieldValue.text.isEmpty()) {
                            Text(
                                                        text = placeholder, // "Search notes..." or "Search in note..."
                        color = colors.searchFieldText.copy(alpha = 0.5f), // Cached semi-transparent text
                                fontSize = 16.sp
                            )
                        }
                        // MAIN FIELD: Here displays actual user text
                        innerTextField()
                    }
                }
            )
        }

        // COUNTER AND NAVIGATION: Show only when there are search results
        // Displayed in note editor for navigation between found matches
        if (matchCount > 0) {
            Spacer(modifier = Modifier.width(8.dp))
            
            // Result counter: "1/5", "3/10" etc.
            Text(
                text = "$currentMatch/$matchCount",
                                    color = colors.searchIcon, // Cached icon color
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            
            Spacer(modifier = Modifier.width(4.dp))
            
            // "PREVIOUS" BUTTON: Go to previous found match
            IconButton(
                onClick = onPreviousMatch,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowUp,
                    contentDescription = stringResource(R.string.previous),
                                            tint = colors.searchIcon, // Cached icon color
                    modifier = Modifier.size(20.dp)
                )
            }
            
            // "NEXT" BUTTON: Go to next found match  
            IconButton(
                onClick = onNextMatch,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = stringResource(R.string.next),
                                            tint = colors.searchIcon, // Cached icon color
                    modifier = Modifier.size(20.dp)
                )
            }
        }

                        // Clear search and exit search mode
        IconButton(
            onClick = onClearSearch,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                                        contentDescription = stringResource(R.string.close_search_desc),
                                        tint = colors.searchIcon, // Cached icon color
                modifier = Modifier.size(20.dp)
            )
        }
    }
} 