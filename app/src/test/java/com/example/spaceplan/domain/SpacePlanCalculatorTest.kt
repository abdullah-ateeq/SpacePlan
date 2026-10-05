package com.example.spaceplan.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class SpacePlanCalculatorTest {

    private fun cm(value: String) = BigDecimal(value).toMillimeters(MeasurementUnit.CENTIMETER)
    private fun assertMm(expected: String, actual: BigDecimal?) {
        requireNotNull(actual)
        assertEquals("expected $expected but was ${actual.toPlainString()}", 0, BigDecimal(expected).compareTo(actual))
    }

    private fun fit(available: String, obj: String, gap: String, edge: String? = null) =
        SpacePlanCalculator.calculateFitCount(
            availableMm = cm(available),
            objectMm = cm(obj),
            gapMm = cm(gap),
            edgeGapMode = if (edge == null) EdgeGapMode.NONE else EdgeGapMode.EQUAL,
            edgeGapMm = if (edge == null) BigDecimal.ZERO else cm(edge),
        )

    // 1. Exact fit
    @Test
    fun exactFit_100cm_20cm_noGap() {
        val r = fit("100", "20", "0")
        assertEquals(5, r.objectsFit)
        assertMm("0", r.remainingSpaceMm)
        assertTrue(r.completeFit)
    }

    // 2. Partial fit
    @Test
    fun partialFit_120cm_25cm_5cmGap() {
        val r = fit("120", "25", "5")
        assertEquals(4, r.objectsFit)
        assertMm("1150", r.usedSpaceMm)
        assertMm("50", r.remainingSpaceMm)
        assertFalse(r.completeFit)
    }

    // 3. Zero gap
    @Test
    fun zeroGap_hasNoGapSpace() {
        val r = fit("100", "18", "0")
        assertEquals(5, r.objectsFit)
        assertMm("0", r.gapSpaceMm)
        assertMm("100", r.remainingSpaceMm)
    }

    // 4. Non-zero gap uses N - 1 gaps
    @Test
    fun nonZeroGap_100cm_18cm_2cmGap() {
        val r = fit("100", "18", "2")
        assertEquals(5, r.objectsFit)
        assertEquals(4, r.internalGapCount)
        assertMm("900", r.objectSpaceMm)
        assertMm("80", r.gapSpaceMm)
        assertMm("980", r.usedSpaceMm)
        assertMm("20", r.remainingSpaceMm)
    }

    // 5. One object
    @Test
    fun oneObject_hasNoInternalGaps() {
        val r = fit("30", "20", "15")
        assertEquals(1, r.objectsFit)
        assertEquals(0, r.internalGapCount)
        assertMm("0", r.gapSpaceMm)
        assertMm("200", r.usedSpaceMm)
        assertMm("100", r.remainingSpaceMm)
    }

    // 6. No object fits
    @Test
    fun noFit_10cm_20cm() {
        val r = fit("10", "20", "0")
        assertEquals(0, r.objectsFit)
        assertTrue(r.doesNotFit)
        assertMm("200", r.requiredSpaceMm)
        assertMm("100", r.shortByMm)
    }

    // 7. Edge gaps
    @Test
    fun edgeGaps_100cm_18cm_2cmGap_2cmEdges() {
        val r = fit("100", "18", "2", edge = "2")
        assertEquals(4, r.objectsFit)
        assertEquals(2, r.edgeGapCount)
        assertMm("720", r.objectSpaceMm)
        assertMm("60", r.gapSpaceMm)
        assertMm("40", r.edgeSpaceMm)
        assertMm("820", r.usedSpaceMm)
        assertMm("180", r.remainingSpaceMm)
    }

    @Test
    fun edgeGaps_noFitIncludesEdgesInRequiredSpace() {
        val r = fit("20", "18", "0", edge = "2")
        assertTrue(r.doesNotFit)
        assertMm("220", r.requiredSpaceMm)
        assertMm("20", r.shortByMm)
    }

    @Test(expected = SpacePlanException::class)
    fun edgeGaps_consumingAllSpace_isRejected() {
        fit("10", "1", "0", edge = "5")
    }

    // 8. Even distribution
    @Test
    fun evenDistribution_noEdgeGaps() {
        val r = SpacePlanCalculator.calculateEvenDistribution(cm("100"), cm("20"), 4, EdgeGapMode.NONE)
        assertMm("200", r.remainingSpaceMm)
        assertEquals(3, r.internalGapCount)
        val gap = requireNotNull(r.equalGapMm)
        assertEquals("66.67", gap.fromMillimeters(MeasurementUnit.MILLIMETER).setScale(2, java.math.RoundingMode.HALF_UP).toPlainString())
        assertEquals("6.67 cm", formatMeasurement(gap, MeasurementUnit.CENTIMETER, 2))
    }

    @Test
    fun evenDistribution_120cm_4objects_25cm() {
        val r = SpacePlanCalculator.calculateEvenDistribution(cm("120"), cm("25"), 4, EdgeGapMode.NONE)
        assertEquals("6.67 cm", formatMeasurement(r.equalGapMm!!, MeasurementUnit.CENTIMETER, 2))
    }

    // 9. Even distribution with edge gaps
    @Test
    fun evenDistribution_equalEdgeGaps() {
        val r = SpacePlanCalculator.calculateEvenDistribution(cm("100"), cm("20"), 4, EdgeGapMode.EQUAL)
        assertMm("200", r.remainingSpaceMm)
        assertEquals(3, r.internalGapCount)
        assertEquals(2, r.edgeGapCount)
        assertMm("40", r.equalGapMm)
        assertMm("40", r.edgeGapMm)
        assertMm("1000", r.usedSpaceMm)
    }

    @Test
    fun evenDistribution_oneObject_noInternalSpacing() {
        val r = SpacePlanCalculator.calculateEvenDistribution(cm("100"), cm("20"), 1, EdgeGapMode.NONE)
        assertNull(r.equalGapMm)
        assertEquals(0, r.internalGapCount)
        assertMm("800", r.remainingSpaceMm)
    }

    @Test
    fun evenDistribution_doesNotFit_neverNegativeGap() {
        val r = SpacePlanCalculator.calculateEvenDistribution(cm("50"), cm("20"), 3, EdgeGapMode.NONE)
        assertTrue(r.doesNotFit)
        assertNull(r.equalGapMm)
        assertMm("600", r.requiredSpaceMm)
        assertMm("100", r.shortByMm)
    }

    // 10. Remaining space
    @Test
    fun remainingSpace_snapsTinyValuesToZero() {
        assertMm("0", SpacePlanCalculator.calculateRemainingSpace(BigDecimal("100"), BigDecimal("99.9999999999")))
        assertMm("2", SpacePlanCalculator.calculateRemainingSpace(BigDecimal("100"), BigDecimal("98")))
    }

    // 11. Used space
    @Test
    fun usedSpace_formula() {
        assertMm("98", SpacePlanCalculator.calculateUsedSpace(5, BigDecimal("18"), BigDecimal("2")))
        assertMm("102", SpacePlanCalculator.calculateUsedSpace(5, BigDecimal("18"), BigDecimal("2"), BigDecimal("2")))
        assertMm("0", SpacePlanCalculator.calculateUsedSpace(0, BigDecimal("18"), BigDecimal("2")))
    }

    // 12. Fit percentage
    @Test
    fun fitPercentage() {
        assertMm("98", SpacePlanCalculator.calculateFitPercentage(BigDecimal("98"), BigDecimal("100")))
        assertMm("2", SpacePlanCalculator.calculateUnusedPercentage(BigDecimal("2"), BigDecimal("100")))
    }

    // 19. Decimal values
    @Test
    fun decimalValues() {
        val r = SpacePlanCalculator.calculateFitCount(
            BigDecimal("1000.5"), BigDecimal("33.3"), BigDecimal("0.7"), EdgeGapMode.NONE, BigDecimal.ZERO,
        )
        // (1000.5 + 0.7) / 34 = 29.44 -> 29 objects: 29*33.3 + 28*0.7 = 965.7 + 19.6 = 985.3
        assertEquals(29, r.objectsFit)
        assertMm("985.3", r.usedSpaceMm)
        assertMm("15.2", r.remainingSpaceMm)
    }

    @Test
    fun boundary_exactFitWithGap() {
        // 3 objects of 30 with 5 gaps = 100 exactly.
        val r = SpacePlanCalculator.calculateFitCount(BigDecimal("100"), BigDecimal("30"), BigDecimal("5"), EdgeGapMode.NONE, BigDecimal.ZERO)
        assertEquals(3, r.objectsFit)
        assertTrue(r.completeFit)
    }

    @Test
    fun extremeValues_doNotCrash() {
        val r = SpacePlanCalculator.calculateFitCount(BigDecimal("1000000"), BigDecimal("1.3"), BigDecimal.ZERO, EdgeGapMode.NONE, BigDecimal.ZERO)
        assertEquals(769230, r.objectsFit)
    }

    @Test(expected = SpacePlanException::class)
    fun extremeValues_tooManyObjectsIsReported() {
        SpacePlanCalculator.calculateFitCount(BigDecimal("1000000000"), BigDecimal("0.001"), BigDecimal.ZERO, EdgeGapMode.NONE, BigDecimal.ZERO)
    }

    // 20. Zero object size
    @Test(expected = SpacePlanException::class)
    fun zeroObjectSize_isRejected() {
        fit("100", "0", "0")
    }

    // 21. Negative object size
    @Test(expected = SpacePlanException::class)
    fun negativeObjectSize_isRejected() {
        fit("100", "-5", "0")
    }

    // 22. Zero available space
    @Test(expected = SpacePlanException::class)
    fun zeroAvailableSpace_isRejected() {
        fit("0", "5", "0")
    }

    // 23. Negative gap
    @Test(expected = SpacePlanException::class)
    fun negativeGap_isRejected() {
        fit("100", "5", "-1")
    }

    @Test
    fun negativeGap_validationMessage() {
        assertEquals("Gap cannot be negative.", InputValidator.validateGap(BigDecimal("-1")))
    }

    // 24. Object count validation
    @Test
    fun objectCountValidation() {
        assertNull(InputValidator.validateCount(1))
        assertNull(InputValidator.validateCount(1000))
        assertTrue(InputValidator.validateCount(0) != null)
        assertTrue(InputValidator.validateCount(1001) != null)
        assertTrue(InputValidator.validateCount(null) != null)
        assertNull(NumberParser.parseCount("4.5"))
    }

    @Test(expected = SpacePlanException::class)
    fun objectCountOutOfRange_isRejectedByEngine() {
        SpacePlanCalculator.calculateEvenDistribution(cm("100"), cm("1"), 1001, EdgeGapMode.NONE)
    }

    @Test
    fun positions_evenDistribution() {
        val r = SpacePlanCalculator.calculateEvenDistribution(cm("100"), cm("20"), 4, EdgeGapMode.EQUAL)
        val p = SpacePlanCalculator.calculatePositions(r)
        assertEquals(4, p.size)
        assertMm("40", p[0])
        assertMm("280", p[1])
        assertMm("760", p[3])
    }
}
