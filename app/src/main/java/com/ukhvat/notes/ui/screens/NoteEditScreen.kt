package com.ukhvat.notes.ui.screens

import android.content.Intent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.runtime.*



import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.ukhvat.notes.R
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.shape.RoundedCornerShape


import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime


import androidx.compose.ui.platform.LocalDensity
import org.koin.androidx.compose.koinViewModel
import com.ukhvat.notes.ui.components.NoteSearchBar
import com.ukhvat.notes.ui.components.TextController
import com.ukhvat.notes.ui.components.BasicTextField2Editor
import com.ukhvat.notes.ui.components.rememberTextController
import com.ukhvat.notes.ui.theme.*
import com.ukhvat.notes.ui.theme.rememberGlobalColors
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.TextStyle
import kotlinx.coroutines.delay





@Composable
fun NoteEditScreen(
    noteId: Long,
    navigateBack: (shouldClearMainSearch: Boolean, shouldScrollToTop: Boolean) -> Unit,
    navigateToVersionHistory: (Long) -> Unit = {},

    initialSearchQuery: String? = null,      // Search query from main search
    initialSearchPosition: Int? = null,      // Found text position
    viewModel: NoteEditViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val colors = rememberGlobalColors()

    // TextController creation: Moved to main function for noteId access
    // 
    // Architecture: TextController instead of MutableState
    // TextController required for search system, provides API:
    // - setCursor(position) to set cursor on found position
    // - setSelection(start, end) to highlight found text
    // - clearSelection() to clear selection when exiting search
    // MutableState lacks this API, replacement would cause functionality loss
    val textController = rememberTextController(
        initialText = uiState.content.text,
        initialSelection = uiState.content.selection
    )
    
    // TextController initialized once
    // Changes handled via InputTransformation

    // Load note when noteId changes
    LaunchedEffect(noteId) {
        viewModel.loadNote(noteId, initialSearchQuery, initialSearchPosition)
    }
    
    // Handle navigation back after deletion
    LaunchedEffect(uiState.shouldNavigateBack) {
        if (uiState.shouldNavigateBack) {
            navigateBack(uiState.shouldClearMainSearch, uiState.shouldScrollToTop)
            viewModel.onNavigationHandled()
        }
    }

    // Handle navigation to version history
    LaunchedEffect(uiState.navigateToVersionHistory, uiState.versionHistoryNoteId) {
        if (uiState.navigateToVersionHistory && uiState.versionHistoryNoteId > 0) {
            navigateToVersionHistory(uiState.versionHistoryNoteId)
            viewModel.onVersionHistoryNavigated()
        }
    }



         NoteEditContent(
         uiState = uiState,
         onEvent = viewModel::onEvent,
         onClearError = viewModel::clearError,
         onClearExport = viewModel::clearExport,
         onClearNoteInfo = viewModel::clearNoteInfo,
         navigateBack = navigateBack,
         snackbarHostState = snackbarHostState,
         initialSearchQuery = initialSearchQuery,
         textController = textController  // Pass TextController
     )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun NoteEditContent(
    uiState: NoteEditUiState,
    onEvent: (NoteEditEvent) -> Unit,
    onClearError: () -> Unit,
    onClearExport: () -> Unit,
    onClearNoteInfo: () -> Unit,
    navigateBack: (shouldClearMainSearch: Boolean, shouldScrollToTop: Boolean) -> Unit,
    snackbarHostState: SnackbarHostState,
    initialSearchQuery: String? = null,
    textController: TextController  // Receive TextController
) {
    val colors = rememberGlobalColors()
         var showMenu by remember { mutableStateOf(false) }
     var showDeleteDialog by remember { mutableStateOf(false) }
     

     
     // TextController passed as parameter for search and cursor control
    // MutableState alternative not suitable - lacks cursor control methods

            // Handle Back button to exit search mode
    BackHandler(enabled = uiState.isSearchMode) {
        onEvent(NoteEditEvent.ClearSearch)
    }
    
            // Handle Back button to exit note
    BackHandler(enabled = !uiState.isSearchMode) {
        onEvent(NoteEditEvent.NavigateBack)
    }



    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .systemBarsPadding()
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
                         // Top App Bar
          Box(
              modifier = Modifier
                  .fillMaxWidth()
                  .height(58.dp)
                  .clipToBounds()
          ) {
              TopAppBar(
                  title = { },
                navigationIcon = {
                    IconButton(onClick = { onEvent(NoteEditEvent.NavigateBack) }) {
                                                 Icon(
                             imageVector = Icons.Default.ArrowBack,
                             contentDescription = stringResource(R.string.back),
                             tint = Color.White
                         )
                    }
                },
                                                                                                   actions = {

                     
                     // Search button
                     IconButton(
                         onClick = { onEvent(NoteEditEvent.StartSearch) },
                         modifier = Modifier.size(40.dp)
                     ) {
                                                  Icon(
                              imageVector = Icons.Default.Search,
                              contentDescription = stringResource(R.string.search_in_note_desc),
                              tint = Color.White,
                              modifier = Modifier.size(24.dp)
                          )
                     }
                     
                      // Undo/redo buttons with built-in functionality
                     IconButton(
                         onClick = { textController.textFieldState.undoState.undo() },
                         enabled = textController.textFieldState.undoState.canUndo,
                         modifier = Modifier.size(40.dp)
                     ) {
                         Icon(
                             imageVector = Icons.Default.ArrowBack,
                             contentDescription = stringResource(R.string.undo),
                             tint = if (textController.textFieldState.undoState.canUndo) 
                                 Color.White 
                             else 
                                 Color.White.copy(alpha = 0.5f),
                             modifier = Modifier.size(20.dp)
                         )
                     }
                     
                     IconButton(
                         onClick = { textController.textFieldState.undoState.redo() },
                         enabled = textController.textFieldState.undoState.canRedo,
                         modifier = Modifier.size(40.dp)
                     ) {
                         Icon(
                             imageVector = Icons.Default.ArrowForward,
                             contentDescription = stringResource(R.string.redo),
                             tint = if (textController.textFieldState.undoState.canRedo) 
                                 Color.White 
                             else 
                                 Color.White.copy(alpha = 0.5f),
                             modifier = Modifier.size(20.dp)
                         )
                     }

                        // Save button
                        IconButton(
                            onClick = {
                                onEvent(NoteEditEvent.ForceSave)
                            },
                            modifier = Modifier.size(40.dp)
                        ) {
                                                         Icon(
                                 painter = painterResource(id = R.drawable.ic_diskette_save),
                                 contentDescription = stringResource(R.string.save),
                                 tint = Color.White,
                                 modifier = Modifier.size(20.dp)
                             )
                        }
                      // AI button and menu
                      var showAiMenu by remember { mutableStateOf(false) }
                      IconButton(
                          onClick = { showAiMenu = true },
                          modifier = Modifier.size(40.dp)
                      ) {
                          val tintColor = if (uiState.isAiBusy) Color.White.copy(alpha = 0.5f) else Color.White
                          Icon(
                              painter = painterResource(id = R.drawable.ic_ai),
                              contentDescription = stringResource(R.string.ai_menu),
                              tint = tintColor,
                              modifier = Modifier.size(20.dp)
                          )
                      }
                      DropdownMenu(
                          expanded = showAiMenu,
                          onDismissRequest = { showAiMenu = false },
                          tonalElevation = if (ColorManager.isDarkTheme()) 0.dp else 2.dp,
                          shadowElevation = if (ColorManager.isDarkTheme()) 0.dp else 4.dp,
                          shape = RoundedCornerShape(8.dp),
                          containerColor = colors.menuBackground
                      ) {
                          DropdownMenuItem(
                              text = { Text(stringResource(R.string.ai_fix_errors), color = colors.menuText) },
                              onClick = {
                                  showAiMenu = false
                                      val sel = textController.textFieldState.selection
                                      if (!sel.collapsed) {
                                          onEvent(NoteEditEvent.AiFixErrorsInRange(sel.start, sel.end))
                                      } else {
                                          onEvent(NoteEditEvent.AiFixErrors)
                                      }
                              }
                          )
                      }
                      // Menu button
                      IconButton(
                          onClick = { showMenu = true },
                          modifier = Modifier.size(40.dp)
                      ) {
                                                    Icon(
                               imageVector = Icons.Default.MoreVert,
                               contentDescription = stringResource(R.string.menu),
                               tint = Color.White,
                               modifier = Modifier.size(26.dp)
                           )
                      }
                                                                                                                                                                     DropdownMenu(
                           expanded = showMenu,
                                                       onDismissRequest = { showMenu = false },
                            tonalElevation = if (ColorManager.isDarkTheme()) 0.dp else 2.dp,
                            shadowElevation = if (ColorManager.isDarkTheme()) 0.dp else 4.dp,
                            shape = RoundedCornerShape(8.dp),
                            containerColor = colors.menuBackground
                     ) {

                         
                         DropdownMenuItem(
                                                           text = { 
                                  Row(verticalAlignment = Alignment.CenterVertically) {
                                      Icon(
                                          imageVector = Icons.Default.Info,
                                          contentDescription = stringResource(R.string.note_info_title),
                                          tint = colors.menuText,
                                          modifier = Modifier.size(18.dp)
                                      )
                                      Spacer(modifier = Modifier.width(8.dp))
                                                                            Text(
                                           stringResource(R.string.note_info_title),
                                           color = colors.menuText
                                       )
                                 }
                             },
                             onClick = {
                                 showMenu = false
                                 onEvent(NoteEditEvent.ShowNoteInfo)
                             }
                         )
                          // Favorites right after "Информация о заметке"
                          DropdownMenuItem(
                              text = { 
                                  Row(verticalAlignment = Alignment.CenterVertically) {
                                      Icon(
                                          painter = painterResource(id = R.drawable.ic_favorite),
                                          contentDescription = stringResource(if (uiState.isFavorite) R.string.remove_from_favorites else R.string.add_to_favorites),
                                          tint = colors.menuText,
                                          modifier = Modifier.size(18.dp)
                                      )
                                      Spacer(modifier = Modifier.width(8.dp))
                                      Text(
                                          stringResource(if (uiState.isFavorite) R.string.remove_from_favorites else R.string.add_to_favorites),
                                          color = colors.menuText
                                      )
                                  }
                              },
                              onClick = {
                                  showMenu = false
                                  onEvent(NoteEditEvent.ToggleFavorite)
                              }
                          )
                          DropdownMenuItem(
                              text = { 
                                  Row(verticalAlignment = Alignment.CenterVertically) {
                                      Icon(
                                          painter = painterResource(id = R.drawable.ic_history_versions),
                                          contentDescription = stringResource(R.string.version_history_with_param),
                                          tint = colors.menuText,
                                          modifier = Modifier.size(18.dp)
                                      )
                                      Spacer(modifier = Modifier.width(8.dp))
                                      Text(
                                          stringResource(R.string.version_history_with_param),
                                          color = colors.menuText
                                      )
                                  }
                              },
                              onClick = {
                                  showMenu = false
                                  onEvent(NoteEditEvent.ShowVersionHistory)
                              }
                          )
                          // Export note (should be before Archive)
                          DropdownMenuItem(
                               text = { 
                                   Row(verticalAlignment = Alignment.CenterVertically) {
                                       Icon(
                                           imageVector = Icons.Default.Share,
                                           contentDescription = stringResource(R.string.export_plain_text_sharing),
                                           tint = colors.menuText,
                                           modifier = Modifier.size(18.dp)
                                       )
                                       Spacer(modifier = Modifier.width(8.dp))
                                       Text(
                                           stringResource(R.string.export_plain_text_sharing),
                                           color = colors.menuText
                                       )
                                  }
                              },
                              onClick = {
                                  showMenu = false
                                  onEvent(NoteEditEvent.ExportNote)
                              }
                          )
                          // Archive
                          DropdownMenuItem(
                              text = {
                                  Row(verticalAlignment = Alignment.CenterVertically) {
                                      Icon(
                                          painter = painterResource(id = R.drawable.ic_archive),
                                          contentDescription = stringResource(R.string.move_to_archive),
                                          tint = colors.menuText,
                                          modifier = Modifier.size(18.dp)
                                      )
                                      Spacer(modifier = Modifier.width(8.dp))
                                      Text(stringResource(R.string.move_to_archive), color = colors.menuText)
                                  }
                              },
                              onClick = {
                                  showMenu = false
                                  onEvent(NoteEditEvent.MoveToArchive)
                              }
                          )
                                                   DropdownMenuItem(
                              text = { 
                                  Row(verticalAlignment = Alignment.CenterVertically) {
                                                                             Icon(
                                           imageVector = Icons.Default.Delete,
                                           contentDescription = stringResource(R.string.delete_note),
                                           tint = colors.menuText,
                                           modifier = Modifier.size(18.dp)
                                       )
                                       Spacer(modifier = Modifier.width(8.dp))
                                                                             Text(
                                            stringResource(R.string.delete_note),
                                            color = colors.menuText
                                        )
                                 }
                             },
                             onClick = {
                                 showMenu = false
                                 showDeleteDialog = true
                             }

                         )
                    }
                },
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               colors = TopAppBarDefaults.topAppBarColors(
                          containerColor = Color(0xFF1c75d3)
                      )
                         )
          }

                                                       // Search bar
                 if (uiState.isSearchMode) {
                   NoteSearchBar(
                       query = uiState.searchQuery,
                       onQueryChange = { query -> onEvent(NoteEditEvent.SearchQueryChanged(query)) },
                       onClearSearch = { onEvent(NoteEditEvent.ClearSearch) },
                       placeholder = stringResource(R.string.search_in_note_placeholder),
                       matchCount = uiState.searchMatches.size,
                       currentMatch = if (uiState.currentMatchIndex >= 0) uiState.currentMatchIndex + 1 else 0,
       
                       onNextMatch = { onEvent(NoteEditEvent.NextSearchMatch) },
                       onPreviousMatch = { onEvent(NoteEditEvent.PreviousSearchMatch) }
                   )
               }

             when {
                uiState.isLoading || uiState.isWaitingForSearchPositioning -> {
                    // Hide content while loading or positioning search
                    // 
                    // Prevent screen jumps during loading:
                    // ════════════════════════════════════════════════════════
                    // This prevents visual jumps when transitioning from note search 
                    // to in-note search. User does NOT see intermediate states:
                    // Hide content during search positioning to prevent visual jumps
                                            // Transparent placeholder ensures proper positioning
                    //
                    // isWaitingForSearchPositioning = true set in loadNote()
                    // isWaitingForSearchPositioning = false after initializeSearchFromNavigationSync()
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        // Transparent placeholder for smooth UX
                        // No loading indicator to make transition instant
                    }
                }
                else -> {
                                // Show content when note loaded and search positioned
                            // UI initialized
                    BasicTextField2Editor(
                        textController = textController,
                        onValueChange = { newContent ->
                            onEvent(NoteEditEvent.ContentChanged(newContent))
                        },
                        modifier = Modifier
                            .padding(
                                horizontal = 16.dp,
                                vertical = 12.dp
                            )
                            .imePadding(), // Automatic keyboard padding
                        searchMatches = uiState.searchMatches,
                        currentMatchIndex = uiState.currentMatchIndex,
                        searchQuery = uiState.searchQuery,
                        isSearchMode = uiState.isSearchMode,
        

                    )
                }
            }
        }

        // Snackbar Host for error messages
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    // Error handling with Snackbar
    uiState.error?.let { error ->
        LaunchedEffect(error) {
            snackbarHostState.showSnackbar(
                message = error,
                duration = SnackbarDuration.Short
            )
            onClearError()
        }
    }

    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            containerColor = colors.dialogBackground,
            shape = RoundedCornerShape(12.dp),
            title = { Text(stringResource(R.string.delete_note_question), color = colors.dialogText) },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.cancel), color = colors.dialogText)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onEvent(NoteEditEvent.DeleteNote)
                    }
                ) {
                    Text(stringResource(R.string.delete), color = colors.buttonAccent)
                }
            }
        )
    }

    // Export dialog for single note
    uiState.exportContent?.let { content ->
        val context = LocalContext.current
        LaunchedEffect(content) {
            val intent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, content)
                type = "text/plain"
            }
            context.startActivity(Intent.createChooser(intent, context.getString(R.string.export_plain_text_sharing)))
            onClearExport()
        }
    }

    // Note info dialog
    if (uiState.showNoteInfo && uiState.noteInfo != null) {
        NoteInfoDialog(
            noteInfo = uiState.noteInfo,
            onDismiss = onClearNoteInfo
        )
    }
}

@Composable
private fun NoteInfoDialog(
    noteInfo: NoteInfo,
    onDismiss: () -> Unit
) {
    val colors = rememberGlobalColors()
    val createdDate = remember(noteInfo.createdAt) {
        java.text.SimpleDateFormat("dd.MM.yyyy HH:mm", java.util.Locale.getDefault())
            .format(java.util.Date(noteInfo.createdAt))
    }
    
    val updatedDate = remember(noteInfo.updatedAt) {
        java.text.SimpleDateFormat("dd.MM.yyyy HH:mm", java.util.Locale.getDefault())
            .format(java.util.Date(noteInfo.updatedAt))
    }

                                     AlertDialog(
           onDismissRequest = onDismiss,
           containerColor = colors.dialogBackground,
          title = { 
                            Text(
                   stringResource(R.string.note_info_title),
                   fontSize = 18.sp,
                   fontWeight = FontWeight.Medium,
                   color = colors.dialogText
               ) 
          },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                            InfoRow(stringResource(R.string.created), createdDate, colors)
            InfoRow(stringResource(R.string.modified), updatedDate, colors)
            InfoRow(stringResource(R.string.characters), noteInfo.characterCount.toString(), colors)
            InfoRow(stringResource(R.string.words), noteInfo.wordCount.toString(), colors)
            }
        },
                                                                       confirmButton = {
               TextButton(onClick = onDismiss) {
                   Text(stringResource(R.string.close), color = colors.buttonAccent)
               }
          }
    )
}

 @Composable
private fun InfoRow(label: String, value: String, colors: GlobalColorBundle) {
    Text(
        text = "$label: $value",
        color = colors.dialogText,
        fontSize = 16.sp,
        modifier = Modifier.fillMaxWidth()
    )
}






 
 