package com.ukhvat.notes.data.export

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.ukhvat.notes.R
import com.ukhvat.notes.domain.model.Note
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
// Removed Hilt imports for Koin migration

/**
  * Safe export of notes to individual MD files.
 * 
 * Security measures:
 * - Cleaning filenames from special characters
 * - Checking Windows reserved names (CON, PRN, etc.)
 * - Protection from Unicode/Emoji characters
 * - Handling control characters
 * - Backup filenames for creation errors
 * - Protection from duplicate names
 */
/**
 * MIGRATED FROM HILT TO KOIN: No dependencies, simple constructor
 */
class IndividualMarkdownExporter(private val context: Context) {
    
    suspend fun exportNotesToFolder(
        context: Context,
        folderUri: Uri,
        notes: List<Note>
    ): Int = withContext(Dispatchers.IO) {
        
        val filteredNotes = notes.filter { it.content.isNotBlank() }
        
        if (filteredNotes.isEmpty()) {
            throw IllegalArgumentException(context.getString(R.string.no_notes_to_export))
        }
        
        val folder = DocumentFile.fromTreeUri(context, folderUri)
            ?: throw IllegalArgumentException(context.getString(R.string.folder_access_failed))
        
        val usedFileNames = mutableSetOf<String>()
        var successCount = 0
        
        filteredNotes.forEachIndexed { index, note ->
            try {
                // Generate safe filename
                var fileName = generateSafeFileName(note, index + 1, usedFileNames)
                usedFileNames.add(fileName)
                
                // Try to create MD file in folder
                var mdFile = folder.createFile("text/markdown", "$fileName.md")
                
                // If file creation failed, try backup name ONCE
                if (mdFile == null) {
                    fileName = "note_${System.currentTimeMillis()}_${index + 1}"
                    usedFileNames.add(fileName)
                    mdFile = folder.createFile("text/markdown", "$fileName.md")
                    
                    // If second attempt failed - skip this note
                    if (mdFile == null) {
                        return@forEachIndexed
                    }
                }
                
                // Write content to file
                context.contentResolver.openOutputStream(mdFile.uri)?.use { outputStream ->
                    val markdownContent = formatNoteAsMarkdown(note)
                    outputStream.write(markdownContent.toByteArray(Charsets.UTF_8))
                    outputStream.flush() // Force save data
                    successCount++
                        } ?: run {
            // If failed to open write stream - skip note
                    return@forEachIndexed
                }
                
                // Smart pause every 10 notes for UI protection
                if ((index + 1) % 10 == 0) {
                    yield()
                }
                
                    } catch (e: Exception) {
            // Skip note with error, continue with others
                return@forEachIndexed
            }
        }
        
        successCount
    }
    
    private fun generateSafeFileName(note: Note, index: Int, usedNames: Set<String>): String {
        // Try to use note title as filename
        val title = extractTitle(note.content)
        var baseName = if (title.isNotBlank()) {
            sanitizeFileName(title)
        } else {
            "note_$index"
        }
        
        // Additional check: if after cleaning name became too generic
        if (baseName.matches(Regex("^(note|file|untitled|_+)$", RegexOption.IGNORE_CASE))) {
            baseName = "note_$index"
        }
        
        // Ensure name is unique
        var fileName = baseName
        var counter = 1
        while (usedNames.contains(fileName)) {
            fileName = "${baseName}_$counter"
            counter++
            
            // Protection from infinite loop (just in case)
            if (counter > 9999) {
                fileName = "note_${System.currentTimeMillis()}_$index"
                break
            }
        }
        
        return fileName
    }
    
    private fun extractTitle(content: String): String {
        // Find first meaningful line for title
        val lines = content.lines()
        
        for (line in lines.take(5)) { // Check first 5 lines
            val cleanLine = line.trim()
            
            // Skip empty lines and lines with only special characters
            if (cleanLine.isBlank()) continue
            if (cleanLine.matches(Regex("^[^\\w\\u0400-\\u04FF]+$"))) continue // Only non-letter characters
            
            // Remove markdown formatting at line beginning
            val title = cleanLine
                .removePrefix("#").trim()  // Remove markdown headers
                .removePrefix("*").trim()  // Remove asterisks
                .removePrefix("-").trim()  // Remove hyphens
                .removePrefix("•").trim()  // Remove bullets
            
            if (title.isNotBlank() && title.length >= 2) {
                return title.take(50)  // Limit title length
            }
        }
        
        // If no suitable line found, take content beginning without line breaks
        val contentStart = content.replace(Regex("\\s+"), " ").trim().take(30)
        return if (contentStart.isNotBlank()) contentStart else ""
    }
    
    private fun sanitizeFileName(fileName: String): String {
        // Maximum safe filename cleaning for all OS
        var safeName = fileName
            // Remove control characters (ASCII 0-31)
            .replace(Regex("[\u0000-\u001F]"), "_")
            // Remove basic forbidden characters for Windows/Unix
            .replace(Regex("[<>:\"/\\\\|?*]"), "_")
            // Remove additional problematic characters
            .replace(Regex("[\\[\\]{}()=+&%$#@!;,`~]"), "_")
            // Replace various types of spaces and line breaks with regular underscores
            .replace(Regex("\\s+"), "_")
            // Remove emoji and extended Unicode symbols (keep only basic Latin, digits and Cyrillic)
            .replace(Regex("[^\u0020-\u007E\u0400-\u04FF_-]"), "_")
            // Remove repeated underscores
            .replace(Regex("_+"), "_")
            // Remove dots at beginning and end (hidden files/extensions)
            .trim('.', '_')
            // Limit length (leave space for .md extension)
            .take(150)
            .trim('_')
        
        // Check Windows reserved names
        val windowsReservedNames = setOf(
            "CON", "PRN", "AUX", "NUL",
            "COM1", "COM2", "COM3", "COM4", "COM5", "COM6", "COM7", "COM8", "COM9",
            "LPT1", "LPT2", "LPT3", "LPT4", "LPT5", "LPT6", "LPT7", "LPT8", "LPT9"
        )
        
        if (windowsReservedNames.contains(safeName.uppercase())) {
            safeName = "file_$safeName"
        }
        
        // If after all transformations name became too short or empty
        if (safeName.length < 3 || safeName.isBlank()) {
            safeName = "note"
        }
        
        // Ensure file doesn't start with dot (hidden files in Unix)
        if (safeName.startsWith(".")) {
            safeName = "file$safeName"
        }
        
        return safeName
    }
    
    private fun formatNoteAsMarkdown(note: Note): String {
        // Simply return note content without headers and metadata
        return note.content
    }
    
    suspend fun getExportInfo(notes: List<Note>): String = withContext(Dispatchers.IO) {
        val filteredNotes = notes.filter { it.content.isNotBlank() }
        val totalSize = filteredNotes.sumOf { it.content.length }
        val avgSize = if (filteredNotes.isNotEmpty()) totalSize / filteredNotes.size else 0
        
        """
            Individual MD files:
• Notes: ${filteredNotes.size}
• Each note = separate .md file in folder
• Total size: ${totalSize / 1024}KB
• Average note size: ${avgSize} characters
• Content: note text only without headers
• Safe filenames for all OS
• Choose folder to save files
        """.trimIndent()
    }
    
    // Function for testing filename safety (for debugging)
    fun testFileNameSafety(testCases: List<String>): List<Pair<String, String>> {
        return testCases.map { original ->
            val safe = sanitizeFileName(original)
            original to safe
        }
    }
} 