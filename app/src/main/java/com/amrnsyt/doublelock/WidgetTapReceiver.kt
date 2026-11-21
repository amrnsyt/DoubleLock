package com.amrnsyt.doublelock

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.VibrationEffect
import android.os.Vibrator
import android.widget.Toast

class WidgetTapReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_TAP) return
        val store = SettingsStore.get(context)
        if (!store.enabled.value) return
        if (!store.registerTap()) return

        context.getSystemService(Vibrator::class.java)
            ?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))

        val bounds = intent.sourceBounds
        val result = LockController.lock(store, bounds?.exactCenterX(), bounds?.exactCenterY())
        if (result == LockResult.NEEDS_ACCESSIBILITY) {
            Toast.makeText(
                context,
                "Enable the DoubleLock accessibility service first",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    companion object {
        const val ACTION_TAP = "com.amrnsyt.doublelock.ACTION_TAP"
    }
}
