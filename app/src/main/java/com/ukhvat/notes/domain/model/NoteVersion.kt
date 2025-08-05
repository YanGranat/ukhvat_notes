package com.ukhvat.notes.domain.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class NoteVersion(
    val id: Long = 0,
    val noteId: Long,
    val content: String,
    val timestamp: Long,
    val changeDescription: String? = null,
    val customName: String? = null,    // User-defined version name
    val isForcedSave: Boolean = false  // Whether version was created by forced save
) {
    val formattedDate: String
        get() = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date(timestamp))
    
    /**
     * Returns time difference in milliseconds between current moment and version timestamp.
     * Used for relative time formatting in UI layer.
     */
    fun getTimeDiffMillis(): Long {
        return System.currentTimeMillis() - timestamp
    }
    
    val shortContent: String
        get() = if (content.length > 100) {
            "${content.take(100)}..."
        } else content
} 