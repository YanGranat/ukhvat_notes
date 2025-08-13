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
            if (!folder.exists() || !folder.isDirectory) return@withContext 0

            val notesToInsert = mutableListOf<Note>()
            val baseTime = System.currentTimeMillis()

            collectNotesFromDocumentFolder(folder, "", baseTime, notesToInsert)

            if (notesToInsert.isNotEmpty()) {
                repository.insertNotesInBatch(notesToInsert)
            }

            notesToInsert.size
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
    private suspend fun collectNotesFromDocumentFolder(
        folder: DocumentFile,
        pathPrefix: String,
        baseTime: Long,
        accumulator: MutableList<Note>
    ) {
        try {
            folder.listFiles().forEach { file ->
                when {
                    file.isDirectory -> {
                        val newPrefix = if (pathPrefix.isEmpty()) file.name ?: "" else "$pathPrefix/${file.name ?: ""}"
                        collectNotesFromDocumentFolder(file, newPrefix, baseTime, accumulator)
                    }
                    file.isFile && (file.name?.endsWith(".md", ignoreCase = true) == true || file.name?.endsWith(".txt", ignoreCase = true) == true) -> {
                        try {
                            val content = context.contentResolver.openInputStream(file.uri)?.use { stream ->
                                val buf = ByteArray(8192)
                                val out = java.io.ByteArrayOutputStream()
                                var r = stream.read(buf)
                                while (r != -1) {
                                    out.write(buf, 0, r)
                                    r = stream.read(buf)
                                }
                                out.toByteArray().toString(Charsets.UTF_8)
                            } ?: ""
                            if (content.isNotBlank()) {
                                val rawFileName = file.name ?: context.resources.getString(R.string.note_fallback)
                                val fileName = when {
                                    rawFileName.endsWith(".md", ignoreCase = true) -> rawFileName.removeSuffix(".md")
                                    rawFileName.endsWith(".txt", ignoreCase = true) -> rawFileName.removeSuffix(".txt")
                                    else -> rawFileName
                                }
                                val fullTitle = if (pathPrefix.isNotEmpty()) "$pathPrefix/$fileName" else fileName
                                val title = sanitizeTitle(fullTitle)
                                val ts = baseTime + accumulator.size
                                accumulator.add(
                                    Note(
                                        id = 0,
                                        content = content,
                                        createdAt = ts,
                                        updatedAt = ts,
                                        cachedTitle = if (title.isNotBlank()) title else null
                                    )
                                )
                            }
                        } catch (_: Exception) { }
                    }
                }
            }
        } catch (_: Exception) { }
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