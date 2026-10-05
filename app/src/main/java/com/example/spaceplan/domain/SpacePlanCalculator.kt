package com.example.spaceplan.domain

import java.math.BigDecimal
import java.math.MathContext

/** Thrown when inputs cannot produce a meaningful layout. */
class SpacePlanException(message: String) : IllegalArgumentException(message)

object SpacePlanCalculator {
    const val MIN_OBJECT_COUNT = 1
    const val MAX_OBJECT_COUNT = 1000
    const val MAX_FIT_COUNT = 1_000_000

    private val MATH: MathContext = MathContext.DECIMAL128
    private val TWO = BigDecimal(2)
    private val HUNDRED = BigDecimal(100)

    /** Differences smaller than this (in mm) are treated as zero. */
    val TOLERANCE_MM: BigDecimal = BigDecimal("1E-9")

    fun calculateFitCount(
        availableMm: BigDecimal,
        objectMm: BigDecimal,
        gapMm: BigDecimal,
        edgeGapMode: EdgeGapMode,
        edgeGapMm: BigDecimal,
    ): SpacePlanResult {
        requirePositive(availableMm, "Available space must be greater than 0.")
        requirePositive(objectMm, "Object size must be greater than 0.")
        requireNotNegative(gapMm, "Gap cannot be negative.")
        requireNotNegative(edgeGapMm, "Edge gap cannot be negative.")

        val edge = if (edgeGapMode == EdgeGapMode.EQUAL) edgeGapMm else BigDecimal.ZERO
        val edgeCount = if (edgeGapMode == EdgeGapMode.EQUAL) 2 else 0
        val innerSpace = availableMm.subtract(edge.multiply(TWO))
        if (innerSpace.signum() <= 0) {
            throw SpacePlanException("Edge gaps use up the entire available space.")
        }

        // Largest N with N*o + (N-1)*g <= inner  <=>  N <= (inner + g) / (o + g)
        val quotient = innerSpace.add(gapMm).divideToIntegralValue(objectMm.add(gapMm))
        if (quotient > BigDecimal(MAX_FIT_COUNT)) {
            throw SpacePlanException("More than ${"%,d".format(MAX_FIT_COUNT)} objects fit. Check the units.")
        }
        var count = quotient.toInt()
        // Guard the closed-form result against any boundary error.
        while (count > 0 && !fits(count, objectMm, gapMm, edge, availableMm)) count--
        while (count < MAX_FIT_COUNT && fits(count + 1, objectMm, gapMm, edge, availableMm)) count++

        if (count == 0) {
            val required = objectMm.add(edge.multiply(TWO))
            return SpacePlanResult(
                mode = SpacePlanMode.FIT_COUNT,
                availableMm = availableMm,
                objectMm = objectMm,
                objectsFit = 0,
                internalGapCount = 0,
                edgeGapCount = edgeCount,
                gapMm = gapMm,
                edgeGapMm = edge,
                objectSpaceMm = BigDecimal.ZERO,
                gapSpaceMm = BigDecimal.ZERO,
                edgeSpaceMm = BigDecimal.ZERO,
                usedSpaceMm = BigDecimal.ZERO,
                remainingSpaceMm = availableMm,
                equalGapMm = null,
                completeFit = false,
                doesNotFit = true,
                requiredSpaceMm = required,
                shortByMm = required.subtract(availableMm),
            )
        }

        val n = BigDecimal(count)
        val objectSpace = n.multiply(objectMm)
        val gapSpace = BigDecimal(count - 1).multiply(gapMm)
        val edgeSpace = edge.multiply(TWO)
        val used = calculateUsedSpace(count, objectMm, gapMm, edge)
        val remaining = calculateRemainingSpace(availableMm, used)
        return SpacePlanResult(
            mode = SpacePlanMode.FIT_COUNT,
            availableMm = availableMm,
            objectMm = objectMm,
            objectsFit = count,
            internalGapCount = count - 1,
            edgeGapCount = edgeCount,
            gapMm = gapMm,
            edgeGapMm = edge,
            objectSpaceMm = objectSpace,
            gapSpaceMm = gapSpace,
            edgeSpaceMm = edgeSpace,
            usedSpaceMm = used,
            remainingSpaceMm = remaining,
            equalGapMm = null,
            completeFit = remaining.signum() == 0,
        )
    }

    fun calculateEvenDistribution(
        availableMm: BigDecimal,
        objectMm: BigDecimal,
        objectCount: Int,
        edgeGapMode: EdgeGapMode,
    ): SpacePlanResult {
        requirePositive(availableMm, "Available space must be greater than 0.")
        requirePositive(objectMm, "Object size must be greater than 0.")
        if (objectCount !in MIN_OBJECT_COUNT..MAX_OBJECT_COUNT) {
            throw SpacePlanException("Number of objects must be from $MIN_OBJECT_COUNT to $MAX_OBJECT_COUNT.")
        }

        val objectSpace = BigDecimal(objectCount).multiply(objectMm)
        val remaining = calculateRemainingSpace(availableMm, objectSpace)
        val edgeCount = if (edgeGapMode == EdgeGapMode.EQUAL) 2 else 0

        if (remaining.signum() < 0) {
            return SpacePlanResult(
                mode = SpacePlanMode.EVEN_DISTRIBUTION,
                availableMm = availableMm,
                objectMm = objectMm,
                objectsFit = objectCount,
                internalGapCount = objectCount - 1,
                edgeGapCount = edgeCount,
                gapMm = BigDecimal.ZERO,
                edgeGapMm = BigDecimal.ZERO,
                objectSpaceMm = objectSpace,
                gapSpaceMm = BigDecimal.ZERO,
                edgeSpaceMm = BigDecimal.ZERO,
                usedSpaceMm = objectSpace,
                remainingSpaceMm = BigDecimal.ZERO,
                equalGapMm = null,
                completeFit = false,
                doesNotFit = true,
                requiredSpaceMm = objectSpace,
                shortByMm = remaining.negate(),
            )
        }

        val equalGap = calculateEqualGap(remaining, objectCount, edgeGapMode)
        val gap = equalGap ?: BigDecimal.ZERO
        val edge = if (edgeGapMode == EdgeGapMode.EQUAL) gap else BigDecimal.ZERO
        val gapSpace = BigDecimal(objectCount - 1).multiply(gap)
        val edgeSpace = edge.multiply(TWO)
        val used = objectSpace.add(gapSpace).add(edgeSpace)
        return SpacePlanResult(
            mode = SpacePlanMode.EVEN_DISTRIBUTION,
            availableMm = availableMm,
            objectMm = objectMm,
            objectsFit = objectCount,
            internalGapCount = objectCount - 1,
            edgeGapCount = edgeCount,
            gapMm = gap,
            edgeGapMm = edge,
            objectSpaceMm = objectSpace,
            gapSpaceMm = gapSpace,
            edgeSpaceMm = edgeSpace,
            usedSpaceMm = used,
            remainingSpaceMm = remaining,
            equalGapMm = equalGap,
            completeFit = remaining.signum() == 0,
        )
    }

    /** N*object + (N-1)*gap + 2*edge, or 0 when no objects are placed. */
    fun calculateUsedSpace(
        count: Int,
        objectMm: BigDecimal,
        gapMm: BigDecimal,
        edgeGapMm: BigDecimal = BigDecimal.ZERO,
    ): BigDecimal {
        if (count <= 0) return BigDecimal.ZERO
        return BigDecimal(count).multiply(objectMm)
            .add(BigDecimal(count - 1).multiply(gapMm))
            .add(edgeGapMm.multiply(TWO))
    }

    /** available - used, snapped to exactly zero when within [TOLERANCE_MM]. */
    fun calculateRemainingSpace(availableMm: BigDecimal, usedMm: BigDecimal): BigDecimal {
        val remaining = availableMm.subtract(usedMm)
        return if (remaining.abs() < TOLERANCE_MM) BigDecimal.ZERO else remaining
    }

    /**
     * Equal gap for distributing [remainingMm] around [count] objects.
     * Returns null when there is no gap to size (a single object without edge gaps)
     * or when the remaining space is negative.
     */
    fun calculateEqualGap(remainingMm: BigDecimal, count: Int, edgeGapMode: EdgeGapMode): BigDecimal? {
        if (remainingMm.signum() < 0 || count < 1) return null
        val divisions = when (edgeGapMode) {
            EdgeGapMode.NONE -> count - 1
            EdgeGapMode.EQUAL -> count + 1
        }
        if (divisions <= 0) return null
        return remainingMm.divide(BigDecimal(divisions), MATH)
    }

    /** Share of the available space that is used, in percent (0-100). */
    fun calculateFitPercentage(usedMm: BigDecimal, availableMm: BigDecimal): BigDecimal {
        if (availableMm.signum() <= 0) return BigDecimal.ZERO
        return usedMm.multiply(HUNDRED).divide(availableMm, MATH).coerceIn(BigDecimal.ZERO, HUNDRED)
    }

    /** Share of the available space left unused, in percent (0-100). */
    fun calculateUnusedPercentage(remainingMm: BigDecimal, availableMm: BigDecimal): BigDecimal {
        if (availableMm.signum() <= 0) return BigDecimal.ZERO
        return remainingMm.multiply(HUNDRED).divide(availableMm, MATH).coerceIn(BigDecimal.ZERO, HUNDRED)
    }

    /** Start offset of each placed object measured from the start of the available space. */
    fun calculatePositions(result: SpacePlanResult): List<BigDecimal> {
        if (result.doesNotFit || result.objectsFit <= 0) return emptyList()
        val step = result.objectMm.add(result.gapMm)
        return List(minOf(result.objectsFit, MAX_OBJECT_COUNT)) { index ->
            result.edgeGapMm.add(step.multiply(BigDecimal(index)))
        }
    }

    private fun fits(
        count: Int,
        objectMm: BigDecimal,
        gapMm: BigDecimal,
        edgeMm: BigDecimal,
        availableMm: BigDecimal,
    ): Boolean = calculateUsedSpace(count, objectMm, gapMm, edgeMm) <= availableMm.add(TOLERANCE_MM)

    private fun requirePositive(value: BigDecimal, message: String) {
        if (value.signum() <= 0) throw SpacePlanException(message)
    }

    private fun requireNotNegative(value: BigDecimal, message: String) {
        if (value.signum() < 0) throw SpacePlanException(message)
    }
}
