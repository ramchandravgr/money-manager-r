package com.mmr.feature.stats.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import com.mmr.app.ui.components.MmrDimens
import com.mmr.app.ui.theme.MmrMmColors
import com.mmr.feature.stats.model.CategoryBreakdownUi
import kotlin.math.min

@Composable
fun StatsPieChart(
    breakdowns: List<CategoryBreakdownUi>,
    modifier: Modifier = Modifier,
) {
    if (breakdowns.isEmpty()) return
    val rawSegments =
        breakdowns.map {
            PieSegment(
                color = parseHexColor(it.colorHex),
                fraction = it.percentage.coerceAtLeast(0f),
            )
        }
    val fractionSum = rawSegments.sumOf { it.fraction.toDouble() }.toFloat()
    val segments =
        if (fractionSum > 0f && fractionSum < 1f - 0.0001f) {
            rawSegments.map { it.copy(fraction = it.fraction / fractionSum) }
        } else {
            rawSegments
        }
    Canvas(
        modifier =
            modifier
                .fillMaxWidth()
                .height(MmrDimens.statsPieHeight),
    ) {
        val diameter = min(size.width, size.height)
        val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
        val arcSize = Size(diameter, diameter)
        val center = Offset(topLeft.x + diameter / 2f, topLeft.y + diameter / 2f)
        var startAngle = -90f
        segments.forEach { segment ->
            val sweep = segment.fraction * 360f
            if (sweep <= 0f) return@forEach
            drawArc(
                color = segment.color,
                startAngle = startAngle,
                sweepAngle = sweep,
                useCenter = true,
                topLeft = topLeft,
                size = arcSize,
            )
            startAngle += sweep
        }
        val innerRadius = diameter * 0.45f / 2f
        drawCircle(
            color = MmrMmColors.ListBackground,
            radius = innerRadius,
            center = center,
        )
    }
}

private data class PieSegment(
    val color: Color,
    val fraction: Float,
)

internal fun parseHexColor(hex: String): Color =
    try {
        val clean = hex.removePrefix("#")
        val colorInt =
            when (clean.length) {
                6 -> (0xFF000000 or clean.toLong(16)).toInt()
                8 -> clean.toLong(16).toInt()
                else -> 0xFF3B82F6.toInt()
            }
        Color(colorInt)
    } catch (_: Exception) {
        Color(0xFF3B82F6)
    }
