package com.amrnsyt.doublelock

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: UiState.Success,
    onToggle: (Boolean) -> Unit,
    onSpeed: (TapSpeed) -> Unit,
    onAnimation: (LockAnimation) -> Unit,
    onDuration: (Int) -> Unit,
    onOpenAccessibility: () -> Unit,
    onAddWidget: () -> Unit
) {
    var sliderValue by remember(state.durationMs) {
        mutableFloatStateOf(state.durationMs.toFloat())
    }
    val animationOn = state.animation != LockAnimation.NONE

    Scaffold(
        topBar = { CenterAlignedTopAppBar(title = { Text("DoubleLock") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
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

            Text("Lock animation", style = MaterialTheme.typography.titleMedium)
            Column {
                LockAnimation.entries.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = state.animation == option,
                                onClick = { onAnimation(option) },
                                role = Role.RadioButton
                            )
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = state.animation == option, onClick = null)
                        Text(
                            text = option.label,
                            modifier = Modifier.padding(start = 12.dp),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }

            Text(
                text = "Animation length: ${sliderValue.roundToInt()} ms",
                style = MaterialTheme.typography.titleSmall
            )
            Slider(
                value = sliderValue,
                onValueChange = { sliderValue = it },
                onValueChangeFinished = { onDuration(sliderValue.roundToInt()) },
                valueRange = SettingsStore.MIN_DURATION_MS.toFloat()..
                    SettingsStore.MAX_DURATION_MS.toFloat(),
                steps = 11,
                enabled = animationOn
            )
            Text(
                text = "The circle collapses toward the center of your pad widget.",
                style = MaterialTheme.typography.bodySmall
            )

            HorizontalDivider()

            Text(
                text = "Step 1: accessibility service is " +
                    if (state.serviceConnected) "Active" else "Off",
                style = MaterialTheme.typography.titleMedium
            )
            OutlinedButton(onClick = onOpenAccessibility) {
                Text("Open accessibility settings")
            }

            Text(
                text = "Step 2: add the invisible pad",
                style = MaterialTheme.typography.titleMedium
            )
            Button(onClick = onAddWidget) {
                Text("Add pad widget to home screen")
            }
            Text(
                text = "Place it on an empty home page and stretch it to cover the blank area. " +
                    "Double-tap the empty space to lock. You can also add the \"Double-tap lock\" " +
                    "Quick Settings tile to switch this on or off.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsScreenPreview() {
    MaterialTheme {
        SettingsScreen(
            state = UiState.Success(true, TapSpeed.NORMAL, LockAnimation.CRT, 400, true),
            onToggle = {},
            onSpeed = {},
            onAnimation = {},
            onDuration = {},
            onOpenAccessibility = {},
            onAddWidget = {}
        )
    }
}
