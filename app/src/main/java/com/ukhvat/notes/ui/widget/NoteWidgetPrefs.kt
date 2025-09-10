package com.ukhvat.notes.ui.widget

import android.content.Context

/**
 * SharedPreferences helper for mapping widgetId -> noteId.
 */
object NoteWidgetPrefs {
    private const val PREFS_NAME = "com.ukhvat.notes.widget_prefs"
    private const val KEY_PREFIX = "note_id_"

    fun saveNoteId(context: Context, appWidgetId: Int, noteId: Long) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putLong(KEY_PREFIX + appWidgetId, noteId).apply()
    }

    fun getNoteId(context: Context, appWidgetId: Int): Long? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (!prefs.contains(KEY_PREFIX + appWidgetId)) return null
        return prefs.getLong(KEY_PREFIX + appWidgetId, 0L)
    }

    fun deleteNoteId(context: Context, appWidgetId: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_PREFIX + appWidgetId).apply()
    }
}


