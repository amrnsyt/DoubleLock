package com.amrnsyt.doublelock

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowManager
import android.view.animation.LinearInterpolator

enum class LockResult { OK, NEEDS_ACCESSIBILITY }

object LockController {

    private const val OVERLAY_LINGER_MS = 400L

    private val handler = Handler(Looper.getMainLooper())
    private var overlay: View? = null
    private var overlayWindowManager: WindowManager? = null
    private var animator: ValueAnimator? = null

    /** Runs the chosen animation (if any) and then locks the screen. */
    fun lock(store: SettingsStore, tapX: Float?, tapY: Float?): LockResult {
        val service = LockAccessibilityService.current() ?: return LockResult.NEEDS_ACCESSIBILITY
        val animation = store.animation.value
        if (animation == LockAnimation.NONE) {
            service.lockNow()
        } else {
            play(service, animation, store.durationMs.value.toLong(), tapX, tapY)
        }
        return LockResult.OK
    }

    private fun play(
        service: LockAccessibilityService,
        animation: LockAnimation,
        durationMs: Long,
        tapX: Float?,
        tapY: Float?
    ) {
        if (overlay != null) return

        val view = LockAnimView(service, animation, tapX ?: -1f, tapY ?: -1f)
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }

        val windowManager = service.getSystemService(WindowManager::class.java)
        try {
            windowManager.addView(view, params)
        } catch (e: Exception) {
            service.lockNow()
            return
        }
        overlay = view
        overlayWindowManager = windowManager

        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = durationMs
            interpolator = LinearInterpolator()
            addUpdateListener { view.progress = it.animatedValue as Float }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    service.lockNow()
                    handler.postDelayed({ clear() }, OVERLAY_LINGER_MS)
                }
            })
            start()
        }
    }

    fun clear() {
        handler.removeCallbacksAndMessages(null)
        animator?.removeAllListeners()
        animator?.cancel()
        animator = null
        overlay?.let {
            try {
                overlayWindowManager?.removeViewImmediate(it)
            } catch (e: Exception) {
                // already removed
            }
        }
        overlay = null
        overlayWindowManager = null
    }
}
