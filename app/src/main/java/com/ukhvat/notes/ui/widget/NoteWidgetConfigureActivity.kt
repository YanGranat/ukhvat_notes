package com.ukhvat.notes.ui.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast
import com.ukhvat.notes.R
import com.ukhvat.notes.domain.repository.NotesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.firstOrNull
import org.koin.android.ext.android.get

/**
 * Simple configuration Activity: shows a list of notes (titles) to pick for the widget.
 * Minimal XML/UI to keep implementation simple and fast.
 */
class NoteWidgetConfigureActivity : Activity() {
    private var appWidgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.widget_config_list)

        // Set the result to CANCELED.  This will cause the widget host to cancel
        // out of the widget placement if they press the back button.
        setResult(RESULT_CANCELED)

        // Find the widget id from the intent.
        appWidgetId = intent?.extras?.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        val listView = findViewById<ListView>(R.id.widget_config_listview)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repo: NotesRepository = get()
                val notes = repo.getAllNotes().firstOrNull() ?: emptyList()
                val items = notes.map { it.id to (it.title.ifBlank { getString(R.string.new_note_placeholder) }) }
                val titles = items.map { it.second }

                withContext(Dispatchers.Main) {
                    val adapter = ArrayAdapter(this@NoteWidgetConfigureActivity, android.R.layout.simple_list_item_1, titles)
                    listView.adapter = adapter

                    listView.setOnItemClickListener { _, _, position, _ ->
                        val (noteId, _) = items[position]
                        NoteWidgetPrefs.saveNoteId(this@NoteWidgetConfigureActivity, appWidgetId, noteId)

                        // Update widget immediately
                        val appWidgetManager = AppWidgetManager.getInstance(this@NoteWidgetConfigureActivity)
                        NoteWidgetProvider.Companion.requestUpdate(this@NoteWidgetConfigureActivity)

                        val resultValue = Intent().apply {
                            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                        }
                        setResult(RESULT_OK, resultValue)
                        finish()
                    }
                }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@NoteWidgetConfigureActivity, R.string.error, Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }
    }
}


