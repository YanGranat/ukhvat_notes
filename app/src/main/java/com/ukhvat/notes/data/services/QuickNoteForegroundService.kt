package com.ukhvat.notes.data.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.ukhvat.notes.MainActivity

/**
 * ForegroundService that pins a persistent quick-note notification in the system tray.
 * This ensures the notification cannot be dismissed with a swipe.
 */
class QuickNoteForegroundService : Service() {

    private var shouldRestartOnDestroy = true

    override fun onCreate() {
        super.onCreate()
        shouldRestartOnDestroy = true
        // Ensure we are in foreground as soon as service is created
        ensureChannel()
        startForeground(QUICK_NOTE_NOTIFICATION_ID, buildNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                shouldRestartOnDestroy = false
                stopForeground(true)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_UPDATE -> {
                ensureChannel()
                // Call startForeground to satisfy Android 12+ foreground constraints
                startForeground(QUICK_NOTE_NOTIFICATION_ID, buildNotification())
                return START_STICKY
            }
            else -> {
                ensureChannel()
                startForeground(QUICK_NOTE_NOTIFICATION_ID, buildNotification())
                return START_STICKY
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (shouldRestartOnDestroy && isQuickNoteEnabled()) {
            // Schedule restart to ensure notification stays visible even if dismissed manually
            val restartContext = applicationContext
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(
                { start(restartContext) },
                250L
            )
        }
    }

    override fun onBind(intent: Intent?) = null

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                QUICK_NOTE_CHANNEL_ID,
                "Быстрое создание заметок",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Уведомление для быстрого создания заметок из шторки"
                setShowBadge(false)
                enableVibration(true)
                enableLights(true)
            }
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val createNewNoteIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            action = ACTION_OPEN_APP
            putExtra("create_new_note", true)
        }
        val createNewNotePendingIntent = android.app.PendingIntent.getActivity(
            this,
            0,
            createNewNoteIntent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )

        val createWithClipboardPendingIntent = android.app.PendingIntent.getActivity(
            this,
            2,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                action = ACTION_CREATE_WITH_CLIPBOARD
                putExtra("create_new_note_with_text", true)
            },
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )

        val deleteIntent = Intent(this, QuickNoteSwipeBlockReceiver::class.java).apply { action = ACTION_BLOCK_DELETE }
        val deletePendingIntent = android.app.PendingIntent.getBroadcast(
            this,
            3,
            deleteIntent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, QUICK_NOTE_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_add)
            .setContentTitle("Быстрое создание заметки")
            .setContentText("Нажмите для создания новой заметки")
            .setContentIntent(createNewNotePendingIntent)
            .setOngoing(true)
            .setAutoCancel(false)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setDeleteIntent(deletePendingIntent)
            .setShowWhen(false)
            .setWhen(0L)
            .addAction(
                android.R.drawable.ic_menu_edit,
                "Создать с текстом",
                createWithClipboardPendingIntent
            )

        // Ensure immediate foreground service behavior on supported platforms
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
        }

        builder.setSortKey("quick-note-persistent")

        val notification = builder.build()
        notification.flags = notification.flags or Notification.FLAG_NO_CLEAR or Notification.FLAG_ONGOING_EVENT or Notification.FLAG_FOREGROUND_SERVICE
        notification.`when` = 0L

        return notification
    }

    private fun isQuickNoteEnabled(): Boolean {
        val prefs = applicationContext.getSharedPreferences("ukhvat_prefs", Context.MODE_PRIVATE)
        return prefs.getBoolean(PREF_QUICK_NOTE_ENABLED, false)
    }

    companion object {
        private const val PREF_QUICK_NOTE_ENABLED = "quick_note_enabled"
        const val QUICK_NOTE_CHANNEL_ID = "quick_note_channel"
        const val QUICK_NOTE_NOTIFICATION_ID = 1001

        const val ACTION_START = "com.ukhvat.notes.quicknote.START"
        const val ACTION_STOP = "com.ukhvat.notes.quicknote.STOP"
        const val ACTION_UPDATE = "com.ukhvat.notes.quicknote.UPDATE"
        const val ACTION_BLOCK_DELETE = "com.ukhvat.notes.quicknote.BLOCK_DELETE"

        const val ACTION_OPEN_APP = "open_app"
        const val ACTION_CREATE_WITH_CLIPBOARD = "com.ukhvat.notes.ACTION_CREATE_WITH_CLIPBOARD"

        fun start(context: Context) {
            val intent = Intent(context, QuickNoteForegroundService::class.java).apply { action = ACTION_START }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ContextCompat.startForegroundService(context, intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, QuickNoteForegroundService::class.java).apply { action = ACTION_STOP }
            context.startService(intent)
        }

        fun update(context: Context) {
            val intent = Intent(context, QuickNoteForegroundService::class.java).apply { action = ACTION_UPDATE }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ContextCompat.startForegroundService(context, intent)
            } else {
                context.startService(intent)
            }
        }
    }
}


