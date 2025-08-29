package com.ukhvat.notes.data.services

import android.content.BroadcastReceiver
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import android.net.Uri
import com.ukhvat.notes.UkhvatApplication
import com.ukhvat.notes.domain.model.Note
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * BroadcastReceiver для обработки действий из quick actions уведомлений
 */
class QuickNoteActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            "com.ukhvat.notes.CREATE_NOTE" -> {
                handleCreateNote(context, "")
            }
            "com.ukhvat.notes.CREATE_NOTE_WITH_CLIPBOARD" -> {
                // Передаем управление MainActivity для чтения clipboard
                val mainIntent = Intent(context, com.ukhvat.notes.MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra("create_new_note_with_text", true)
                    putExtra("read_clipboard", true) // Флаг для чтения clipboard в Activity
                }
                context.startActivity(mainIntent)
            }
        }
    }

    /**
     * Обрабатывает создание новой заметки
     */
    private fun handleCreateNote(context: Context, content: String) {
        // Получаем доступ к репозиторию через Application
        val application = context.applicationContext as? UkhvatApplication
        val notesRepository = application?.getNotesRepository()

        if (notesRepository == null) {
            showToast(context, "Ошибка: репозиторий недоступен")
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val note = Note(
                    content = content,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )

                notesRepository.insertNote(note)

                showToast(context, "Заметка создана!")

            } catch (e: Exception) {
                showToast(context, "Ошибка создания заметки: ${e.localizedMessage ?: e.message}")
            }
        }
    }

    private fun showToast(context: Context, message: String) {
        CoroutineScope(Dispatchers.Main).launch {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }




}
