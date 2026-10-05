package com.example.spaceplan.ui.settings

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.spaceplan.data.AppSettings
import com.example.spaceplan.data.ThemeMode
import com.example.spaceplan.domain.InputValidator
import com.example.spaceplan.domain.MeasurementUnit
import com.example.spaceplan.domain.NumberParser
import com.example.spaceplan.domain.Orientation
import com.example.spaceplan.domain.SpacePlanMode
import com.example.spaceplan.ui.SpacePlanViewModel
import com.example.spaceplan.ui.components.NumberField
import com.example.spaceplan.ui.components.ScreenHeader
import com.example.spaceplan.ui.components.SectionLabel
import com.example.spaceplan.ui.components.SegmentedSelector
import com.example.spaceplan.ui.components.SwitchRow

private enum class PendingConfirm { RESET_SETTINGS, CLEAR_HISTORY }

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    viewModel: SpacePlanViewModel,
    showMessage: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    var pending by rememberSaveable { mutableStateOf<PendingConfirm?>(null) }
    val focusManager = LocalFocusManager.current
    val update: ((AppSettings) -> AppSettings) -> Unit = { viewModel.updateSettings(it) }

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(Modifier.widthIn(max = 720.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ScreenHeader("Settings", "Preferences are stored on this device.")

            SectionHeader("Appearance")
            SectionLabel("Theme")
            SegmentedSelector(ThemeMode.entries, settings.themeMode, { mode -> update { it.copy(themeMode = mode) } }, { it.label })
            val dynamicSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
            SwitchRow(
                title = "Dynamic color",
                subtitle = if (dynamicSupported) "Use colors from your wallpaper" else "Requires Android 12 or later",
                checked = settings.dynamicColor && dynamicSupported,
                enabled = dynamicSupported,
                onCheckedChange = { on -> update { it.copy(dynamicColor = on) } },
            )

            SectionHeader("Calculator defaults")
            SectionLabel("Default mode")
            SegmentedSelector(SpacePlanMode.entries, settings.defaultMode, { mode -> update { it.copy(defaultMode = mode) } }, { it.label })
            SectionLabel("Default unit")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MeasurementUnit.entries.forEach { unit ->
                    FilterChip(
                        selected = settings.defaultUnit == unit,
                        onClick = { update { it.copy(defaultUnit = unit) } },
                        label = { Text(unit.symbol) },
                    )
                }
            }
            val unitSymbol = settings.defaultUnit.symbol
            DefaultDecimalField(
                label = "Default available space",
                stored = settings.defaultAvailable,
                suffix = unitSymbol,
                validate = { InputValidator.validateAvailable(it) },
                onValid = { value -> update { it.copy(defaultAvailable = value) } },
            )
            DefaultDecimalField(
                label = "Default object size",
                stored = settings.defaultObjectSize,
                suffix = unitSymbol,
                validate = { InputValidator.validateObjectSize(it) },
                onValid = { value -> update { it.copy(defaultObjectSize = value) } },
            )
            DefaultDecimalField(
                label = "Default gap",
                stored = settings.defaultGap,
                suffix = unitSymbol,
                validate = { InputValidator.validateGap(it) },
                onValid = { value -> update { it.copy(defaultGap = value) } },
            )
            DefaultCountField(
                stored = settings.defaultObjectCount,
                onValid = { value -> update { it.copy(defaultObjectCount = value) } },
            )
            SectionLabel("Orientation")
            SegmentedSelector(Orientation.entries, settings.orientation, { o -> update { it.copy(orientation = o) } }, { it.label })
            SectionLabel("Decimal places")
            SegmentedSelector(listOf(1, 2, 3, 4), settings.decimalPlaces, { d -> update { it.copy(decimalPlaces = d) } }, { it.toString() })
            Text(
                "Defaults apply on app start and when you reset the calculator.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            SectionHeader("Display")
            SwitchRow("Show formula", settings.showFormula, { on -> update { it.copy(showFormula = on) } })
            SwitchRow("Show breakdown", settings.showBreakdown, { on -> update { it.copy(showBreakdown = on) } })
            SwitchRow("Show visualization", settings.showVisualization, { on -> update { it.copy(showVisualization = on) } })
            SwitchRow(
                "Haptic feedback",
                settings.hapticFeedback,
                { on -> update { it.copy(hapticFeedback = on) } },
                subtitle = "Light feedback on mode change, calculate, save and reset",
            )

            SectionHeader("Data")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = {
                    focusManager.clearFocus()
                    pending = PendingConfirm.RESET_SETTINGS
                }) { Text("Reset settings") }
                OutlinedButton(onClick = { pending = PendingConfirm.CLEAR_HISTORY }) { Text("Clear history") }
            }
            Text(
                "Resetting settings does not clear history.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp),
            )
        }
    }

    pending?.let { confirm ->
        val (title, body, action) = when (confirm) {
            PendingConfirm.RESET_SETTINGS -> Triple("Reset settings?", "All settings return to their defaults.", "Reset")
            PendingConfirm.CLEAR_HISTORY -> Triple("Clear history?", "All saved calculations will be removed.", "Clear")
        }
        AlertDialog(
            onDismissRequest = { pending = null },
            title = { Text(title) },
            text = { Text(body) },
            confirmButton = {
                TextButton(onClick = {
                    pending = null
                    when (confirm) {
                        PendingConfirm.RESET_SETTINGS -> {
                            viewModel.resetSettings()
                            showMessage("Settings reset")
                        }
                        PendingConfirm.CLEAR_HISTORY -> {
                            viewModel.clearHistory()
                            showMessage("History cleared")
                        }
                    }
                }) { Text(action) }
            },
            dismissButton = { TextButton(onClick = { pending = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
    }
}

/** Edits a stored default; only valid values are persisted. Re-syncs when the stored value changes (e.g. reset). */
@Composable
private fun DefaultDecimalField(
    label: String,
    stored: String,
    suffix: String,
    validate: (java.math.BigDecimal?) -> String?,
    onValid: (String) -> Unit,
) {
    var text by rememberSaveable { mutableStateOf(stored) }
    var focused by remember { mutableStateOf(false) }
    // Only adopt external changes (such as Reset settings) while the user is not editing.
    LaunchedEffect(stored) { if (!focused) text = stored }
    val error = validate(NumberParser.parseDecimal(text))
    NumberField(
        modifier = Modifier.onFocusChanged { focused = it.isFocused },
        label = label,
        value = text,
        onValueChange = {
            text = it
            if (validate(NumberParser.parseDecimal(it)) == null) onValid(it.trim())
        },
        suffix = suffix,
        error = error,
    )
}

@Composable
private fun DefaultCountField(stored: Int, onValid: (Int) -> Unit) {
    var text by rememberSaveable { mutableStateOf(stored.toString()) }
    var focused by remember { mutableStateOf(false) }
    LaunchedEffect(stored) { if (!focused) text = stored.toString() }
    NumberField(
        modifier = Modifier.onFocusChanged { focused = it.isFocused },
        label = "Default object count",
        value = text,
        onValueChange = { value ->
            text = value.filter(Char::isDigit).take(4)
            NumberParser.parseCount(text)?.takeIf { InputValidator.validateCount(it) == null }?.let(onValid)
        },
        decimal = false,
        error = InputValidator.validateCount(NumberParser.parseCount(text)),
    )
}
