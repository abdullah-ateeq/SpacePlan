package com.example.spaceplan.domain

import java.math.BigDecimal
import java.math.MathContext

/** Centralized unit conversion. All calculations run in millimeters. */
object UnitConverter {
    val MATH: MathContext = MathContext.DECIMAL128

    fun toMillimeters(value: BigDecimal, unit: MeasurementUnit): BigDecimal =
        value.multiply(unit.millimetersPerUnit)

    fun fromMillimeters(millimeters: BigDecimal, unit: MeasurementUnit): BigDecimal =
        millimeters.divide(unit.millimetersPerUnit, MATH)

    fun convert(value: BigDecimal, from: MeasurementUnit, to: MeasurementUnit): BigDecimal =
        if (from == to) value else fromMillimeters(toMillimeters(value, from), to)
}

fun BigDecimal.toMillimeters(unit: MeasurementUnit): BigDecimal = UnitConverter.toMillimeters(this, unit)

fun BigDecimal.fromMillimeters(unit: MeasurementUnit): BigDecimal = UnitConverter.fromMillimeters(this, unit)
