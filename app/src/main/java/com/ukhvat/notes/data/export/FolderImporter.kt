package com.ukhvat.notes.data.export

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.ukhvat.notes.domain.repository.NotesRepository
import com.ukhvat.notes.domain.model.Note
import kotlinx.coroutines.Dispatchers
import com.ukhvat.notes.R
import kotlinx.coroutines.withContext
// Removed Hilt imports for Koin migration

/**
 * Notes importer from folder with individual Markdown and text files.
 * 
 * Scans selected folder and imports all .md and .txt files as separate notes.
 * Uses Storage Access Framework for file access.
 * 
 * Architecture solution - Individual import:
 * Uses repository.insertNote() for each file instead of batch operations
 * to provide visual progress during long import operations.
 * 
 * UX RATIONALE:
 * - MD files: slow disk read operation → visual progress needed
 * - Database import: fast operation → batch mode justified
 * - User sees work result, can interrupt process
 * 
 * TECHNICAL IMPLEMENTATION:
 * - repository.insertNote() for instant appearance in list via Flow
 * - Recursive DocumentFile traversal for nested folder support
 * - Unique timestamps (baseTime + count) for proper sorting
 * 
 * Transition from batch to individual import
 */
/**
 * MIGRATED FROM HILT TO KOIN
 * 
 * Dependencies injected via Koin instead of @Inject constructor:
 * - Context: for file operations
 * - NotesRepositoryImpl: specific implementation for individual import operations
 */
/**
      * MIGRATION: Moved to NotesRepository interface.
     * Now works with ModularNotesRepository through unified interface.
 */
class FolderImporter(
    private val context: Context,
    private val repository: NotesRepository
) {

    /**
     * Imports all .md and .txt files from selected folder.
     * 
     * @param folderUri Folder URI for import
     * @return Number of successfully imported notes
     */
    suspend fun importFromFolder(folderUri: Uri): Int = withContext(Dispatchers.IO) {
        try {
            val folder = DocumentFile.fromTreeUri(context, folderUri) ?: return@withContext 0
            
            if (!folder.exists() || !folder.isDirectory) {
                return@withContext 0
            }
            
            var importedCount = 0
            val currentTime = System.currentTimeMillis()
            
            // Recursively import all .md and .txt files with instant display
            importedCount += importFromDocumentFolder(folder, "", currentTime, importedCount)
            
            importedCount
            
        } catch (e: Exception) {
            e.printStackTrace()
            0
        }
    }

    /**
     * Recursively imports .md and .txt files from DocumentFile folder with instant display.
     * 
     * @param folder Folder for scanning
     * @param pathPrefix Path prefix for nested folders
     * @param baseTime Base time for timestamps
     * @param currentCount Current count of imported notes
     * @return Number of imported files from this folder
     */
    private suspend fun importFromDocumentFolder(
        folder: DocumentFile,
        pathPrefix: String,
        baseTime: Long,
        currentCount: Int
    ): Int {
        var importedCount = 0
        
        try {
            // Scan all files and folders
            folder.listFiles().forEach { file ->
                when {
                    // Recursively process subfolders
                    file.isDirectory -> {
                        val newPrefix = if (pathPrefix.isEmpty()) {
                            file.name ?: ""
                        } else {
                            "$pathPrefix/${file.name ?: ""}"
                        }
                        importedCount += importFromDocumentFolder(
                            file, 
                            newPrefix, 
                            baseTime, 
                            currentCount + importedCount
                        )
                    }
                    
                    // Import .md and .txt files with instant UI appearance
                    file.isFile && (file.name?.endsWith(".md", ignoreCase = true) == true || file.name?.endsWith(".txt", ignoreCase = true) == true) -> {
                        try {
                            // Read file content
                            val content = context.contentResolver.openInputStream(file.uri)?.use { stream ->
                                stream.readBytes().toString(Charsets.UTF_8)
                            } ?: ""
                            
                            // Skip empty files
                            if (content.isNotBlank()) {
                                // Extract title considering path
                                val rawFileName = file.name ?: context.resources.getString(R.string.note_fallback)
                                val fileName = when {
                                    rawFileName.endsWith(".md", ignoreCase = true) -> rawFileName.removeSuffix(".md")
                                    rawFileName.endsWith(".txt", ignoreCase = true) -> rawFileName.removeSuffix(".txt")
                                    else -> rawFileName
                                }
                                val fullTitle = if (pathPrefix.isNotEmpty()) {
                                    "$pathPrefix/$fileName"
                                } else {
                                    fileName
                                }
                                val title = sanitizeTitle(fullTitle)
                                
                                // Create and immediately save note (instant list appearance)
                                val note = Note(
                                    id = 0, // Auto-generate ID
                                    content = content,
                                    createdAt = baseTime + currentCount + importedCount, // Unique timestamps
                                    updatedAt = baseTime + currentCount + importedCount,
                                    cachedTitle = if (title.isNotBlank()) title else null
                                )
                                
                                repository.insertNote(note)
                                importedCount++
                            }
                            
                                    } catch (e: Exception) {
                // Skip corrupted files
                            e.printStackTrace()
                        }
                    }
                }
            }
            
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        return importedCount
    }

    /**
     * Cleans path and filename from service symbols and prefixes
     */
    private fun sanitizeTitle(fullPath: String): String {
        return fullPath
            .replace(Regex(context.getString(R.string.note_prefix_pattern), RegexOption.IGNORE_CASE), "") // Remove "note_N_" prefix
            .replace(Regex("note_\\d+_", RegexOption.IGNORE_CASE), "") // Remove "note_N_" prefix
            .replace(Regex("[_-]+"), " ") // Replace underscores and hyphens with spaces
            .replace("/", " → ") // Replace slashes with arrows to show folder structure
            .replace(Regex("\\s+"), " ") // Remove multiple spaces
            .trim()
            .take(100) // Increased length for path display
    }

    /**
     * Returns information about import capabilities from folder.
     */
    fun getImportInfo(): String {
        return context.getString(R.string.folder_import_description)
    }
} 