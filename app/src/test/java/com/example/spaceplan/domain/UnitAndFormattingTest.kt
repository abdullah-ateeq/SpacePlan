package com.example.spaceplan.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.math.BigDecimal

class UnitAndFormattingTest {

    private fun assertSame(expected: String, actual: BigDecimal) =
        assertEquals(0, BigDecimal(expected).compareTo(actual))

    // 13-18. Unit conversion
    @Test fun millimeters() = assertSame("25", BigDecimal("25").toMillimeters(MeasurementUnit.MILLIMETER))
    @Test fun centimeters() = assertSame("1000", BigDecimal("100").toMillimeters(MeasurementUnit.CENTIMETER))
    @Test fun meters() = assertSame("1500", BigDecimal("1.5").toMillimeters(MeasurementUnit.METER))
    @Test fun inches() = assertSame("254", BigDecimal("10").toMillimeters(MeasurementUnit.INCH))
    @Test fun feet() = assertSame("609.6", BigDecimal("2").toMillimeters(MeasurementUnit.FOOT))
    @Test fun yards() = assertSame("914.4", BigDecimal("1").toMillimeters(MeasurementUnit.YARD))

    @Test
    fun roundTripConversion_preservesValue() {
        val mm = BigDecimal("100").toMillimeters(MeasurementUnit.CENTIMETER)
        assertSame("1", mm.fromMillimeters(MeasurementUnit.METER))
        assertSame("100", UnitConverter.convert(BigDecimal("1"), MeasurementUnit.METER, MeasurementUnit.CENTIMETER))
    }

    @Test
    fun unitChangeFormatting_100cmBecomes1_00m() {
        val mm = BigDecimal("1000")
        assertEquals("1.00", MeasurementFormatter.formatInputValue(mm, MeasurementUnit.METER, 2))
        assertEquals("39.3701", MeasurementFormatter.formatInputValue(mm, MeasurementUnit.INCH, 2))
        assertEquals("0", MeasurementFormatter.formatInputValue(BigDecimal.ZERO, MeasurementUnit.METER, 2))
    }

    @Test
    fun formatMeasurement_examples() {
        assertEquals("25 mm", formatMeasurement(BigDecimal("25"), MeasurementUnit.MILLIMETER, 2))
        assertEquals("2.50 cm", formatMeasurement(BigDecimal("25"), MeasurementUnit.CENTIMETER, 2))
        assertEquals("1.25 m", formatMeasurement(BigDecimal("1250"), MeasurementUnit.METER, 2))
        assertEquals("4.92 in", formatMeasurement(BigDecimal("125"), MeasurementUnit.INCH, 2))
        assertEquals("100 cm", formatMeasurement(BigDecimal("1000"), MeasurementUnit.CENTIMETER, 2))
        assertEquals("6.667 cm", formatMeasurement(BigDecimal("66.66666666"), MeasurementUnit.CENTIMETER, 3))
        assertEquals("0 cm", formatMeasurement(BigDecimal("0.00000001"), MeasurementUnit.CENTIMETER, 2))
    }

    @Test
    fun safeParsing() {
        assertSame("12.5", NumberParser.parseDecimal(" 12,5 ")!!)
        assertNull(NumberParser.parseDecimal(""))
        assertNull(NumberParser.parseDecimal("abc"))
        assertNull(NumberParser.parseDecimal("NaN"))
        assertNull(NumberParser.parseDecimal("Infinity"))
        assertNull(NumberParser.parseDecimal("1e5"))
        assertNull(NumberParser.parseDecimal("1.2.3"))
        assertNull(NumberParser.parseDecimal("99999999999999999"))
    }

    @Test
    fun summaryText_fitCount() {
        val r = SpacePlanCalculator.calculateFitCount(
            BigDecimal("1000"), BigDecimal("180"), BigDecimal("20"), EdgeGapMode.NONE, BigDecimal.ZERO,
        )
        val summary = ResultText(MeasurementUnit.CENTIMETER, 2).summary(r)
        org.junit.Assert.assertTrue(summary.contains("Objects fit: 5"))
        org.junit.Assert.assertTrue(summary.contains("Used space: 98 cm"))
        org.junit.Assert.assertTrue(summary.contains("Remaining: 2 cm"))
    }
}
