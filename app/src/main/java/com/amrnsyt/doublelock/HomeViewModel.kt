package com.amrnsyt.doublelock

import android.app.Application
import android.content.Intent
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class Screen { HOME, DRAWER, SETTINGS }

@Immutable
data class AppEntry(
    val label: String,
    val packageName: String,
    val icon: ImageBitmap
)

sealed interface UiState {
    data object Loading : UiState

    @Immutable
    data class Success(
        val enabled: Boolean,
        val speed: TapSpeed,
        val serviceConnected: Boolean,
        val screen: Screen,
        val apps: List<AppEntry>
    ) : UiState
}

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val store = SettingsStore.get(application)
    private val screen = MutableStateFlow(Screen.HOME)
    private val apps = MutableStateFlow<List<AppEntry>?>(null)

    val uiState: StateFlow<UiState> = combine(
        store.enabled,
        store.speed,
        LockAccessibilityService.connected,
        screen,
        apps
    ) { enabled, speed, connected, currentScreen, appList ->
        UiState.Success(enabled, speed, connected, currentScreen, appList.orEmpty())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Loading)

    fun show(target: Screen) {
        screen.value = target
        if (target == Screen.DRAWER) loadApps()
    }

    fun setEnabled(value: Boolean) = store.setEnabled(value)

    fun setSpeed(value: TapSpeed) = store.setSpeed(value)

    fun lock(): Boolean = LockAccessibilityService.lockScreen()

    private fun loadApps() {
        viewModelScope.launch(Dispatchers.Default) {
            val app = getApplication<Application>()
            val pm = app.packageManager
            val query = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            apps.value = pm.queryIntentActivities(query, 0)
                .filter { it.activityInfo.packageName != app.packageName }
                .distinctBy { it.activityInfo.packageName }
                .map {
                    AppEntry(
                        label = it.loadLabel(pm).toString(),
                        packageName = it.activityInfo.packageName,
                        icon = it.loadIcon(pm).toBitmap(96, 96).asImageBitmap()
                    )
                }
                .sortedBy { it.label.lowercase() }
        }
    }
}
