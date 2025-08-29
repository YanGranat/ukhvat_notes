package com.ukhvat.notes.data.services

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.ukhvat.notes.MainActivity
import com.ukhvat.notes.R
import com.ukhvat.notes.domain.model.Note
import com.ukhvat.notes.domain.repository.NotesRepository
import com.ukhvat.notes.domain.util.NotificationService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Реализация сервиса для работы с уведомлениями и quick actions
 */
class NotificationServiceImpl(
    private val context: Context,
    private val notesRepository: NotesRepository
) : NotificationService {

    companion object {
        private const val QUICK_NOTE_CHANNEL_ID = "quick_note_channel"
        private const val QUICK_NOTE_NOTIFICATION_ID = 1001
        private const val PREF_QUICK_NOTE_ENABLED = "quick_note_enabled"

        // Action IDs для действий
        const val ACTION_OPEN_APP = "open_app"
        const val ACTION_CREATE_NOTE_WITH_TEXT = "create_note_with_clipboard"
    }

    private val notificationManager = NotificationManagerCompat.from(context)
    private val sharedPreferences = context.getSharedPreferences("ukhvat_prefs", Context.MODE_PRIVATE)

    init {
        createNotificationChannel()
    }

    /**
     * Создает канал уведомлений для Android 8.0+
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                QUICK_NOTE_CHANNEL_ID,
                "Быстрое создание заметок",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Уведомление для быстрого создания заметок из шторки"
                setShowBadge(false)
                enableVibration(true)
                enableLights(true)
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    override suspend fun showQuickNoteNotification() {
        val notification = buildQuickNoteNotification()
        withContext(Dispatchers.Main) {
            notificationManager.notify(QUICK_NOTE_NOTIFICATION_ID, notification)
        }
    }

    override suspend fun hideQuickNoteNotification() {
        withContext(Dispatchers.Main) {
            notificationManager.cancel(QUICK_NOTE_NOTIFICATION_ID)
        }
    }

    override suspend fun updateQuickNoteNotification() {
        if (!isQuickNoteEnabled()) {
            hideQuickNoteNotification()
            return
        }

        // При обновлении уведомления перечитываем clipboard для актуальности
        withContext(Dispatchers.Main) {
            showQuickNoteNotification()
        }
    }

    /**
     * Строит уведомление с quick actions
     */
    private fun buildQuickNoteNotification(): android.app.Notification {
        // Intent для создания новой заметки (при нажатии на уведомление)
        val createNewNoteIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            action = ACTION_OPEN_APP
            putExtra("create_new_note", true) // Флаг для создания новой заметки
        }
        val createNewNotePendingIntent = PendingIntent.getActivity(
            context, 0, createNewNoteIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )





        return NotificationCompat.Builder(context, QUICK_NOTE_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_add) // Используем системную иконку
            .setContentTitle("Быстрое создание заметки")
            .setContentText("Нажмите для создания новой заметки")
            .setContentIntent(createNewNotePendingIntent)
            .setOngoing(true) // Уведомление не исчезает при свайпе
            .setAutoCancel(false)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .addAction(
                android.R.drawable.ic_menu_edit, // Иконка для вставки текста
                "Создать с текстом",
                PendingIntent.getActivity(
                    context,
                    2,
                    Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        action = "com.ukhvat.notes.ACTION_CREATE_WITH_CLIPBOARD"
                        putExtra("create_new_note_with_text", true)
                    },
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
            .build()
    }

    override suspend fun handleQuickAction(actionId: String, inputText: String?): Note? {
        // Этот метод больше не используется, так как мы убрали кнопки quick actions
        // Оставляем для совместимости интерфейса
        return null
    }

    /**
     * Создает новую заметку
     */
    private suspend fun createNote(content: String): Note {
        val note = Note(
            content = content,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        // Сохраняем заметку через репозиторий
        val savedNoteId = notesRepository.insertNote(note)

        // Получаем сохраненную заметку по ID
        val savedNote = notesRepository.getNoteById(savedNoteId)

        // Обновляем уведомление с новым счетчиком
        updateQuickNoteNotification()

        return savedNote ?: note // Возвращаем сохраненную заметку или исходную если не найдена
    }

    override fun isQuickNoteEnabled(): Boolean {
        return sharedPreferences.getBoolean(PREF_QUICK_NOTE_ENABLED, false)
    }

    override suspend fun setQuickNoteEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean(PREF_QUICK_NOTE_ENABLED, enabled).apply()

        if (enabled) {
            showQuickNoteNotification()
        } else {
            hideQuickNoteNotification()
        }
    }

    /**
     * Проверяет разрешения на уведомления
     */
    fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Для старых версий Android разрешение не требуется
                   }
       }
   }
