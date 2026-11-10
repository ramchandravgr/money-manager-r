package com.rrparallelstudio.miapp.feature.stats.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rrparallelstudio.miapp.feature.stats.model.StatsSiblingCategoryUi
import com.rrparallelstudio.miapp.ui.components.MmrDimens
import com.rrparallelstudio.miapp.ui.theme.MmrBrandColors
import com.rrparallelstudio.miapp.ui.theme.MmrChipPaints

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
                .background(MmrBrandColors.expressiveChromeSurface())
                .padding(vertical = MmrDimens.spaceSm, horizontal = MmrDimens.spaceSm),
        horizontalArrangement = Arrangement.spacedBy(MmrDimens.spaceSm),
    ) {
        categories.forEach { item ->
            val selected = item.categoryId == selectedCategoryId
            val paint = MmrChipPaints.colorFilled(selected, parseHexColor(item.colorHex))
            val bg = paint.fill
            val textColor = paint.content
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
