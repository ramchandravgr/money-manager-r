package com.rrparallelstudio.miapp.feature.stats.ui

import androidx.compose.ui.graphics.Color
import com.rrparallelstudio.miapp.feature.stats.model.CategoryBreakdownUi
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * Cylinder-only colors. Stored category hex is unchanged.
 * Duplicate or muddy slices are repainted into a mid luminance band and kept
 * away from theme-template accent hues so segments stay readable on light,
 * dark, and pure-black canvases.
 */
internal object StatsCylinderPaint {
    private const val MIN_RELATIVE_LUMINANCE = 0.28f
    private const val MAX_RELATIVE_LUMINANCE = 0.52f
    private const val HUE_GAP_DEGREES = 22f
    private const val THEME_HUE_GAP_DEGREES = 18f
    private const val RELAXED_THEME_HUE_GAP_DEGREES = 8f
    private const val LOW_SATURATION = 0.18f
    private const val PAINT_SATURATION = 0.62f
    private const val HUE_STEP_DEGREES = 15f

    /** Accent hues from [com.rrparallelstudio.miapp.ui.theme.MmrThemePresets]. */
    private val themeAccentHexes =
        listOf(
            "#FF6255",
            "#F97316",
            "#D4AF37",
            "#52B788",
            "#2DD4BF",
            "#22D3EE",
            "#38BDF8",
            "#6366F1",
            "#E879F9",
            "#E8B4B8",
        )

    private val themeAccentHues: List<Float> by lazy {
        themeAccentHexes.map { hueOf(parseHex(it)) }
    }

    fun resolve(
        breakdowns: List<CategoryBreakdownUi>,
        themeAccent: Color,
    ): Map<String, Color> {
        val usedHues = mutableListOf<Float>()
        val accentHue = hueOf(themeAccent)
        val assigned = LinkedHashMap<String, Color>()
        breakdowns.forEach { row ->
            val key = categoryBridgeKey(row)
            val stored = parseStatsCategoryColor(row.colorHex)
            val storedHue = hueOf(stored)
            val storedSat = saturationOf(stored)
            val base = if (storedSat < LOW_SATURATION) null else paintHue(storedHue)
            val tooCloseToAssigned =
                base != null && usedHues.any { hueDistance(it, storedHue) < HUE_GAP_DEGREES }
            val color =
                if (base != null && !tooCloseToAssigned) {
                    base
                } else {
                    val start = base?.let { storedHue } ?: (usedHues.lastOrNull() ?: accentHue)
                    val hue =
                        nextFreeHue(
                            startHue = start,
                            usedHues = usedHues,
                            themeGap = THEME_HUE_GAP_DEGREES,
                        )
                            ?: nextFreeHue(
                                startHue = start,
                                usedHues = usedHues,
                                themeGap = RELAXED_THEME_HUE_GAP_DEGREES,
                            )
                            ?: nextFreeHue(
                                startHue = start,
                                usedHues = usedHues,
                                themeGap = 0f,
                            )
                            ?: start
                    paintHue(hue)
                }
            assigned[key] = color
            usedHues += hueOf(color)
        }
        return assigned
    }

    private fun nextFreeHue(
        startHue: Float,
        usedHues: List<Float>,
        themeGap: Float,
    ): Float? {
        var hue = startHue
        repeat(24) {
            hue = (hue + HUE_STEP_DEGREES) % 360f
            val crowded = usedHues.any { hueDistance(it, hue) < HUE_GAP_DEGREES }
            val nearTemplate = themeAccentHues.any { hueDistance(it, hue) < themeGap }
            if (!crowded && !nearTemplate) return hue
        }
        return null
    }

    private fun paintHue(hue: Float): Color {
        val sat = PAINT_SATURATION
        var low = 0.34f
        var high = 0.74f
        var best = Color.hsl(hue, sat, 0.52f)
        repeat(12) {
            val mid = (low + high) / 2f
            val candidate = Color.hsl(hue, sat, mid)
            best = candidate
            val lum = relativeLuminance(candidate)
            when {
                lum < MIN_RELATIVE_LUMINANCE -> low = mid
                lum > MAX_RELATIVE_LUMINANCE -> high = mid
                else -> return candidate
            }
        }
        return best
    }

    private fun parseHex(hex: String): Color {
        val clean = hex.removePrefix("#")
        val argb = clean.toLong(16) or 0xFF000000
        return Color(argb.toInt())
    }

    private fun hueOf(color: Color): Float = hsl(color)[0]

    private fun saturationOf(color: Color): Float = hsl(color)[1]

    private fun hsl(color: Color): FloatArray {
        val r = color.red
        val g = color.green
        val b = color.blue
        val max = max(r, max(g, b))
        val min = min(r, min(g, b))
        val lightness = (max + min) / 2f
        if (max == min) return floatArrayOf(0f, 0f, lightness)
        val delta = max - min
        val saturation =
            if (lightness > 0.5f) {
                delta / (2f - max - min)
            } else {
                delta / (max + min)
            }
        val hue =
            when (max) {
                r -> ((g - b) / delta + if (g < b) 6f else 0f)
                g -> ((b - r) / delta + 2f)
                else -> ((r - g) / delta + 4f)
            } * 60f
        return floatArrayOf(hue, saturation, lightness)
    }

    private fun hueDistance(
        a: Float,
        b: Float,
    ): Float {
        val delta = kotlin.math.abs(a - b) % 360f
        return min(delta, 360f - delta)
    }

    internal fun relativeLuminance(color: Color): Float {
        fun linear(channel: Float): Float =
            if (channel <= 0.04045f) {
                channel / 12.92f
            } else {
                ((channel + 0.055f) / 1.055f).pow(2.4f)
            }
        return 0.2126f * linear(color.red) + 0.7152f * linear(color.green) + 0.0722f * linear(color.blue)
    }
}
