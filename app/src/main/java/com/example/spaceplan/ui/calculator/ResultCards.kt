package com.example.spaceplan.ui.calculator

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.example.spaceplan.domain.MeasurementFormatter
import com.example.spaceplan.domain.MeasurementUnit
import com.example.spaceplan.domain.ResultText
import com.example.spaceplan.domain.SpacePlanCalculator
import com.example.spaceplan.domain.SpacePlanMode
import com.example.spaceplan.domain.SpacePlanResult
import com.example.spaceplan.ui.components.LabeledValue
import com.example.spaceplan.ui.components.SectionLabel
import com.example.spaceplan.ui.theme.FormulaStyle

private val CardShape = RoundedCornerShape(20.dp)

@Composable
fun OutlinedSection(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = CardShape,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
    }
}

@Composable
fun InvalidResultCard(message: String?) {
    OutlinedSection {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(12.dp))
            Text(
                message ?: "Enter valid values to see the result.",
                style = MaterialTheme.typography.bodyMedium,
                color = if (message != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun StatusBadge(label: String, icon: ImageVector, container: Color, content: Color) {
    Surface(shape = RoundedCornerShape(50), color = container, contentColor = content) {
        Row(
            Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(label, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
fun MainResultCard(result: SpacePlanResult, text: ResultText, unit: MeasurementUnit, decimals: Int) {
    val spoken = { mm: java.math.BigDecimal -> MeasurementFormatter.spokenMeasurement(mm, unit, decimals) }
    Surface(
        shape = CardShape,
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            Modifier
                .padding(20.dp)
                .animateContentSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            when {
                result.doesNotFit -> NoFitContent(result, text, spoken)
                result.mode == SpacePlanMode.FIT_COUNT -> FitCountContent(result, text, spoken)
                else -> DistributionContent(result, text, spoken)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FitCountContent(result: SpacePlanResult, text: ResultText, spoken: (java.math.BigDecimal) -> String) {
    val count = result.objectsFit
    val summary = buildString {
        append(if (count == 1) "1 object fits" else "$count objects fit")
        append(if (result.completeFit) ", exact fit, no remaining space" else ", ${spoken(result.remainingSpaceMm)} remaining")
    }
    Column(
        Modifier.semantics(mergeDescendants = true) { contentDescription = summary },
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (result.completeFit) {
            StatusBadge(
                "EXACT FIT",
                Icons.Outlined.CheckCircle,
                MaterialTheme.colorScheme.primary,
                MaterialTheme.colorScheme.onPrimary,
            )
        }
        SectionLabel("Objects fit")
        Text(count.toString(), style = MaterialTheme.typography.displayLarge, color = MaterialTheme.colorScheme.onSurface)
        if (result.completeFit) {
            Text("$count ${if (count == 1) "object" else "objects"} · No remaining space.", style = MaterialTheme.typography.bodyLarge)
        } else {
            Row(verticalAlignment = Alignment.Bottom) {
                LabeledValue("Remaining", text.m(result.remainingSpaceMm))
                Spacer(Modifier.width(12.dp))
                Text(
                    "${MeasurementFormatter.formatPercent(SpacePlanCalculator.calculateUnusedPercentage(result.remainingSpaceMm, result.availableMm))} unused",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 2.dp),
                )
            }
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        LabeledValue("Object size", text.m(result.objectMm))
        LabeledValue("Gap", text.m(result.gapMm))
        if (result.edgeGapCount > 0) LabeledValue("Edge gaps", "2 × ${text.m(result.edgeGapMm)}")
        LabeledValue("Used space", text.m(result.usedSpaceMm))
        LabeledValue(
            "Used",
            MeasurementFormatter.formatPercent(SpacePlanCalculator.calculateFitPercentage(result.usedSpaceMm, result.availableMm)),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DistributionContent(result: SpacePlanResult, text: ResultText, spoken: (java.math.BigDecimal) -> String) {
    val gap = result.equalGapMm
    val withEdges = result.edgeGapCount > 0
    val summary = if (gap == null) {
        "One object. With one object, there is no internal spacing. ${spoken(result.remainingSpaceMm)} remaining"
    } else {
        "Equal gap ${spoken(gap)}, ${result.objectsFit} objects" + if (withEdges) ", including both edges" else ""
    }
    Column(
        Modifier.semantics(mergeDescendants = true) { contentDescription = summary },
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (gap != null && gap.signum() == 0) {
            StatusBadge("EXACT FIT", Icons.Outlined.CheckCircle, MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.onPrimary)
        }
        SectionLabel("Equal gap")
        if (gap == null) {
            Text("—", style = MaterialTheme.typography.displayMedium)
            Text("With one object, there is no internal spacing.", style = MaterialTheme.typography.bodyLarge)
        } else {
            Text(text.m(gap), style = MaterialTheme.typography.displayMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(
                if (withEdges) "Between objects and on both edges" else "Between objects · no edge gaps",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        LabeledValue("Objects", result.objectsFit.toString())
        LabeledValue("Object size", text.m(result.objectMm))
        LabeledValue("Total object space", text.m(result.objectSpaceMm))
        LabeledValue("Available space", text.m(result.availableMm))
        LabeledValue(if (gap == null) "Remaining" else "Space to distribute", text.m(result.remainingSpaceMm))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NoFitContent(result: SpacePlanResult, text: ResultText, spoken: (java.math.BigDecimal) -> String) {
    val required = result.requiredSpaceMm ?: result.objectMm
    val shortBy = result.shortByMm ?: java.math.BigDecimal.ZERO
    val fitMode = result.mode == SpacePlanMode.FIT_COUNT
    val headline = if (fitMode) "0 objects fit" else "These objects do not fit in the available space."
    Column(
        Modifier.semantics(mergeDescendants = true) {
            contentDescription = "$headline. Short by ${spoken(shortBy)}"
        },
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        StatusBadge(
            "DOES NOT FIT",
            Icons.Outlined.ErrorOutline,
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer,
        )
        if (fitMode) {
            SectionLabel("Objects fit")
            Text("0", style = MaterialTheme.typography.displayLarge)
        } else {
            Text(headline, style = MaterialTheme.typography.titleLarge)
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        LabeledValue(if (fitMode) "Required for 1 object" else "Required space", text.m(required))
        LabeledValue("Available", text.m(result.availableMm))
        LabeledValue("Short by", text.m(shortBy), valueColor = MaterialTheme.colorScheme.error)
    }
}

@Composable
fun FormulaCard(formula: String) {
    OutlinedSection {
        SectionLabel("Formula")
        Text(formula, style = FormulaStyle, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun BreakdownSection(result: SpacePlanResult, text: ResultText) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    OutlinedSection {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(role = Role.Button, onClickLabel = if (expanded) "Collapse" else "Expand") { expanded = !expanded }
                .semantics {
                    heading()
                    stateDescription = if (expanded) "Expanded" else "Collapsed"
                },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SectionLabel("Calculation breakdown", Modifier.weight(1f))
            Icon(if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, contentDescription = null)
        }
        if (expanded) {
            breakdownRows(result, text).forEach { (label, value) ->
                Row(Modifier.fillMaxWidth().semantics(mergeDescendants = true) {}) {
                    Text(
                        label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    Text(value, style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum"))
                }
            }
        }
    }
}

private fun breakdownRows(r: SpacePlanResult, t: ResultText): List<Pair<String, String>> = buildList {
    add("Available space" to t.m(r.availableMm))
    add("Object size" to t.m(r.objectMm))
    if (r.doesNotFit) {
        add("Objects" to r.objectsFit.toString())
        add("Required space" to t.m(r.requiredSpaceMm ?: r.objectMm))
        add("Short by" to t.m(r.shortByMm ?: java.math.BigDecimal.ZERO))
        return@buildList
    }
    add("Objects" to r.objectsFit.toString())
    add("Internal gaps" to r.internalGapCount.toString())
    add("Gap size" to t.m(r.gapMm))
    if (r.edgeGapCount > 0) {
        add("Edge gaps" to "2 × ${t.m(r.edgeGapMm)}")
    }
    add("Object space" to "${r.objectsFit} × ${t.m(r.objectMm)} = ${t.m(r.objectSpaceMm)}")
    add("Gap space" to "${r.internalGapCount} × ${t.m(r.gapMm)} = ${t.m(r.gapSpaceMm)}")
    if (r.edgeGapCount > 0) add("Edge space" to t.m(r.edgeSpaceMm))
    add("Used space" to t.m(r.usedSpaceMm))
    if (r.mode == SpacePlanMode.FIT_COUNT) {
        add("Remaining" to if (r.completeFit) "0 (exact fit)" else t.m(r.remainingSpaceMm))
        add("Used" to MeasurementFormatter.formatPercent(SpacePlanCalculator.calculateFitPercentage(r.usedSpaceMm, r.availableMm)))
        add("Unused" to MeasurementFormatter.formatPercent(SpacePlanCalculator.calculateUnusedPercentage(r.remainingSpaceMm, r.availableMm)))
    } else {
        add("Space to distribute" to t.m(r.remainingSpaceMm))
        val divisions = if (r.edgeGapCount > 0) r.objectsFit + 1 else r.objectsFit - 1
        add("Equal divisions" to divisions.coerceAtLeast(0).toString())
        add("Leftover" to t.m(r.availableMm.subtract(r.usedSpaceMm).max(java.math.BigDecimal.ZERO)))
    }
}
