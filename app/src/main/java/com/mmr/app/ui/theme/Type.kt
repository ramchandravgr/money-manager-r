package com.mmr.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle

private val base = Typography()

val Typography: Typography = Typography(
    displayLarge = base.displayLarge.withMmrFont(),
    displayMedium = base.displayMedium.withMmrFont(),
    displaySmall = base.displaySmall.withMmrFont(),
    headlineLarge = base.headlineLarge.withMmrFont(),
    headlineMedium = base.headlineMedium.withMmrFont(),
    headlineSmall = base.headlineSmall.withMmrFont(),
    titleLarge = base.titleLarge.withMmrFont(),
    titleMedium = base.titleMedium.withMmrFont(),
    titleSmall = base.titleSmall.withMmrFont(),
    bodyLarge = base.bodyLarge.withMmrFont(),
    bodyMedium = base.bodyMedium.withMmrFont(),
    bodySmall = base.bodySmall.withMmrFont(),
    labelLarge = base.labelLarge.withMmrFont(),
    labelMedium = base.labelMedium.withMmrFont(),
    labelSmall = base.labelSmall.withMmrFont()
)

private fun TextStyle.withMmrFont(): TextStyle = copy(fontFamily = MmrFontFamily)
