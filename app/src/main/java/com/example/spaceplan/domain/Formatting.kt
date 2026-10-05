package com.example.spaceplan.domain

import java.math.BigDecimal
import java.math.RoundingMode

object MeasurementFormatter {
    const val DEFAULT_DECIMALS = 2

    /**
     * Rounds only for display. Whole values drop their decimals ("100"),
     * others keep the configured precision ("6.67", "2.50").
     */
    fun formatNumber(value: BigDecimal, decimals: Int): String {
        val rounded = value.setScale(decimals.coerceIn(0, 6), RoundingMode.HALF_UP)
        if (rounded.signum() == 0) return "0"
        return if (rounded.stripTrailingZeros().scale() <= 0) {
            rounded.setScale(0, RoundingMode.HALF_UP).toPlainString()
        } else {
            rounded.toPlainString()
        }
    }

    fun formatMeasurement(millimeters: BigDecimal, unit: MeasurementUnit, decimals: Int): String =
        "${formatNumber(millimeters.fromMillimeters(unit), decimals)} ${unit.symbol}"

    /** Spoken form for accessibility, e.g. "6.67 centimeters". */
    fun spokenMeasurement(millimeters: BigDecimal, unit: MeasurementUnit, decimals: Int): String =
        "${formatNumber(millimeters.fromMillimeters(unit), decimals)} ${unit.spokenName}"

    fun formatPercent(percent: BigDecimal, decimals: Int = 1): String = "${formatNumber(percent, decimals)}%"

    /**
     * Text placed back into an input field after a unit change. Keeps up to 4 decimals
     * and at least [minDecimals] for non-whole values, e.g. 100 cm -> "1.00" m.
     */
    fun formatInputValue(millimeters: BigDecimal, unit: MeasurementUnit, minDecimals: Int): String {
        val value = millimeters.fromMillimeters(unit).setScale(4, RoundingMode.HALF_UP).stripTrailingZeros()
        if (value.signum() == 0) return "0"
        val scaled = if (value.scale() < minDecimals) value.setScale(minDecimals) else value
        return scaled.toPlainString()
    }
}

fun formatMeasurement(millimeters: BigDecimal, unit: MeasurementUnit, decimals: Int): String =
    MeasurementFormatter.formatMeasurement(millimeters, unit, decimals)
