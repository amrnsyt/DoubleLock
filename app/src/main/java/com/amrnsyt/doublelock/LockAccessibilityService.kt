package com.amrnsyt.doublelock

import android.accessibilityservice.AccessibilityService
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.animation.LinearInterpolator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class LockAccessibilityService : AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())
    private var overlay: View? = null
    private var animator: ValueAnimator? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        _connected.value = true
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit

    override fun onUnbind(intent: Intent?): Boolean {
        clearOverlay()
        instance = null
        _connected.value = false
        return super.onUnbind(intent)
    }

    private fun lockNow(): Boolean =
        performGlobalAction(AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN)

    private fun playAndLock(
        animation: LockAnimation,
        durationMs: Long,
        tapX: Float?,
        tapY: Float?
    ) {
        if (overlay != null) return

        val view = LockAnimView(this, animation, tapX ?: -1f, tapY ?: -1f)
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

        val windowManager = getSystemService(WindowManager::class.java)
        try {
            windowManager.addView(view, params)
        } catch (e: Exception) {
            lockNow()
            return
        }
        overlay = view

        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = durationMs
            interpolator = LinearInterpolator()
            addUpdateListener { view.progress = it.animatedValue as Float }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    lockNow()
                    handler.postDelayed({ clearOverlay() }, OVERLAY_LINGER_MS)
                }
            })
            start()
        }
    }

    private fun clearOverlay() {
        handler.removeCallbacksAndMessages(null)
        animator?.removeAllListeners()
        animator?.cancel()
        animator = null
        overlay?.let {
            try {
                getSystemService(WindowManager::class.java).removeViewImmediate(it)
            } catch (e: Exception) {
                // already removed
            }
        }
        overlay = null
    }

    companion object {
        private const val OVERLAY_LINGER_MS = 400L

        @Volatile
        private var instance: LockAccessibilityService? = null

        private val _connected = MutableStateFlow(false)
        val connected: StateFlow<Boolean> = _connected.asStateFlow()

        /** Plays the chosen animation, then locks. Returns false if the service is not running. */
        fun lockWithAnimation(
            animation: LockAnimation,
            durationMs: Long,
            tapX: Float?,
            tapY: Float?
        ): Boolean {
            val service = instance ?: return false
            if (animation == LockAnimation.NONE) return service.lockNow()
            service.playAndLock(animation, durationMs, tapX, tapY)
            return true
        }
    }
}
