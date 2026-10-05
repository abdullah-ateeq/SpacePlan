package com.example.spaceplan.domain

import java.math.BigDecimal

enum class SpacePlanMode(val label: String) {
    FIT_COUNT("Fit Count"),
    EVEN_DISTRIBUTION("Even Distribution"),
}

enum class MeasurementUnit(
    val symbol: String,
    val spokenName: String,
    val millimetersPerUnit: BigDecimal,
) {
    MILLIMETER("mm", "millimeters", BigDecimal("1")),
    CENTIMETER("cm", "centimeters", BigDecimal("10")),
    METER("m", "meters", BigDecimal("1000")),
    INCH("in", "inches", BigDecimal("25.4")),
    FOOT("ft", "feet", BigDecimal("304.8")),
    YARD("yd", "yards", BigDecimal("914.4")),
}

enum class Orientation(val label: String) {
    HORIZONTAL("Horizontal"),
    VERTICAL("Vertical"),
}

enum class EdgeGapMode {
    NONE,
    EQUAL,
}

/** Validated calculator input. Lengths are in millimeters; [unit] is the display unit. */
data class SpacePlanInput(
    val mode: SpacePlanMode,
    val availableSpace: BigDecimal,
    val objectSize: BigDecimal,
    val gap: BigDecimal,
    val objectCount: Int,
    val unit: MeasurementUnit,
    val edgeGapMode: EdgeGapMode,
    val edgeGap: BigDecimal,
)

/**
 * Result of a layout calculation. All lengths are in millimeters.
 *
 * In [SpacePlanMode.FIT_COUNT], [remainingSpaceMm] is the unused space after placing the objects.
 * In [SpacePlanMode.EVEN_DISTRIBUTION], [remainingSpaceMm] is the space left after the objects,
 * which is then shared out as [equalGapMm].
 */
data class SpacePlanResult(
    val mode: SpacePlanMode,
    val availableMm: BigDecimal,
    val objectMm: BigDecimal,
    val objectsFit: Int,
    val internalGapCount: Int,
    val edgeGapCount: Int,
    /** Size of one gap between neighbouring objects. */
    val gapMm: BigDecimal,
    /** Size of one edge gap (0 when edge gaps are off). */
    val edgeGapMm: BigDecimal,
    val objectSpaceMm: BigDecimal,
    /** Total of all internal gaps. */
    val gapSpaceMm: BigDecimal,
    /** Total of both edge gaps. */
    val edgeSpaceMm: BigDecimal,
    val usedSpaceMm: BigDecimal,
    val remainingSpaceMm: BigDecimal,
    val equalGapMm: BigDecimal?,
    val completeFit: Boolean,
    val doesNotFit: Boolean = false,
    val requiredSpaceMm: BigDecimal? = null,
    val shortByMm: BigDecimal? = null,
)
