package com.ukhvat.notes.ui.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.widget.RemoteViews
import com.ukhvat.notes.MainActivity
import com.ukhvat.notes.R
import com.ukhvat.notes.domain.repository.NotesRepository
import com.ukhvat.notes.ui.theme.ThemePreference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.context.GlobalContext
import androidx.core.content.ContextCompat

/**
 * Home screen widget that displays a specific note's title and content.
 * Note selection is stored in SharedPreferences per widgetId.
 */
class NoteWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        appWidgetIds.forEach { NoteWidgetPrefs.deleteNoteId(context, it) }
        super.onDeleted(context, appWidgetIds)
    }

    companion object {
        const val ACTION_REFRESH_NOTE_WIDGET = "com.ukhvat.notes.widget.ACTION_REFRESH_NOTE_WIDGET"

        fun requestUpdate(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, NoteWidgetProvider::class.java)
            val ids = manager.getAppWidgetIds(component)
            if (ids.isNotEmpty()) {
                ids.forEach { updateAppWidget(context, manager, it) }
            }
        }

        private fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_note)

            val noteId = NoteWidgetPrefs.getNoteId(context, appWidgetId)
            if (noteId == null || noteId <= 0L) {
                views.setTextViewText(R.id.widget_content, context.getString(R.string.note_is_empty))
                appWidgetManager.updateAppWidget(appWidgetId, views)
                return
            }

            // Click opens the note in MainActivity
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("open_note_id", noteId)
            }
            val pending = PendingIntent.getActivity(
                context,
                appWidgetId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, pending)

            // Load note content asynchronously, then update
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val repo: NotesRepository = GlobalContext.get().get()

                    // Determine theme (respect user's preference; fallback to system)
                    val themePref = try { repo.getThemePreference() } catch (_: Exception) { ThemePreference.SYSTEM }
                    val isSystemDark = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
                    val isDark = when (themePref) {
                        ThemePreference.DARK -> true
                        ThemePreference.LIGHT -> false
                        else -> isSystemDark
                    }

                    // Apply background and text color according to theme
                    val bgRes = if (isDark) R.drawable.widget_bg_dark else R.drawable.widget_bg_light
                    val textColor = if (isDark) ContextCompat.getColor(context, R.color.widget_text_dark) else ContextCompat.getColor(context, R.color.widget_text_light)
                    views.setInt(R.id.widget_root, "setBackgroundResource", bgRes)
                    views.setTextColor(R.id.widget_content, textColor)

                    val note = repo.getNoteById(noteId)
                    val content = (note?.content?.takeIf { it.isNotBlank() }
                        ?: context.getString(R.string.note_is_empty))

                    views.setTextViewText(R.id.widget_content, content)
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                } catch (_: Exception) {
                    // Silent failure
                }
            }
        }
    }
}


