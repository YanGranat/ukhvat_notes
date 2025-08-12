package com.ukhvat.notes.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.ui.res.painterResource
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.shadow
import org.koin.androidx.compose.koinViewModel
import com.ukhvat.notes.domain.model.Note
import com.ukhvat.notes.R
import com.ukhvat.notes.ui.theme.ColorManager
import com.ukhvat.notes.ui.theme.rememberGlobalColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchiveScreen(
    onNavigateBack: () -> Unit,
    viewModel: ArchiveViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val colors = rememberGlobalColors()
    var showPreviewDialog by remember { mutableStateOf<Note?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.archive_screen_title), color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back), tint = Color.White)
                    }
                },
                actions = {
                    // Restore all
                    IconButton(onClick = { viewModel.onEvent(ArchiveEvent.RestoreAll) }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_restore_all),
                            contentDescription = stringResource(R.string.restore_all),
                            tint = Color.White,
                            modifier = Modifier.size(22.dp) // ~20% меньше 28dp
                        )
                    }
                    // Delete all (soft delete to trash) with confirm dialog
                    IconButton(onClick = { viewModel.onEvent(ArchiveEvent.ShowDeleteAllDialog) }) {
                        Icon(painter = painterResource(id = R.drawable.ic_delete_all), contentDescription = stringResource(R.string.delete_all_soft), tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF1c75d3))
            )
        },
        containerColor = colors.background
    ) { paddingValues ->
        Box(Modifier.fillMaxSize().padding(paddingValues)) {
            if (uiState.archivedNotes.isEmpty()) {
                EmptyArchiveMessage(Modifier.align(Alignment.Center))
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.archivedNotes) { note ->
                        ArchivedNoteItem(
                            note = note,
                            onRestore = { viewModel.onEvent(ArchiveEvent.RestoreNote(note.id)) },
                            onDelete = { viewModel.onEvent(ArchiveEvent.DeleteToTrash(note.id)) },
                            onNoteClick = { showPreviewDialog = note }
                        )
                    }
                }
            }
        }
    }

    // Preview dialog like in TrashScreen
    showPreviewDialog?.let { note ->
        AlertDialog(
            onDismissRequest = { showPreviewDialog = null },
            containerColor = colors.dialogBackground,
            shape = RoundedCornerShape(12.dp),
            title = {
                Text(
                    text = stringResource(R.string.note_preview_title),
                    color = colors.menuText,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
            },
            text = {
                LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
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
                TextButton(onClick = { showPreviewDialog = null }, colors = ButtonDefaults.textButtonColors(contentColor = colors.text)) {
                    Text(stringResource(R.string.close))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        viewModel.onEvent(ArchiveEvent.RestoreNote(note.id))
                        showPreviewDialog = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = colors.primary),
                    modifier = Modifier.offset(x = (-34).dp)
                ) {
                    Text(stringResource(R.string.restore_from_archive))
                }
            }
        )
    }

    // Confirm delete all
    if (uiState.showDeleteAllDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.onEvent(ArchiveEvent.DismissDeleteAllDialog) },
            containerColor = colors.dialogBackground,
            shape = RoundedCornerShape(12.dp),
            title = { Text(stringResource(R.string.delete_all_soft), color = colors.menuText, fontSize = 18.sp, fontWeight = FontWeight.Medium) },
            text = { Text(stringResource(R.string.delete_all_soft_confirm), color = colors.text) },
            confirmButton = {
                TextButton(onClick = { viewModel.onEvent(ArchiveEvent.DeleteAll) }, colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFC62828))) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onEvent(ArchiveEvent.DismissDeleteAllDialog) }, colors = ButtonDefaults.textButtonColors(contentColor = colors.primary)) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun ArchivedNoteItem(
    note: Note,
    onRestore: () -> Unit,
    onDelete: () -> Unit,
    onNoteClick: () -> Unit
) {
    val colors = rememberGlobalColors()
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = if (ColorManager.isDarkTheme()) 0.dp else 2.dp, shape = RoundedCornerShape(8.dp))
            .clickable { onNoteClick() },
        colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = stringResource(R.string.archived_date, note.formattedArchivedDate ?: stringResource(R.string.unknown)), color = colors.textSecondary, fontSize = 12.sp)
            Spacer(Modifier.height(8.dp))
            Text(text = note.content.take(100).ifEmpty { stringResource(R.string.note_is_empty) }, color = colors.text, fontSize = 14.sp, lineHeight = 20.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
                OutlinedButton(
                    onClick = onDelete,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFC62828)),
                    border = BorderStroke(1.dp, Color(0xFFC62828)),
                    modifier = Modifier.height(32.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                ) { Text(text = stringResource(R.string.archive_delete_to_trash), fontSize = 11.sp, textAlign = TextAlign.Center) }

                OutlinedButton(
                    onClick = onRestore,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.primary),
                    border = BorderStroke(1.dp, colors.primary),
                    modifier = Modifier.height(32.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) { Text(text = stringResource(R.string.restore_from_archive), fontSize = 11.sp, textAlign = TextAlign.Center) }
            }
        }
    }
}

@Composable
private fun EmptyArchiveMessage(modifier: Modifier = Modifier) {
    val colors = rememberGlobalColors()
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(text = "📦", fontSize = 48.sp)
        Spacer(Modifier.height(16.dp))
        Text(text = stringResource(R.string.archive_empty), color = colors.text, fontSize = 18.sp)
    }
}


