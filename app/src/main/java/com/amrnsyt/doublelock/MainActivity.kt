package com.amrnsyt.doublelock

import android.app.admin.DevicePolicyManager
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
                        onAnimation = vm::setAnimation,
                        onDuration = vm::setDurationMs,
                        onOpenAccessibility = ::openAccessibilitySettings,
                        onActivateDeviceAdmin = ::activateDeviceAdmin,
                        onAddWidget = ::requestPinWidget
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        vm.refreshAdminStatus()
    }

    private fun openAccessibilitySettings() {
        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }

    private fun activateDeviceAdmin() {
        if (DeviceAdminManager.isActive(this)) {
            Toast.makeText(
                this,
                "Device Administrator is already active",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
            putExtra(
                DevicePolicyManager.EXTRA_DEVICE_ADMIN,
                ComponentName(this@MainActivity, DoubleLockDeviceAdminReceiver::class.java)
            )
            putExtra(
                DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                "DoubleLock uses this permission only to lock your phone screen when you double-tap its widget."
            )
        }

        startActivity(intent)
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
