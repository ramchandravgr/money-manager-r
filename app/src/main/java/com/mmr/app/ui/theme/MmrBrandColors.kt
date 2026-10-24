package com.mmr.app.ui.theme

import androidx.compose.ui.graphics.Color
import com.mmr.core.model.settings.MmrAmountColorScheme

/**
 * Semantic brand colors for the active theme. Updated from [MoneyInboxTheme] when the user
 * changes app color theme in Settings.
 */
object MmrBrandColors {
    private val p: MmrBrandPalette
        get() = MmrBrandColorState.palette

    val Header: Color get() = p.header
    val HeaderIcon: Color get() = p.headerIcon
    val HeaderSelection: Color get() = p.headerSelection
    val HeaderBrandTile: Color get() = p.headerBrandTile
    val TabText: Color get() = p.tabText
    val TabTextSelected: Color get() = p.tabTextSelected
    val TabIndicator: Color get() = p.tabIndicator
    val Active: Color get() = p.active
    val ListBackground: Color get() = p.listBackground
    val SurfaceWhite: Color get() = p.surface
    val BorderMain: Color get() = p.borderMain

    /** Soft gray fill for settings subsection headers (between screen bg and white rows). */
    val SettingsSectionHeaderFill: Color
        get() {
            val base = p.listBackground
            val edge = p.borderMain
            val mix = 0.42f
            return Color(
                red = base.red + (edge.red - base.red) * mix,
                green = base.green + (edge.green - base.green) * mix,
                blue = base.blue + (edge.blue - base.blue) * mix,
                alpha = 1f,
            )
        }
    val TransactionListBorder: Color get() = p.transactionListBorder
    val TextStrong: Color get() = p.textStrong
    val TextMedium: Color get() = p.textMedium
    val TextLight: Color get() = p.textLight
    val Reversal: Color get() = p.reversal
    val NavBackground: Color get() = p.navBackground
    val NavIcon: Color get() = p.navIcon
    val NavIconSelected: Color get() = p.navIconSelected
    val ExpenseSetA: Color get() = p.expenseSetA
    val IncomeSetA: Color get() = p.incomeSetA
    val ExpenseSetB: Color get() = p.expenseSetB
    val IncomeSetB: Color get() = p.incomeSetB
    val ConfigBackground: Color get() = p.configBackground
    val ConfigTitle: Color get() = p.configTitle
    val ConfigLabel: Color get() = p.configLabel
    val ConfigValue: Color get() = p.configValue
    val AmountPositive: Color get() = p.amountPositive
    val AmountNegative: Color get() = p.amountNegative
    val ApprovalApproveBackground: Color get() = p.approvalApproveBackground
    val ApprovalRejectBackground: Color get() = p.approvalRejectBackground
    val ApprovalApproveIcon: Color get() = p.approvalApproveIcon
    val ApprovalRejectIcon: Color get() = p.approvalRejectIcon
    val CalendarExpense: Color get() = p.calendarExpense
    val CalendarSaturday: Color get() = p.calendarSaturday
    val CalendarSunday: Color get() = p.calendarSunday
    val CalendarSelectedDay: Color get() = p.calendarSelectedDay
    val CalendarCellBorder: Color get() = p.calendarCellBorder
    val MonthlyIncome: Color get() = p.monthlyIncome
    val MonthlyExpense: Color get() = p.monthlyExpense
    val WeekHighlight: Color get() = p.weekHighlight
    val DialogHeader: Color get() = p.dialogHeader
    val DialogAccent: Color get() = p.dialogAccent
    val ChartLinePrimary: Color get() = p.chartLinePrimary
    val ChartPieFallback: Color get() = p.chartPieFallback
    val HomeSummaryAccent: Color get() = p.homeSummaryAccent

    fun expenseColor(scheme: MmrAmountColorScheme): Color = p.expenseColor(scheme)

    fun incomeColor(scheme: MmrAmountColorScheme): Color = p.incomeColor(scheme)
}
