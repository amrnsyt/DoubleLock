package com.amrnsyt.doublelock

import android.os.SystemClock
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

private const val DOUBLE_TAP_MAX_DISTANCE_PX = 200f
private const val SWIPE_UP_THRESHOLD_PX = 150f

@Composable
fun HomeScreen(
    state: UiState.Success,
    onLock: () -> Unit,
    onOpenDrawer: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAccessibility: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val current by rememberUpdatedState(state)
    val lock by rememberUpdatedState(onLock)
    val drawer by rememberUpdatedState(onOpenDrawer)
    val settings by rememberUpdatedState(onOpenSettings)
    var lastTapTime by remember { mutableLongStateOf(0L) }
    var lastTapPos by remember { mutableStateOf(Offset.Zero) }

    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets.safeDrawing
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onLongPress = { settings() },
                        onTap = { pos ->
                            val now = SystemClock.uptimeMillis()
                            val isDouble =
                                now - lastTapTime <= current.speed.windowMs &&
                                    (pos - lastTapPos).getDistance() < DOUBLE_TAP_MAX_DISTANCE_PX
                            if (isDouble) {
                                lastTapTime = 0L
                                if (current.enabled) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    lock()
                                }
                            } else {
                                lastTapTime = now
                                lastTapPos = pos
                            }
                        }
                    )
                }
                .pointerInput(Unit) {
                    var total = 0f
                    detectVerticalDragGestures(
                        onDragStart = { total = 0f },
                        onDragEnd = { if (total < -SWIPE_UP_THRESHOLD_PX) drawer() },
                        onVerticalDrag = { _, dy -> total += dy }
                    )
                }
                .padding(padding)
        ) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.6f)
            ) {
                Text(
                    text = "Double-tap: lock  ·  Swipe up: apps  ·  Hold: settings",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (!state.serviceConnected) {
                Card(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Turn on the DoubleLock accessibility service to enable locking.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Button(onClick = onOpenAccessibility) {
                            Text("Open accessibility settings")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DrawerScreen(apps: List<AppEntry>, onLaunch: (String) -> Unit) {
    Scaffold { padding ->
        if (apps.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(84.dp),
                contentPadding = padding,
                modifier = Modifier.fillMaxSize()
            ) {
                items(apps, key = { it.packageName }) { app ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onLaunch(app.packageName) }
                            .padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Image(
                            bitmap = app.icon,
                            contentDescription = app.label,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = app.label,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: UiState.Success,
    onBack: () -> Unit,
    onToggle: (Boolean) -> Unit,
    onSpeed: (TapSpeed) -> Unit,
    onOpenAccessibility: () -> Unit,
    onOpenHomeSettings: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Double-tap lock") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Lock on double-tap", style = MaterialTheme.typography.titleMedium)
                Switch(checked = state.enabled, onCheckedChange = onToggle)
            }

            Text("Double-tap speed", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TapSpeed.entries.forEach { speed ->
                    FilterChip(
                        selected = state.speed == speed,
                        onClick = { onSpeed(speed) },
                        label = { Text(speed.label) }
                    )
                }
            }

            HorizontalDivider()

            Text(
                text = "Accessibility service: " + if (state.serviceConnected) "Active" else "Off",
                style = MaterialTheme.typography.bodyLarge
            )
            Button(onClick = onOpenAccessibility) {
                Text("Open accessibility settings")
            }
            OutlinedButton(onClick = onOpenHomeSettings) {
                Text("Set as default home app")
            }
            Text(
                text = "Quick Settings: edit the tiles panel and add \"Double-tap lock\" to toggle from anywhere.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    MaterialTheme {
        HomeScreen(
            state = UiState.Success(true, TapSpeed.NORMAL, false, Screen.HOME, emptyList()),
            onLock = {},
            onOpenDrawer = {},
            onOpenSettings = {},
            onOpenAccessibility = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsScreenPreview() {
    MaterialTheme {
        SettingsScreen(
            state = UiState.Success(true, TapSpeed.NORMAL, true, Screen.SETTINGS, emptyList()),
            onBack = {},
            onToggle = {},
            onSpeed = {},
            onOpenAccessibility = {},
            onOpenHomeSettings = {}
        )
    }
}
