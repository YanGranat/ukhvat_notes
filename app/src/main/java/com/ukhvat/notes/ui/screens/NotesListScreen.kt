package com.ukhvat.notes.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.material3.LocalMinimumInteractiveComponentEnforcement
import androidx.activity.compose.BackHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.ukhvat.notes.ui.components.ThemeIconButton
import com.ukhvat.notes.ui.components.ThemeSelectionDialog
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.ukhvat.notes.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.koin.androidx.compose.koinViewModel
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.systemBarsPadding
import kotlinx.coroutines.delay
import com.ukhvat.notes.domain.model.Note
import com.ukhvat.notes.ui.components.NoteSearchBar

import com.ukhvat.notes.ui.theme.Dimensions.TOP_APP_BAR_HEIGHT_DP
import com.ukhvat.notes.ui.theme.*
import com.ukhvat.notes.ui.theme.ColorManager
import com.ukhvat.notes.ui.theme.rememberGlobalColors
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import com.ukhvat.notes.data.datasource.SearchResultInfo
import com.ukhvat.notes.data.datasource.SearchMatchInfo



@Composable
fun NotesListScreen(
    onNavigateToNote: (Long, SearchResultInfo?) -> Unit,
    onNavigateToNewNote: () -> Unit,
    onNavigateToTrash: () -> Unit = {},
    onNavigateToArchive: () -> Unit = {},
    shouldClearSearch: Boolean = false,
    onClearSearchHandled: () -> Unit = {},
    shouldScrollToTop: Boolean = false,
    onScrollToTopHandled: () -> Unit = {},
    viewModel: NotesListViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    // LazyListState for managing note list scrolling
    val listState = rememberLazyListState()
    
    // Clear search if request came from navigation
    LaunchedEffect(shouldClearSearch) {
        if (shouldClearSearch) {
            viewModel.clearSearch()
            onClearSearchHandled()
        }
    }
    
    // Unified scroll to top - handle race conditions between navigation and UI state
    // Combines two potentially conflicting LaunchedEffect into one with priority logic
    LaunchedEffect(shouldScrollToTop, uiState.shouldScrollToTop) {
        when {
            // Priority to navigation scroll (external trigger)
            shouldScrollToTop -> {
                // INSTANT scroll without flashing - use scrollToItem with exact offset
                listState.scrollToItem(index = 0, scrollOffset = 0)
                onScrollToTopHandled()
            }
            // In-screen scroll (search reset, data update)
            uiState.shouldScrollToTop -> {
                // Handle flashing: removed delay, use instant scroll
                listState.scrollToItem(index = 0, scrollOffset = 0)
                viewModel.onScrollToTopHandled()
            }
        }
    }
    
    NotesListContent(
        uiState = uiState,
        listState = listState,
        onEvent = viewModel::onEvent,
        onNoteClick = { noteId ->
            // Get search info for this note (if any)
            val searchInfo = viewModel.getSearchInfoForNote(noteId)
            onNavigateToNote(noteId, searchInfo)
        },
        onNavigateToNote = onNavigateToNote,
        onNavigateToTrash = onNavigateToTrash,
        onNavigateToArchive = onNavigateToArchive,
        onNavigationHandled = viewModel::onNavigationHandled,
        onClearError = viewModel::clearError
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun NotesListContent(
    uiState: NotesListUiState,
    listState: LazyListState,
    onEvent: (NotesListEvent) -> Unit,
    onNoteClick: (Long) -> Unit,
    onNavigateToNote: (Long, SearchResultInfo?) -> Unit,
    onNavigateToTrash: () -> Unit,
    onNavigateToArchive: () -> Unit,
    onNavigationHandled: () -> Unit,
    onClearError: () -> Unit
) {
    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    
    // PERFORMANCE CRITICAL: Unified Global Color Caching
    // Fixed: ColorBundle duplication caused unnecessary color recalculations
    // Solution: Unified GlobalColorBundle architecture for entire application
    val colors = rememberGlobalColors()
    
    // Backward compatibility for existing code
    val highlightColor = colors.highlight
    val highlightBackgroundColor = colors.highlightBackground
    val selectionBackgroundColor = colors.selection  
    val backgroundColor = colors.background
    val textColor = colors.text
    val textSecondaryColor = colors.textSecondary
    val textSecondaryColorAlpha = textSecondaryColor.copy(alpha = 0.3f)
    val backgroundColorAlpha = backgroundColor.copy(alpha = 0.5f)
    
    // Simplified UI state reads
    val selectedNotesSet = uiState.selectedNotes
    val searchContextsMap = uiState.searchContexts
    val searchResultsMap = uiState.searchResults  
    val expandedNotesSet = uiState.expandedNotes
    val isSelectionMode = uiState.isSelectionMode
    val isSearchMode = uiState.isSearchMode
    val notesSize = uiState.notes.size
    
    // Export launchers - persistent to prevent lifecycle issues
    val saveMarkdownLauncher: androidx.activity.result.ActivityResultLauncher<String> = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/markdown")
    ) { uri: Uri? ->
        uri?.let { 
            onEvent(NotesListEvent.ExportMarkdownFileWithSelection(context, it))
        }
    }
    
    val saveArchiveLauncher: androidx.activity.result.ActivityResultLauncher<String> = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri: Uri? ->
        uri?.let { 
            onEvent(NotesListEvent.ExportArchiveFileWithSelection(context, it))
        }
    }

    val saveMdFolderLauncher: androidx.activity.result.ActivityResultLauncher<Uri?> = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        uri?.let { folderUri ->
            onEvent(NotesListEvent.ExportIndividualMarkdownFolderWithSelection(context, folderUri))
        }
    }

    // Import launchers - ALWAYS created to prevent null reference
    val importDatabaseLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { fileUri ->
            onEvent(NotesListEvent.ImportDatabaseFile(context, fileUri))
        }
    }
    
    val importArchiveLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { fileUri ->
            onEvent(NotesListEvent.ImportArchiveFile(context, fileUri))
        }
    }
    
    val importFolderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        uri?.let { folderUri ->
            onEvent(NotesListEvent.ImportFromFolderUri(context, folderUri))
        }
    }

    // Handle "Back" button to reset selection
    BackHandler(enabled = uiState.isSelectionMode) {
        onEvent(NotesListEvent.ClearSelection)
    }
    
    // Handle "Back" button to exit search mode
    BackHandler(enabled = uiState.isSearchMode && !uiState.isSelectionMode) {
        onEvent(NotesListEvent.ClearSearch)
    }

    Box(modifier = Modifier.fillMaxSize()) {
            Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding() // Top padding for status bar
    ) {
                         // Top App Bar
                         NotesTopAppBar(
                isSelectionMode = uiState.isSelectionMode,
                selectedCount = uiState.selectedNotes.size,
                showMenu = showMenu,
                onShowMenuChange = { showMenu = it },
                onThemeClick = { showThemeDialog = true },
                onEvent = onEvent,
                uiState = uiState,
                onNavigateToTrash = onNavigateToTrash,
                onNavigateToArchive = onNavigateToArchive
            )

            // Search Field
            if (uiState.isSearchMode) {
                NoteSearchBar(
                    query = uiState.searchQuery,
                    onQueryChange = { query -> onEvent(NotesListEvent.SearchQueryChanged(query)) },
                    onClearSearch = { onEvent(NotesListEvent.ClearSearch) },
                    placeholder = stringResource(R.string.search_notes),
        
                )
            }

            when {
                uiState.isLoading -> {
                    // Transparent loading - no indicator for better performance
                }
                uiState.notes.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.no_notes),
                            color = colors.textSecondary,
                            fontSize = 16.sp
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        itemsIndexed(
                            items = uiState.notes,
                            key = { index, note -> note.id }  // Stable keys for performance
                        ) { index, note ->
                            // O(1) index instead of O(N) indexOf() - performance optimization
                            
                            // Notes load instantly even with 10K+ entries
                            
                            Column {
                                NoteItem(
                                    note = note,
                                    isSelected = note.id in selectedNotesSet,
                                    isSelectionMode = isSelectionMode,
                                    isSearchMode = isSearchMode,
                                    searchContext = searchContextsMap[note.id],
                                    searchResult = searchResultsMap[note.id],
                                    isExpanded = note.id in expandedNotesSet,
                                    isFirst = (index == 0),
                                    precomputedHighlightColor = highlightColor,
                                    precomputedBackgroundColor = highlightBackgroundColor,
                                    precomputedSelectionColor = selectionBackgroundColor,
                                    precomputedBgColor = backgroundColor,
                                    precomputedTextColor = textColor,
                                    precomputedSecondaryColor = textSecondaryColorAlpha,
                                     onClick = { 
                                         if (uiState.isSelectionMode) {
                                             onEvent(NotesListEvent.ToggleNoteSelection(note.id))
                                         } else {
                                             onNoteClick(note.id)
                                          }
                                     },
                                     onLongClick = {
                                         if (!uiState.isSelectionMode) {
                                             onEvent(NotesListEvent.StartSelectionMode(note.id))
                                         }
                                     },
                                     onToggleExpansion = {
                                         onEvent(NotesListEvent.ToggleNoteExpansion(note.id))
                                     }
                                 )
                                 
                                 // Show expanded search results if note is expanded
                                 if (note.id in expandedNotesSet && searchResultsMap[note.id] != null) {  // Local variables
                                     ExpandedSearchResults(
                                                                                             searchResult = searchResultsMap[note.id]!!,  // Local variable (third read eliminated)
                        // Precomputed colors for ExpandedSearchResults
                                         precomputedHighlightColor = highlightColor,
                                         precomputedBackgroundColor = highlightBackgroundColor,
                                         // Precomputed colors for performance
                                         precomputedBgColor = backgroundColor,
                                         precomputedTextColor = textColor,
                                         precomputedBgColorAlpha = backgroundColorAlpha,
                                         onResultClick = { matchInfo ->
                                             // Navigate to note with specific search result
                                             val searchInfo = SearchResultInfo(
                                                 context = matchInfo.context,
                                                 searchQuery = searchResultsMap[note.id]!!.searchQuery,  // Local variable
                                                 foundPosition = matchInfo.position
                                             )
                                             onNavigateToNote(note.id, searchInfo)
                                         }
                                     )
                                 }
                                 
                                if (index < notesSize - 1) {  // Local variable for batch optimization
                                    HorizontalDivider(
                                                                thickness = 1.dp,  // Thinner than before (1.5dp)
                        color = textSecondaryColorAlpha  // Precomputed value instead of expensive call
                                    )
                                }
                            }
                        }
                        
                
                        // All notes load instantly with single query
                    }
                }
            }
        }
        // Floating Action Button above everything
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .navigationBarsPadding(), // Bottom padding for navigation panel
            contentAlignment = Alignment.BottomEnd
        ) {
            FloatingActionButton(
                                 onClick = {
                     onEvent(NotesListEvent.CreateNewNote)
                 },
                containerColor = Color(0xFF1c75d3), // Same color as TopAppBar
                contentColor = Color.White,
                shape = CircleShape // Make button perfectly round
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                                            contentDescription = stringResource(R.string.create_note_desc)
                )
            }
        }
    }

         // Export Options Dialog
          if (uiState.showExportOptions) {
        ExportOptionsDialog(
            isSelectedMode = uiState.selectedNotes.isNotEmpty(),
            selectedCount = uiState.selectedNotes.size,
            onDismiss = { onEvent(NotesListEvent.DismissExportOptions) },
            saveMarkdownLauncher = saveMarkdownLauncher,
            saveArchiveLauncher = saveArchiveLauncher,
            saveMdFolderLauncher = saveMdFolderLauncher,
            onEvent = onEvent
        )
    }

     // Import Options Dialog
          if (uiState.showImportOptions) {
        ImportOptionsDialog(
            onDismiss = { onEvent(NotesListEvent.DismissImportOptions) },
            importDatabaseLauncher = importDatabaseLauncher,
            importArchiveLauncher = importArchiveLauncher,
            importFolderLauncher = importFolderLauncher,
            onEvent = onEvent
        )
    }
    
    // About Dialog
    if (uiState.showAboutDialog) {
        AboutDialog(
            onDismiss = { onEvent(NotesListEvent.DismissAboutDialog) }
        )
    }

    // Settings Dialog
    if (uiState.showSettingsDialog) {
        SettingsDialog(
            onApiKeysClick = { onEvent(NotesListEvent.ShowApiKeysDialog) },
            onModelSelectionClick = { onEvent(NotesListEvent.ShowModelSelectionDialog) },
            onDismiss = { onEvent(NotesListEvent.DismissSettingsDialog) }
        )
    }

    // API Keys Dialog
    if (uiState.showApiKeysDialog) {
        ApiKeysDialog(
            openAiKeyInitial = uiState.openAiKeyDraft,
            geminiKeyInitial = uiState.geminiKeyDraft,
            anthropicKeyInitial = uiState.anthropicKeyDraft,
            openRouterKeyInitial = uiState.openRouterKeyDraft,
            onSave = { openAi, gemini, anthropic, openRouter ->
                onEvent(NotesListEvent.SaveApiKeys(openAi, gemini, anthropic, openRouter))
            },
            onDismiss = { onEvent(NotesListEvent.DismissApiKeysDialog) }
        )
    }

    if (uiState.showModelSelectionDialog) {
        ModelSelectionDialog(
            currentProvider = uiState.aiProviderDraft,
            currentOpenAiModel = uiState.openAiModelDraft,
            currentGeminiModel = uiState.geminiModelDraft,
            currentAnthropicModel = uiState.anthropicModelDraft,
            currentOpenRouterModel = uiState.openRouterModelDraft,
            onSave = { provider, openAi, gemini, anthropic, openRouter ->
                onEvent(NotesListEvent.SaveModelSelection(provider, openAi, gemini, anthropic, openRouter))
            },
            onDismiss = { onEvent(NotesListEvent.DismissModelSelectionDialog) }
        )
    }
    
    // Language Dialog  
    if (uiState.showLanguageDialog) {
        val notesViewModel: NotesListViewModel = koinViewModel()
        // Get current actual language from ViewModel
        val currentLanguage = remember(uiState.showLanguageDialog) { 
            notesViewModel.getCurrentLanguage() 
        }
        LanguageDialog(
            currentLanguage = currentLanguage,
            onLanguageSelected = { language ->
                onEvent(NotesListEvent.ChangeLanguage(language))
            },
            onDismiss = { onEvent(NotesListEvent.DismissLanguageDialog) }
        )
    }

    // Navigation handling
    uiState.navigateToNote?.let { noteId ->
        LaunchedEffect(noteId) {
            onNoteClick(noteId)
            onNavigationHandled()
        }
    }

    // Error handling
    uiState.error?.let { error ->
                 AlertDialog(
             onDismissRequest = onClearError,
                         containerColor = colors.dialogBackground,
            title = { Text(stringResource(R.string.error), color = colors.dialogText) },
            text = { Text(error, color = colors.dialogText) },
            confirmButton = {
                                                   TextButton(
                       onClick = onClearError
                   ) {
                       Text(stringResource(R.string.ok), color = colors.buttonAccent)
                   }
            }
        )
    }
    
    // Android handles language change through configChanges
    
    // Theme selection dialog
    if (showThemeDialog) {
        ThemeSelectionDialog(
            currentTheme = uiState.currentTheme,
            onThemeSelected = { theme ->
                onEvent(NotesListEvent.SelectTheme(theme))
                showThemeDialog = false
            },
            onDismiss = { showThemeDialog = false }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NoteItem(
    note: Note,
    isSelected: Boolean = false,
    isSelectionMode: Boolean = false,
    isSearchMode: Boolean = false,
    searchContext: String? = null,
    searchResult: SearchResultInfo? = null,
    isExpanded: Boolean = false,
    isFirst: Boolean = false,
    precomputedHighlightColor: Color,
    precomputedBackgroundColor: Color,
    precomputedSelectionColor: Color,
    precomputedBgColor: Color,
    precomputedTextColor: Color,
    precomputedSecondaryColor: Color,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    onToggleExpansion: () -> Unit = {}
) {
                    // Optimization: use cached title
    val displayText = remember(note.title, isSearchMode, searchContext) {
        if (isSearchMode && searchContext != null) {
            null // Will use AnnotatedString
        } else {
            note.title // Use cached title
        }
    }
    
    // Colors passed from top level
    
    val annotatedText = remember(searchContext, precomputedHighlightColor, precomputedBackgroundColor) {
        if (isSearchMode && searchContext != null && searchContext.contains("<<HIGHLIGHT>>")) {
            val parts = searchContext.split("<<HIGHLIGHT>>", "<</HIGHLIGHT>>")
            if (parts.size >= 3) {
                buildAnnotatedString {
                    append(parts[0]) // Before highlight
                    withStyle(style = SpanStyle(
                        color = precomputedHighlightColor,
                        fontWeight = FontWeight.Bold,
                        background = precomputedBackgroundColor
                    )) {
                        append(parts[1]) // Highlighted text
                    }
                    append(parts[2]) // After highlight
                }
            } else {
                AnnotatedString(searchContext)
            }
        } else if (isSearchMode && searchContext != null) {
            AnnotatedString(searchContext)
        } else {
            null
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
                        .background(
                if (isSelected) precomputedSelectionColor
                else precomputedBgColor
            )
                         .padding(horizontal = 16.dp, vertical = if (isFirst) 10.dp else 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isSelectionMode) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(
                        if (isSelected) precomputedHighlightColor else precomputedSecondaryColor,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                                 if (isSelected) {
                                         Icon(
                        painter = painterResource(id = R.drawable.ic_checkmark),
                        contentDescription = stringResource(R.string.note_selected),
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                 }
            }
            Spacer(modifier = Modifier.width(12.dp))
        }
        
        Column(modifier = Modifier.weight(1f)) {
            if (annotatedText != null) {
                // In search mode show context with highlighting
                Text(
                    text = annotatedText,
                    color = if (note.isFavorite) precomputedHighlightColor else precomputedTextColor,
                    fontSize = MaterialTheme.typography.bodyLarge.fontSize,
                    fontWeight = FontWeight.Normal,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            } else {
                // In normal mode show first line
                Text(
                    text = displayText ?: "",
                    color = if (note.isFavorite) precomputedHighlightColor else precomputedTextColor,
                    fontSize = MaterialTheme.typography.bodyLarge.fontSize,
                    fontWeight = FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        
                        // Show counter and expand button only in search mode with multiple matches
        if (isSearchMode && searchResult != null && searchResult.matchCount > 1) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(start = 8.dp)
            ) {
                // Match counter
                Text(
                    text = "${searchResult.matchCount}",
                                                color = precomputedHighlightColor,  // Precomputed value
                    fontSize = MaterialTheme.typography.bodySmall.fontSize,
                    fontWeight = FontWeight.Bold
                )
                
                // Expand/collapse button
                IconButton(
                    onClick = onToggleExpansion,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowDown else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = if (isExpanded) stringResource(R.string.collapse_results_desc) else stringResource(R.string.expand_results_desc),
                        tint = precomputedHighlightColor,  // Precomputed value
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ExpandedSearchResults(
    searchResult: SearchResultInfo,
            // Precomputed colors
    precomputedHighlightColor: Color,
    precomputedBackgroundColor: Color,
            // Precomputed colors
    precomputedBgColor: Color,
    precomputedTextColor: Color,
    precomputedBgColorAlpha: Color,
    onResultClick: (SearchMatchInfo) -> Unit
) {
    val colors = rememberGlobalColors()
    Column(
        modifier = Modifier
            .fillMaxWidth()
                                .background(precomputedBgColorAlpha)  // Precomputed value instead of expensive call
            .padding(horizontal = 24.dp, vertical = 8.dp)
    ) {
        searchResult.allMatches.take(5).forEachIndexed { index, matchInfo ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp)
                    .clickable { onResultClick(matchInfo) },
                colors = CardDefaults.cardColors(
                                                containerColor = precomputedBgColor  // Precomputed value (using base color)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                                // Result number
            val numberColor = Color.White          // White color for better visibility
            val numberBackgroundColor = precomputedBackgroundColor.copy(alpha = 0.5f)  // Reuse precomputed value
                    
                    // Centered digit positioning in result counter circle
                    // Use Box instead of wrapContentSize for more precise positioning
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .background(numberBackgroundColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${index + 1}",
                            color = numberColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.offset(y = (-2).dp) // Offset for alignment
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    // Expensive color computations replaced with precomputed values
                    // val highlightColor = primaryColor()  // Was: computations in each item
                    // val highlightBackgroundColor = primaryColor().copy(alpha = 0.2f)  // Was: even more computations
                    
                    // Context with highlighting (re-parse for display)
                    val annotatedText = remember(matchInfo.context, precomputedHighlightColor, precomputedBackgroundColor) {
                        if (matchInfo.context.contains("<<HIGHLIGHT>>")) {
                            val parts = matchInfo.context.split("<<HIGHLIGHT>>", "<</HIGHLIGHT>>")
                            if (parts.size >= 3) {
                                buildAnnotatedString {
                                    append(parts[0])
                                    withStyle(style = SpanStyle(
                                        color = precomputedHighlightColor,        // Precomputed value
                                        fontWeight = FontWeight.Bold,
                                        background = precomputedBackgroundColor  // Precomputed value
                                    )) {
                                        append(parts[1])
                                    }
                                    append(parts[2])
                                }
                            } else {
                                AnnotatedString(matchInfo.context)
                            }
                        } else {
                            AnnotatedString(matchInfo.context)
                        }
                    }
                    
                    Text(
                        text = annotatedText,
                        color = precomputedTextColor,  // Use precomputed value instead of expensive call
                        fontSize = 14.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
        
                                // Show if more than 5 results
        if (searchResult.allMatches.size > 5) {
            Text(
                                            text = stringResource(R.string.more_results, searchResult.allMatches.size - 5),
                color = colors.textSecondary,
                fontSize = MaterialTheme.typography.bodySmall.fontSize,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun ExportOptionsDialog(
    isSelectedMode: Boolean = false,
    selectedCount: Int = 0,
    onDismiss: () -> Unit,
    saveMarkdownLauncher: androidx.activity.result.ActivityResultLauncher<String>?,
    saveArchiveLauncher: androidx.activity.result.ActivityResultLauncher<String>?,
    saveMdFolderLauncher: androidx.activity.result.ActivityResultLauncher<Uri?>?,
    onEvent: (NotesListEvent) -> Unit
) {
    val context = LocalContext.current
    val colors = rememberGlobalColors()
         AlertDialog(
         onDismissRequest = onDismiss,
                     containerColor = colors.dialogBackground,
        shape = RoundedCornerShape(12.dp),
                   title = { 
              Text(
                  if (isSelectedMode) stringResource(R.string.export_selected_notes_msg) else stringResource(R.string.select_export_format_msg), 
                  color = colors.dialogText
              ) 
          },
         text = {
             Column(
                 modifier = Modifier
                     .fillMaxWidth()
                     .padding(vertical = 8.dp)
             ) {
                 Text(
                     if (isSelectedMode) {
                         val noteWord = when {
                             selectedCount == 1 -> stringResource(R.string.note_singular)
                             selectedCount < 5 -> stringResource(R.string.note_few)
                             else -> stringResource(R.string.note_many)
                         }
                         stringResource(R.string.export_selected_notes, selectedCount, noteWord)
                     } else {
                         stringResource(R.string.select_export_format)
                     },
                     color = colors.dialogText
                 )
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedButton(
                    onClick = {
                        // Plain text - use ViewModel event
                        onEvent(NotesListEvent.ExportAsText(context))
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = colors.dialogText,
                        containerColor = Color.Transparent
                    ),
                    border = BorderStroke(1.dp, Color(0xFF757575))
                ) {
                    Text(stringResource(R.string.export_plain_text))
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedButton(
                    onClick = {
                                                 // Markdown file - immediately start saving
                         val fileName = "${java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())}_notes.md"
                        saveMarkdownLauncher!!.launch(fileName)
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = colors.dialogText,
                        containerColor = Color.Transparent
                    ),
                    border = BorderStroke(1.dp, Color(0xFF757575))
                ) {
                    Text(stringResource(R.string.export_markdown_file))
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedButton(
                    onClick = {
                        // ZIP archive - immediately start saving
                        val fileName = "${java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())}_notes.zip"
                        saveArchiveLauncher!!.launch(fileName)
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = colors.dialogText,
                        containerColor = Color.Transparent
                    ),
                    border = BorderStroke(1.dp, Color(0xFF757575))
                ) {
                    Text(stringResource(R.string.export_archive))
                }
                
                // Database export available only for general export (all notes)
                if (!isSelectedMode) {
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    OutlinedButton(
                        onClick = {
                            // Export SQLite database
                            onEvent(NotesListEvent.ExportDatabaseFile(context))
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = colors.dialogText,
                            containerColor = Color.Transparent
                        ),
                        border = BorderStroke(1.dp, Color(0xFF757575))
                    ) {
                        Text(stringResource(R.string.export_database))
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                }
                
                OutlinedButton(
                    onClick = {
                        // Individual MD files - choose folder for saving
                        saveMdFolderLauncher!!.launch(null)
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = colors.dialogText,
                        containerColor = Color.Transparent
                    ),
                    border = BorderStroke(1.dp, Color(0xFF757575))
                ) {
                    Text(stringResource(R.string.export_individual_md))
                }
            }
        },
                 confirmButton = {},
         dismissButton = {
             TextButton(onClick = onDismiss) {
                 Text(
                     stringResource(R.string.cancel), 
                     color = colors.dialogText
                 )
             }
         }
    )
}



@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotesTopAppBar(
    isSelectionMode: Boolean,
    selectedCount: Int,
    showMenu: Boolean,
    onShowMenuChange: (Boolean) -> Unit,
    onThemeClick: () -> Unit,
    onEvent: (NotesListEvent) -> Unit,
    uiState: NotesListUiState,
    onNavigateToTrash: () -> Unit,
    onNavigateToArchive: () -> Unit
) {
    val colors = rememberGlobalColors()
                   if (isSelectionMode) {
          Box(
              modifier = Modifier
                  .fillMaxWidth()
                  .height(58.dp)
                  .clipToBounds()
          ) {
              TopAppBar(
                  title = {
                  Text(
                          text = stringResource(R.string.selected_count, selectedCount),
                          color = Color.White,
                          fontSize = MaterialTheme.typography.titleLarge.fontSize * 0.85f,
                          fontWeight = FontWeight.Normal
                      )
              },
             navigationIcon = {
                 IconButton(onClick = { onEvent(NotesListEvent.ClearSelection) }) {
                     Icon(
                         imageVector = Icons.Default.Close,
                         contentDescription = stringResource(R.string.cancel_selection_desc),
                         tint = Color.White
                     )
                 }
             },
             actions = {
                 // Order: Select All → Export → Favorite → Archive → Delete
                 IconButton(onClick = { onEvent(NotesListEvent.SelectAllNotes) }, modifier = Modifier.offset(x = 6.dp)) {
                     Icon(painter = painterResource(id = R.drawable.ic_select_all), contentDescription = stringResource(R.string.select_all_desc), tint = Color.White, modifier = Modifier.size(24.dp))
                 }
                  IconButton(onClick = { onEvent(NotesListEvent.ShowExportOptionsForSelected) }, modifier = Modifier.offset(x = 2.dp)) {
                      Icon(painter = painterResource(id = R.drawable.ic_export), contentDescription = stringResource(R.string.export_selected_desc), tint = Color.White, modifier = Modifier.size(24.dp))
                  }
                  IconButton(onClick = { onEvent(NotesListEvent.ToggleFavoriteForSelected) }, modifier = Modifier.size(36.dp).offset(x = 2.dp)) {
                     Icon(painter = painterResource(id = R.drawable.ic_favorite), contentDescription = stringResource(R.string.add_to_favorites), tint = Color.White, modifier = Modifier.size(20.dp))
                 }
                   IconButton(onClick = { onEvent(NotesListEvent.MoveSelectedToArchive) }, modifier = Modifier.offset(x = 2.dp)) {
                       Icon(painter = painterResource(id = R.drawable.ic_archive), contentDescription = stringResource(R.string.move_to_archive), tint = Color.White, modifier = Modifier.size(22.dp))
                   }
                  IconButton(onClick = { onEvent(NotesListEvent.DeleteSelectedNotes) }) {
                     Icon(imageVector = Icons.Default.Delete, contentDescription = stringResource(R.string.delete_selected_desc), tint = Color.White)
                 }
             },
                           colors = TopAppBarDefaults.topAppBarColors(
                  containerColor = Color(0xFF1c75d3)
              )
         )
     }
     } else {
          Box(
              modifier = Modifier
                  .fillMaxWidth()
                  .height(58.dp)
                  .clipToBounds()
          ) {
              TopAppBar(
                  title = {
                  Text(
                          text = stringResource(R.string.notes),
                          color = Color.White, 
                          fontSize = MaterialTheme.typography.titleLarge.fontSize * 0.85f,
                          fontWeight = FontWeight.Normal
                      )
              },
                                         actions = {
                IconButton(onClick = { onEvent(NotesListEvent.StartSearch) }) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = stringResource(R.string.search),
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
                ThemeIconButton(
                    currentTheme = uiState.currentTheme,
                    onThemeClick = onThemeClick,
                    iconTint = Color.White
                )
                 IconButton(onClick = { onShowMenuChange(true) }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = stringResource(R.string.menu),
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }
                 DropdownMenu(
                       expanded = showMenu,
                       onDismissRequest = { onShowMenuChange(false) },
                                          tonalElevation = if (ColorManager.isDarkTheme()) 0.dp else 2.dp,
                   shadowElevation = if (ColorManager.isDarkTheme()) 0.dp else 4.dp,
                   shape = RoundedCornerShape(8.dp),
                   containerColor = colors.menuBackground
                    ) {
                     // Вернули стандартные отступы пунктов меню
                      DropdownMenuItem(
                         text = { Text(stringResource(R.string.language), color = colors.menuText) },
                         onClick = {
                             onShowMenuChange(false)
                             onEvent(NotesListEvent.ShowLanguageDialog)
                         }
                      )
                     DropdownMenuItem(
                         text = { Text(stringResource(R.string.import_notes_menu), color = colors.menuText) },
                         onClick = {
                             onShowMenuChange(false)
                             onEvent(NotesListEvent.ShowImportOptions)
                         }
                     )
                     DropdownMenuItem(
                         text = { Text(stringResource(R.string.export_notes_menu), color = colors.menuText) },
                         onClick = {
                             onShowMenuChange(false)
                             onEvent(NotesListEvent.ShowExportOptions)
                         }
                     )
                      DropdownMenuItem(
                          text = { Text(stringResource(R.string.settings), color = colors.menuText) },
                          onClick = {
                              onShowMenuChange(false)
                              onEvent(NotesListEvent.ShowSettingsDialog)
                          }
                      )
                     DropdownMenuItem(
                         text = { Text(stringResource(R.string.about_app), color = colors.menuText) },
                         onClick = {
                             onShowMenuChange(false)
                             onEvent(NotesListEvent.ShowAboutDialog)
                         }
                      )
                      DropdownMenuItem(
                         text = { Text(stringResource(R.string.archive), color = colors.menuText) },
                         onClick = {
                             onShowMenuChange(false)
                             onNavigateToArchive()
                         }
                      )
                     DropdownMenuItem(
                         text = { Text(stringResource(R.string.trash), color = colors.menuText) },
                         onClick = {
                             onShowMenuChange(false)
                             onNavigateToTrash()
                         }
                     )
                                   }
             },
                                                                                                           colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1c75d3)
                )
        )
    }
}
}



@Composable
private fun ImportOptionsDialog(
    onDismiss: () -> Unit,
    importDatabaseLauncher: androidx.activity.result.ActivityResultLauncher<Array<String>>,
    importArchiveLauncher: androidx.activity.result.ActivityResultLauncher<Array<String>>,
    importFolderLauncher: androidx.activity.result.ActivityResultLauncher<Uri?>,
    onEvent: (NotesListEvent) -> Unit
) {
    val colors = rememberGlobalColors()
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.dialogBackground,
        shape = RoundedCornerShape(12.dp),
        title = { 
            Text(
                stringResource(R.string.select_import_source), 
                color = colors.dialogText
            ) 
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Text(
                    stringResource(R.string.import_from_sources), 
                    color = colors.dialogText
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                                        // Import from SQLite database
                OutlinedButton(
                    onClick = {
                        importDatabaseLauncher.launch(arrayOf("application/octet-stream", "*/*"))
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = colors.dialogText,
                        containerColor = Color.Transparent
                    ),
                    border = BorderStroke(1.dp, Color(0xFF757575))
                ) {
                                            Text(stringResource(R.string.export_database))
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                                        // Import from ZIP archive
                OutlinedButton(
                    onClick = {
                        importArchiveLauncher.launch(arrayOf("application/zip"))
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = colors.dialogText,
                        containerColor = Color.Transparent
                    ),
                    border = BorderStroke(1.dp, Color(0xFF757575))
                ) {
                    Text(stringResource(R.string.import_from_archive))
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                                        // Import from folder
                OutlinedButton(
                    onClick = {
                        importFolderLauncher.launch(null)
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = colors.dialogText,
                        containerColor = Color.Transparent
                    ),
                    border = BorderStroke(1.dp, Color(0xFF757575))  
                ) {
                    Text(stringResource(R.string.import_from_folder))
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = colors.dialogText)
            ) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable 
private fun LoadingIndicator() {
    val colors = rememberGlobalColors()
            val loadingDots = stringResource(R.string.loading_dots)
        val loadingDoubleDots = stringResource(R.string.loading_double_dots)
        val loadingTripleDots = stringResource(R.string.loading_triple_dots)
        var animatedText by remember { mutableStateOf(loadingDots) }
    
    LaunchedEffect(Unit) {
        while (true) {
            delay(500)
            animatedText = when (animatedText) {
                loadingDots -> loadingDoubleDots
                loadingDoubleDots -> loadingTripleDots
                else -> loadingDots
            }
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = animatedText,
                            color = colors.textSecondary,
            fontSize = 14.sp
        )
    }
}