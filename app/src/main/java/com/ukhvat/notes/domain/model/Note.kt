package com.ukhvat.notes.domain.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class Note(
    val id: Long = 0,
    val content: String,
    val cachedTitle: String? = null,  // Cached title
    val createdAt: Long,
    val updatedAt: Long,
    val isDeleted: Boolean = false,   // Trash flag (soft delete)
    val deletedAt: Long? = null       // Deletion time for auto-cleanup
) {
    val title: String
        get() = cachedTitle ?: NoteUtils.generateTitle(content, createdAt)
    
    val formattedDate: String
        get() = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date(updatedAt))
    
    val formattedDeletedDate: String?
        get() = deletedAt?.let { SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date(it)) }
}

/**
 * Note utilities
 */
object NoteUtils {
    /**
     * Generates note title:
     * - Takes text up to first line break
     * - UI will trim if needed
     */
    fun generateTitle(content: String, createdAt: Long): String {
        return content.substringBefore("\n").trim()
    }
} 