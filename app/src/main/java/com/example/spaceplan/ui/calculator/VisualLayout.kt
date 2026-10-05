package com.example.spaceplan.ui.calculator

import com.example.spaceplan.domain.SpacePlanResult

enum class SegmentKind { EDGE_GAP, OBJECT, GAP, REMAINING, ELLIPSIS }

/** A segment along the layout axis. [start] and [end] are fractions (0..1) of the drawn extent. */
data class VisualSegment(val kind: SegmentKind, val start: Float, val end: Float)

data class VisualLayout(
    val segments: List<VisualSegment>,
    /** Where the end of the available space falls; below 1 when the layout overflows it. */
    val availableEnd: Float,
    val overflow: Boolean,
    val simplified: Boolean,
    val objectCount: Int,
)

object VisualLayoutBuilder {
    /** Above this many objects the diagram switches to a simplified "first … last" form. */
    const val MAX_DETAILED_OBJECTS = 24
    private const val SIMPLIFIED_LEADING = 4

    fun build(result: SpacePlanResult): VisualLayout {
        val available = result.availableMm.toDouble()
        val objectSize = result.objectMm.toDouble()
        val gap = result.gapMm.toDouble()
        val edge = result.edgeGapMm.toDouble()

        // When nothing fits in Fit Count mode, show the single object that is too large.
        val count = if (result.doesNotFit && result.objectsFit == 0) 1 else result.objectsFit
        val extentUsed = if (result.doesNotFit) {
            result.requiredSpaceMm?.toDouble() ?: (count * objectSize)
        } else {
            result.usedSpaceMm.toDouble()
        }
        val total = maxOf(available, extentUsed).takeIf { it > 0.0 && it.isFinite() } ?: 1.0
        fun f(mm: Double): Float = (mm / total).toFloat().coerceIn(0f, 1f)

        val segments = mutableListOf<VisualSegment>()
        val simplified = count > MAX_DETAILED_OBJECTS
        var position = 0.0
        if (edge > 0) {
            segments += VisualSegment(SegmentKind.EDGE_GAP, 0f, f(edge))
            position = edge
        }

        if (!simplified) {
            repeat(count) { index ->
                segments += VisualSegment(SegmentKind.OBJECT, f(position), f(position + objectSize))
                position += objectSize
                if (index < count - 1) {
                    if (gap > 0) segments += VisualSegment(SegmentKind.GAP, f(position), f(position + gap))
                    position += gap
                }
            }
        } else {
            // Leading objects, an ellipsis, then the last object, spread across the occupied run.
            val runStart = position
            val runEnd = extentUsed - edge
            val cells = SIMPLIFIED_LEADING + 2
            val cell = (runEnd - runStart) / cells
            val objectShare = if (gap > 0) objectSize / (objectSize + gap) else 0.92
            val objectWidth = cell * objectShare.coerceIn(0.35, 0.92)
            for (i in 0 until cells) {
                val cellStart = runStart + i * cell
                val kind = if (i == SIMPLIFIED_LEADING) SegmentKind.ELLIPSIS else SegmentKind.OBJECT
                val end = if (kind == SegmentKind.ELLIPSIS) cellStart + cell else cellStart + objectWidth
                segments += VisualSegment(kind, f(cellStart), f(end))
            }
            position = runEnd
        }

        if (edge > 0) {
            segments += VisualSegment(SegmentKind.EDGE_GAP, f(position), f(position + edge))
            position += edge
        }
        if (!result.doesNotFit && available - position > available * 1e-9) {
            segments += VisualSegment(SegmentKind.REMAINING, f(position), f(available))
        }
        return VisualLayout(
            segments = segments,
            availableEnd = f(available),
            overflow = result.doesNotFit,
            simplified = simplified,
            objectCount = count,
        )
    }
}
