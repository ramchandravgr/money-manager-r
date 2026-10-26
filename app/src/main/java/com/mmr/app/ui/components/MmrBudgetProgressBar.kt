package com.mmr.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.mmr.app.ui.theme.MmrBrandColors

/**
 * Spending vs budget limit. [progressFraction] may exceed 1 when over budget;
 * the trailing segment highlights overflow.
 */
@Composable
fun MmrBudgetProgressBar(
    progressFraction: Float,
    modifier: Modifier = Modifier,
    onTrackColor: Color = MmrBrandColors.Active,
    overflowColor: Color = MmrBrandColors.AmountNegative,
    trackColor: Color = MmrBrandColors.BorderMain,
) {
    val p = progressFraction.coerceAtLeast(0f)
    val (normalWeight, overflowWeight, trackWeight) =
        when {
            p <= 1f -> Triple(p.coerceIn(0f, 1f), 0f, (1f - p).coerceIn(0f, 1f))
            else -> Triple(0.85f, 0.15f, 0f)
        }
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(trackColor),
    ) {
        if (normalWeight > 0f) {
            Box(
                Modifier
                    .weight(normalWeight)
                    .fillMaxHeight()
                    .background(if (p > 1f) overflowColor else onTrackColor),
            )
        }
        if (overflowWeight > 0f) {
            Box(
                Modifier
                    .weight(overflowWeight)
                    .fillMaxHeight()
                    .background(overflowColor.copy(alpha = 0.72f)),
            )
        }
        if (trackWeight > 0f) {
            Box(Modifier.weight(trackWeight).fillMaxHeight())
        }
    }
}
