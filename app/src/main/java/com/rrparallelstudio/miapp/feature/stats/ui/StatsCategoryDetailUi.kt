package com.rrparallelstudio.miapp.feature.stats.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rrparallelstudio.miapp.feature.home.model.TransactionGroupUi
import com.rrparallelstudio.miapp.feature.stats.model.MonthlyChartPointUi
import com.rrparallelstudio.miapp.feature.stats.model.SubcategoryBreakdownUi
import com.rrparallelstudio.miapp.ui.adaptive.mmrScreenHorizontalPadding
import com.rrparallelstudio.miapp.ui.components.MmrDimens
import com.rrparallelstudio.miapp.ui.theme.MmrBrandColors
import com.rrparallelstudio.miapp.ui.theme.MmrChipPaints
import com.rrparallelstudio.miapp.ui.theme.mmrSettingsRowPrimaryStyle
import com.rrparallelstudio.miapp.ui.theme.mmrStatsChartLabel10Style
import com.rrparallelstudio.miapp.ui.theme.mmrStatsChartLabel11Style
import com.rrparallelstudio.miapp.ui.theme.mmrStatsChartLabel12Style
import com.rrparallelstudio.miapp.ui.theme.mmrStatsHeroAmountStyle
import kotlin.math.max

@Composable
fun StatsCategoryDetailHero(
    categoryName: String,
    categoryColor: Color,
    totalFormatted: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = mmrScreenHorizontalPadding(), vertical = MmrDimens.spaceSm),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier =
                    Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(categoryColor),
            )
            Spacer(Modifier.width(MmrDimens.spaceSm))
            Text(
                text = categoryName,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = "Total Balance",
            style = mmrStatsChartLabel12Style(MmrBrandColors.TextMedium),
            modifier = Modifier.padding(top = MmrDimens.spaceSm),
        )
        Text(
            text = totalFormatted,
            style = mmrStatsHeroAmountStyle(MmrBrandColors.Active).copy(fontWeight = FontWeight.ExtraBold),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun StatsSubcategoryBreakdownCard(
    rows: List<SubcategoryBreakdownUi>,
    accentColor: Color,
    selectedSubcategoryId: Long?,
    onSubcategoryClick: (Long?) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (rows.isEmpty()) return
    val barRows = rows.filter { it.name != "All" }
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = mmrScreenHorizontalPadding())
                .clip(RoundedCornerShape(16.dp))
                .background(MmrBrandColors.SurfaceWhite)
                .padding(MmrDimens.spaceMd),
        verticalArrangement = Arrangement.spacedBy(MmrDimens.spaceSm),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Sub-category Breakdown",
                style =
                    mmrStatsChartLabel12Style(MmrBrandColors.TextStrong).copy(
                        fontWeight = FontWeight.SemiBold,
                    ),
            )
            Text(
                text = "${rows.size} items",
                style = mmrStatsChartLabel10Style(MmrBrandColors.TextMedium),
                modifier =
                    Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(MmrBrandColors.ListBackground)
                        .padding(horizontal = 8.dp, vertical = 2.dp),
            )
        }
        if (barRows.isNotEmpty()) {
            StatsSubcategoryStackedBar(
                rows = barRows,
                accentColor = accentColor,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        rows.forEachIndexed { index, row ->
            val selected = isSubcategoryRowSelected(row, selectedSubcategoryId)
            StatsSubcategoryMiniRow(
                row = row,
                tint = subcategoryTint(accentColor, row, index),
                selected = selected,
                onClick = {
                    onSubcategoryClick(if (row.name == "All") null else row.subcategoryId)
                },
            )
        }
    }
}

@Composable
private fun StatsSubcategoryStackedBar(
    rows: List<SubcategoryBreakdownUi>,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    val total = rows.sumOf { it.amount }
    if (total <= 0.0) return
    Row(
        modifier =
            modifier
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(MmrBrandColors.ListBackground),
    ) {
        rows.forEachIndexed { index, row ->
            val fraction = (row.amount / total).toFloat().coerceAtLeast(0.001f)
            Box(
                modifier =
                    Modifier
                        .weight(fraction)
                        .fillMaxHeight()
                        .background(subcategoryTint(accentColor, row, index)),
            )
        }
    }
}

@Composable
private fun StatsSubcategoryMiniRow(
    row: SubcategoryBreakdownUi,
    tint: Color,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(
                    if (selected) tint.copy(alpha = 0.12f) else MmrBrandColors.ListBackground.copy(alpha = 0.6f),
                ).clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick,
                ).padding(horizontal = MmrDimens.spaceSm, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(tint),
        )
        Spacer(Modifier.width(MmrDimens.spaceSm))
        Text(
            text = row.name,
            style = mmrStatsChartLabel12Style(Color.Unspecified).copy(fontWeight = FontWeight.Medium),
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = row.percentageFormatted,
            style = mmrStatsChartLabel11Style(MmrBrandColors.TextMedium),
        )
        Spacer(Modifier.width(MmrDimens.spaceSm))
        Text(
            text = row.amountFormatted,
            style = mmrStatsChartLabel12Style(Color.Unspecified).copy(fontWeight = FontWeight.Bold),
        )
    }
}

private fun isSubcategoryRowSelected(
    row: SubcategoryBreakdownUi,
    selectedSubcategoryId: Long?,
): Boolean =
    if (row.name == "All") {
        selectedSubcategoryId == null
    } else {
        selectedSubcategoryId == row.subcategoryId
    }

private fun subcategoryTint(
    base: Color,
    row: SubcategoryBreakdownUi,
    index: Int,
): Color {
    val steps = listOf(1f, 0.82f, 0.65f, 0.5f)
    val alpha = steps.getOrElse(index) { 0.4f }
    return base.copy(alpha = alpha.coerceIn(0.35f, 1f))
}

@Composable
fun StatsCategorySpendingBarChart(
    points: List<MonthlyChartPointUi>,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    if (points.isEmpty()) return
    val maxAmount = max(points.maxOf { it.amount }, 1.0)
    val rangeLabel = "${points.first().label} – ${points.last().label}"
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = mmrScreenHorizontalPadding())
                .clip(RoundedCornerShape(16.dp))
                .background(MmrBrandColors.SurfaceWhite)
                .padding(MmrDimens.spaceMd),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Spending Trend",
                style =
                    mmrStatsChartLabel12Style(MmrBrandColors.TextStrong).copy(
                        fontWeight = FontWeight.SemiBold,
                    ),
            )
            Text(
                text = rangeLabel,
                style = mmrStatsChartLabel11Style(MmrBrandColors.TextMedium),
            )
        }
        Spacer(Modifier.height(MmrDimens.spaceMd))
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(176.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            points.forEachIndexed { index, point ->
                val isLast = index == points.lastIndex
                val heightFraction =
                    (point.amount / maxAmount).toFloat().coerceIn(0.08f, 1f)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                ) {
                    Box(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentAlignment = Alignment.BottomCenter,
                    ) {
                        Box(
                            modifier =
                                Modifier
                                    .width(14.dp)
                                    .fillMaxHeight(heightFraction)
                                    .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                    .background(
                                        if (isLast) accentColor else MmrBrandColors.BorderMain.copy(alpha = 0.55f),
                                    ),
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = point.label,
                        style =
                            mmrStatsChartLabel11Style(
                                if (isLast) MmrBrandColors.TextStrong else MmrBrandColors.TextMedium,
                            ).copy(fontWeight = if (isLast) FontWeight.Bold else FontWeight.Medium),
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
fun StatsCategoryTransactionsSectionHeader(
    recordCount: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = mmrScreenHorizontalPadding(), vertical = MmrDimens.spaceSm),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Transactions History",
            style = mmrSettingsRowPrimaryStyle(MmrBrandColors.TextStrong).copy(fontWeight = FontWeight.Bold),
        )
        Text(
            text = "$recordCount Records",
            style = mmrStatsChartLabel12Style(MmrBrandColors.TextMedium),
            modifier =
                Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(MmrBrandColors.SurfaceWhite)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

@Composable
fun StatsCategoryDetailBottomChrome(
    categoryName: String,
    categoryColor: Color,
    siblingCategories: List<com.rrparallelstudio.miapp.feature.stats.model.StatsSiblingCategoryUi>,
    selectedCategoryId: Long,
    onCategorySelected: (Long) -> Unit,
    groupedTransactions: List<TransactionGroupUi>,
    modifier: Modifier = Modifier,
) {
    val stats = remember(groupedTransactions) { transactionQuickStats(groupedTransactions) }
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .background(MmrBrandColors.SurfaceWhite),
    ) {
        if (siblingCategories.isNotEmpty()) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = MmrDimens.spaceSm, vertical = MmrDimens.spaceSm),
                horizontalArrangement = Arrangement.spacedBy(MmrDimens.spaceSm),
            ) {
                siblingCategories.forEach { item ->
                    val selected = item.categoryId == selectedCategoryId
                    val paint = MmrChipPaints.colorFilled(selected, parseHexColor(item.colorHex))
                    val bg = paint.fill
                    val textColor = paint.content
                    Text(
                        text = item.name,
                        style = mmrStatsChartLabel12Style(textColor).copy(fontWeight = FontWeight.SemiBold),
                        maxLines = 1,
                        modifier =
                            Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(bg)
                                .clickable { onCategorySelected(item.categoryId) }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                    )
                }
            }
        }
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MmrDimens.spaceMd, vertical = MmrDimens.spaceSm),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(MmrDimens.spaceSm)) {
                Text(
                    text = "Avg/Tx: ${stats.avgFormatted}",
                    style = mmrStatsChartLabel11Style(MmrBrandColors.TextMedium),
                )
                Text(text = "•", style = mmrStatsChartLabel11Style(MmrBrandColors.TextLight))
                Text(
                    text = "Highest: ${stats.maxFormatted}",
                    style = mmrStatsChartLabel11Style(MmrBrandColors.TextMedium),
                )
            }
            Text(
                text = categoryName,
                style = mmrStatsChartLabel10Style(categoryColor).copy(fontWeight = FontWeight.Bold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier =
                    Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(categoryColor.copy(alpha = 0.12f))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
    }
}

private data class TxQuickStats(
    val avgFormatted: String,
    val maxFormatted: String,
)

private fun transactionQuickStats(groups: List<TransactionGroupUi>): TxQuickStats {
    var count = 0
    var sum = 0.0
    var max = 0.0
    groups.forEach { group ->
        group.items.forEach { row ->
            count++
            val amount = parseAmountLabel(row.amountLabel)
            sum += amount
            max = max(max, amount)
        }
    }
    val avg = if (count > 0) sum / count else 0.0
    return TxQuickStats(
        avgFormatted = formatInrCompact(avg),
        maxFormatted = formatInrCompact(max),
    )
}

private fun parseAmountLabel(label: String): Double {
    val digits = label.filter { it.isDigit() || it == '.' }
    return digits.toDoubleOrNull() ?: 0.0
}

private fun formatInrCompact(value: Double): String {
    val rounded = kotlin.math.round(value).toLong()
    return "₹$rounded"
}
