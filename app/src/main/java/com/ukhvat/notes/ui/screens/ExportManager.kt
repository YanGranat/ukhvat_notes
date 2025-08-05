package com.ukhvat.notes.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.annotation.StringRes
import com.ukhvat.notes.R
import com.ukhvat.notes.data.export.DatabaseExporter
import com.ukhvat.notes.data.export.IndividualMarkdownExporter
import com.ukhvat.notes.data.export.MarkdownExporter
import com.ukhvat.notes.domain.model.Note
import com.ukhvat.notes.domain.util.Toaster
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Note export manager
 * 
 * Centralized class for all note export operations in various formats.
 * Extracted from NotesListViewModel for architecture and testability.
 * 
 * Supported formats:
 * - Text: Direct sharing via Android Intent
 * - Markdown: Single file with all notes
 * - Archive: ZIP file with separate MD files
 * - Database: Full SQLite database copy
 * - Individual MD: Separate files for each note in folder
 * 
 * All operations include Toast notifications and error handling.
 * 
 * @param markdownExporter Exporter for creating markdown files
 * @param databaseExporter Exporter for DB backup
 * @param individualMarkdownExporter Exporter for creating separate MD files
 * @param toaster Service for showing Toast notifications to user
 */
class ExportManager(
    private val markdownExporter: MarkdownExporter,
    private val databaseExporter: DatabaseExporter,
    private val individualMarkdownExporter: IndividualMarkdownExporter,
    private val toaster: Toaster,
    private val context: Context,
    private val fileIoDispatcher: kotlinx.coroutines.CoroutineDispatcher
) {
    
    /**
     * Universal export system with Toast notifications
     * 
     * Processes note export in various formats with instant feedback.
     * Uses centralized Toast system for user notifications.
     * 
     * Export types:
     * - "text": Direct sharing via Intent.ACTION_SEND (no dialogs)
     * - "markdown", "archive", "database", "individual_markdown": Via dialogs
     * 
     * Text export features:
     * - 512KB size limit (Android Intent limitations)
     * - Toast notification on limit exceeded with suggestion to use file export
     * - Direct integration with Android sharing menu
     * - No Toast notification on successful export
     * 
     * @param type Export type ("text", "markdown", "archive", "database", "individual_markdown")
     * @param context Android Context for creating Intent and Toast notifications
     * @param notes Notes for export
     * @param onResult Callback for result processing (success: Boolean, content: String?, type: String?, error: String?)
     */
    suspend fun exportNotes(
        type: String, 
        context: Context, 
        notes: List<Note>,
        onResult: (success: Boolean, content: String?, exportType: String?, error: String?) -> Unit
    ) {
        try {
            if (notes.isEmpty()) {
                val errorMsg = context.getString(R.string.no_notes_for_export)
                toaster.toast(errorMsg)
                onResult(false, null, null, errorMsg)
                return
            }
            
            when (type) {
                "text" -> {
                    // Text export via Android sharing
                    // Combine all notes into single text with separators
                    val textContent = notes.filter { it.content.isNotBlank() }
                         .joinToString("\n\n------\n\n") { note ->
                             note.content
                         }
                    
                    // Size check: Android Intent has ~1MB limit, using 500KB for reliability
                    val maxSizeBytes = 512 * 1024 // 512KB in bytes
                    val contentSizeBytes = textContent.toByteArray(Charsets.UTF_8).size
                    
                    if (contentSizeBytes > maxSizeBytes) {
                        // Too much data for Intent export
                        val sizeMB = String.format("%.1f", contentSizeBytes / (1024.0 * 1024.0))
                        toaster.toast(context.getString(R.string.export_text_too_large, sizeMB))
                        onResult(false, null, null, context.getString(R.string.content_too_large_sharing))
                        return
                    }
                    
                    // Create standard Android sharing Intent
                    val shareIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        setType("text/plain")
                        putExtra(Intent.EXTRA_TEXT, textContent)
                    }
                    
                    // Launch system app selection menu for sharing
                    val chooserIntent = Intent.createChooser(shareIntent, context.getString(R.string.export_choose_app))
                    chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(chooserIntent)
                    
                    // No Toast notification for text export
                    // User gets feedback via Android sharing interface
                    onResult(true, null, null, null)
                }
                else -> {
                    // For other types show dialog as before
                    val content = when (type) {
                        "markdown" -> {
                            markdownExporter.exportNotes(notes.filter { it.content.isNotBlank() })
                        }
                        "archive" -> {
                            notes.filter { it.content.isNotBlank() }
                                 .joinToString("\n\n------\n\n") { note ->
                                     note.content // No size limits
                                 }
                        }
                        "database" -> {
                            """
                            SQLite database file will be exported:
                            • All notes with full text (including unselected)
                            • Note version history
                            • Creation and modification timestamps
                            • Format: .db file for opening in SQLite programs
                            • Available only for general export of all notes
                            
                            Use corresponding button in export menu to create file.
                            """.trimIndent()
                        }
                        "individual_markdown" -> {
                            val filteredNotes = notes.filter { it.content.isNotBlank() }
                            """
                            Separate MD files will be created in folder:
                            • Notes: ${filteredNotes.size}
                            • Each note = separate .md file in selected folder
                            • File names from note titles (cleaned of special chars)
                            • Content: only note text without headers
                            • Safe names for Windows, Mac, Linux
                            • Select folder to save files
                            
                            ${context.getString(R.string.use_individual_md_button)}
                            """.trimIndent()
                        }
                        else -> {
                            onResult(false, null, null, context.getString(R.string.unsupported_export_type, type))
                            return
                        }
                    }
                    
                    // No size limits for export
                    
                    onResult(true, content, type, null)
                }
            }
        } catch (e: Exception) {
            onResult(false, null, null, context.getString(R.string.export_generic_error, e.localizedMessage ?: "Unknown error"))
        }
    }

    /**
     * Database export with Toast notifications
     * 
     * Exports full SQLite database including all notes and version history.
     * Integrated with centralized Toast system for instant feedback.
     * 
     * Logic:
     * 1. Creating DB copy via DatabaseExporter
     * 2. Launching Android sharing Intent for app selection
     * 3. Toast notification on successful export (R.string.exported_to_database)
     * 4. Error handling via error state
     * 
     * @param context Android Context for creating Intent and displaying chooser
     * @param onResult Callback for result processing
     */
    suspend fun exportDatabaseFile(
        context: Context,
        onResult: (success: Boolean, error: String?) -> Unit
    ) {
        try {
            val exportIntent = databaseExporter.exportDatabase(context)
            
            if (exportIntent != null) {
                context.startActivity(Intent.createChooser(exportIntent, context.getString(R.string.database_export_chooser)))
                
                // Show toast notification about successful export
                toaster.toast(R.string.exported_to_database)
                onResult(true, null)
            } else {
                onResult(false, context.getString(R.string.database_export_failed))
            }

        } catch (e: Exception) {
            onResult(false, context.getString(R.string.database_export_error, e.localizedMessage ?: "Unknown error"))
        }
    }

    suspend fun exportIndividualMarkdownToFolder(
        context: Context, 
        folderUri: Uri, 
        notes: List<Note>,
        onResult: (success: Boolean, successCount: Int?, error: String?) -> Unit
    ) {
        withContext(fileIoDispatcher) {
            try {
                val notesToExport = notes.filter { it.content.isNotBlank() }
                
                if (notesToExport.isEmpty()) {
                    val errorMsg = context.getString(R.string.no_content_notes_export)
                    toaster.toast(errorMsg)
                    onResult(false, null, errorMsg)
                    return@withContext
                }

                // Size limit: Protection from memory and disk space overflow
                val totalContentSize = notesToExport.sumOf { it.content.toByteArray(Charsets.UTF_8).size }
                val maxSizeBytes = 200 * 1024 * 1024 // 200MB limit (text compresses well)
                
                if (totalContentSize > maxSizeBytes) {
                    val sizeMB = String.format("%.1f", totalContentSize / (1024.0 * 1024.0))
                    val errorMsg = context.getString(R.string.data_too_large_200mb, sizeMB)
                    toaster.toast(errorMsg)
                    onResult(false, null, errorMsg)
                    return@withContext
                }

                val successCount = try {
                    individualMarkdownExporter.exportNotesToFolder(
                        context = context,
                        folderUri = folderUri,
                        notes = notesToExport
                    )
                } catch (e: SecurityException) {
                    val errorMsg = context.getString(R.string.folder_access_denied)
                    toaster.toast(errorMsg)
                    onResult(false, null, errorMsg)
                    return@withContext
                } catch (e: IllegalArgumentException) {
                    val errorMsg = e.localizedMessage ?: context.getString(R.string.folder_selection_problem)
                    toaster.toast(errorMsg)
                    onResult(false, null, errorMsg)
                    return@withContext
                } catch (e: Exception) {
                    val errorMsg = context.getString(R.string.md_export_error, e.localizedMessage ?: context.getString(R.string.unknown_error))
                    toaster.toast(errorMsg)
                    onResult(false, null, errorMsg)
                    return@withContext
                }

                if (successCount > 0) {
                    toaster.toast(
                        context.getString(R.string.exported_md_files, successCount)
                    )
                    onResult(true, successCount, null)
                } else {
                    val errorMsg = context.getString(R.string.no_files_exported)
                    toaster.toast(errorMsg)
                    onResult(false, null, errorMsg)
                }

            } catch (e: Exception) {
                val errorMsg = context.getString(R.string.md_export_error, e.localizedMessage ?: context.getString(R.string.unknown_error))
                toaster.toast(errorMsg)
                onResult(false, null, errorMsg)
            }
        }
    }
    
    suspend fun exportMarkdownFileWithSelection(
        context: Context, 
        fileUri: Uri, 
        notes: List<Note>,
        onResult: (success: Boolean, error: String?) -> Unit
    ) {
        withContext(fileIoDispatcher) {
            try {
                val notesToExport = notes.filter { it.content.isNotBlank() }
                
                if (notesToExport.isEmpty()) {
                    val errorMsg = context.getString(R.string.no_content_notes_export)
                    toaster.toast(errorMsg)
                    onResult(false, errorMsg)
                    return@withContext
                }
                
                val markdownContent = notesToExport.joinToString("\n\n------\n\n") { note ->
                    note.content
                }
                
                // Size check: Protection from very large files
                val contentSizeBytes = markdownContent.toByteArray(Charsets.UTF_8).size
                val maxSizeBytes = 10 * 1024 * 1024 // 10MB limit for file export
                
                if (contentSizeBytes > maxSizeBytes) {
                    val sizeMB = String.format("%.1f", contentSizeBytes / (1024.0 * 1024.0))
                    val errorMsg = context.getString(R.string.data_too_large_10mb, sizeMB)
                    toaster.toast(errorMsg)
                    onResult(false, errorMsg)
                    return@withContext
                }
                
                // Write file with forced close
                context.contentResolver.openOutputStream(fileUri)?.use { outputStream ->
                    outputStream.write(markdownContent.toByteArray(Charsets.UTF_8))
                    outputStream.flush()
                } ?: run {
                    val errorMsg = context.getString(R.string.file_write_access_denied)
                    toaster.toast(errorMsg)
                    onResult(false, errorMsg)
                    return@withContext
                }
                
                // Show toast notification about successful export
                toaster.toast(R.string.exported_to_markdown)
                onResult(true, null)
                
            } catch (e: SecurityException) {
                val errorMsg = context.getString(R.string.file_access_denied)
                toaster.toast(errorMsg)
                onResult(false, errorMsg)
            } catch (e: java.io.IOException) {
                val errorMsg = context.getString(R.string.file_write_error, e.localizedMessage ?: context.getString(R.string.disk_space_problem))
                toaster.toast(errorMsg)
                onResult(false, errorMsg)
            } catch (e: Exception) {
                val errorMsg = context.getString(R.string.markdown_save_error, e.localizedMessage ?: context.getString(R.string.unknown_error))
                toaster.toast(errorMsg)
                onResult(false, errorMsg)
            }
        }
    }
    
    suspend fun exportArchiveFileWithSelection(
        context: Context, 
        fileUri: Uri, 
        notes: List<Note>,
        onResult: (success: Boolean, error: String?) -> Unit
    ) {
        withContext(fileIoDispatcher) {
            try {
                val notesToExport = notes.filter { it.content.isNotBlank() }
                
                if (notesToExport.isEmpty()) {
                    val errorMsg = context.getString(R.string.no_content_notes_export)
                    toaster.toast(errorMsg)
                    onResult(false, errorMsg)
                    return@withContext
                }
                
                // Size check: Protection from very large archives
                val totalContentSize = notesToExport.sumOf { it.content.toByteArray(Charsets.UTF_8).size }
                val maxSizeBytes = 20 * 1024 * 1024 // 20MB limit for ZIP archives
                
                if (totalContentSize > maxSizeBytes) {
                    val sizeMB = String.format("%.1f", totalContentSize / (1024.0 * 1024.0))
                    val errorMsg = context.getString(R.string.data_too_large_20mb, sizeMB)
                    toaster.toast(errorMsg)
                    onResult(false, errorMsg)
                    return@withContext
                }
                
                // Creating ZIP archive with error handling
                val zipContent = try {
                    createZipArchive(notesToExport)
                } catch (e: Exception) {
                    val errorMsg = context.getString(R.string.archive_creation_error, e.localizedMessage ?: context.getString(R.string.compression_data_problem))
                    toaster.toast(errorMsg)
                    onResult(false, errorMsg)
                    return@withContext
                }
                
                if (zipContent.isEmpty()) {
                    val errorMsg = context.getString(R.string.empty_archive_created)
                    toaster.toast(errorMsg)
                    onResult(false, errorMsg)
                    return@withContext
                }
                
                // Write archive with forced close
                context.contentResolver.openOutputStream(fileUri)?.use { outputStream ->
                    outputStream.write(zipContent)
                    outputStream.flush()
                } ?: run {
                    val errorMsg = context.getString(R.string.archive_write_access_denied)
                    toaster.toast(errorMsg)
                    onResult(false, errorMsg)
                    return@withContext
                }
                
                // Show toast notification about successful export
                toaster.toast(R.string.exported_to_archive)
                onResult(true, null)
                
            } catch (e: SecurityException) {
                val errorMsg = context.getString(R.string.archive_access_denied)
                toaster.toast(errorMsg)
                onResult(false, errorMsg)
            } catch (e: java.io.IOException) {
                val errorMsg = context.getString(R.string.archive_write_error, e.localizedMessage ?: context.getString(R.string.disk_space_problem))
                toaster.toast(errorMsg)
                onResult(false, errorMsg)
            } catch (e: Exception) {
                val errorMsg = context.getString(R.string.archive_creation_error, e.localizedMessage ?: context.getString(R.string.unknown_error))
                toaster.toast(errorMsg)
                onResult(false, errorMsg)
            }
        }
    }
    
    private fun createZipArchive(notes: List<Note>): ByteArray {
        val byteArrayOutputStream = java.io.ByteArrayOutputStream()
        val usedFileNames = mutableSetOf<String>()
        
        java.util.zip.ZipOutputStream(byteArrayOutputStream).use { zipOutputStream ->
            notes.forEachIndexed { index, note ->
                try {
                    // Generate unique filename
                    val cleanTitle = sanitizeFileName(note.title)
                    var fileName = if (cleanTitle.isNotBlank() && cleanTitle.length >= 3) {
                        cleanTitle
                    } else {
                        "note_${index + 1}"
                    }
                    
                    // Ensure filename uniqueness
                    var counter = 1
                    val originalFileName = fileName
                    while (usedFileNames.contains("$fileName.md")) {
                        fileName = "${originalFileName}_$counter"
                        counter++
                        if (counter > 100) { // Protection from infinite loop
                            fileName = "note_${System.currentTimeMillis()}_${index + 1}"
                            break
                        }
                    }
                    val fullFileName = "$fileName.md"
                    usedFileNames.add(fullFileName)
                    
                    // Create ZIP entry
                    val entry = java.util.zip.ZipEntry(fullFileName)
                    entry.time = System.currentTimeMillis() // Set modification time
                    zipOutputStream.putNextEntry(entry)
                    
                    // Write note content
                    val noteContentBytes = note.content.toByteArray(Charsets.UTF_8)
                    if (noteContentBytes.isNotEmpty()) {
                        zipOutputStream.write(noteContentBytes)
                    }
                    zipOutputStream.closeEntry()
                    
                } catch (e: Exception) {
                                    // Skip problematic note, continue with others
                // This ensures creation of at least partial archive
                    return@forEachIndexed
                }
            }
            
            // Force finish ZIP stream
            zipOutputStream.finish()
        }
        
        val result = byteArrayOutputStream.toByteArray()
        
                    // Check that archive is not empty
        if (result.isEmpty()) {
            throw IllegalStateException(context.getString(R.string.archive_creation_failed))
        }
        
        return result
    }
    
    private fun sanitizeFileName(fileName: String): String {
        return fileName
            .replace(Regex("[\\\\/:*?\"<>|]"), "_")
            .take(50)
            .trim()
    }
}