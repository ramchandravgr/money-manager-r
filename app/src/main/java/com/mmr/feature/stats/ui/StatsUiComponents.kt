package com.mmr.feature.stats.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mmr.app.ui.adaptive.mmrScreenHorizontalPadding
import com.mmr.app.ui.components.MmrDimens
import com.mmr.app.ui.theme.MmrBrandColors
import com.mmr.feature.stats.model.CategoryBreakdownUi
import com.mmr.feature.stats.model.StatsSiblingCategoryUi
import com.mmr.feature.stats.model.SubcategoryBreakdownUi

@Composable
fun StatsPieChartSection(
    breakdowns: List<CategoryBreakdownUi>,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        StatsWebPieChart(breakdowns = breakdowns)
        HorizontalDivider(
            color = MmrBrandColors.TransactionListBorder,
            thickness = MmrDimens.transactionListDividerThickness,
        )
        Spacer(Modifier.height(MmrDimens.spaceXs))
    }
}

@Composable
fun StatsCategoryList(
    breakdowns: List<CategoryBreakdownUi>,
    onCategoryClick: (CategoryBreakdownUi) -> Unit,
    showNativeAdPlaceholder: Boolean,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = mmrScreenHorizontalPadding()),
    headerContent: (@Composable () -> Unit)? = null,
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = contentPadding,
    ) {
        if (headerContent != null) {
            item(key = "stats_pie_header") {
                headerContent()
            }
        }
        items(
            count = breakdowns.size,
            key = { index ->
                val item = breakdowns[index]
                item.categoryId ?: item.categoryName.hashCode().toLong()
            },
        ) { index ->
            if (showNativeAdPlaceholder && index == 3) {
                StatsNativeAdPlaceholder()
            }
            val item = breakdowns[index]
            StatsMmCategoryRow(
                breakdown = item,
                onClick = { onCategoryClick(item) },
            )
            HorizontalDivider(
                color = MmrBrandColors.TransactionListBorder,
                thickness = MmrDimens.transactionListDividerThickness,
            )
        }
    }
}

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
fun StatsMmCategoryRow(
    breakdown: CategoryBreakdownUi,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val categoryColor = parseHexColor(breakdown.colorHex)
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = MmrDimens.statsCategoryRowHeight)
                .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .padding(
                        start = MmrDimens.statsCategoryRowStartMargin,
                        end = MmrDimens.statsCategoryPercentEndMargin,
                    ).widthIn(min = MmrDimens.statsPercentPillMinWidth)
                    .defaultMinSize(minHeight = MmrDimens.statsPercentPillMinHeight)
                    .clip(RoundedCornerShape(MmrDimens.statsPercentPillRadius))
                    .background(categoryColor)
                    .padding(
                        horizontal = MmrDimens.statsPercentPillPaddingHorizontal,
                        vertical = MmrDimens.statsPercentPillPaddingVertical,
                    ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = breakdown.percentageFormatted,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
            )
        }
        Text(
            text = breakdown.categoryName,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = breakdown.amountFormatted,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(end = MmrDimens.statsCategoryAmountEndMargin),
        )
    }
}

@Composable
fun StatsDetailTotalBlock(
    amountFormatted: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = MmrDimens.statsDetailTotalMinHeight)
                .background(MmrBrandColors.SurfaceWhite)
                .padding(start = MmrDimens.statsCategoryRowStartMargin, end = MmrDimens.spaceXl, top = 15.dp),
    ) {
        Text(
            text = "Total Balance",
            fontSize = 12.sp,
            color = MmrBrandColors.TextLight,
        )
        Text(
            text = amountFormatted,
            fontSize = 24.sp,
            fontWeight = FontWeight.SemiBold,
            color = MmrBrandColors.Active,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 1.dp),
        )
    }
}

@Composable
fun StatsMmSubcategoryRow(
    row: SubcategoryBreakdownUi,
    selected: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bg = if (selected) accentColor.copy(alpha = 0.12f) else Color.Transparent
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(bg)
                    .clickable(onClick = onClick)
                    .padding(
                        horizontal = MmrDimens.statsDetailSubcategoryPaddingHorizontal,
                        vertical = MmrDimens.spaceMd,
                    ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MmrDimens.spaceSm),
        ) {
            Text(
                text = row.name,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = row.percentageFormatted,
                fontSize = 12.sp,
                color = MmrBrandColors.TextMedium,
                modifier = Modifier.widthIn(min = 50.dp),
            )
            Text(
                text = row.amountFormatted,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        HorizontalDivider(
            modifier = Modifier.padding(start = MmrDimens.statsDetailSubcategoryDividerInset),
            color = MmrBrandColors.BorderMain,
            thickness = MmrDimens.statsDetailSubcategoryDividerThickness,
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
