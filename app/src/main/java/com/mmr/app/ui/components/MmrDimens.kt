package com.mmr.app.ui.components

import androidx.compose.ui.unit.dp

/**
 * Shared spacing and sizing for consistent alignment across screens.
 */
object MmrDimens {
    /** MM-style tight spacing (default for chrome and lists). */
    val spaceXs = 3.dp
    val spaceSm = 6.dp
    val spaceMd = 10.dp
    val spaceLg = 12.dp
    val spaceXl = 16.dp
    val space2xl = 20.dp

    val iconSm = 20.dp
    val iconMd = 24.dp
    val iconLg = 28.dp

    val touchTargetMin = 44.dp
    val topBarIconButton = 40.dp

    /** Fixed label column width for form rows and search filters (golden-ratio friendly ~112dp). */
    val formLabelWidth = 112.dp

    /** Left label column on Search filter rows (keeps values aligned on narrow phones). */
    val searchFilterLabelWidth = 88.dp

    val cardPadding = 12.dp
    val listItemPaddingVertical = 10.dp

    /** Edge inset for home chrome (filters, summary strips). */
    val listItemPaddingHorizontal = 8.dp

    /** MM `title_bar_height` (content row below status bar). */
    val mmTitleBarHeight = 40.dp
    val mmHeaderIconSize = 20.dp
    val mmSummaryStripVerticalPadding = 4.dp
    val mmFilterChipHeight = 26.dp
    val mmBottomNavHeight = 52.dp
    val mmFormRowHorizontalPadding = 15.dp

    /** MM Trans list (`item_day_adapter` / `item_day_adapter_middle`). */
    val transactionListPaddingHorizontal = 15.dp
    val transactionCategoryColumnWidth = 80.dp
    val transactionRowMinHeight = 48.dp
    val transactionRowColumnGap = 3.dp
    val transactionDayGroupGap = 8.dp
    val transactionDayHeaderMinHeight = 34.dp
    val transactionDayHeaderPaddingVertical = 5.dp
    val transactionTitleSubtitleGap = 2.dp
    val transactionListDividerThickness = 0.5.dp
    val transactionDayHeaderDividerThickness = 0.75.dp

    val labelColumnWeight = 0.4f
    val valueColumnWeight = 0.6f
    val labelValueGap = 12.dp

    val chevronSize = 22.dp

    /** Extra list bottom inset so content clears the main FAB. */
    val fabListBottomInset = 48.dp

    /** Dense home chrome (toolbar, tabs, summary). */
    val homeToolbarHeight = 40.dp
    val homeTopBarIconButton = 36.dp
    val homeIconSm = 18.dp
    val homeMonthTitleWidth = 72.dp
    val homeTabRowHeight = 28.dp
    val homeSummaryVerticalPadding = 4.dp
    val homeTabLabelVerticalPadding = 4.dp

    /** MM `stats.xml` Income/Expense bar. */
    val statsTabRowHeight = 43.dp
    val statsTabButtonHeight = 40.dp
    val statsTabIndicatorHeight = 3.dp

    /** MM `stats_list_item.xml`. */
    val statsCategoryRowHeight = 44.dp
    val statsCategoryRowStartMargin = 15.dp
    val statsCategoryPercentEndMargin = 12.dp
    val statsCategoryAmountEndMargin = 20.dp
    val statsPercentPillMinWidth = 38.dp
    val statsPercentPillMinHeight = 22.dp
    val statsPercentPillRadius = 4.dp
    val statsPercentPillPaddingHorizontal = 4.5.dp
    val statsPercentPillPaddingVertical = 3.dp

    val statsPieHeight = 300.dp
    val statsDetailLineChartHeight = 180.dp
    val statsDetailTotalMinHeight = 73.dp
    val statsDetailSubcategoryPaddingHorizontal = 30.dp
    val statsDetailSubcategoryDividerInset = 21.dp
    val statsDetailSubcategoryDividerThickness = 0.3.dp

    /** Primary / secondary full-width action buttons (Save, Create loan, …). */
    val actionButtonCorner = 10.dp
    val actionButtonMinHeight = 40.dp

    val progressBarHeight = 6.dp
    val categoryColorDot = 12.dp
}
