package com.mmr.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val base = Typography()

/** System sans-serif, slightly smaller sizes, normal weight — compact list density. */
val Typography: Typography =
    Typography(
        displayLarge = base.displayLarge.withMmrFont().copy(fontSize = 48.sp, fontWeight = FontWeight.Normal),
        displayMedium = base.displayMedium.withMmrFont().copy(fontSize = 40.sp, fontWeight = FontWeight.Normal),
        displaySmall = base.displaySmall.withMmrFont().copy(fontSize = 32.sp, fontWeight = FontWeight.Normal),
        headlineLarge = base.headlineLarge.withMmrFont().copy(fontSize = 26.sp, fontWeight = FontWeight.Normal),
        headlineMedium = base.headlineMedium.withMmrFont().copy(fontSize = 22.sp, fontWeight = FontWeight.Normal),
        headlineSmall = base.headlineSmall.withMmrFont().copy(fontSize = 18.sp, fontWeight = FontWeight.Normal),
        titleLarge =
            base.titleLarge.withMmrFont().copy(
                fontSize = 17.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.Normal,
            ),
        titleMedium =
            base.titleMedium.withMmrFont().copy(
                fontSize = 15.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Normal,
            ),
        titleSmall =
            base.titleSmall.withMmrFont().copy(
                fontSize = 13.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Normal,
            ),
        bodyLarge = base.bodyLarge.withMmrFont().copy(fontSize = 15.sp, lineHeight = 21.sp),
        bodyMedium = base.bodyMedium.withMmrFont().copy(fontSize = 13.sp, lineHeight = 18.sp),
        bodySmall = base.bodySmall.withMmrFont().copy(fontSize = 11.sp, lineHeight = 15.sp),
        labelLarge =
            base.labelLarge.withMmrFont().copy(
                fontSize = 13.sp,
                lineHeight = 17.sp,
                fontWeight = FontWeight.Normal,
            ),
        labelMedium = base.labelMedium.withMmrFont().copy(fontSize = 11.sp, lineHeight = 15.sp),
        labelSmall = base.labelSmall.withMmrFont().copy(fontSize = 10.sp, lineHeight = 14.sp),
    )

private fun TextStyle.withMmrFont(): TextStyle = copy(fontFamily = MmrFontFamily)
