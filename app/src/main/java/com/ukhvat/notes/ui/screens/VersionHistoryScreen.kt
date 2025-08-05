package com.ukhvat.notes.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import com.ukhvat.notes.ui.theme.ColorManager
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.core.content.FileProvider
import androidx.compose.ui.text.font.FontWeight
import android.content.Intent
import android.widget.Toast
import java.io.File
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import org.koin.androidx.compose.koinViewModel
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.ui.draw.shadow
import kotlinx.coroutines.launch
import com.ukhvat.notes.domain.model.NoteVersion
import com.ukhvat.notes.ui.theme.*
import com.ukhvat.notes.ui.theme.rememberGlobalColors
import androidx.compose.ui.graphics.Color
import java.text.SimpleDateFormat
import java.util.*
import com.ukhvat.notes.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VersionHistoryScreen(
    noteId: Long,
    onBackClick: () -> Unit,
    onVersionClick: (NoteVersion) -> Unit,
    onNavigateToNewNote: (Long) -> Unit = {},
    viewModel: VersionHistoryViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val versions by viewModel.getVersionsForNote(noteId).collectAsState(initial = null)
    val coroutineScope = rememberCoroutineScope()
    var showRollbackDialog by remember { mutableStateOf<NoteVersion?>(null) }
    var showPreviewDialog by remember { mutableStateOf<NoteVersion?>(null) }
    var rollbackFromPreview by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    
                // New states for extended functionality
    var showDeleteDialog by remember { mutableStateOf<NoteVersion?>(null) }
    var showRenameDialog by remember { mutableStateOf<NoteVersion?>(null) }
    var showExportDialog by remember { mutableStateOf(false) }
    var renameText by remember { mutableStateOf("") }
    val colors = rememberGlobalColors()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .systemBarsPadding() // Padding for all system bars
    ) {
        // Top App Bar
        TopAppBar(
            title = {
                Text(
                    text = stringResource(R.string.version_history_title),
                    color = Color.White,
                    fontSize = MaterialTheme.typography.titleLarge.fontSize,
                    fontWeight = FontWeight.Normal
                )
            },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = stringResource(R.string.back),
                        tint = Color.White
                    )
                }
            },
            actions = {
                IconButton(onClick = { showExportDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = stringResource(R.string.export_history),
                        tint = Color.White
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = Color(0xFF1c75d3)
            )
        )
        
        val versionsList = versions
        when {
            versionsList == null -> {
                // Loading state - NO display for transparent appearance (as intended)
            }
            versionsList.isEmpty() -> {
                // Empty list state
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = stringResource(R.string.empty_version_history),
                            tint = colors.textSecondary,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.no_versions_saved),
                            color = colors.textSecondary,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.versions_auto_created),
                            color = colors.textSecondary,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(horizontal = 32.dp)
                        )
                    }
                }
            }
            else -> {
                // Version list
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(versionsList) { version ->
                        VersionItem(
                            version = version,
                            onClick = { showPreviewDialog = version },
                            onRollbackClick = { 
                                rollbackFromPreview = false
                                showRollbackDialog = version 
                            },
                            onDeleteClick = {
                                showDeleteDialog = version
                            },
                            isLoading = isLoading
                        )
                    }
                }
            }
        }     
    }
    
    // Rollback dialog
    showRollbackDialog?.let { version ->
        AlertDialog(
            onDismissRequest = { 
                if (rollbackFromPreview) {
                    showPreviewDialog = version
                }
                showRollbackDialog = null
            },
            containerColor = colors.dialogBackground,
            shape = RoundedCornerShape(12.dp),
            title = { 
                Text(
                    stringResource(R.string.restore_version_question), 
                    color = colors.dialogText
                ) 
            },
           text = { 
               Column(
                   modifier = Modifier
                       .fillMaxWidth()
                       .padding(vertical = 8.dp),
                   horizontalAlignment = Alignment.CenterHorizontally
               ) {
                   Text(
                       stringResource(R.string.select_version_action, version.formattedDate),
                       color = colors.dialogText
                   )
                   Spacer(modifier = Modifier.height(16.dp))
                   
                                           // Roll back current note
                   OutlinedButton(
                       onClick = {
                           coroutineScope.launch {
                               isLoading = true
                               val success = viewModel.rollbackToVersion(noteId, version.id)
                               isLoading = false
                               showRollbackDialog = null
                               if (success) {
                                   onBackClick() // Return to note
                               }
                           }
                       },
                       enabled = !isLoading,
                       modifier = Modifier.fillMaxWidth(),
                       colors = ButtonDefaults.outlinedButtonColors(
                           contentColor = colors.dialogText,
                           containerColor = Color.Transparent
                       ),
                       border = BorderStroke(1.dp, Color(0xFF757575))
                   ) {
                                               Text(stringResource(R.string.rollback_current_note))
                   }
                   
                   Spacer(modifier = Modifier.height(8.dp))
                   
                                           // Create new note
                   OutlinedButton(
                       onClick = {
                           coroutineScope.launch {
                               isLoading = true
                               val newNoteId = viewModel.createNoteFromVersion(version.id)
                               isLoading = false
                               showRollbackDialog = null
                               newNoteId?.let { 
                                   onNavigateToNewNote(it)
                               }
                           }
                       },
                       enabled = !isLoading,
                       modifier = Modifier.fillMaxWidth(),
                       colors = ButtonDefaults.outlinedButtonColors(
                           contentColor = colors.dialogText,
                           containerColor = Color.Transparent
                       ),
                       border = BorderStroke(1.dp, Color(0xFF757575))
                   ) {
                                               Text(stringResource(R.string.create_new_note_button))
                   }
               }
           },
           confirmButton = {},
           dismissButton = {
               TextButton(onClick = { 
                   if (rollbackFromPreview) {
                       showPreviewDialog = version
                   }
                   showRollbackDialog = null
               }) {
                   Text(
                       stringResource(R.string.cancel), 
                       color = colors.dialogText
                   )
               }
           }
       )
   }
   
       // Version preview dialog
   showPreviewDialog?.let { version ->
       val clipboardManager = LocalClipboardManager.current
       
       VersionPreviewDialog(
           version = version,
           onDismiss = { showPreviewDialog = null },
           onRestore = { 
               rollbackFromPreview = true
               showRollbackDialog = version
               showPreviewDialog = null
           },
           onCopy = {
               clipboardManager.setText(AnnotatedString(version.content))
               showPreviewDialog = null
           },
           onRename = {
               renameText = version.customName ?: ""
               showRenameDialog = version
               showPreviewDialog = null
           }
       )
   }
   
       // Version deletion dialog
   showDeleteDialog?.let { version ->
       AlertDialog(
           onDismissRequest = { showDeleteDialog = null },
                           containerColor = colors.dialogBackground,
           shape = RoundedCornerShape(12.dp),
           title = { 
               Text(
                   stringResource(R.string.delete_version), 
                   color = colors.dialogText
               ) 
           },
           text = { 
               Text(
                   stringResource(R.string.delete_version_confirm),
                   color = colors.dialogText
               ) 
           },
           confirmButton = {
               TextButton(
                   onClick = {
                       coroutineScope.launch {
                           val success = viewModel.deleteVersion(version.id)
                           showDeleteDialog = null
                           // Can add Toast notification
                       }
                   }
               ) {
                   Text(
                       stringResource(R.string.delete), 
                       color = Color(0xFFC62828)  // Less bright red
                   )
               }
           },
           dismissButton = {
               TextButton(onClick = { showDeleteDialog = null }) {
                   Text(
                       stringResource(R.string.cancel), 
                       color = colors.dialogText
                   )
               }
           }
       )
   }
   
       // Version rename dialog
   showRenameDialog?.let { version ->
       AlertDialog(
           onDismissRequest = { showRenameDialog = null },
                           containerColor = colors.dialogBackground,
           shape = RoundedCornerShape(12.dp),
           title = { 
               Text(
                   stringResource(R.string.rename_version), 
                   color = colors.dialogText
               ) 
           },
           text = {
               OutlinedTextField(
                   value = renameText,
                   onValueChange = { renameText = it },
                   label = { Text(stringResource(R.string.version_name_hint)) },
                   singleLine = true,
                   colors = OutlinedTextFieldDefaults.colors(
                       focusedTextColor = if (ColorManager.isDarkTheme()) Color.White else Color.Black,
                       unfocusedTextColor = if (ColorManager.isDarkTheme()) Color.White else Color.Black
                   )
               )
           },
           confirmButton = {
               TextButton(
                   onClick = {
                       coroutineScope.launch {
                           viewModel.updateVersionName(
                               version.id, 
                               if (renameText.isBlank()) null else renameText
                           )
                           showRenameDialog = null
                       }
                   }
               ) {
                   Text(
                       stringResource(R.string.ok), 
                       color = colors.dialogText
                   )
               }
           },
           dismissButton = {
               TextButton(onClick = { showRenameDialog = null }) {
                   Text(
                       stringResource(R.string.cancel), 
                       color = colors.dialogText
                   )
               }
           }
       )
   }
   
       // History export dialog
   if (showExportDialog) {
       AlertDialog(
           onDismissRequest = { showExportDialog = false },
                           containerColor = colors.dialogBackground,
           shape = RoundedCornerShape(12.dp),
           title = { 
               Text(
                   stringResource(R.string.export_history), 
                   color = colors.dialogText
               ) 
           },
           text = {
               Column {
                   Text(stringResource(R.string.select_export_format_msg), color = colors.dialogText)
                   Spacer(modifier = Modifier.height(16.dp))
                   
                   OutlinedButton(
                       onClick = {
                           coroutineScope.launch {
                               try {
                                   val exportedText = viewModel.exportVersionHistory(noteId, "text")
                                   clipboardManager.setText(AnnotatedString(exportedText))
                                   Toast.makeText(context, context.getString(R.string.version_history_copied), Toast.LENGTH_SHORT).show()
                                   showExportDialog = false
                               } catch (e: Exception) {
                                   Toast.makeText(context, context.getString(R.string.copy_error, e.message ?: ""), Toast.LENGTH_SHORT).show()
                               }
                           }
                       },
                       modifier = Modifier.fillMaxWidth(),
                       colors = ButtonDefaults.outlinedButtonColors(
                           contentColor = if (ColorManager.isDarkTheme()) Color.White else Color.Black
                       ),
                       border = BorderStroke(1.dp, if (ColorManager.isDarkTheme()) Color.White else Color.Black)
                   ) {
                       Text(stringResource(R.string.export_as_text))
                   }
                   
                   Spacer(modifier = Modifier.height(8.dp))
                   
                   OutlinedButton(
                       onClick = {
                           coroutineScope.launch {
                               try {
                                   val exportedMarkdown = viewModel.exportVersionHistory(noteId, "markdown")
                                   
                                   // Create temporary file
                                   val fileName = "version_history_${System.currentTimeMillis()}.md"
                                   val file = File(context.cacheDir, fileName)
                                   file.writeText(exportedMarkdown)
                                   
                                   // Create URI for file
                                   val uri = FileProvider.getUriForFile(
                                       context,
                                       "${context.packageName}.fileprovider",
                                       file
                                   )
                                   
                                   // Create Intent for sending file
                                   val shareIntent = Intent().apply {
                                       action = Intent.ACTION_SEND
                                       putExtra(Intent.EXTRA_STREAM, uri)
                                       type = "text/markdown"
                                       addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                   }
                                   
                                   context.startActivity(
                                       Intent.createChooser(shareIntent, context.getString(R.string.share_version_history))
                                   )
                                   
                                   showExportDialog = false
                               } catch (e: Exception) {
                                   Toast.makeText(context, context.getString(R.string.file_creation_error, e.message ?: ""), Toast.LENGTH_SHORT).show()
                               }
                           }
                       },
                       modifier = Modifier.fillMaxWidth(),
                       colors = ButtonDefaults.outlinedButtonColors(
                           contentColor = if (ColorManager.isDarkTheme()) Color.White else Color.Black
                       ),
                       border = BorderStroke(1.dp, if (ColorManager.isDarkTheme()) Color.White else Color.Black)
                   ) {
                       Text(stringResource(R.string.export_as_markdown))
                   }
               }
           },
           confirmButton = {},
           dismissButton = {
               TextButton(onClick = { showExportDialog = false }) {
                   Text(
                       stringResource(R.string.cancel), 
                       color = colors.dialogText
                   )
               }
           }
       )
   }
}

@Composable
private fun VersionItem(
    version: NoteVersion,
    onClick: () -> Unit,
    onRollbackClick: () -> Unit,
    onDeleteClick: () -> Unit,
    isLoading: Boolean
) {
    val colors = rememberGlobalColors()
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = if (ColorManager.isDarkTheme()) 0.dp else 2.dp,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = colors.cardBackground
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Version creation time and creation method
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatTimeAgo(version.getTimeDiffMillis()),
                    color = colors.primary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                
                version.changeDescription?.let { description ->
                    Text(
                        text = localizeVersionDescription(description),
                        color = colors.textSecondary,
                        fontSize = 12.sp
                    )
                }
            }
            
            // User-defined version name (below creation method description)
            version.customName?.let { name ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = name,
                    color = colors.text,
                    fontSize = 15.sp,  // Increased font size
                    fontWeight = FontWeight.SemiBold
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Content preview
            Text(
                text = version.shortContent,
                color = colors.textSecondary,
                fontSize = 14.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Date and action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = version.formattedDate,
                    color = colors.textSecondary,
                    fontSize = 12.sp
                )
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Delete button
                    OutlinedButton(
                        onClick = onDeleteClick,
                        enabled = !isLoading,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFFC62828)  // Less bright red
                        ),
                        border = BorderStroke(1.dp, Color(0xFFC62828)),
                        modifier = Modifier.height(32.dp),
                        contentPadding = PaddingValues(
                            horizontal = 8.dp,
                            vertical = 6.dp
                        )
                    ) {
                        Text(
                            text = stringResource(R.string.delete_version),
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                    
                    // Restore button
                    OutlinedButton(
                        onClick = onRollbackClick,
                        enabled = !isLoading,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = colors.primary
                        ),
                        border = BorderStroke(1.dp, colors.primary),
                        modifier = Modifier.height(32.dp),
                        contentPadding = PaddingValues(
                            horizontal = 12.dp,
                            vertical = 6.dp
                        )
                    ) {
                        Text(
                            text = stringResource(R.string.restore),
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VersionPreviewDialog(
    version: NoteVersion,
    onDismiss: () -> Unit,
    onRestore: () -> Unit,
    onCopy: () -> Unit = {},
    onRename: () -> Unit = {}
) {
    var showMenu by remember { mutableStateOf(false) }
    
    // Performance critical: cached colors instead of expensive calls
    val colors = rememberGlobalColors()
    
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(12.dp),
                        containerColor = colors.dialogBackground,
        title = { 
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.version_preview_title),
                        color = colors.menuText,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = version.formattedDate,
                        color = colors.textSecondary,
                        fontSize = 14.sp
                    )
                    version.changeDescription?.let { description ->
                        Text(
                            text = localizeVersionDescription(description),
                            color = colors.primary,
                            fontSize = 12.sp
                        )
                    }
                    version.customName?.let { name ->
                        Text(
                            text = name,
                            color = colors.text,
                            fontSize = 15.sp,  // Increased font size
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                
                // Three-dot menu
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = stringResource(R.string.settings),
                            tint = colors.menuText
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
                                Text(
                                    stringResource(R.string.copy_version),
                                    color = colors.menuText
                                ) 
                            },
                            onClick = {
                                showMenu = false
                                onCopy()
                            }
                        )
                        DropdownMenuItem(
                            text = { 
                                Text(
                                    stringResource(R.string.rename_version),
                                    color = colors.menuText
                                ) 
                            },
                            onClick = {
                                showMenu = false
                                onRename()
                            }
                        )
                    }
                }
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .heightIn(max = 400.dp)
                    .fillMaxWidth()
            ) {
                item {
                    Text(
                        text = version.content,
                        color = colors.text,
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(
                                            contentColor = colors.text
                )
            ) {
                Text(stringResource(R.string.close))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onRestore,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = colors.primary
                ),
                modifier = Modifier.offset(x = (-73).dp)
            ) {
                Text(stringResource(R.string.restore))
            }
        }
    )
}

/**
 * Formats relative version time with localization.
 * Moved from domain layer to UI for Clean Architecture compliance.
 */
@Composable
private fun formatTimeAgo(diffMillis: Long): String {
    return when {
        diffMillis < 60_000 -> stringResource(R.string.just_now)
        diffMillis < 3600_000 -> stringResource(R.string.minutes_ago, diffMillis / 60_000)
        diffMillis < 86400_000 -> stringResource(R.string.hours_ago, diffMillis / 3600_000)
        diffMillis < 2592000_000 -> stringResource(R.string.days_ago, diffMillis / 86400_000)
        else -> {
            val timestamp = System.currentTimeMillis() - diffMillis
            SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date(timestamp))
        }
    }
}

/**
 * Localizes version descriptions for compatibility with old DB records
 * 
 * Issue: Language switching leaves old Russian descriptions in database
 * Solution: Convert known legacy descriptions through string resources
 */
@Composable
private fun localizeVersionDescription(description: String): String {
            // Get localized strings for comparison with legacy data
    val forceSaveText = stringResource(R.string.version_force_save)
    val autosaveText = stringResource(R.string.version_autosave)
    val creationText = stringResource(R.string.version_creation)
    
    return when (description.trim()) {
        // Already localized strings (for new records)
        forceSaveText -> forceSaveText
        autosaveText -> autosaveText  
        creationText -> creationText
        // Legacy hardcoded strings (for backward compatibility)
                            "Force Save" -> forceSaveText
                            "Autosave" -> autosaveText
                            "Note Creation" -> creationText
        else -> description // Keep as is for user-defined descriptions
    }
} 