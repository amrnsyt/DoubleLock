package com.amrnsyt.doublelock

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
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
                    is UiState.Success -> {
                        BackHandler(enabled = true) { vm.show(Screen.HOME) }
                        when (state.screen) {
                            Screen.HOME -> HomeScreen(
                                state = state,
                                onLock = {
                                    if (!vm.lock()) {
                                        Toast.makeText(
                                            this@MainActivity,
                                            "Enable the accessibility service first",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                },
                                onOpenDrawer = { vm.show(Screen.DRAWER) },
                                onOpenSettings = { vm.show(Screen.SETTINGS) },
                                onOpenAccessibility = ::openAccessibilitySettings
                            )

                            Screen.DRAWER -> DrawerScreen(
                                apps = state.apps,
                                onLaunch = ::launchApp
                            )

                            Screen.SETTINGS -> SettingsScreen(
                                state = state,
                                onBack = { vm.show(Screen.HOME) },
                                onToggle = vm::setEnabled,
                                onSpeed = vm::setSpeed,
                                onOpenAccessibility = ::openAccessibilitySettings,
                                onOpenHomeSettings = ::openHomeSettings
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.hasCategory(Intent.CATEGORY_HOME)) vm.show(Screen.HOME)
    }

    private fun launchApp(packageName: String) {
        packageManager.getLaunchIntentForPackage(packageName)?.let {
            startActivity(it)
            vm.show(Screen.HOME)
        }
    }

    private fun openAccessibilitySettings() {
        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }

    private fun openHomeSettings() {
        startActivity(Intent(Settings.ACTION_HOME_SETTINGS))
    }
}
