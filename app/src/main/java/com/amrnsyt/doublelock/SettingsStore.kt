package com.amrnsyt.doublelock

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class TapSpeed(val label: String, val windowMs: Long) {
    FAST("Fast", 250L),
    NORMAL("Normal", 350L),
    SLOW("Slow", 500L)
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

    fun setEnabled(value: Boolean) {
        prefs.edit().putBoolean(KEY_ENABLED, value).apply()
        _enabled.value = value
    }

    fun setSpeed(value: TapSpeed) {
        prefs.edit().putString(KEY_SPEED, value.name).apply()
        _speed.value = value
    }

    companion object {
        private const val KEY_ENABLED = "enabled"
        private const val KEY_SPEED = "speed"

        @Volatile
        private var instance: SettingsStore? = null

        fun get(context: Context): SettingsStore =
            instance ?: synchronized(this) {
                instance ?: SettingsStore(context).also { instance = it }
            }
    }
}
