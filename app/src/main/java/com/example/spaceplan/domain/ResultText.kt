package com.example.spaceplan.domain

import java.math.BigDecimal

/** Builds the plain-text summaries, formulas and position lists shown or copied by the UI. */
class ResultText(private val unit: MeasurementUnit, private val decimals: Int) {

    fun m(mm: BigDecimal): String = MeasurementFormatter.formatMeasurement(mm, unit, decimals)

    private fun n(mm: BigDecimal): String = MeasurementFormatter.formatNumber(mm.fromMillimeters(unit), decimals)

    fun summary(result: SpacePlanResult): String = buildString {
        appendLine("SpacePlan — ${result.mode.label}")
        appendLine("Available: ${m(result.availableMm)}")
        appendLine("Object size: ${m(result.objectMm)}")
        when (result.mode) {
            SpacePlanMode.FIT_COUNT -> {
                appendLine("Gap: ${m(result.gapMm)}")
                if (result.edgeGapCount > 0) appendLine("Edge gaps: ${m(result.edgeGapMm)} each side")
                appendLine()
                appendLine("Objects fit: ${result.objectsFit}")
                if (result.doesNotFit) {
                    appendLine("Required for 1 object: ${m(result.requiredSpaceMm ?: BigDecimal.ZERO)}")
                    append("Short by: ${m(result.shortByMm ?: BigDecimal.ZERO)}")
                } else {
                    appendLine("Used space: ${m(result.usedSpaceMm)}")
                    append(if (result.completeFit) "Exact fit — no remaining space" else "Remaining: ${m(result.remainingSpaceMm)}")
                }
            }
            SpacePlanMode.EVEN_DISTRIBUTION -> {
                appendLine("Objects: ${result.objectsFit}")
                appendLine("Edge gaps: ${if (result.edgeGapCount > 0) "Equal" else "None"}")
                appendLine()
                if (result.doesNotFit) {
                    appendLine("These objects do not fit in the available space.")
                    appendLine("Required space: ${m(result.requiredSpaceMm ?: BigDecimal.ZERO)}")
                    append("Short by: ${m(result.shortByMm ?: BigDecimal.ZERO)}")
                } else {
                    val gap = result.equalGapMm
                    if (gap == null) {
                        appendLine("With one object, there is no internal spacing.")
                    } else {
                        appendLine("Equal gap: ${m(gap)}" + if (result.edgeGapCount > 0) " (including both edges)" else "")
                    }
                    appendLine("Total object space: ${m(result.objectSpaceMm)}")
                    append("Remaining space: ${m(result.remainingSpaceMm)}")
                }
            }
        }
    }

    fun positions(result: SpacePlanResult): String = buildString {
        appendLine("SpacePlan — object positions (from start)")
        val starts = SpacePlanCalculator.calculatePositions(result)
        starts.forEachIndexed { index, start ->
            appendLine("Object ${index + 1}: ${m(start)} – ${m(start.add(result.objectMm))}")
        }
        append("Gap: ${m(result.gapMm)}")
        if (result.edgeGapCount > 0) append(" · Edge gaps: ${m(result.edgeGapMm)}")
    }

    fun formula(result: SpacePlanResult): String = when (result.mode) {
        SpacePlanMode.FIT_COUNT -> if (result.edgeGapCount > 0) {
            "N = ⌊(A − 2e + g) ÷ (o + g)⌋\n" +
                "  = ⌊(${n(result.availableMm)} − 2×${n(result.edgeGapMm)} + ${n(result.gapMm)}) ÷ " +
                "(${n(result.objectMm)} + ${n(result.gapMm)})⌋ = ${result.objectsFit}"
        } else {
            "N = ⌊(A + g) ÷ (o + g)⌋\n" +
                "  = ⌊(${n(result.availableMm)} + ${n(result.gapMm)}) ÷ " +
                "(${n(result.objectMm)} + ${n(result.gapMm)})⌋ = ${result.objectsFit}"
        }
        SpacePlanMode.EVEN_DISTRIBUTION -> {
            val divisor = if (result.edgeGapCount > 0) "(N + 1)" else "(N − 1)"
            val divisorValue = if (result.edgeGapCount > 0) result.objectsFit + 1 else result.objectsFit - 1
            val head = "gap = (A − N×o) ÷ $divisor\n" +
                "    = (${n(result.availableMm)} − ${result.objectsFit}×${n(result.objectMm)}) ÷ $divisorValue"
            when {
                result.doesNotFit -> "$head\nA − N×o is negative: the objects do not fit."
                result.equalGapMm == null -> "$head\nWith one object, there is no internal spacing."
                else -> "$head = ${m(result.equalGapMm)}"
            }
        }
    }
}
