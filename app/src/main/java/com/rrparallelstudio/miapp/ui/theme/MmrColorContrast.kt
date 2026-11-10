package com.rrparallelstudio.miapp.ui.theme

import androidx.compose.ui.graphics.Color
import kotlin.math.pow

fun Color.mmrRelativeLuminance(): Float {
    fun channel(c: Float): Float =
        if (c <= 0.03928f) {
            c / 12.92f
        } else {
            ((c + 0.055f) / 1.055f).pow(2.4f)
        }
    return 0.2126f * channel(red) + 0.7152f * channel(green) + 0.0722f * channel(blue)
}

/** Black or white content that stays readable on this fill. */
fun Color.mmrContrastingContent(): Color =
    if (mmrRelativeLuminance() >= 0.45f) {
        Color(0xFF111827)
    } else {
        Color.White
    }
