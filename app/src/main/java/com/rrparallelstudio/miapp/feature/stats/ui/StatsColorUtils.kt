package com.rrparallelstudio.miapp.feature.stats.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.rrparallelstudio.miapp.ui.theme.MmrBrandColors

internal fun parseHexColor(hex: String): Color =
    try {
        val clean = hex.removePrefix("#")
        val colorInt =
            when (clean.length) {
                6 -> (0xFF000000 or clean.toLong(16)).toInt()
                8 -> clean.toLong(16).toInt()
                else -> MmrBrandColors.ChartPieFallback.toArgb()
            }
        Color(colorInt)
    } catch (_: Exception) {
        MmrBrandColors.ChartPieFallback
    }
