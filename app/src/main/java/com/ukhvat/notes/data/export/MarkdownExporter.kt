package com.ukhvat.notes.data.export

import com.ukhvat.notes.domain.model.Note
// Removed Hilt imports for Koin migration

/**
 * Exporter for notes to single Markdown file.
 * 
 * Combines all notes into one file with separators.
 * Used to create single MD file with all notes.
 * 
 * @author AI Assistant  
 * @since Project start
 */
/**
 * MIGRATED FROM HILT TO KOIN: No dependencies, simple constructor
 */
class MarkdownExporter {

    /**
     * Exports note list to single Markdown string.
     * 
     * @param notes Note list for export
     * @return Markdown string with separators between notes
     */
    fun exportNotes(notes: List<Note>): String {
        return buildString {
            notes.forEachIndexed { index, note ->
                append(note.content)
                if (index < notes.size - 1) {
                    appendLine()
                    appendLine()
                    appendLine("------") // Separator between notes
                    appendLine()
                }
            }
        }
    }
} 