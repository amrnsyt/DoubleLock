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
            ?.vibrate(
                VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
            )

        val bounds = intent.sourceBounds
        val animation = store.animation.value

        val locked = if (animation == LockAnimation.NONE) {
            DeviceAdminManager.lockNow(context) ||
                LockAccessibilityService.lockWithAnimation(
                    LockAnimation.NONE,
                    store.durationMs.value.toLong(),
                    bounds?.exactCenterX(),
                    bounds?.exactCenterY()
                )
        } else {
            LockAccessibilityService.lockWithAnimation(
                animation,
                store.durationMs.value.toLong(),
                bounds?.exactCenterX(),
                bounds?.exactCenterY()
            ) || DeviceAdminManager.lockNow(context)
        }

        if (!locked) {
            Toast.makeText(
                context,
                "Activate Device Administrator or Accessibility in DoubleLock settings",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    companion object {
        const val ACTION_TAP = "com.amrnsyt.doublelock.ACTION_TAP"
    }
}
