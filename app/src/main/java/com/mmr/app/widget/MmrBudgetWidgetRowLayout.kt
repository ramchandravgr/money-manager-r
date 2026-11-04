package com.mmr.app.widget

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

internal data class BudgetWidgetRowColumnWidths(
    val category: Dp,
    val period: Dp,
    val percent: Dp,
)

/**
 * Option A: fixed period / % columns; category gets remainder; [budgetCategoryToPeriodGap] before first divider.
 * See docs/budget-widget-row-layout-review.md.
 */
internal fun budgetWidgetRowColumnWidths(
    rowInnerWidthDp: Float,
    columnScale: Float = 1f,
): BudgetWidgetRowColumnWidths {
    val scale = columnScale.coerceIn(1f, 1.35f)
    val separatorsDp =
        (budgetWidgetColumnSeparatorWidth + budgetWidgetColumnSeparatorMargin * 2).value * 2f
    val gapDp = budgetCategoryToPeriodGap.value
    val periodW = budgetWidgetPeriodColumnWidth.value * scale
    val percentW = budgetWidgetPercentColumnWidth.value * scale
    val categoryDp =
        (rowInnerWidthDp - gapDp - separatorsDp - periodW - percentW).coerceIn(24f, rowInnerWidthDp)
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

internal val budgetWidgetPeriodColumnWidth = 40.dp
internal val budgetWidgetPercentColumnWidth = 36.dp
internal val budgetCategoryToPeriodGap = 8.dp
internal val budgetWidgetLabelToProgressGap = 8.dp

internal val budgetWidgetColumnSeparatorWidth = 1.dp
internal val budgetWidgetColumnSeparatorMargin = 4.dp
