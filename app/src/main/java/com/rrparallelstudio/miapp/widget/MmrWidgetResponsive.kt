package com.rrparallelstudio.miapp.widget

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.LocalSize
import androidx.glance.appwidget.SizeMode
import kotlin.math.max
import kotlin.math.min

/** Launcher grid sizes: (70 × cells − 30) dp per [Android widget sizing](https://developer.android.com/develop/ui/views/appwidgets/layouts). */
internal object MmrWidgetSizes {
    fun cellDp(cells: Int): Dp = (70 * cells - 30).coerceAtLeast(40).dp

    fun square(cells: Int): DpSize = DpSize(cellDp(cells), cellDp(cells))

    fun rect(
        widthCells: Int,
        heightCells: Int,
    ): DpSize = DpSize(cellDp(widthCells), cellDp(heightCells))

    /** Pending triage default 2×2; user may grow to 5×5. */
    val pendingResponsive: SizeMode.Responsive =
        SizeMode.Responsive(
            setOf(
                square(2),
                square(3),
                square(4),
                square(5),
            ),
        )

    /** Month summary default 2×1; common resize up to 4×3. */
    val monthSpendResponsive: SizeMode.Responsive =
        SizeMode.Responsive(
            buildSet {
                for (widthCells in 2..4) {
                    for (heightCells in 1..3) {
                        add(rect(widthCells, heightCells))
                    }
                }
            },
        )

    /**
     * Budget list default **4×2** home-grid cells (~**320×207 dp** on One UI; see [budgetsDefaultSize]).
     * Still resizes down to 3×1 … up to ~5×6. **At most 16** [DpSize] entries — Android's responsive
     * `RemoteViews` ctor throws `Too many RemoteViews in constructor` above that (Glance `SizeMode.Responsive`).
     */
    val budgetsDefaultSize: DpSize = DpSize(320.dp, 207.dp)

    val budgetsResponsive: SizeMode.Responsive =
        SizeMode.Responsive(
            setOf(
                budgetsDefaultSize,
                rect(3, 1),
                rect(3, 2),
                rect(3, 4),
                rect(4, 2),
                rect(4, 4),
                rect(5, 2),
                rect(5, 4),
                rect(5, 6),
            ),
        )
}

internal data class MmrWidgetTextScale(
    val contentPadding: Dp,
    val gapSmall: Dp,
    val gapMedium: Dp,
    val header: TextUnit,
    val icon: TextUnit,
    val body: TextUnit,
    val amount: TextUnit,
    val subtitle: TextUnit,
    val meta: TextUnit,
    val chip: TextUnit,
    val textAction: TextUnit,
    val chipVerticalPadding: Dp,
    val progressBarHeight: Dp,
) {
    companion object {
        private const val PENDING_BASE = 110f
        private const val MONTH_WIDTH_BASE = 110f
        private const val MONTH_HEIGHT_BASE = 40f
        private const val BUDGET_WIDTH_BASE = 180f

        fun fromPendingSize(size: DpSize): MmrWidgetTextScale {
            val scale =
                min(size.width.value, size.height.value)
                    .div(PENDING_BASE)
                    .coerceIn(1f, 2.35f)
            return scale(
                scale,
                header = 11f,
                icon = 16f,
                body = 13f,
                amount = 15f,
                subtitle = 11f,
                meta = 10f,
                chip = 12f,
                textAction = 10f,
                contentPadding = 8f,
                gapSmall = 2f,
                gapMedium = 6f,
                chipVerticalPadding = 6f,
                progressBarHeight = 0f,
            )
        }

        fun fromMonthSpendSize(size: DpSize): MmrWidgetTextScale {
            val scaleW =
                size.width.value
                    .div(MONTH_WIDTH_BASE)
                    .coerceIn(1f, 2.5f)
            val scaleH =
                size.height.value
                    .div(MONTH_HEIGHT_BASE)
                    .coerceIn(1f, 3f)
            val scale = max(scaleW, scaleH)
            return scale(
                scale,
                header = 14f,
                icon = 11f,
                body = 10f,
                amount = 13f,
                subtitle = 10f,
                meta = 10f,
                chip = 11f,
                textAction = 9f,
                contentPadding = 6f,
                gapSmall = 2f,
                gapMedium = 4f,
                chipVerticalPadding = 0f,
                progressBarHeight = 0f,
            )
        }

        fun fromBudgetsSize(
            size: DpSize,
            rowSlotHeightDp: Float,
        ): MmrWidgetTextScale {
            val scaleW =
                size.width.value
                    .div(BUDGET_WIDTH_BASE)
                    .coerceIn(1f, 1.65f)
            // Typography must fit the label band (slot minus bar + gap), not grow with tall empty rows.
            val labelBandDp =
                rowSlotHeightDp - BUDGET_ROW_PROGRESS_GAP_DP - BUDGET_ROW_PROGRESS_BAR_DP
            val scaleByLabel = (labelBandDp / 22f).coerceIn(0.9f, 1.65f)
            val scale = min(scaleW, scaleByLabel)
            return scale(
                scale,
                header = 12f,
                icon = 11f,
                body = 14f,
                amount = 14f,
                subtitle = 11f,
                meta = 13f,
                chip = 11f,
                textAction = 10f,
                contentPadding = 6f,
                gapSmall = 5f,
                gapMedium = 5f,
                chipVerticalPadding = 0f,
                progressBarHeight = BUDGET_ROW_PROGRESS_BAR_DP,
                budgetsProgressBar = true,
            )
        }

        private fun scale(
            factor: Float,
            header: Float,
            icon: Float,
            body: Float,
            amount: Float,
            subtitle: Float,
            meta: Float,
            chip: Float,
            textAction: Float,
            contentPadding: Float,
            gapSmall: Float,
            gapMedium: Float,
            chipVerticalPadding: Float,
            progressBarHeight: Float,
            budgetsProgressBar: Boolean = false,
        ): MmrWidgetTextScale =
            MmrWidgetTextScale(
                contentPadding = (contentPadding * factor).dp,
                gapSmall = (gapSmall * factor).dp,
                gapMedium = (gapMedium * factor).dp,
                header = (header * factor).sp,
                icon = (icon * factor).sp,
                body = (body * factor).sp,
                amount = (amount * factor).sp,
                subtitle = (subtitle * factor).sp,
                meta = (meta * factor).sp,
                chip = (chip * factor).sp,
                textAction = (textAction * factor).sp,
                chipVerticalPadding = (chipVerticalPadding * factor).dp,
                progressBarHeight =
                    if (budgetsProgressBar) {
                        progressBarHeight.dp
                    } else {
                        (progressBarHeight * factor).coerceAtLeast(3f).dp
                    },
            )
    }
}

/** Label row + [budgetWidgetLabelToProgressGap] + progress bar — minimum per budget row. */
internal const val BUDGET_ROW_PROGRESS_GAP_DP = 8f
internal const val BUDGET_ROW_PROGRESS_BAR_DP = 6f
internal const val BUDGET_ROW_LABEL_MIN_DP = 14f

internal val BUDGET_WIDGET_MIN_ROW_DP: Float =
    BUDGET_ROW_LABEL_MIN_DP + BUDGET_ROW_PROGRESS_GAP_DP + BUDGET_ROW_PROGRESS_BAR_DP

/** Split widget height evenly across rows when they fit; otherwise use scroll-friendly row height. */
internal fun budgetWidgetRowSlotHeight(
    widgetHeightDp: Float,
    contentPaddingDp: Float,
    rowCount: Int,
): Float {
    if (rowCount <= 0) return widgetHeightDp
    val inner = (widgetHeightDp - contentPaddingDp * 2f).coerceAtLeast(20f)
    val even = inner / rowCount
    return if (even >= BUDGET_WIDGET_MIN_ROW_DP) {
        even
    } else {
        BUDGET_WIDGET_MIN_ROW_DP
    }
}

@Composable
internal fun rememberPendingWidgetTextScale(): MmrWidgetTextScale =
    MmrWidgetTextScale.fromPendingSize(LocalSize.current)

@Composable
internal fun rememberMonthSpendWidgetTextScale(): MmrWidgetTextScale =
    MmrWidgetTextScale.fromMonthSpendSize(LocalSize.current)

@Composable
internal fun rememberBudgetsWidgetTextScale(rowCount: Int): MmrWidgetTextScale {
    val size = LocalSize.current
    val basePadding = 5f
    val slot =
        budgetWidgetRowSlotHeight(
            widgetHeightDp = size.height.value,
            contentPaddingDp = basePadding,
            rowCount = rowCount.coerceAtLeast(1),
        )
    return MmrWidgetTextScale.fromBudgetsSize(size, slot)
}

@Composable
internal fun rememberBudgetRowSlotHeight(rowCount: Int): Dp {
    val size = LocalSize.current
    val pad = rememberBudgetsWidgetTextScale(rowCount).contentPadding.value
    return budgetWidgetRowSlotHeight(size.height.value, pad, rowCount.coerceAtLeast(1)).dp
}

/** More preview lines when the user stretches the pending-approvals widget taller. */
@Composable
internal fun rememberPendingSmsPreviewMaxLines(): Int {
    val heightDp = LocalSize.current.height.value
    return when {
        heightDp < 130f -> 2
        heightDp < 200f -> 3
        heightDp < 270f -> 5
        else -> 7
    }
}
