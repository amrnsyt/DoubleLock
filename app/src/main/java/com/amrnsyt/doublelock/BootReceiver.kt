package com.amrnsyt.doublelock

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.service.quicksettings.TileService

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            SettingsStore.get(context)
            TileService.requestListeningState(
                context,
                ComponentName(context, LockTileService::class.java)
            )
        }
    }
}
