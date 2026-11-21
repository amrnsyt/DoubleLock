package com.amrnsyt.doublelock

import android.app.Application
import androidx.compose.runtime.Immutable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

sealed interface UiState {
    data object Loading : UiState

    @Immutable
    data class Success(
        val enabled: Boolean,
        val speed: TapSpeed,
        val animation: LockAnimation,
        val durationMs: Int,
        val serviceConnected: Boolean,
        val adminActive: Boolean
    ) : UiState
}

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val store = SettingsStore.get(application)

    private val adminActive = MutableStateFlow(
        DeviceAdminManager.isActive(application)
    )

    private val baseState = combine(
        store.enabled,
        store.speed,
        store.animation,
        store.durationMs,
        LockAccessibilityService.connected
    ) { enabled, speed, animation, durationMs, connected ->
        UiState.Success(
            enabled,
            speed,
            animation,
            durationMs,
            connected,
            adminActive.value
        )
    }

    val uiState: StateFlow<UiState> = combine(
        baseState,
        adminActive
    ) { state, active ->
        if (state is UiState.Success) {
            state.copy(adminActive = active)
        } else {
            state
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        UiState.Loading
    )

    fun refreshAdminStatus() {
        adminActive.value = DeviceAdminManager.isActive(getApplication())
    }

    fun setEnabled(value: Boolean) = store.setEnabled(value)

    fun setSpeed(value: TapSpeed) = store.setSpeed(value)

    fun setAnimation(value: LockAnimation) = store.setAnimation(value)

    fun setDurationMs(value: Int) = store.setDurationMs(value)
}
