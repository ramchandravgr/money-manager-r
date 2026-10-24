package com.mmr.feature.home.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mmr.app.ui.components.MmrDimens
import com.mmr.app.ui.theme.MmrBrandColors
import com.mmr.feature.home.model.MonthlyPeriodUi
import com.mmr.feature.home.model.WeeklyPeriodUi
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun HomeMonthlyTabContent(
    periods: List<MonthlyPeriodUi>,
    expandedMonths: Set<YearMonth>,
    onMonthToggle: (YearMonth) -> Unit,
    modifier: Modifier = Modifier,
) {
    val today = LocalDate.now()
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding =
            PaddingValues(
                bottom = MmrDimens.fabListBottomInset,
                top = MmrDimens.spaceXs,
            ),
    ) {
        items(periods, key = { it.yearMonth.toString() }) { period ->
            val expanded = period.yearMonth in expandedMonths
            MonthlyPeriodHeaderRow(
                period = period,
                expanded = expanded,
                onClick = { onMonthToggle(period.yearMonth) },
            )
            if (expanded) {
                period.weeks.forEach { week ->
                    val highlight = today >= week.startDate && today <= week.endDate
                    WeeklyPeriodRow(
                        week = week,
                        highlighted = highlight,
                    )
                }
            }
            HorizontalDivider(
                color = MmrBrandColors.BorderMain.copy(alpha = 0.45f),
                thickness = 0.5.dp,
            )
        }
    }
}

@Composable
private fun MonthlyPeriodHeaderRow(
    period: MonthlyPeriodUi,
    expanded: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(
                    horizontal = MmrDimens.listItemPaddingHorizontal,
                    vertical = MmrDimens.spaceMd,
                ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector =
                if (expanded) {
                    Icons.Filled.KeyboardArrowDown
                } else {
                    Icons.AutoMirrored.Filled.KeyboardArrowRight
                },
            contentDescription = null,
            tint = MmrBrandColors.TextMedium,
            modifier = Modifier.padding(end = MmrDimens.spaceXs),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = period.monthLabel,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = period.rangeLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MmrBrandColors.TextLight,
            )
        }
        PeriodAmountColumn(
            income = period.income,
            expense = period.expense,
        )
    }
}

@Composable
private fun WeeklyPeriodRow(
    week: WeeklyPeriodUi,
    highlighted: Boolean,
) {
    val bg = if (highlighted) MmrBrandColors.WeekHighlight else Color.Transparent
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(bg)
                .padding(
                    start = 36.dp,
                    end = MmrDimens.listItemPaddingHorizontal,
                    top = MmrDimens.spaceSm,
                    bottom = MmrDimens.spaceSm,
                ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = week.rangeLabel,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onSurface,
        )
        PeriodAmountColumn(
            income = week.income,
            expense = week.expense,
            compact = true,
        )
    }
}

@Composable
private fun PeriodAmountColumn(
    income: Double,
    expense: Double,
    compact: Boolean = false,
) {
    val net = income - expense
    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        Text(
            text = formatRupeeAmount(income),
            style =
                MaterialTheme.typography.labelSmall.copy(
                    fontSize = if (compact) 11.sp else 12.sp,
                ),
            color = MmrBrandColors.MonthlyIncome,
        )
        Text(
            text = formatRupeeAmount(expense),
            style =
                MaterialTheme.typography.labelSmall.copy(
                    fontSize = if (compact) 11.sp else 12.sp,
                ),
            color = MmrBrandColors.MonthlyExpense,
            fontWeight = FontWeight.Medium,
        )
        Text(
            text = formatSignedRupee(net),
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = MmrBrandColors.TextLight,
        )
    }
}

private fun formatSignedRupee(amount: Double): String {
    val prefix = if (amount >= 0) "" else "-"
    return "$prefix${formatRupeeAmount(kotlin.math.abs(amount))}"
}
