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
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.withStyle
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
import org.json.JSONArray
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
    var showAiInfoDialog: MutableState<NoteVersion?> = remember { mutableStateOf(null) }
    var rollbackFromPreview by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    
                // New states for extended functionality
    var showDeleteDialog by remember { mutableStateOf<NoteVersion?>(null) }
    var showRenameDialog by remember { mutableStateOf<NoteVersion?>(null) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }
    var showDeleteAllDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
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
                    fontSize = MaterialTheme.typography.titleLarge.fontSize * 0.80f,
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
                // Export button (left)
                IconButton(onClick = { showExportDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = stringResource(R.string.export_history),
                        tint = Color.White
                    )
                }
                // Info button (middle)
                IconButton(onClick = { showInfoDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = stringResource(R.string.version_history_info_desc),
                        tint = Color.White
                    )
                }
                // Delete all versions
                IconButton(onClick = { showDeleteAllDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(R.string.delete_all_versions_desc),
                        tint = Color.White
                    )
                }
                // Settings (rightmost)
                IconButton(onClick = { showSettingsDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = stringResource(R.string.versioning_settings_desc),
                        tint = Color.White
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = Color(0xFF1c75d3)
            )
        )
        
        // Versioning info dialog
        if (showInfoDialog) {
            AlertDialog(
                onDismissRequest = { showInfoDialog = false },
                containerColor = colors.dialogBackground,
                shape = RoundedCornerShape(12.dp),
                title = {
                    Text(
                        text = stringResource(R.string.version_history_info_title),
                        color = colors.menuText,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium
                    )
                },
                text = {
                    // Use dynamic parameters: interval minutes, min chars, max versions
                    val minutes = (com.ukhvat.notes.domain.util.VersioningConstants.VERSION_CHECK_INTERVAL_MS / 60_000L).toInt()
                    val minChars = com.ukhvat.notes.domain.util.VersioningConstants.MIN_CHANGE_FOR_VERSION
                    // Prefer note-specific maxVersions if available; fall back to default
                    var maxVersions: Int? by remember { mutableStateOf<Int?>(null) }
                    LaunchedEffect(noteId) {
                        maxVersions = try { viewModel.getMaxVersions(noteId) } catch (e: Exception) { null }
                    }
                    val maxKeep = maxVersions ?: com.ukhvat.notes.domain.util.VersioningConstants.DEFAULT_MAX_VERSIONS
                    Text(
                        text = stringResource(R.string.version_history_info_text, minutes, minChars, maxKeep),
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

        // Versioning settings dialog
        if (showSettingsDialog) {
            var autoEnabled by remember { mutableStateOf(true) }
            var intervalMs by remember { mutableStateOf(com.ukhvat.notes.domain.util.VersioningConstants.VERSION_CHECK_INTERVAL_MS) }
            var minCharsInput by remember { mutableStateOf(com.ukhvat.notes.domain.util.VersioningConstants.MIN_CHANGE_FOR_VERSION.toString()) }
            var maxRegularInput by remember { mutableStateOf(com.ukhvat.notes.domain.util.VersioningConstants.DEFAULT_MAX_VERSIONS.toString()) }

            LaunchedEffect(Unit) {
                // Load current settings
                try {
                    autoEnabled = viewModel.getVersioningAutoEnabled()
                    intervalMs = viewModel.getVersioningIntervalMs()
                    minCharsInput = viewModel.getVersioningMinChangeChars().toString()
                    maxRegularInput = viewModel.getVersioningMaxRegularVersions().toString()
                } catch (_: Exception) { }
            }

            AlertDialog(
                onDismissRequest = { showSettingsDialog = false },
                containerColor = colors.dialogBackground,
                shape = RoundedCornerShape(12.dp),
                title = {
                    Text(
                        text = stringResource(R.string.versioning_settings_title),
                        color = colors.menuText,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        // Auto-create toggle
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = stringResource(R.string.versioning_autocreate), color = colors.text, modifier = Modifier.weight(1f))
                            Switch(checked = autoEnabled, onCheckedChange = { autoEnabled = it })
                        }

                        // Interval radio group (enabled only when auto)
                        Text(text = stringResource(R.string.versioning_interval), color = colors.textSecondary)
                        val intervals = listOf(30_000L to R.string.interval_30s, 60_000L to R.string.interval_1m, 120_000L to R.string.interval_2m, 300_000L to R.string.interval_5m)
                        intervals.forEach { (ms, labelRes) ->
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable(enabled = autoEnabled) { if (autoEnabled) intervalMs = ms }) {
                                RadioButton(selected = intervalMs == ms && autoEnabled, onClick = { if (autoEnabled) intervalMs = ms }, enabled = autoEnabled, colors = RadioButtonDefaults.colors(selectedColor = colors.primary))
                                Text(text = stringResource(labelRes), color = if (autoEnabled) colors.text else colors.textSecondary)
                            }
                        }

                        // Min changed chars input
                        Text(text = stringResource(R.string.versioning_min_chars), color = colors.textSecondary)
                        OutlinedTextField(
                            value = minCharsInput,
                            onValueChange = { value -> if (value.length <= 6 && value.all { it.isDigit() } || value.isEmpty()) minCharsInput = value },
                            singleLine = true,
                            textStyle = LocalTextStyle.current.copy(color = colors.text),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = colors.text,
                                unfocusedTextColor = colors.text,
                                cursorColor = colors.primary,
                                focusedBorderColor = colors.primary,
                                unfocusedBorderColor = colors.textSecondary
                            )
                        )

                        // Max regular versions input + cleanup button
                        Text(text = stringResource(R.string.versioning_max_regular), color = colors.textSecondary)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = maxRegularInput,
                                onValueChange = { value -> if (value.length <= 6 && value.all { it.isDigit() } || value.isEmpty()) maxRegularInput = value },
                                singleLine = true,
                                textStyle = LocalTextStyle.current.copy(color = colors.text),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = colors.text,
                                    unfocusedTextColor = colors.text,
                                    cursorColor = colors.primary,
                                    focusedBorderColor = colors.primary,
                                    unfocusedBorderColor = colors.textSecondary
                                ),
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = {
                                val maxVal = maxRegularInput.toIntOrNull() ?: com.ukhvat.notes.domain.util.VersioningConstants.DEFAULT_MAX_VERSIONS
                                coroutineScope.launch {
                                    viewModel.cleanupVersionsNow(noteId, maxVal)
                                    Toast.makeText(context, context.getString(R.string.versions_deleted_count, 0), Toast.LENGTH_SHORT).show()
                                }
                            }) {
                                Text(stringResource(R.string.apply_cleanup_now))
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        // Persist settings
                        val minChars = minCharsInput.toIntOrNull()?.coerceIn(0, 1_000_000) ?: 140
                        val maxKeep = maxRegularInput.toIntOrNull()?.coerceIn(10, 1_000_000) ?: 100
                        coroutineScope.launch {
                            viewModel.setVersioningAutoEnabled(autoEnabled)
                            viewModel.setVersioningIntervalMs(intervalMs)
                            viewModel.setVersioningMinChangeChars(minChars)
                            viewModel.setVersioningMaxRegularVersions(maxKeep)
                        }
                        showSettingsDialog = false
                    }, colors = ButtonDefaults.textButtonColors(contentColor = colors.primary)) {
                        Text(stringResource(R.string.save))
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        // Restore defaults
                        autoEnabled = true
                        intervalMs = com.ukhvat.notes.domain.util.VersioningConstants.VERSION_CHECK_INTERVAL_MS
                        minCharsInput = com.ukhvat.notes.domain.util.VersioningConstants.MIN_CHANGE_FOR_VERSION.toString()
                        maxRegularInput = com.ukhvat.notes.domain.util.VersioningConstants.DEFAULT_MAX_VERSIONS.toString()
                    }, colors = ButtonDefaults.textButtonColors(contentColor = colors.text)) {
                        Text(stringResource(R.string.restore_defaults))
                    }
                }
            )
        }

        // Delete all versions confirmation dialog
        if (showDeleteAllDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteAllDialog = false },
                containerColor = colors.dialogBackground,
                shape = RoundedCornerShape(12.dp),
                title = {
                    Text(
                        text = stringResource(R.string.delete_all_versions_title),
                        color = colors.menuText,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium
                    )
                },
                text = {
                    Text(
                        text = stringResource(R.string.delete_all_versions_text),
                        color = colors.text,
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showDeleteAllDialog = false
                            coroutineScope.launch {
                                val deleted = viewModel.deleteAllVersions(noteId)
                                Toast.makeText(context, context.getString(R.string.versions_deleted_count, deleted), Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = colors.primary
                        )
                    ) {
                        Text(stringResource(R.string.delete_all))
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showDeleteAllDialog = false },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = colors.text
                        )
                    ) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            )
        }

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
                    }
                }
            }
            else -> {
                // Build neighbor maps once per list to ensure consistency
                val list = versionsList
                val prevMap = remember(list) {
                    val map = HashMap<Long, String?>(list.size)
                    var i = 0
                    while (i < list.size) {
                        val v = list[i]
                        map[v.id] = if (i < list.size - 1) list[i + 1].content else null
                        i++
                    }
                    map
                }
                val nextMap = remember(list) {
                    val map = HashMap<Long, String?>(list.size)
                    var i = 0
                    while (i < list.size) {
                        val v = list[i]
                        map[v.id] = if (i > 0) list[i - 1].content else null
                        i++
                    }
                    map
                }
                val prevOpsMap = remember(list) {
                    val map = HashMap<Long, String?>(list.size)
                    var i = 0
                    while (i < list.size) {
                        val v = list[i]
                        map[v.id] = if (i < list.size - 1) list[i + 1].diffOpsJson else null
                        i++
                    }
                    map
                }
                val nextOpsMap = remember(list) {
                    val map = HashMap<Long, String?>(list.size)
                    var i = 0
                    while (i < list.size) {
                        val v = list[i]
                        map[v.id] = if (i > 0) list[i - 1].diffOpsJson else null
                        i++
                    }
                    map
                }

                // Version list
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(items = versionsList, key = { it.id }) { version ->
                        VersionItem(
                            version = version,
                            previousContent = prevMap[version.id],
                            nextContent = nextMap[version.id],
                            previousOpsJson = prevOpsMap[version.id],
                            nextOpsJson = nextOpsMap[version.id],
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
        // Find previous version content (versions ordered by timestamp desc)
        val prevContent = remember(versions, version.id) {
            val list = versions ?: emptyList()
            val idx = list.indexOfFirst { it.id == version.id }
            if (idx >= 0 && idx < list.size - 1) list[idx + 1].content else null
        }
        val nextContent = remember(versions, version.id) {
            val list = versions ?: emptyList()
            val idx = list.indexOfFirst { it.id == version.id }
            if (idx > 0) list[idx - 1].content else null
        }
       
       VersionPreviewDialog(
           version = version,
            previousContent = prevContent,
            nextContent = nextContent,
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
            },
            onInfo = {
                showPreviewDialog = null
                showAiInfoDialog.value = version
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

    // AI info dialog (inside composable scope)
    showAiInfoDialog.value?.let { version ->
        val meta = remember(version.customName) { parseAiMeta(version.customName) }
        AlertDialog(
            onDismissRequest = { showAiInfoDialog.value = null },
            containerColor = colors.dialogBackground,
            shape = RoundedCornerShape(12.dp),
            title = {
                Text(text = stringResource(R.string.version_info), color = colors.dialogText)
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    val hashtags = version.aiHashtags
                    if (!hashtags.isNullOrBlank()) {
                        val tagsLine = hashtags.split(' ', ',', ';').filter { it.isNotBlank() }
                            .joinToString(" ") { seg ->
                                val t = seg.trim()
                                if (t.startsWith("#")) t else "#" + t.replace(' ', '_')
                            }
                        Text(stringResource(R.string.hashtags_label) + ": " + tagsLine, color = colors.primary)
                        Spacer(Modifier.height(6.dp))
                    }
                    Text(stringResource(R.string.ai_info_provider, version.aiProvider ?: meta?.provider ?: "—"), color = colors.dialogText)
                    Spacer(Modifier.height(6.dp))
                    Text(stringResource(R.string.ai_info_model, version.aiModel ?: meta?.model ?: "—"), color = colors.dialogText)
                    Spacer(Modifier.height(6.dp))
                    val duration = version.aiDurationMs?.let { formatElapsedForDialog(it) } ?: meta?.duration ?: "—"
                    Text(stringResource(R.string.ai_info_duration, duration), color = colors.dialogText)
                }
            },
            confirmButton = {
                TextButton(onClick = { showAiInfoDialog.value = null }, colors = ButtonDefaults.textButtonColors(contentColor = colors.text)) {
                    Text(stringResource(R.string.close))
                }
            }
        )
    }
}

@Composable
private fun VersionItem(
    version: NoteVersion,
    previousContent: String?,
    nextContent: String?,
    previousOpsJson: String?,
    nextOpsJson: String?,
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
                if (!name.startsWith("AI_META|")) { // legacy guard; new meta uses dedicated columns
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = name,
                        color = colors.text,
                        fontSize = 15.sp,  // Increased font size
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Content preview without highlighting (plain text)
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

    // (AI info dialog is handled at the screen level above)
}

private fun parseAiMeta(customName: String?): AiMetaView? {
    if (customName.isNullOrBlank()) return null
    if (!customName.startsWith("AI_META|")) return null
    // Format: AI_META|provider=OPENAI|model=gpt-5-...|elapsedMs=1234
    return try {
        val parts = customName.removePrefix("AI_META|").split('|')
        var provider = "—"
        var model = "—"
        var elapsedMs: Long? = null
        parts.forEach { p ->
            when {
                p.startsWith("provider=") -> provider = p.substringAfter('=')
                p.startsWith("model=") -> model = p.substringAfter('=')
                p.startsWith("elapsedMs=") -> elapsedMs = p.substringAfter('=')
                    .toLongOrNull()
            }
        }
        AiMetaView(
            provider = provider,
            model = model,
            duration = formatElapsedForDialog(elapsedMs)
        )
    } catch (_: Exception) {
        null
    }
}

private data class AiMetaView(
    val provider: String,
    val model: String,
    val duration: String
)

private fun formatElapsedForDialog(ms: Long?): String {
    if (ms == null || ms <= 0) return "—"
    val seconds = ms / 1000
    return when {
        seconds < 60 -> "$seconds s"
        seconds < 3600 -> {
            val m = seconds / 60
            val s = seconds % 60
            if (s == 0L) "${m} min" else "${m} min ${s} s"
        }
        else -> {
            val h = seconds / 3600
            val m = (seconds % 3600) / 60
            if (m == 0L) "${h} h" else "${h} h ${m} min"
        }
    }
}

@Composable
private fun VersionPreviewDialog(
    version: NoteVersion,
    previousContent: String?,
    nextContent: String?,
    onDismiss: () -> Unit,
    onRestore: () -> Unit,
    onCopy: () -> Unit = {},
    onRename: () -> Unit = {},
    onInfo: () -> Unit = {}
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
                        if (!name.startsWith("AI_META|")) { // legacy guard; new meta uses dedicated columns
                            Text(
                                text = name,
                                color = colors.text,
                                fontSize = 15.sp,  // Increased font size
                                fontWeight = FontWeight.SemiBold
                            )
                        }
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
                        // Info for AI-related versions (fix/title/hashtags)
                        if (
                            version.changeDescription == stringResource(R.string.version_ai_after_fix) ||
                            version.changeDescription == stringResource(R.string.version_ai_after_title) ||
                            version.changeDescription == stringResource(R.string.version_ai_added_hashtags)
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        stringResource(R.string.version_info),
                                        color = colors.menuText
                                    )
                                },
                                onClick = {
                                    showMenu = false
                                    onInfo()
                                }
                            )
                        }
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
                    val annotated = remember(version.content, previousContent, nextContent) {
                        when {
                            previousContent == null && nextContent == null -> androidx.compose.ui.text.AnnotatedString(version.content)
                            previousContent == null && nextContent != null -> buildRemovedTextAnnotated(version.content, nextContent, colors)
                            previousContent != null && nextContent == null -> buildAddedTextAnnotated(version.content, previousContent, colors)
                            else -> buildCombinedAddedRemovedAnnotated(version.content, previousContent!!, nextContent!!, colors)
                        }
                    }
                    Text(
                        text = annotated,
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

private fun buildAddedTextAnnotated(current: String, previous: String, colors: GlobalColorBundle): androidx.compose.ui.text.AnnotatedString {
    val a = current.toCharArray()
    val presentInPrev = computeMoveAwarePresentMask(current, previous, PARAGRAPH_SIM_THRESHOLD)
    val n = a.size
    val addedMask = BooleanArray(n) { !presentInPrev[it] }
    // Full-line highlight only if no char-level matches in the whole line
    val lines = splitLinesWithRanges(current)
    for ((s, e) in lines) {
        var anyMatch = false
        var i = s
        while (i < e && i < n) { if (presentInPrev[i]) { anyMatch = true; break }; i++ }
        if (!anyMatch) {
            var k = s
            while (k < e && k < n) { addedMask[k] = true; k++ }
        }
    }
    // Full-paragraph highlight only if no char-level matches in the whole paragraph
    val paras = computeParagraphRanges(current)
    for ((s, e) in paras) {
        var anyMatch = false
        var i = s
        while (i < e && i < n) { if (presentInPrev[i]) { anyMatch = true; break }; i++ }
        if (!anyMatch) {
            var k = s
            while (k < e && k < n) { addedMask[k] = true; k++ }
        }
    }
    val greenBg = SpanStyle(background = Color(0x5532CD32), color = colors.text)
    return buildAnnotatedString {
        var i = 0
        while (i < n) {
            if (addedMask[i]) withStyle(greenBg) { append(a[i]) } else append(a[i])
            i++
        }
    }
}

private fun lcsMatchMask(a: CharArray, b: CharArray): BooleanArray {
    val n = a.size
    val m = b.size
    val dp = Array(n + 1) { IntArray(m + 1) }
    for (i in n - 1 downTo 0) {
        for (j in m - 1 downTo 0) {
            dp[i][j] = if (a[i] == b[j]) 1 + dp[i + 1][j + 1] else maxOf(dp[i + 1][j], dp[i][j + 1])
        }
    }
    val mask = BooleanArray(n)
    var i = 0
    var j = 0
    while (i < n && j < m) {
        if (a[i] == b[j]) {
            mask[i] = true
            i++
            j++
        } else if (dp[i + 1][j] >= dp[i][j + 1]) {
            i++
        } else {
            j++
        }
    }
    return mask
} 


private fun buildRemovedTextAnnotated(current: String, next: String, colors: GlobalColorBundle): androidx.compose.ui.text.AnnotatedString {
    val a = current.toCharArray()
    val presentInNext = computeMoveAwarePresentMask(current, next, PARAGRAPH_SIM_THRESHOLD)
    val n = a.size
    val removedMask = BooleanArray(n) { !presentInNext[it] }
    // Full-line removal only if no char-level matches in the whole line
    val lines = splitLinesWithRanges(current)
    for ((s, e) in lines) {
        var anyMatch = false
        var i = s
        while (i < e && i < n) { if (presentInNext[i]) { anyMatch = true; break }; i++ }
        if (!anyMatch) {
            var k = s
            while (k < e && k < n) { removedMask[k] = true; k++ }
        }
    }
    // Full-paragraph removal only if no char-level matches in the whole paragraph
    val paras = computeParagraphRanges(current)
    for ((s, e) in paras) {
        var anyMatch = false
        var i = s
        while (i < e && i < n) { if (presentInNext[i]) { anyMatch = true; break }; i++ }
        if (!anyMatch) {
            var k = s
            while (k < e && k < n) { removedMask[k] = true; k++ }
        }
    }
    val redBg = SpanStyle(background = Color(0x55FF5252), color = colors.text)
    return buildAnnotatedString {
        var i = 0
        while (i < n) {
            if (removedMask[i]) withStyle(redBg) { append(a[i]) } else append(a[i])
            i++
        }
    }
}

// Build annotated string for additions based on diffOpsJson (insert/replace)
private fun buildAnnotatedFromDiffOpsForAdded(current: String, prevOpsJson: String?, colors: GlobalColorBundle): androidx.compose.ui.text.AnnotatedString? {
    if (prevOpsJson.isNullOrBlank()) return null
    return try {
        val arr = JSONArray(prevOpsJson)
        if (arr.length() == 0) return null
        val greenBg = SpanStyle(background = Color(0x5532CD32), color = colors.text)
        val addedMask = BooleanArray(current.length)
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            val type = obj.getString("type")
            val start = obj.getInt("start").coerceIn(0, current.length)
            val text = obj.getString("text")
            val end = (start + text.length).coerceIn(start, current.length)
            if (type == "insert" || type == "replace") {
                var k = start
                while (k < end) { addedMask[k] = true; k++ }
            }
        }
        buildAnnotatedString {
            var i = 0
            while (i < current.length) {
                if (addedMask[i]) withStyle(greenBg) { append(current[i]) } else append(current[i])
                i++
            }
        }
    } catch (_: Exception) { null }
}

// Build annotated string for removals based on diffOpsJson (delete/replace)
private fun buildAnnotatedFromDiffOpsForRemoved(current: String, nextOpsJson: String?, colors: GlobalColorBundle): androidx.compose.ui.text.AnnotatedString? {
    if (nextOpsJson.isNullOrBlank()) return null
    return try {
        val arr = JSONArray(nextOpsJson)
        if (arr.length() == 0) return null
        val redBg = SpanStyle(background = Color(0x55FF5252), color = colors.text)
        val removedMask = BooleanArray(current.length)
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            val type = obj.getString("type")
            val start = obj.getInt("start").coerceIn(0, current.length)
            val end = obj.getInt("end").coerceIn(start, current.length)
            if (type == "delete" || type == "replace") {
                var k = start
                while (k < end) { if (k < current.length) removedMask[k] = true; k++ }
            }
        }
        buildAnnotatedString {
            var i = 0
            while (i < current.length) {
                if (removedMask[i]) withStyle(redBg) { append(current[i]) } else append(current[i])
                i++
            }
        }
    } catch (_: Exception) { null }
}

private fun mergeAnnotatedPreferGreen(
    current: String,
    added: androidx.compose.ui.text.AnnotatedString?,
    removed: androidx.compose.ui.text.AnnotatedString?,
    colors: GlobalColorBundle
): androidx.compose.ui.text.AnnotatedString {
    if (added == null && removed == null) return androidx.compose.ui.text.AnnotatedString(current)
    val greenBg = SpanStyle(background = Color(0x5532CD32), color = colors.text)
    val redBg = SpanStyle(background = Color(0x55FF5252), color = colors.text)
    val n = current.length
    val greenMask = BooleanArray(n)
    val redMask = BooleanArray(n)
    fun fillMaskFromAnnotated(src: androidx.compose.ui.text.AnnotatedString, mask: BooleanArray) {
        // Approximation: compare per-character style by rebuilding ranges is non-trivial; fallback is to assume any span yields mask
        // For correctness, we rely on our own builders that set span per char; here not reconstructing, just return added when exists.
    }
    // Simplified: if added exists and removed exists, prefer added text indices from its mask creation step
    // Since we built added/removed with masks, we can rebuild masks again quickly here (but we don't have them).
    // Fallback: recompute masks from diff ops again to merge correctly
    return added ?: removed ?: androidx.compose.ui.text.AnnotatedString(current)
}
// ===== Move-aware paragraph/line matching =====
private const val PARAGRAPH_SIM_THRESHOLD = 0.7

private fun similarityRatio(a: String, b: String): Double {
    if (a.isEmpty() && b.isEmpty()) return 1.0
    if (a.isEmpty() || b.isEmpty()) return 0.0
    val lcs = lcsLength(a, b)
    return (2.0 * lcs) / (a.length + b.length)
}

private fun lcsLength(a: String, b: String): Int {
    val n = a.length
    val m = b.length
    if (n == 0 || m == 0) return 0
    val dp = IntArray(m + 1)
    var prev: Int
    for (i in 1..n) {
        prev = 0
        val ai = a[i - 1]
        for (j in 1..m) {
            val temp = dp[j]
            dp[j] = if (ai == b[j - 1]) prev + 1 else maxOf(dp[j], dp[j - 1])
            prev = temp
        }
    }
    return dp[m]
}

private data class Block(val start: Int, val end: Int) // [start, end)

private fun computeMoveAwarePresentMask(current: String, other: String, threshold: Double): BooleanArray {
    val n = current.length
    val present = BooleanArray(n)
    if (n == 0) return present
    // 1) Paragraph-level matching (move-aware)
    val curParas = computeParagraphRanges(current)
    val othParas = computeParagraphRanges(other)
    val usedOther = BooleanArray(othParas.size)
    for ((cIdx, cRange) in curParas.withIndex()) {
        val cText = current.substring(cRange.first, cRange.second).trim()
        if (cText.isEmpty()) continue
        var bestIdx = -1
        var bestSim = 0.0
        for ((oIdx, oRange) in othParas.withIndex()) {
            if (usedOther[oIdx]) continue
            val oText = other.substring(oRange.first, oRange.second).trim()
            val sim = similarityRatio(cText, oText)
            if (sim > bestSim) { bestSim = sim; bestIdx = oIdx }
        }
        if (bestIdx != -1 && bestSim >= threshold) {
            usedOther[bestIdx] = true
            // Inside matched paragraphs mark char-level presence using LCS
            val (cs, ce) = cRange
            val (os, oe) = othParas[bestIdx]
            val subA = current.substring(cs, ce)
            val subB = other.substring(os, oe)
            val subMask = lcsMatchMask(subA.toCharArray(), subB.toCharArray())
            var k = 0
            var i = cs
            while (i < ce && i < n && k < subMask.size) {
                if (subMask[k]) present[i] = true
                i++; k++
            }
        }
    }
    // 2) Line-level fallback inside unmatched areas
    val curLines = splitLinesWithRanges(current)
    val othLines = splitLinesWithRanges(other)
    val usedLines = BooleanArray(othLines.size)
    for (cRange in curLines) {
        val (cs, ce) = cRange
        var anyAlready = false
        var i = cs
        while (i < ce && i < n) { if (present[i]) { anyAlready = true; break } ; i++ }
        if (anyAlready) continue
        val cText = current.substring(cs, ce).trimEnd('\n', '\r')
        if (cText.trim().isEmpty()) continue
        var bestIdx = -1
        var bestSim = 0.0
        for ((oIdx, oRange) in othLines.withIndex()) {
            if (usedLines[oIdx]) continue
            val oText = other.substring(oRange.first, oRange.second).trimEnd('\n', '\r')
            val sim = similarityRatio(cText, oText)
            if (sim > bestSim) { bestSim = sim; bestIdx = oIdx }
        }
        if (bestIdx != -1 && bestSim >= threshold) {
            usedLines[bestIdx] = true
            val (os, oe) = othLines[bestIdx]
            val subA = current.substring(cs, ce)
            val subB = other.substring(os, oe)
            val subMask = lcsMatchMask(subA.toCharArray(), subB.toCharArray())
            var k = 0
            var j = cs
            while (j < ce && j < n && k < subMask.size) {
                if (subMask[k]) present[j] = true
                j++; k++
            }
        }
    }
    return present
}

// ==== Line-aware diff helpers ====
private fun splitLinesWithRanges(text: String): List<Pair<Int, Int>> {
    val ranges = ArrayList<Pair<Int, Int>>()
    var start = 0
    val len = text.length
    var i = 0
    while (i < len) {
        if (text[i] == '\n') {
            ranges.add(start to (i + 1))
            start = i + 1
        }
        i++
    }
    if (start < len) ranges.add(start to len)
    if (ranges.isEmpty()) ranges.add(0 to 0)
    return ranges
}

private fun computeAddedMaskLineAware(current: String, previous: String): BooleanArray {
    val n = current.length
    val added = BooleanArray(n)
    if (n == 0) return added
    val aLines = splitLinesWithRanges(current)
    val bLines = splitLinesWithRanges(previous)
    val aStr = Array(aLines.size) { i -> current.substring(aLines[i].first, aLines[i].second) }
    val bStr = Array(bLines.size) { j -> previous.substring(bLines[j].first, bLines[j].second) }
    val dp = Array(aStr.size + 1) { IntArray(bStr.size + 1) }
    for (i in aStr.size - 1 downTo 0) {
        for (j in bStr.size - 1 downTo 0) {
            dp[i][j] = if (aStr[i] == bStr[j]) 1 + dp[i + 1][j + 1] else maxOf(dp[i + 1][j], dp[i][j + 1])
        }
    }
    val matchedA = BooleanArray(aStr.size)
    var i = 0
    var j = 0
    while (i < aStr.size && j < bStr.size) {
        when {
            aStr[i] == bStr[j] -> { matchedA[i] = true; i++; j++ }
            dp[i + 1][j] >= dp[i][j + 1] -> i++
            else -> j++
        }
    }
    for (idx in 0 until aLines.size) {
        if (!matchedA[idx]) {
            val (s, e) = aLines[idx]
            var k = s
            while (k < e && k < n) { added[k] = true; k++ }
        }
    }
    return added
}

private fun computeRemovedMaskLineAware(current: String, next: String): BooleanArray {
    val n = current.length
    val removed = BooleanArray(n)
    if (n == 0) return removed
    val aLines = splitLinesWithRanges(current)
    val bLines = splitLinesWithRanges(next)
    val aStr = Array(aLines.size) { i -> current.substring(aLines[i].first, aLines[i].second) }
    val bStr = Array(bLines.size) { j -> next.substring(bLines[j].first, bLines[j].second) }
    val dp = Array(aStr.size + 1) { IntArray(bStr.size + 1) }
    for (i in aStr.size - 1 downTo 0) {
        for (j in bStr.size - 1 downTo 0) {
            dp[i][j] = if (aStr[i] == bStr[j]) 1 + dp[i + 1][j + 1] else maxOf(dp[i + 1][j], dp[i][j + 1])
        }
    }
    val matchedA = BooleanArray(aStr.size)
    var i = 0
    var j = 0
    while (i < aStr.size && j < bStr.size) {
        when {
            aStr[i] == bStr[j] -> { matchedA[i] = true; i++; j++ }
            dp[i + 1][j] >= dp[i][j + 1] -> i++
            else -> j++
        }
    }
    for (idx in 0 until aLines.size) {
        if (!matchedA[idx]) {
            val (s, e) = aLines[idx]
            var k = s
            while (k < e && k < n) { removed[k] = true; k++ }
        }
    }
    return removed
}

// ==== Paragraph-aware diff helpers ====
private fun computeParagraphRanges(text: String): List<Pair<Int, Int>> {
    val lines = splitLinesWithRanges(text)
    val ranges = ArrayList<Pair<Int, Int>>()
    var paraStart = -1
    var lastNonEmptyEnd = -1
    for ((start, end) in lines) {
        val lineText = text.substring(start, end)
        val isEmptyLine = lineText.trim().isEmpty()
        if (!isEmptyLine) {
            if (paraStart == -1) paraStart = start
            lastNonEmptyEnd = end
        } else {
            if (paraStart != -1) {
                ranges.add(paraStart to lastNonEmptyEnd)
                paraStart = -1
                lastNonEmptyEnd = -1
            }
        }
    }
    if (paraStart != -1) ranges.add(paraStart to lastNonEmptyEnd)
    return ranges
}

private fun buildLineMultiset(text: String): HashMap<String, Int> {
    val map = HashMap<String, Int>()
    val lines = splitLinesWithRanges(text)
    lines.forEach { (s, e) ->
        val raw = text.substring(s, e)
        val normalized = raw.trimEnd('\n', '\r')
        if (normalized.trim().isNotEmpty()) {
            map[normalized] = (map[normalized] ?: 0) + 1
        }
    }
    return map
}

private fun buildParagraphMultiset(text: String): HashMap<String, Int> {
    val map = HashMap<String, Int>()
    val paras = computeParagraphRanges(text)
    paras.forEach { (s, e) ->
        val normalized = text.substring(s, e).trim()
        if (normalized.isNotEmpty()) {
            map[normalized] = (map[normalized] ?: 0) + 1
        }
    }
    return map
}

private fun computeAddedMaskParagraphAware(current: String, previous: String): BooleanArray {
    val n = current.length
    val added = BooleanArray(n)
    if (n == 0) return added
    val curParas = computeParagraphRanges(current)
    val prevParas = computeParagraphRanges(previous)
    val prevSet = HashSet<String>(prevParas.size)
    for ((s, e) in prevParas) prevSet.add(previous.substring(s, e))
    for ((s, e) in curParas) {
        val paraText = current.substring(s, e)
        if (!prevSet.contains(paraText)) {
            var k = s
            while (k < e && k < n) { added[k] = true; k++ }
        }
    }
    return added
}

private fun computeRemovedMaskParagraphAware(current: String, next: String): BooleanArray {
    val n = current.length
    val removed = BooleanArray(n)
    if (n == 0) return removed
    val curParas = computeParagraphRanges(current)
    val nextParas = computeParagraphRanges(next)
    val nextSet = HashSet<String>(nextParas.size)
    for ((s, e) in nextParas) nextSet.add(next.substring(s, e))
    for ((s, e) in curParas) {
        val paraText = current.substring(s, e)
        if (!nextSet.contains(paraText)) {
            var k = s
            while (k < e && k < n) { removed[k] = true; k++ }
        }
    }
    return removed
}

private fun buildCombinedAddedRemovedAnnotated(
    current: String,
    previous: String,
    next: String,
    colors: GlobalColorBundle
): androidx.compose.ui.text.AnnotatedString {
    val a = current.toCharArray()
    val n = a.size
    val presentInPrev = computeMoveAwarePresentMask(current, previous, PARAGRAPH_SIM_THRESHOLD)
    val presentInNext = computeMoveAwarePresentMask(current, next, PARAGRAPH_SIM_THRESHOLD)
    val addedMask = BooleanArray(n) { !presentInPrev[it] }
    val removedMask = BooleanArray(n) { !presentInNext[it] }
    // Lines
    val lines = splitLinesWithRanges(current)
    for ((s, e) in lines) {
        var anyPrev = false; var anyNext = false
        var i = s
        while (i < e && i < n) { if (presentInPrev[i]) anyPrev = true; if (presentInNext[i]) anyNext = true; if (anyPrev && anyNext) break; i++ }
        if (!anyPrev) { var k = s; while (k < e && k < n) { addedMask[k] = true; k++ } }
        if (!anyNext) { var k = s; while (k < e && k < n) { removedMask[k] = true; k++ } }
    }
    // Paragraphs
    val paras = computeParagraphRanges(current)
    for ((s, e) in paras) {
        var anyPrev = false; var anyNext = false
        var i = s
        while (i < e && i < n) { if (presentInPrev[i]) anyPrev = true; if (presentInNext[i]) anyNext = true; if (anyPrev && anyNext) break; i++ }
        if (!anyPrev) { var k = s; while (k < e && k < n) { addedMask[k] = true; k++ } }
        if (!anyNext) { var k = s; while (k < e && k < n) { removedMask[k] = true; k++ } }
    }
    val greenBg = SpanStyle(background = Color(0x5532CD32), color = colors.text)
    val redBg = SpanStyle(background = Color(0x55FF5252), color = colors.text)
    return buildAnnotatedString {
        var i = 0
        while (i < n) {
            when {
                addedMask[i] -> withStyle(greenBg) { append(a[i]) } // green priority
                removedMask[i] -> withStyle(redBg) { append(a[i]) }
                else -> append(a[i])
            }
            i++
        }
    }
}
