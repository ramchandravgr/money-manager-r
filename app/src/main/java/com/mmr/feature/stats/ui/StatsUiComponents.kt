package com.mmr.feature.stats.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mmr.app.ui.components.MmrDimens
import com.mmr.app.ui.theme.MmrBrandColors
import com.mmr.feature.stats.model.StatsSiblingCategoryUi

@Composable
fun StatsNativeAdPlaceholder(modifier: Modifier = Modifier) {
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(72.dp)
                .padding(vertical = MmrDimens.spaceSm)
                .clip(RoundedCornerShape(6.dp))
                .background(MmrBrandColors.ListBackground),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "Ad",
            fontSize = 11.sp,
            color = MmrBrandColors.TextLight,
        )
    }
}

@Composable
fun StatsCategoryPickerBar(
    categories: List<StatsSiblingCategoryUi>,
    selectedCategoryId: Long,
    onCategorySelected: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (categories.isEmpty()) return
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .background(MmrBrandColors.SurfaceWhite)
                .padding(vertical = MmrDimens.spaceSm, horizontal = MmrDimens.spaceSm),
        horizontalArrangement = Arrangement.spacedBy(MmrDimens.spaceSm),
    ) {
        categories.forEach { item ->
            val selected = item.categoryId == selectedCategoryId
            val bg = if (selected) parseHexColor(item.colorHex) else MmrBrandColors.ListBackground
            val textColor = if (selected) Color.White else MmrBrandColors.TextStrong
            Text(
                text = item.name,
                fontSize = 12.sp,
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier =
                    Modifier
                        .weight(1f, fill = false)
                        .clip(RoundedCornerShape(6.dp))
                        .background(bg)
                        .clickable { onCategorySelected(item.categoryId) }
                        .padding(horizontal = 10.dp, vertical = 8.dp),
            )
        }
    }
}
