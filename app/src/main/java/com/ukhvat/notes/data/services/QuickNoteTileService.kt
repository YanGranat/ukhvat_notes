package com.ukhvat.notes.data.services

import android.content.Intent
import android.graphics.drawable.Icon
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.ukhvat.notes.MainActivity
import com.ukhvat.notes.R

/**
 * Quick Settings tile to create a new note instantly from the system shade.
 */
class QuickNoteTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        val tile = qsTile ?: return
        tile.label = getString(R.string.qs_quick_note)
        tile.contentDescription = getString(R.string.qs_quick_note)
        tile.state = Tile.STATE_ACTIVE
        tile.icon = Icon.createWithResource(this, android.R.drawable.ic_input_add)
        tile.updateTile()
    }

    override fun onClick() {
        super.onClick()
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("create_new_note", true)
        }
        startActivityAndCollapse(intent)
    }
}


