package com.mmr.feature.stats.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mmr.app.ui.theme.MmrBrandColors
import com.mmr.feature.stats.model.CategoryBreakdownUi

@Composable
internal fun StatsCategoryPillBar(
    breakdowns: List<CategoryBreakdownUi>,
    focusedCategoryKey: String?,
    onSegmentClick: (CategoryBreakdownUi) -> Unit,
    modifier: Modifier = Modifier,
    barWidth: Dp = 40.dp,
) {
    val total = breakdowns.sumOf { it.amount }
    if (total <= 0L) return

    val pillShape = RoundedCornerShape(barWidth / 2)
    Column(
        modifier =
            modifier
                .width(barWidth)
                .fillMaxHeight()
                .clip(pillShape)
                .background(MmrBrandColors.BorderMain.copy(alpha = 0.18f)),
    ) {
        breakdowns.forEach { item ->
            val key = categoryBridgeKey(item)
            val fraction = (item.amount.toDouble() / total.toDouble()).toFloat().coerceAtLeast(0.001f)
            val segmentAlpha =
                when {
                    focusedCategoryKey == null -> 1f
                    focusedCategoryKey == key -> 1f
                    else -> 0.32f
                }
            val color = parseStatsCategoryColor(item.colorHex)
            Box(
                modifier =
                    Modifier
                        .weight(fraction)
                        .fillMaxWidth()
                        .background(color.copy(alpha = segmentAlpha))
                        .clickable { onSegmentClick(item) },
            )
        }
    }
}

internal fun parseStatsCategoryColor(hex: String): Color {
    val normalized =
        hex.trim().let { if (it.startsWith("#")) it else "#$it" }
    return try {
        Color(android.graphics.Color.parseColor(normalized))
    } catch (_: IllegalArgumentException) {
        Color.Gray
    }
}
