package com.example.spaceplan.ui.calculator

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.spaceplan.domain.MeasurementUnit
import com.example.spaceplan.domain.Orientation
import com.example.spaceplan.domain.ResultText
import com.example.spaceplan.domain.SpacePlanResult
import com.example.spaceplan.ui.components.SectionLabel

private data class VisualColors(
    val object_: Color,
    val objectOutline: Color,
    val track: Color,
    val boundary: Color,
    val dimension: Color,
    val remaining: Color,
    val overflow: Color,
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LayoutVisualization(
    result: SpacePlanResult,
    orientation: Orientation,
    unit: MeasurementUnit,
    decimals: Int,
    modifier: Modifier = Modifier,
) {
    val layout = remember(result) { VisualLayoutBuilder.build(result) }
    val text = remember(unit, decimals) { ResultText(unit, decimals) }
    val scheme = MaterialTheme.colorScheme
    val colors = VisualColors(
        object_ = scheme.primary.copy(alpha = 0.78f),
        objectOutline = scheme.primary,
        track = scheme.surfaceVariant.copy(alpha = 0.55f),
        boundary = scheme.outline,
        dimension = scheme.onSurfaceVariant,
        remaining = scheme.tertiary,
        overflow = scheme.error,
    )
    val description = buildString {
        append("Layout diagram. ")
        append("${layout.objectCount} objects")
        if (result.doesNotFit) append(", exceeding the available space") else if (!result.completeFit) {
            append(", ${text.m(result.remainingSpaceMm)} remaining")
        }
        append(".")
    }

    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionLabel("Available space", Modifier.weight(1f))
            Text(text.m(result.availableMm), style = MaterialTheme.typography.labelLarge)
        }
        val canvasModifier = Modifier
            .fillMaxWidth()
            .height(if (orientation == Orientation.HORIZONTAL) 92.dp else 320.dp)
            .semantics { contentDescription = description }
        Canvas(canvasModifier) {
            drawLayout(layout, orientation, colors)
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            LegendItem("Object ${text.m(result.objectMm)}") { drawRoundRect(colors.object_, cornerRadius = CornerRadius(3f)) }
            if (result.gapMm.signum() > 0) {
                LegendItem("Gap ${text.m(result.gapMm)}") { drawGapMark(Offset(0f, size.height / 2), Offset(size.width, size.height / 2), colors.dimension) }
            }
            if (result.edgeGapCount > 0 && result.edgeGapMm.signum() > 0) {
                LegendItem("Edge ${text.m(result.edgeGapMm)}") { drawGapMark(Offset(0f, size.height / 2), Offset(size.width, size.height / 2), colors.dimension) }
            }
            if (!result.doesNotFit && result.remainingSpaceMm.signum() > 0 && result.equalGapMm == null) {
                LegendItem("Remaining ${text.m(result.remainingSpaceMm)}") { drawHatch(Rect(Offset.Zero, size), colors.remaining) }
            }
            if (result.doesNotFit) {
                LegendItem("Overflow ${text.m(result.shortByMm ?: result.objectMm)}") {
                    drawRect(colors.overflow.copy(alpha = 0.5f))
                }
            }
            if (layout.simplified) {
                Text(
                    "Simplified · ${layout.objectCount} objects",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun LegendItem(label: String, swatch: DrawScope.() -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Canvas(Modifier.size(width = 16.dp, height = 10.dp), onDraw = swatch)
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun DrawScope.drawLayout(layout: VisualLayout, orientation: Orientation, colors: VisualColors) {
    val horizontal = orientation == Orientation.HORIZONTAL
    val axisLength = if (horizontal) size.width else size.height
    val cross = if (horizontal) size.height else size.width
    val pad = 4.dp.toPx()
    val usable = axisLength - pad * 2
    val dimCross = if (horizontal) 10.dp.toPx() else cross / 2 - 44.dp.toPx()
    val bandThickness = if (horizontal) cross - 34.dp.toPx() else 56.dp.toPx()
    val bandStart = if (horizontal) 26.dp.toPx() else cross / 2 - bandThickness / 2 + 12.dp.toPx()

    fun axis(fraction: Float) = pad + usable * fraction
    fun rect(start: Float, end: Float, inset: Float = 0f): Rect {
        val a = axis(start)
        val b = axis(end).coerceAtLeast(a + 1f)
        return if (horizontal) {
            Rect(a, bandStart + inset, b, bandStart + bandThickness - inset)
        } else {
            Rect(bandStart + inset, a, bandStart + bandThickness - inset, b)
        }
    }
    fun point(along: Float, across: Float) = if (horizontal) Offset(along, across) else Offset(across, along)

    // Dimension line spanning the available space.
    val availEnd = axis(layout.availableEnd)
    val tick = 5.dp.toPx()
    val stroke = 1.dp.toPx()
    drawLine(colors.dimension, point(axis(0f), dimCross), point(availEnd, dimCross), stroke)
    drawLine(colors.dimension, point(axis(0f), dimCross - tick), point(axis(0f), dimCross + tick), stroke)
    drawLine(colors.dimension, point(availEnd, dimCross - tick), point(availEnd, dimCross + tick), stroke)
    drawArrowHead(point(axis(0f), dimCross), forward = true, horizontal = horizontal, color = colors.dimension)
    drawArrowHead(point(availEnd, dimCross), forward = false, horizontal = horizontal, color = colors.dimension)

    // Track representing the available space.
    val track = rect(0f, layout.availableEnd)
    drawRect(colors.track, track.topLeft, track.size)

    layout.segments.forEach { segment ->
        when (segment.kind) {
            SegmentKind.OBJECT -> {
                val r = rect(segment.start, segment.end, inset = 4.dp.toPx())
                val radius = CornerRadius(3.dp.toPx())
                drawRoundRect(colors.object_, r.topLeft, r.size, radius)
                drawRoundRect(colors.objectOutline, r.topLeft, r.size, radius, style = Stroke(stroke))
                if (layout.overflow) {
                    // Tint the part of the object that sticks out past the available space.
                    val overflowRect = rect(layout.availableEnd, segment.end, inset = 4.dp.toPx())
                    if (segment.end > layout.availableEnd) {
                        drawRect(colors.overflow.copy(alpha = 0.55f), overflowRect.topLeft, overflowRect.size)
                    }
                }
            }
            SegmentKind.GAP, SegmentKind.EDGE_GAP -> {
                val a = axis(segment.start)
                val b = axis(segment.end)
                if (b - a > 6.dp.toPx()) {
                    val mid = bandStart + bandThickness / 2
                    drawGapMark(point(a, mid), point(b, mid), colors.dimension)
                }
            }
            SegmentKind.REMAINING -> drawHatch(rect(segment.start, segment.end, inset = 4.dp.toPx()), colors.remaining)
            SegmentKind.ELLIPSIS -> {
                val r = rect(segment.start, segment.end)
                val dot = 2.dp.toPx()
                for (i in -1..1) {
                    val offset = if (horizontal) Offset(r.center.x + i * dot * 3.5f, r.center.y) else Offset(r.center.x, r.center.y + i * dot * 3.5f)
                    drawCircle(colors.dimension, dot, offset)
                }
            }
        }
    }

    // Boundary of the available space, drawn last so it stays visible.
    drawRect(colors.boundary, track.topLeft, track.size, style = Stroke(stroke))
    if (layout.overflow) {
        val over = rect(layout.availableEnd, 1f)
        drawRect(
            colors.overflow,
            over.topLeft,
            over.size,
            style = Stroke(stroke, pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f))),
        )
    }
}

private fun DrawScope.drawArrowHead(tip: Offset, forward: Boolean, horizontal: Boolean, color: Color) {
    val len = 5.dp.toPx()
    val half = 3.dp.toPx()
    val dir = if (forward) 1f else -1f
    val back = if (horizontal) Offset(tip.x + dir * len, tip.y) else Offset(tip.x, tip.y + dir * len)
    val a = if (horizontal) Offset(back.x, back.y - half) else Offset(back.x - half, back.y)
    val b = if (horizontal) Offset(back.x, back.y + half) else Offset(back.x + half, back.y)
    drawLine(color, tip, a, 1.dp.toPx())
    drawLine(color, tip, b, 1.dp.toPx())
}

/** Small double-ended dimension mark across a gap. */
private fun DrawScope.drawGapMark(from: Offset, to: Offset, color: Color) {
    val stroke = 1.dp.toPx()
    drawLine(color.copy(alpha = 0.8f), from, to, stroke, pathEffect = PathEffect.dashPathEffect(floatArrayOf(3f, 3f)))
    val horizontal = kotlin.math.abs(to.x - from.x) >= kotlin.math.abs(to.y - from.y)
    val t = 3.dp.toPx()
    listOf(from, to).forEach { p ->
        if (horizontal) drawLine(color, Offset(p.x, p.y - t), Offset(p.x, p.y + t), stroke)
        else drawLine(color, Offset(p.x - t, p.y), Offset(p.x + t, p.y), stroke)
    }
}

/** Diagonal hatching used for remaining (unused) space. */
private fun DrawScope.drawHatch(area: Rect, color: Color) {
    if (area.width <= 0f || area.height <= 0f) return
    drawRect(color.copy(alpha = 0.10f), area.topLeft, Size(area.width, area.height))
    clipRect(area.left, area.top, area.right, area.bottom) {
        val step = 6.dp.toPx()
        var x = area.left - area.height
        while (x < area.right) {
            drawLine(color.copy(alpha = 0.55f), Offset(x, area.bottom), Offset(x + area.height, area.top), 1.dp.toPx())
            x += step
        }
    }
}
