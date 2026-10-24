package com.mmr.app.ui.components

import androidx.compose.ui.unit.dp

/**
 * Shared spacing and sizing for consistent alignment across screens.
 */
object MmrDimens {
    /** Tight spacing (default for chrome and lists). */
    val spaceXs = 3.dp
    val spaceSm = 6.dp
    val spaceMd = 10.dp
    val spaceLg = 12.dp
    val spaceXl = 16.dp
    val space2xl = 20.dp

    val iconSm = 20.dp
    val iconMd = 24.dp
    val iconLg = 28.dp

    /** “+” on list-screen [MmrAddListFab] (Small FAB). */
    val listAddFabPlusIcon = 22.dp

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

    /** Title bar content row height (below status bar). */
    val chromeTitleBarHeight = 40.dp
    val chromeHeaderIconSize = 20.dp
    val chromeSummaryStripVerticalPadding = 4.dp
    val chromeFilterChipHeight = 26.dp

    /** Tab bar content height (labels + icons); nav bar insets added separately. */
    val chromeBottomNavHeight = 58.dp
    val bottomNavIconSize = 24.dp
    val chromeFormRowHorizontalPadding = 15.dp

    /** Transaction list row layout. */
    val transactionListPaddingHorizontal = 15.dp
    val transactionCategoryColumnWidth = 80.dp
    val transactionRowMinHeight = 48.dp
    val transactionRowColumnGap = 3.dp
    val transactionDayGroupGap = 4.dp
    val transactionDayHeaderMinHeight = 34.dp
    val transactionDayHeaderPaddingVertical = 5.dp
    val transactionTitleSubtitleGap = 2.dp
    val transactionListDividerThickness = 0.5.dp
    val transactionDayHeaderDividerThickness = 0.75.dp

    /** Ledger pill cards: inset from list edge and gap between pills. */
    val transactionPillHorizontalInset = 10.dp
    val transactionPillGap = 4.dp

    /** Space below the last pill before the next day header (half of [transactionPillGap]). */
    val transactionPillDayEndGap = 2.dp
    val transactionPillContentPaddingHorizontal = 14.dp
    val transactionPillContentPaddingVertical = 12.dp

    val labelColumnWeight = 0.4f
    val valueColumnWeight = 0.6f
    val labelValueGap = 12.dp

    /** Transaction detail metadata card (Type / Date / … rows). */
    val transactionDetailMetaCardPadding = 6.dp
    val transactionDetailMetaRowSpacing = 4.dp
    val transactionDetailLabelValueGap = 6.dp
    val transactionDetailLabelValueRowVertical = 1.5.dp

    val chevronSize = 22.dp

    /** Settings subsection headers (Web Manage, LAN sync, Configuration). */
    val settingsSectionHeaderCorner = 10.dp
    val settingsSectionContentGap = 12.dp
    val settingsSectionTopSpacing = 20.dp
    val settingsSectionTopSpacingCompact = 12.dp
    val settingsSectionContentGapCompact = 6.dp
    val settingsFormPaddingHorizontalCompact = 10.dp
    val settingsFormPaddingVerticalCompact = 8.dp
    val settingsFormItemGapCompact = 8.dp
    val settingsPanelPaddingCompact = 10.dp

    /** iOS inset grouped Settings (UITableView insetGrouped). */
    val iosSettingsHorizontalInset = 20.dp
    val iosSettingsGroupCorner = 10.dp
    val iosSettingsRowMinHeight = 44.dp
    val iosSettingsRowHorizontalPadding = 16.dp
    val iosSettingsSectionHeaderInset = 16.dp
    val iosSettingsSectionHeaderBottom = 7.dp
    val iosSettingsSectionFooterTop = 8.dp
    val iosSettingsSectionFirstTop = 8.dp
    val iosSettingsSectionTop = 22.dp

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

    /** Stats Income/Expense tab bar. */
    val statsTabRowHeight = 43.dp
    val statsTabButtonHeight = 40.dp
    val statsTabIndicatorHeight = 3.dp

    /** Stats category list rows. */
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
