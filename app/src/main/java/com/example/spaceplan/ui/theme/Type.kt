package com.example.spaceplan.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val base = Typography()

val Typography = Typography(
    displayLarge = base.displayLarge.copy(fontWeight = FontWeight.Light, fontFeatureSettings = "tnum"),
    displayMedium = base.displayMedium.copy(fontWeight = FontWeight.Light, fontFeatureSettings = "tnum"),
    displaySmall = base.displaySmall.copy(fontWeight = FontWeight.Normal, fontFeatureSettings = "tnum"),
    headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.Medium, fontFeatureSettings = "tnum"),
    titleLarge = base.titleLarge.copy(fontWeight = FontWeight.SemiBold),
    titleMedium = base.titleMedium.copy(fontWeight = FontWeight.Medium, fontFeatureSettings = "tnum"),
    labelMedium = base.labelMedium.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 1.2.sp),
    labelSmall = base.labelSmall.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp),
)

/** Wide-tracked wordmark style used for the app name. */
val WordmarkStyle = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontWeight = FontWeight.Bold,
    fontSize = 22.sp,
    letterSpacing = 5.sp,
)

/** Monospaced style for formulas. */
val FormulaStyle = TextStyle(
    fontFamily = FontFamily.Monospace,
    fontSize = 13.sp,
    lineHeight = 20.sp,
)
