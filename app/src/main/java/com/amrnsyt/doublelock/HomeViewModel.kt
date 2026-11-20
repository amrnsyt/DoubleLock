package com.amrnsyt.doublelock

import android.app.Application
import androidx.compose.runtime.Immutable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
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
        val serviceConnected: Boolean
    ) : UiState
}

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val store = SettingsStore.get(application)

    val uiState: StateFlow<UiState> = combine(
        store.enabled,
        store.speed,
        LockAccessibilityService.connected
    ) { enabled, speed, connected ->
        UiState.Success(enabled, speed, connected)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Loading)

    fun setEnabled(value: Boolean) = store.setEnabled(value)

    fun setSpeed(value: TapSpeed) = store.setSpeed(value)
}
