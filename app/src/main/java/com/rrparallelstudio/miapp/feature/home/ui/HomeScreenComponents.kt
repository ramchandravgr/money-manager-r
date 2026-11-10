package com.rrparallelstudio.miapp.feature.home.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.rrparallelstudio.miapp.core.model.settings.MmrAmountColorScheme
import com.rrparallelstudio.miapp.feature.home.model.HomeSummaryUi
import com.rrparallelstudio.miapp.feature.home.model.HomeViewTab
import com.rrparallelstudio.miapp.feature.home.ui.formatRupeeAmount
import com.rrparallelstudio.miapp.testing.MmrTestTags
import com.rrparallelstudio.miapp.ui.components.MmrHomeTopBar
import com.rrparallelstudio.miapp.ui.components.MmrLedgerViewModeDropdown
import com.rrparallelstudio.miapp.ui.components.MmrSummaryStrip

@Composable
fun HomeMonthTopBar(
    monthTitle: String,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onMonthTitleClick: () -> Unit,
    onSearchClick: () -> Unit,
    pendingApprovalCount: Int,
    onApprovalsClick: () -> Unit,
) {
    MmrHomeTopBar(
        monthTitle = monthTitle,
        onPreviousMonth = onPreviousMonth,
        onNextMonth = onNextMonth,
        onMonthTitleClick = onMonthTitleClick,
        onSearchClick = onSearchClick,
        onApprovalsClick = onApprovalsClick,
        pendingApprovalCount = pendingApprovalCount,
    )
}

@Composable
fun HomeViewTabRow(
    selectedTab: HomeViewTab,
    onTabSelected: (HomeViewTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tabs = HomeViewTab.menuEntries
    MmrLedgerViewModeDropdown(
        options = tabs.map { it.mmLabel },
        selectedIndex = tabs.indexOf(selectedTab).coerceAtLeast(0),
        onOptionSelected = { index -> onTabSelected(tabs[index]) },
        modifier = modifier,
        triggerTestTag = MmrTestTags.homeViewModeTrigger,
        testTagForIndex = { index -> MmrTestTags.homeViewTab(tabs[index]) },
    )
}

@Composable
fun HomePeriodSummaryStrip(
    summary: HomeSummaryUi,
    amountColorScheme: MmrAmountColorScheme = MmrAmountColorScheme.SET_B,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        val carryForward = summary.carriedForward
        val carryOverAmount = carryForward.takeIf { it != 0.0 }?.let { formatRupeeCarryOver(it) }
        MmrSummaryStrip(
            incomeAmount = formatRupeeAmount(summary.totalIncome),
            expenseAmount = formatRupeeAmount(summary.totalExpense),
            totalAmount = formatRupeeAmount(summary.netBalance),
            carryOverAmount = carryOverAmount,
            carryOverIsIncome = carryForward >= 0.0,
            totalIncludesCarryOver = carryOverAmount != null,
            amountColorScheme = amountColorScheme,
            incomeAmountTestTag = MmrTestTags.homeSummaryIncome,
            expenseAmountTestTag = MmrTestTags.homeSummaryExpense,
            carryOverAmountTestTag = MmrTestTags.homeSummaryCarryOver,
            totalAmountTestTag = MmrTestTags.homeSummaryTotal,
        )
    }
}
