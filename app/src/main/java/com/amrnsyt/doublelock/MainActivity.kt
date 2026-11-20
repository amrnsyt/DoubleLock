package com.amrnsyt.doublelock

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle

class MainActivity : ComponentActivity() {

    private val vm: HomeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DoubleLockTheme {
                val ui by vm.uiState.collectAsStateWithLifecycle()
                when (val state = ui) {
                    UiState.Loading -> Unit
                    is UiState.Success -> SettingsScreen(
                        state = state,
                        onToggle = vm::setEnabled,
                        onSpeed = vm::setSpeed,
                        onOpenAccessibility = ::openAccessibilitySettings,
                        onAddWidget = ::requestPinWidget
                    )
                }
            }
        }
    }

    private fun openAccessibilitySettings() {
        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }

    private fun requestPinWidget() {
        val manager = AppWidgetManager.getInstance(this)
        if (manager.isRequestPinAppWidgetSupported) {
            manager.requestPinAppWidget(
                ComponentName(this, LockWidgetProvider::class.java),
                null,
                null
            )
        } else {
            Toast.makeText(
                this,
                "Long-press the home screen, Widgets, DoubleLock",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}
