package com.mmr.feature.home.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mmr.app.testing.MmrTestTags
import com.mmr.app.ui.components.MmrHomeTopBar
import com.mmr.app.ui.components.MmrSummaryStrip
import com.mmr.app.ui.components.MmrTabRow
import com.mmr.app.ui.components.MmrTabRowSurface
import com.mmr.app.ui.theme.MmrBrandColors
import com.mmr.core.model.settings.MmrAmountColorScheme
import com.mmr.feature.home.model.HomeSummaryUi
import com.mmr.feature.home.model.HomeViewTab
import com.mmr.feature.home.ui.formatRupeeAmount

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
    val tabs = HomeViewTab.entries
    MmrTabRow(
        tabs = tabs.map { it.mmLabel },
        selectedIndex = tabs.indexOf(selectedTab).coerceAtLeast(0),
        onTabSelected = { index -> onTabSelected(tabs[index]) },
        modifier = modifier,
        testTagForIndex = { index -> MmrTestTags.homeViewTab(tabs[index]) },
        surface = MmrTabRowSurface.Ledger,
    )
}

@Composable
fun HomePeriodSummaryStrip(
    summary: HomeSummaryUi,
    amountColorScheme: MmrAmountColorScheme = MmrAmountColorScheme.SET_B,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        MmrSummaryStrip(
            incomeLabel = "Income",
            incomeAmount = formatRupeeAmount(summary.totalIncome),
            expenseLabel = "Expenses",
            expenseAmount = formatRupeeAmount(summary.totalExpense),
            totalLabel = "Total",
            totalAmount = formatRupeeAmount(summary.netBalance),
            amountColorScheme = amountColorScheme,
            incomeAmountTestTag = MmrTestTags.homeSummaryIncome,
            expenseAmountTestTag = MmrTestTags.homeSummaryExpense,
            totalAmountTestTag = MmrTestTags.homeSummaryTotal,
        )
        if (summary.carriedForward != 0.0) {
            Text(
                text = "Includes ${formatRupeeAmount(summary.carriedForward)} carried forward",
                style = MaterialTheme.typography.labelSmall,
                color = MmrBrandColors.TextMedium,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp, start = 16.dp, end = 16.dp),
                textAlign = TextAlign.Center,
            )
        }
    }
}
