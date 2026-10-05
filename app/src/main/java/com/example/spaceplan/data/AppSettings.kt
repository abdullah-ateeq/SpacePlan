package com.example.spaceplan.data

import com.example.spaceplan.domain.MeasurementUnit
import com.example.spaceplan.domain.Orientation
import com.example.spaceplan.domain.SpacePlanMode

enum class ThemeMode(val label: String) {
    LIGHT("Light"),
    DARK("Dark"),
    SYSTEM("System"),
}

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = false,
    val defaultMode: SpacePlanMode = SpacePlanMode.FIT_COUNT,
    val defaultUnit: MeasurementUnit = MeasurementUnit.CENTIMETER,
    val defaultAvailable: String = "100",
    val defaultObjectSize: String = "20",
    val defaultGap: String = "0",
    val defaultObjectCount: Int = 4,
    val orientation: Orientation = Orientation.HORIZONTAL,
    val decimalPlaces: Int = 2,
    val showFormula: Boolean = true,
    val showBreakdown: Boolean = true,
    val showVisualization: Boolean = true,
    val hapticFeedback: Boolean = true,
)
