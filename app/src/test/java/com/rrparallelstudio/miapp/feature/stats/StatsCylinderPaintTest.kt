package com.rrparallelstudio.miapp.feature.stats

import androidx.compose.ui.graphics.Color
import com.rrparallelstudio.miapp.feature.stats.model.CategoryBreakdownUi
import com.rrparallelstudio.miapp.feature.stats.ui.StatsCylinderPaint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class StatsCylinderPaintTest {
    @Test
    fun duplicateHex_segmentsAreSeparatedByHue() {
        val rows =
            listOf(
                row(1L, "Food", "#EF4444"),
                row(2L, "Rent", "#EF4444"),
                row(3L, "Fuel", "#EF4444"),
            )
        val colors = StatsCylinderPaint.resolve(rows, Color(0xFFFF6255))
        val hues = colors.values.map { hueOf(it) }
        assertEquals(3, hues.distinctBy { it.toInt() }.size)
        for (i in hues.indices) {
            for (j in i + 1 until hues.size) {
                assertTrue(hueDistance(hues[i], hues[j]) >= 20f)
            }
        }
    }

    @Test
    fun generatedColorsStayInMidLuminanceAndOffTemplateAccents() {
        val rows =
            (1L..6L).map { id ->
                row(id, "Cat$id", "#3B82F6")
            }
        val colors = StatsCylinderPaint.resolve(rows, Color(0xFF6366F1)).values.drop(1)
        val templateHues =
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
            ).map { hueOf(Color(android.graphics.Color.parseColor(it))) }
        colors.forEach { color ->
            val lum = StatsCylinderPaint.relativeLuminance(color)
            assertTrue("luminance $lum", lum in 0.22f..0.62f)
            val hue = hueOf(color)
            templateHues.forEach { template ->
                assertTrue(
                    "hue $hue near template $template",
                    hueDistance(hue, template) >= 8f,
                )
            }
        }
    }

    @Test
    fun uniqueColorsKeepTheirHue() {
        val colors =
            StatsCylinderPaint.resolve(
                listOf(
                    row(1L, "Food", "#EF4444"),
                    row(2L, "Travel", "#10B981"),
                ),
                Color(0xFF6366F1),
            )
        assertTrue(hueDistance(hueOf(colors.getValue("1")), hueOf(Color(0xFFEF4444))) < 8f)
        assertTrue(hueDistance(hueOf(colors.getValue("2")), hueOf(Color(0xFF10B981))) < 8f)
    }

    private fun row(
        id: Long,
        name: String,
        hex: String,
    ) = CategoryBreakdownUi(
        categoryId = id,
        categoryName = name,
        colorHex = hex,
        amount = 10.0,
        amountFormatted = "10",
        percentage = 0.1f,
        percentageFormatted = "10%",
        transactionCount = 1,
    )

    private fun hueOf(color: Color): Float {
        val r = color.red
        val g = color.green
        val b = color.blue
        val max = max(r, max(g, b))
        val min = min(r, min(g, b))
        if (max == min) return 0f
        val d = max - min
        val h =
            when (max) {
                r -> ((g - b) / d + if (g < b) 6f else 0f)
                g -> ((b - r) / d + 2f)
                else -> ((r - g) / d + 4f)
            } * 60f
        return h
    }

    private fun hueDistance(
        a: Float,
        b: Float,
    ): Float {
        val delta = abs(a - b) % 360f
        return min(delta, 360f - delta)
    }
}
