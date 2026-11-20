package com.amrnsyt.doublelock

import android.content.Context
import android.os.SystemClock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class TapSpeed(val label: String, val windowMs: Long) {
    FAST("Fast", 250L),
    NORMAL("Normal", 350L),
    SLOW("Slow", 500L)
}

enum class LockAnimation(val label: String) {
    NONE("None (instant lock)"),
    CRT("CRT TV off"),
    FADE("Fade to black"),
    CIRCLE("Circle collapse"),
    CURTAIN("Curtain slides down")
}

class SettingsStore private constructor(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("doublelock", Context.MODE_PRIVATE)

    private val _enabled = MutableStateFlow(prefs.getBoolean(KEY_ENABLED, true))
    val enabled: StateFlow<Boolean> = _enabled.asStateFlow()

    private val _speed = MutableStateFlow(
        TapSpeed.entries.firstOrNull { it.name == prefs.getString(KEY_SPEED, null) }
            ?: TapSpeed.NORMAL
    )
    val speed: StateFlow<TapSpeed> = _speed.asStateFlow()

    private val _animation = MutableStateFlow(
        LockAnimation.entries.firstOrNull { it.name == prefs.getString(KEY_ANIMATION, null) }
            ?: LockAnimation.CRT
    )
    val animation: StateFlow<LockAnimation> = _animation.asStateFlow()

    private val _durationMs = MutableStateFlow(
        prefs.getInt(KEY_DURATION, DEFAULT_DURATION_MS).coerceIn(MIN_DURATION_MS, MAX_DURATION_MS)
    )
    val durationMs: StateFlow<Int> = _durationMs.asStateFlow()

    fun setEnabled(value: Boolean) {
        prefs.edit().putBoolean(KEY_ENABLED, value).apply()
        _enabled.value = value
    }

    fun setSpeed(value: TapSpeed) {
        prefs.edit().putString(KEY_SPEED, value.name).apply()
        _speed.value = value
    }

    fun setAnimation(value: LockAnimation) {
        prefs.edit().putString(KEY_ANIMATION, value.name).apply()
        _animation.value = value
    }

    fun setDurationMs(value: Int) {
        val safe = value.coerceIn(MIN_DURATION_MS, MAX_DURATION_MS)
        prefs.edit().putInt(KEY_DURATION, safe).apply()
        _durationMs.value = safe
    }

    /** Records a tap. Returns true when it completes a double-tap. */
    @Synchronized
    fun registerTap(): Boolean {
        val now = SystemClock.elapsedRealtime()
        val last = prefs.getLong(KEY_LAST_TAP, 0L)
        val isDouble = last != 0L && now - last in 0.._speed.value.windowMs
        prefs.edit().putLong(KEY_LAST_TAP, if (isDouble) 0L else now).apply()
        return isDouble
    }

    companion object {
        const val MIN_DURATION_MS = 200
        const val MAX_DURATION_MS = 800
        const val DEFAULT_DURATION_MS = 400

        private const val KEY_ENABLED = "enabled"
        private const val KEY_SPEED = "speed"
        private const val KEY_ANIMATION = "animation"
        private const val KEY_DURATION = "duration_ms"
        private const val KEY_LAST_TAP = "last_tap"

        @Volatile
        private var instance: SettingsStore? = null

        fun get(context: Context): SettingsStore =
            instance ?: synchronized(this) {
                instance ?: SettingsStore(context).also { instance = it }
            }
    }
}
