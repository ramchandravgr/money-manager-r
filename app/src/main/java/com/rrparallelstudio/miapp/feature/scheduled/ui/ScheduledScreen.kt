package com.rrparallelstudio.miapp.feature.scheduled.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.rrparallelstudio.miapp.feature.loan.ui.LoanListScreen
import com.rrparallelstudio.miapp.feature.recurring.ui.RecurringListScreen
import com.rrparallelstudio.miapp.feature.scheduled.model.ScheduledSection
import com.rrparallelstudio.miapp.testing.MmrTestTags
import com.rrparallelstudio.miapp.ui.components.MmrAddListFab
import com.rrparallelstudio.miapp.ui.components.MmrHeaderBar
import com.rrparallelstudio.miapp.ui.components.MmrNestedScaffoldInsets
import com.rrparallelstudio.miapp.ui.theme.MmrBrandColors
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ScheduledScreen(
    initialSection: ScheduledSection,
    onNavigateToLoanDetail: (Long) -> Unit,
    onNavigateToAddLoan: () -> Unit,
    onNavigateToEditLoan: (Long) -> Unit,
    onNavigateToRecurringDetail: (Long) -> Unit,
    onNavigateToAddRecurringRule: () -> Unit,
) {
    val pagerState =
        rememberPagerState(
            initialPage = initialSection.ordinal,
            pageCount = { ScheduledSection.entries.size },
        )
    val scope = rememberCoroutineScope()
    val currentSection = ScheduledSection.entries[pagerState.currentPage.coerceIn(ScheduledSection.entries.indices)]

    Scaffold(
        modifier = Modifier.testTag(MmrTestTags.scheduledScreen),
        containerColor = MmrBrandColors.ScreenContainer,
        contentWindowInsets = MmrNestedScaffoldInsets,
        topBar = {
            Column(modifier = Modifier.fillMaxWidth()) {
                MmrHeaderBar(
                    title = "Scheduled",
                    showBack = false,
                    onBack = null,
                )
                ScheduledSectionTabRow(
                    selectedSection = currentSection,
                    onSectionSelected = { section ->
                        scope.launch {
                            pagerState.animateScrollToPage(section.ordinal)
                        }
                    },
                )
            }
        },
        floatingActionButton = {
            when (currentSection) {
                ScheduledSection.LOANS ->
                    MmrAddListFab(
                        modifier = Modifier.testTag(MmrTestTags.loanFabAdd),
                        contentDescription = "Add loan",
                        onClick = onNavigateToAddLoan,
                    )
                ScheduledSection.RECURRING ->
                    MmrAddListFab(
                        modifier = Modifier.testTag(MmrTestTags.recurringFabAdd),
                        contentDescription = "Add recurring rule",
                        onClick = onNavigateToAddRecurringRule,
                    )
            }
        },
    ) { innerPadding ->
        HorizontalPager(
            state = pagerState,
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            beyondViewportPageCount = 1,
            userScrollEnabled = true,
        ) { page ->
            when (ScheduledSection.entries[page]) {
                ScheduledSection.LOANS ->
                    LoanListScreen(
                        onNavigateBack = {},
                        onNavigateToLoanDetail = onNavigateToLoanDetail,
                        onNavigateToAddLoan = onNavigateToAddLoan,
                        onNavigateToEditLoan = onNavigateToEditLoan,
                        showBackNavigation = false,
                        embeddedInScheduledHub = true,
                    )
                ScheduledSection.RECURRING ->
                    RecurringListScreen(
                        onNavigateBack = {},
                        onNavigateToDetail = onNavigateToRecurringDetail,
                        onNavigateToAddRule = onNavigateToAddRecurringRule,
                        showBackNavigation = false,
                        embeddedInScheduledHub = true,
                    )
            }
        }
    }
}
