package com.ukhvat.notes.data.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Receiver to intercept deleteIntent from the ongoing notification and re-post it,
 * effectively preventing user from dismissing it via swipe.
 */
class QuickNoteSwipeBlockReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        // Re-post the notification immediately; service will re-enter foreground
        QuickNoteForegroundService.update(context)
    }
}
