package com.ukhvat.notes.data.export

import android.content.Context
import android.net.Uri
import com.ukhvat.notes.domain.repository.NotesRepository
import com.ukhvat.notes.domain.model.Note
import kotlinx.coroutines.Dispatchers
import com.ukhvat.notes.R
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream
// Removed Hilt imports for Koin migration

/**
 * Notes importer from ZIP archives with Markdown and text files.
 * 
 * Supports note import from ZIP archives created by export function.
 * Each .md and .txt file in archive becomes separate note.
 * 
 * Architecture solution - Individual import:
 * Uses repository.insertNote() for each file from archive instead of
 * preliminary accumulation in List + batch operations.
 * 
 * UX ADVANTAGES:
 * - Notes appear in main menu as archive is processed
 * - User sees import progress in real-time
 * - Ability to interrupt import while keeping already processed files
 * - UI remains responsive when processing large archives
 * 
 * TECHNICAL IMPLEMENTATION:
 * - ZipInputStream for sequential file reading
 * - repository.insertNote() with instant UI reflection via Flow
 * - Incremental timestamps for correct sorting
 * - Error handling at individual file level without interrupting entire import
 * 
 * Transition from batch to individual import
 */
/**
 * MIGRATED FROM HILT TO KOIN
 * 
 * Dependencies injected via Koin instead of @Inject constructor:
 * - Context: for file operations
 * - NotesRepositoryImpl: specific implementation for batch operations
 */
/**
      * MIGRATION: Moved to NotesRepository interface.
     * Now works with ModularNotesRepository through unified interface.
 */
class ArchiveImporter(
    private val context: Context,
    private val repository: NotesRepository
) {

    /**
     * Imports notes from ZIP archive with Markdown and text files.
     * 
     * @param uri ZIP file URI for import
     * @return Number of successfully imported notes
     */
    suspend fun importFromArchive(uri: Uri): Int = withContext(Dispatchers.IO) {
        try {
            var importedCount = 0
            val currentTime = System.currentTimeMillis()
            
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                ZipInputStream(inputStream).use { zipStream ->
                    var entry = zipStream.nextEntry
                    
                    while (entry != null) {
                        // Process only .md and .txt files, skip folders
                        if (!entry.isDirectory && (entry.name.endsWith(".md", ignoreCase = true) || entry.name.endsWith(".txt", ignoreCase = true))) {
                            try {
                                // Read file content
                                val content = zipStream.readBytes().toString(Charsets.UTF_8)
                                
                                // Skip empty files
                                if (content.isNotBlank()) {
                                    // Extract title from filename
                                    val fileName = File(entry.name).nameWithoutExtension
                                    val title = sanitizeTitle(fileName)
                                    
                                    // Create and immediately save note (instant list appearance)
                                    val note = Note(
                                        id = 0, // Auto-generate ID
                                        content = content,
                                        createdAt = currentTime + importedCount, // Small shifts for uniqueness
                                        updatedAt = currentTime + importedCount,
                                        cachedTitle = if (title.isNotBlank()) title else null
                                    )
                                    
                                    repository.insertNote(note)
                                    importedCount++
                                }
                                
                                        } catch (e: Exception) {
                // Skip corrupted files, continue import
                                e.printStackTrace()
                            }
                        }
                        
                        zipStream.closeEntry()
                        entry = zipStream.nextEntry
                    }
                }
            }
            
            importedCount
            
        } catch (e: Exception) {
            // Log error and return count of already imported
            e.printStackTrace()
            0
        }
    }

    /**
     * Cleans filename from service symbols and prefixes
     */
    private fun sanitizeTitle(fileName: String): String {
        return fileName
            .replace(Regex("^${context.getString(R.string.note_prefix_pattern)}", RegexOption.IGNORE_CASE), "") // Remove "note_N_" prefix
            .replace(Regex("^note_\\d+_", RegexOption.IGNORE_CASE), "") // Remove "note_N_" prefix
            .replace(Regex("[_-]+"), " ") // Replace underscores and hyphens with spaces
            .replace(Regex("\\s+"), " ") // Remove multiple spaces
            .trim()
            .take(50) // Limit title length
    }

    /**
     * Returns information about archive import capabilities.
     */
    fun getImportInfo(): String {
        return context.getString(R.string.archive_import_description)
    }
} 