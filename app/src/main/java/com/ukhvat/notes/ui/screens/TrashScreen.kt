package com.ukhvat.notes.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.ukhvat.notes.R
import org.koin.androidx.compose.koinViewModel
import com.ukhvat.notes.domain.model.Note
import com.ukhvat.notes.ui.theme.*
import com.ukhvat.notes.ui.theme.rememberGlobalColors

/**
 * Trash screen
 * 
 * Displays list of deleted notes with viewing, restoration capabilities
 * or permanent deletion. Design based on VersionHistoryScreen for consistency.
 * 
 * Architectural features:
 * - Soft delete pattern with isDeleted + deletedAt flags
 * - Dialogs with adaptive colors (dark/light theme)
 * - Batch operations for mass deletion performance
 * - Auto-deletion after 30 days on app launch
 * 
 * UI components:
 * - TrashScreen: main screen with Scaffold
 * - DeletedNoteItem: note cards (date at top → content → buttons)
 * - AlertDialog: note preview (like VersionPreviewDialog)
 * - AlertDialog: auto-deletion info after 30 days
 * - AlertDialog: trash clearing confirmation
 * 
 * @param onNavigateBack Callback for returning to main menu
 * @param viewModel TrashViewModel with reactive StateFlow for state management
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashScreen(
    onNavigateBack: () -> Unit,
    viewModel: TrashViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showPreviewDialog by remember { mutableStateOf<Note?>(null) }
    var showInfoDialog by remember { mutableStateOf(false) }
    val colors = rememberGlobalColors()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = stringResource(R.string.trash_screen_title),
                        color = Color.White
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    // Auto-deletion info button
                    IconButton(
                        onClick = { showInfoDialog = true }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = stringResource(R.string.trash_info_desc),
                            tint = Color.White
                        )
                    }
                    
                    // Clear trash button (shown only if notes exist)
                    if (uiState.deletedNotes.isNotEmpty()) {
                        IconButton(
                            onClick = { viewModel.onEvent(TrashEvent.ShowClearTrashDialog) }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = stringResource(R.string.clear_trash_desc),
                                tint = Color.White
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1c75d3)
                )
            )
        },
        containerColor = colors.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> {
                    // No loading indicator - conditional display is more efficient than transparent indicator
                    // GPU optimization: avoid rendering transparent elements
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .align(Alignment.Center)
                    )
                }
                
                uiState.deletedNotes.isEmpty() -> {
                    EmptyTrashMessage(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(uiState.deletedNotes) { note ->
                            DeletedNoteItem(
                                note = note,
                                onNoteClick = { showPreviewDialog = note },
                                onRestore = { 
                                    viewModel.onEvent(TrashEvent.RestoreNote(note.id)) 
                                },
                                onPermanentDelete = { 
                                    viewModel.onEvent(TrashEvent.PermanentlyDeleteNote(note.id)) 
                                }
                            )
                        }
                    }
                }
            }
        }
    }
    
    // Deleted note preview dialog
    // Uses same color scheme as VersionPreviewDialog for design consistency
    showPreviewDialog?.let { note ->
        AlertDialog(
            onDismissRequest = { showPreviewDialog = null },
            containerColor = colors.dialogBackground,
            shape = RoundedCornerShape(12.dp),
            title = { 
                Text(
                    text = stringResource(R.string.deleted_note_title),
                    color = colors.menuText,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
            },
            text = {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 400.dp)
                ) {
                    item {
                        Text(
                            text = if (note.content.isNotEmpty()) note.content else stringResource(R.string.note_is_empty),
                            color = colors.text,
                            fontSize = 14.sp,
                            lineHeight = 20.sp
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showPreviewDialog = null },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = colors.text
                    )
                ) {
                    Text(stringResource(R.string.close))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        viewModel.onEvent(TrashEvent.RestoreNote(note.id))
                        showPreviewDialog = null
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = colors.primary
                    )
                ) {
                    Text(stringResource(R.string.restore))
                }
            }
        )
    }
    
            // Auto-deletion info dialog
        // Explains 30-day auto-cleanup rule to user
    if (showInfoDialog) {
        AlertDialog(
            onDismissRequest = { showInfoDialog = false },
            containerColor = colors.dialogBackground,
            shape = RoundedCornerShape(12.dp),
            title = {
                Text(
                    text = stringResource(R.string.trash_info_title),
                    color = colors.menuText,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.deleted_notes_info),
                    color = colors.text,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { showInfoDialog = false },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = colors.primary
                    )
                ) {
                    Text(stringResource(R.string.understand))
                }
            }
        )
    }
    
            // Trash cleanup confirmation dialog
        // Shows note count and warns about irreversible operation
    if (uiState.showClearTrashDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.onEvent(TrashEvent.DismissClearTrashDialog) },
            containerColor = colors.dialogBackground,
            shape = RoundedCornerShape(12.dp),
            title = {
                Text(
                    text = stringResource(R.string.clear_trash_title),
                    color = colors.menuText,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
            },
            text = {
                val noteCount = uiState.deletedNotes.size
                Text(
                    text = if (noteCount == 1) {
                        stringResource(R.string.clear_trash_confirm_single)
                    } else {
                        stringResource(R.string.clear_trash_confirm, noteCount)
                    },
                    color = colors.text,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.onEvent(TrashEvent.ClearTrash) },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Color(0xFFD32F2F)
                    )
                ) {
                    Text(
                        text = stringResource(R.string.clear),
                        fontWeight = FontWeight.Medium
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.onEvent(TrashEvent.DismissClearTrashDialog) },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = colors.primary
                    )
                ) {
                    Text(
                        text = stringResource(R.string.cancel), 
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        )
    }
}

/**
 * Deleted note card
 * 
 * Displays deleted note as card with information and action buttons.
 * Card structure (per user request):
 * 1. Deletion date (at top, gray color)
 * 2. Note content (first 100 characters, up to 3 lines)
 * 3. "Restore" and "Delete" buttons (on right)
 * 
 * Color scheme:
 * - Card background: colors.cardBackground (adaptive for theme)
 * - Deletion date: colors.textSecondary (gray)
 * - Content: colors.text (main text color for theme)
 * - "Restore": colors.primary (blue #1c75d3)
 * - "Delete": red #D32F2F (dangerous action)
 * 
 * @param note Deleted note to display
 * @param onNoteClick Callback for opening note preview dialog
 * @param onRestore Callback for restoring note from trash
 * @param onPermanentDelete Callback for permanent note deletion
 */
@Composable
private fun DeletedNoteItem(
    note: Note,
    onNoteClick: () -> Unit,
    onRestore: () -> Unit,
    onPermanentDelete: () -> Unit
) {
    val colors = rememberGlobalColors()
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onNoteClick() },
        colors = CardDefaults.cardColors(
            containerColor = colors.cardBackground
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
                 Column(
             modifier = Modifier.padding(16.dp),
             verticalArrangement = Arrangement.spacedBy(8.dp)
         ) {
                         // Deletion date (top placement per user request)
            // Shows when note was deleted in readable format
             Text(
                 text = stringResource(R.string.deleted_date, note.formattedDeletedDate ?: stringResource(R.string.unknown)),
                 color = colors.textSecondary,
                 fontSize = 12.sp
             )
             
                         // Note content preview
            // Shows first 100 characters for quick note identification
             Text(
                 text = note.content.take(100).ifEmpty { stringResource(R.string.note_is_empty) },
                 color = colors.text,
                 fontSize = 14.sp,
                 lineHeight = 20.sp,
                 maxLines = 3,
                 overflow = TextOverflow.Ellipsis
             )
            
                                     // Action buttons (style similar to VersionHistoryScreen)
            // Design: OutlinedButton with border, compact size, smaller font
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Permanent delete button (dangerous action)
                OutlinedButton(
                    onClick = onPermanentDelete,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFFC62828)  // Less bright red like in VersionHistoryScreen
                    ),
                    border = BorderStroke(1.dp, Color(0xFFC62828)),
                    modifier = Modifier.height(32.dp),  // Compact height like version buttons
                    contentPadding = PaddingValues(
                        horizontal = 8.dp,
                        vertical = 6.dp
                    )
                ) {
                    Text(
                        text = stringResource(R.string.delete),
                        fontSize = 11.sp,  // Small font like in VersionHistoryScreen
                        textAlign = TextAlign.Center
                    )
                }
                
                // Restore button (primary action)
                OutlinedButton(
                    onClick = onRestore,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = colors.primary  // Blue color like version buttons
                    ),
                    border = BorderStroke(1.dp, colors.primary),
                    modifier = Modifier.height(32.dp),  // Compact height
                    contentPadding = PaddingValues(
                        horizontal = 12.dp,
                        vertical = 6.dp
                    )
                ) {
                    Text(
                        text = stringResource(R.string.restore),
                        fontSize = 11.sp,  // Small font for consistency
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

/**
 * Empty trash state
 * 
 * Displayed when trash has no deleted notes.
 * Friendly message with emoji and explanatory text.
 * Design is centered and uses typography hierarchy.
 */
@Composable
private fun EmptyTrashMessage(modifier: Modifier = Modifier) {
    val colors = rememberGlobalColors()
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "🗑️",
            fontSize = 48.sp
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
                            text = stringResource(R.string.trash_empty),
                                color = colors.text,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
                            text = stringResource(R.string.deleted_notes_shown_here),
            color = colors.textSecondary,
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )
    }
}