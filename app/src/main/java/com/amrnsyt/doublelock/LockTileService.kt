package com.amrnsyt.doublelock

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

class LockTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        refresh()
    }

    override fun onClick() {
        super.onClick()
        val store = SettingsStore.get(this)
        store.setEnabled(!store.enabled.value)
        refresh()
    }

    private fun refresh() {
        val tile = qsTile ?: return
        val on = SettingsStore.get(this).enabled.value
        tile.state = if (on) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = getString(R.string.tile_label)
        tile.subtitle = if (on) "On" else "Off"
        tile.updateTile()
    }
}
