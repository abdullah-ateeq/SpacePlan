package com.example.spaceplan.ui.calculator

import android.content.ClipData
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BookmarkAdd
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.FormatListNumbered
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.spaceplan.data.AppSettings
import com.example.spaceplan.domain.EdgeGapMode
import com.example.spaceplan.domain.InputField
import com.example.spaceplan.domain.Orientation
import com.example.spaceplan.domain.ResultText
import com.example.spaceplan.domain.SpacePlanMode
import com.example.spaceplan.ui.CalculatorUiState
import com.example.spaceplan.ui.SpacePlanViewModel
import com.example.spaceplan.ui.components.NumberField
import com.example.spaceplan.ui.components.ScreenHeader
import com.example.spaceplan.ui.components.SectionLabel
import com.example.spaceplan.ui.components.SegmentedSelector
import com.example.spaceplan.ui.components.SwitchRow
import com.example.spaceplan.ui.components.UnitSelector
import kotlinx.coroutines.launch

private val QUICK_COUNTS = listOf(2, 3, 4, 5, 6, 10)

@Composable
fun CalculatorScreen(
    viewModel: SpacePlanViewModel,
    showMessage: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current
    val focusManager = LocalFocusManager.current
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val tick: () -> Unit = {
        if (settings.hapticFeedback) haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
    }
    val text = remember(state.inputs.unit, settings.decimalPlaces) {
        ResultText(state.inputs.unit, settings.decimalPlaces)
    }

    val actions = ResultActions(
        onCopy = { value ->
            scope.launch {
                clipboard.setClipEntry(ClipEntry(ClipData.newPlainText("SpacePlan", value)))
                showMessage("Result copied")
            }
        },
        onCopyPositions = { value ->
            scope.launch {
                clipboard.setClipEntry(ClipEntry(ClipData.newPlainText("SpacePlan positions", value)))
                showMessage("Positions copied")
            }
        },
        onSave = {
            tick()
            showMessage(if (viewModel.saveCurrent()) "Calculation saved" else "Enter valid values before saving")
        },
        onReset = {
            tick()
            focusManager.clearFocus()
            viewModel.reset()
            showMessage("Calculator reset")
        },
    )

    BoxWithConstraints(modifier.fillMaxSize()) {
        val wide = maxWidth >= 720.dp
        if (wide) {
            Row(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                Column(
                    Modifier
                        .weight(0.9f)
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    ScreenHeader("SpacePlan", "Calculate how many objects fit in a space.")
                    InputsSection(
                        state = state,
                        viewModel = viewModel,
                        tick = tick,
                        onCalculate = {
                            focusManager.clearFocus()
                            tick()
                        },
                    )
                }
                Column(
                    Modifier
                        .weight(1.1f)
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    ResultsSection(state, settings, text, actions)
                }
            }
        } else {
            val scroll = rememberScrollState()
            var resultsOffset by remember { mutableIntStateOf(0) }
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(scroll)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ScreenHeader("SpacePlan", "Calculate how many objects fit in a space.")
                InputsSection(
                    state = state,
                    viewModel = viewModel,
                    tick = tick,
                    onCalculate = {
                        focusManager.clearFocus()
                        tick()
                        scope.launch { scroll.animateScrollTo(resultsOffset) }
                    },
                )
                Spacer(
                    Modifier
                        .height(4.dp)
                        .onGloballyPositioned { resultsOffset = it.positionInParent().y.toInt() },
                )
                ResultsSection(state, settings, text, actions)
            }
        }
    }
}

private class ResultActions(
    val onCopy: (String) -> Unit,
    val onCopyPositions: (String) -> Unit,
    val onSave: () -> Unit,
    val onReset: () -> Unit,
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun InputsSection(
    state: CalculatorUiState,
    viewModel: SpacePlanViewModel,
    tick: () -> Unit,
    onCalculate: () -> Unit,
) {
    val inputs = state.inputs
    val unit = inputs.unit
    val errors = state.errors

    SegmentedSelector(
        options = SpacePlanMode.entries,
        selected = inputs.mode,
        onSelect = {
            if (it != inputs.mode) tick()
            viewModel.setMode(it)
        },
        label = { it.label },
    )

    Row(verticalAlignment = Alignment.Top) {
        NumberField(
            label = "Available space",
            value = inputs.available.text,
            onValueChange = viewModel::setAvailable,
            error = errors[InputField.AVAILABLE],
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.size(8.dp))
        UnitSelector(unit = unit, onUnitSelected = viewModel::setUnit)
    }

    NumberField(
        label = "Object size",
        value = inputs.objectSize.text,
        onValueChange = viewModel::setObjectSize,
        suffix = unit.symbol,
        error = errors[InputField.OBJECT_SIZE],
        supporting = "The dimension repeated along the space",
    )

    when (inputs.mode) {
        SpacePlanMode.FIT_COUNT -> {
            NumberField(
                label = "Gap between objects",
                value = inputs.gap.text,
                onValueChange = viewModel::setGap,
                suffix = unit.symbol,
                error = errors[InputField.GAP],
            )
            SwitchRow(
                title = "Edge gaps",
                subtitle = edgeGapCaption(inputs.orientation),
                checked = inputs.edgeGapMode == EdgeGapMode.EQUAL,
                onCheckedChange = { viewModel.setEdgeGapMode(if (it) EdgeGapMode.EQUAL else EdgeGapMode.NONE) },
            )
            if (inputs.edgeGapMode == EdgeGapMode.EQUAL) {
                NumberField(
                    label = "Edge gap",
                    value = inputs.edgeGap.text,
                    onValueChange = viewModel::setEdgeGap,
                    suffix = unit.symbol,
                    error = errors[InputField.EDGE_GAP],
                    supporting = "Same value on both sides",
                )
            }
        }
        SpacePlanMode.EVEN_DISTRIBUTION -> {
            NumberField(
                label = "Number of objects",
                value = inputs.countText,
                onValueChange = viewModel::setCount,
                decimal = false,
                error = errors[InputField.OBJECT_COUNT],
                supporting = "1–1000",
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                QUICK_COUNTS.forEach { count ->
                    FilterChip(
                        selected = inputs.countText == count.toString(),
                        onClick = { viewModel.setCount(count.toString()) },
                        label = { Text(count.toString()) },
                        modifier = Modifier.semantics { contentDescription = "$count objects" },
                    )
                }
            }
            SectionLabel("Edge gaps")
            SegmentedSelector(
                options = EdgeGapMode.entries,
                selected = inputs.edgeGapMode,
                onSelect = viewModel::setEdgeGapMode,
                label = { if (it == EdgeGapMode.NONE) "No edge gaps" else "Equal edge gaps" },
            )
            Text(
                if (inputs.edgeGapMode == EdgeGapMode.EQUAL) {
                    "All gaps are equal, including both edges."
                } else {
                    "Gaps only between objects; the first and last objects touch the ends."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    SectionLabel("Orientation")
    SegmentedSelector(
        options = Orientation.entries,
        selected = inputs.orientation,
        onSelect = viewModel::setOrientation,
        label = { it.label },
    )

    Button(
        onClick = onCalculate,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
    ) {
        Text("Calculate", style = MaterialTheme.typography.titleMedium)
    }
}

private fun edgeGapCaption(orientation: Orientation) = when (orientation) {
    Orientation.HORIZONTAL -> "Includes left and right edge gaps."
    Orientation.VERTICAL -> "Includes top and bottom edge gaps."
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ResultsSection(
    state: CalculatorUiState,
    settings: AppSettings,
    text: ResultText,
    actions: ResultActions,
) {
    val result = state.result
    if (result == null) {
        InvalidResultCard(state.calculationError)
    } else {
        MainResultCard(result, text, state.inputs.unit, settings.decimalPlaces)
        if (settings.showVisualization) {
            OutlinedSection {
                LayoutVisualization(
                    result = result,
                    orientation = state.inputs.orientation,
                    unit = state.inputs.unit,
                    decimals = settings.decimalPlaces,
                )
            }
        }
        if (settings.showFormula) FormulaCard(text.formula(result))
        if (settings.showBreakdown) BreakdownSection(result, text)
    }

    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        if (result != null) {
            FilledTonalButton(
                onClick = { actions.onCopy(text.summary(result)) },
                modifier = Modifier.semantics { contentDescription = "Copy result" },
            ) {
                Icon(Icons.Outlined.ContentCopy, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                Text("Copy result")
            }
            if (result.mode == SpacePlanMode.EVEN_DISTRIBUTION && !result.doesNotFit) {
                OutlinedButton(onClick = { actions.onCopyPositions(text.positions(result)) }) {
                    Icon(Icons.Outlined.FormatListNumbered, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                    Text("Copy positions")
                }
            }
            Button(
                onClick = actions.onSave,
                modifier = Modifier.semantics { contentDescription = "Save calculation" },
            ) {
                Icon(Icons.Outlined.BookmarkAdd, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                Text("Save")
            }
        }
        TextButton(
            onClick = actions.onReset,
            modifier = Modifier.semantics { contentDescription = "Reset calculator" },
        ) {
            Icon(Icons.Outlined.RestartAlt, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
            Text("Reset")
        }
    }

    Text(
        "SpacePlan performs mathematical layout calculations. Verify physical dimensions before cutting, " +
            "drilling, installation, or permanent placement.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .widthIn(max = 640.dp)
            .padding(top = 4.dp, bottom = 16.dp),
    )
}
