package com.mmr.app.widget

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.max

internal data class BudgetWidgetRowColumnWidths(
    val category: Dp,
    val period: Dp,
    val percent: Dp,
)

/** Period and % keep minimum width; category takes the remainder (never `defaultWeight` — Glance clips trailing columns). */
internal fun budgetWidgetRowColumnWidths(
    rowInnerWidthDp: Float,
    bodyFontSizeSp: Float,
    metaFontSizeSp: Float,
): BudgetWidgetRowColumnWidths {
    val separatorsDp =
        (budgetWidgetColumnSeparatorWidth + budgetWidgetColumnSeparatorMargin * 2).value * 2f
    var periodW = budgetPeriodColumnWidth(metaFontSizeSp).value
    var percentW = budgetPercentColumnWidth(bodyFontSizeSp).value
    val sideCap = (rowInnerWidthDp * 0.52f).coerceAtLeast(BUDGET_PERCENT_MIN_DP + 24f)
    val sideTotal = periodW + percentW + separatorsDp
    if (sideTotal > sideCap) {
        val scale = sideCap / sideTotal
        periodW = max(periodW * scale, 36f)
        percentW = max(percentW * scale, BUDGET_PERCENT_MIN_DP)
    }
    val categoryDp =
        (rowInnerWidthDp - periodW - percentW - separatorsDp).coerceIn(24f, rowInnerWidthDp)
    return BudgetWidgetRowColumnWidths(
        category = categoryDp.dp,
        period = periodW.dp,
        percent = percentW.dp,
    )
}

internal fun budgetRowInnerWidthDp(
    widgetWidthDp: Float,
    horizontalPaddingDp: Float,
): Float = (widgetWidthDp - horizontalPaddingDp * 2f).coerceAtLeast(0f)

/** Right column: three digits + `%` (e.g. `100%`). */
internal fun budgetPercentColumnWidth(fontSizeSp: Float): Dp =
    max(fontSizeSp * 2.75f + 10f, BUDGET_PERCENT_MIN_DP).dp

/** Longest label: `Period`. */
internal fun budgetPeriodColumnWidth(fontSizeSp: Float): Dp =
    max(fontSizeSp * 3.75f + 12f, BUDGET_PERIOD_MIN_DP).dp

private const val BUDGET_PERCENT_MIN_DP = 42f
private const val BUDGET_PERIOD_MIN_DP = 50f

internal val budgetWidgetColumnSeparatorWidth = 1.dp
internal val budgetWidgetColumnSeparatorMargin = 4.dp
