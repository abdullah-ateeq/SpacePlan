package com.example.spaceplan.domain

import java.math.BigDecimal

object NumberParser {
    private const val MAX_LENGTH = 24
    private val MAX_MAGNITUDE = BigDecimal("1000000000000")

    /** Safely parses user input. Returns null for empty, malformed or absurdly large values. */
    fun parseDecimal(text: String): BigDecimal? {
        val cleaned = text.trim().replace(',', '.')
        if (cleaned.isEmpty() || cleaned.length > MAX_LENGTH) return null
        if (cleaned.any { it != '.' && it != '-' && it != '+' && !it.isDigit() }) return null
        val value = try {
            BigDecimal(cleaned)
        } catch (_: NumberFormatException) {
            return null
        }
        return if (value.abs() > MAX_MAGNITUDE) null else value
    }

    fun parseCount(text: String): Int? {
        val cleaned = text.trim()
        if (cleaned.isEmpty() || cleaned.length > 7 || !cleaned.all { it.isDigit() }) return null
        return cleaned.toIntOrNull()
    }
}

enum class InputField { AVAILABLE, OBJECT_SIZE, GAP, EDGE_GAP, OBJECT_COUNT }

data class ValidationResult(
    val input: SpacePlanInput?,
    val errors: Map<InputField, String>,
)

object InputValidator {

    fun validateAvailable(mm: BigDecimal?): String? = when {
        mm == null -> "Enter the available space."
        mm.signum() <= 0 -> "Available space must be greater than 0."
        else -> null
    }

    fun validateObjectSize(mm: BigDecimal?): String? = when {
        mm == null -> "Enter the object size."
        mm.signum() <= 0 -> "Object size must be greater than 0."
        else -> null
    }

    fun validateGap(mm: BigDecimal?): String? = when {
        mm == null -> "Enter a gap (0 for none)."
        mm.signum() < 0 -> "Gap cannot be negative."
        else -> null
    }

    fun validateEdgeGap(mm: BigDecimal?, availableMm: BigDecimal?): String? = when {
        mm == null -> "Enter the edge gap."
        mm.signum() < 0 -> "Edge gap cannot be negative."
        availableMm != null && availableMm.signum() > 0 && mm.multiply(BigDecimal(2)) >= availableMm ->
            "Edge gaps use up the entire available space."
        else -> null
    }

    fun validateCount(count: Int?): String? = when {
        count == null || count !in SpacePlanCalculator.MIN_OBJECT_COUNT..SpacePlanCalculator.MAX_OBJECT_COUNT ->
            "Enter a whole number from ${SpacePlanCalculator.MIN_OBJECT_COUNT} to ${SpacePlanCalculator.MAX_OBJECT_COUNT}."
        else -> null
    }

    /** Validates millimeter values; only fields relevant to [mode] are checked. */
    fun validate(
        mode: SpacePlanMode,
        unit: MeasurementUnit,
        availableMm: BigDecimal?,
        objectMm: BigDecimal?,
        gapMm: BigDecimal?,
        count: Int?,
        edgeGapMode: EdgeGapMode,
        edgeGapMm: BigDecimal?,
    ): ValidationResult {
        val errors = buildMap {
            validateAvailable(availableMm)?.let { put(InputField.AVAILABLE, it) }
            validateObjectSize(objectMm)?.let { put(InputField.OBJECT_SIZE, it) }
            when (mode) {
                SpacePlanMode.FIT_COUNT -> {
                    validateGap(gapMm)?.let { put(InputField.GAP, it) }
                    if (edgeGapMode == EdgeGapMode.EQUAL) {
                        validateEdgeGap(edgeGapMm, availableMm)?.let { put(InputField.EDGE_GAP, it) }
                    }
                }
                SpacePlanMode.EVEN_DISTRIBUTION ->
                    validateCount(count)?.let { put(InputField.OBJECT_COUNT, it) }
            }
        }
        if (errors.isNotEmpty()) return ValidationResult(null, errors)
        return ValidationResult(
            SpacePlanInput(
                mode = mode,
                availableSpace = availableMm!!,
                objectSize = objectMm!!,
                gap = gapMm ?: BigDecimal.ZERO,
                objectCount = count ?: SpacePlanCalculator.MIN_OBJECT_COUNT,
                unit = unit,
                edgeGapMode = edgeGapMode,
                edgeGap = edgeGapMm ?: BigDecimal.ZERO,
            ),
            emptyMap(),
        )
    }
}

/** Runs the calculation for a validated input whose lengths are in millimeters. */
fun SpacePlanInput.calculate(): SpacePlanResult = when (mode) {
    SpacePlanMode.FIT_COUNT -> SpacePlanCalculator.calculateFitCount(
        availableMm = availableSpace,
        objectMm = objectSize,
        gapMm = gap,
        edgeGapMode = edgeGapMode,
        edgeGapMm = edgeGap,
    )
    SpacePlanMode.EVEN_DISTRIBUTION -> SpacePlanCalculator.calculateEvenDistribution(
        availableMm = availableSpace,
        objectMm = objectSize,
        objectCount = objectCount,
        edgeGapMode = edgeGapMode,
    )
}
