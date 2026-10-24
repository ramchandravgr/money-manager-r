package com.mmr.feature.stats.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mmr.app.ui.adaptive.MmrAdaptiveWidthBox
import com.mmr.app.ui.components.MmrHeaderBar
import com.mmr.app.ui.components.MmrNestedScaffoldInsets
import com.mmr.app.ui.components.MmrPeriodChip
import com.mmr.app.ui.components.MmrPeriodSwipeBox
import com.mmr.app.ui.components.MmrStatsTabRow
import com.mmr.app.ui.state.MmrEmptyState
import com.mmr.app.ui.state.MmrErrorState
import com.mmr.app.ui.state.MmrLoadingState
import com.mmr.app.ui.theme.MmrBrandColors
import com.mmr.feature.home.ui.HomeMonthPickerDialog
import com.mmr.feature.stats.StatsPeriodUtils
import com.mmr.feature.stats.model.*
import com.mmr.feature.stats.viewmodel.StatsViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun StatsScreen(
    onNavigateBack: () -> Unit,
    showBackNavigation: Boolean = true,
    onNavigateToCategoryDetail: (StatsCategoryDrillDown) -> Unit = {},
    viewModel: StatsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val zoneId = ZoneId.systemDefault()

    if (uiState.showPeriodStartPicker) {
        StatsDatePickerDialog(
            initialMillis =
                uiState.periodStartDate
                    ?.atStartOfDay(zoneId)
                    ?.toInstant()
                    ?.toEpochMilli(),
            onDismiss = { viewModel.onEvent(StatsUiEvent.OnDismissPeriodStartPicker) },
            onConfirm = { viewModel.onEvent(StatsUiEvent.OnCustomPeriodStartSelected(it)) },
        )
    }
    if (uiState.showPeriodEndPicker) {
        StatsDatePickerDialog(
            initialMillis =
                uiState.periodEndDate
                    ?.atStartOfDay(zoneId)
                    ?.toInstant()
                    ?.toEpochMilli(),
            onDismiss = { viewModel.onEvent(StatsUiEvent.OnDismissPeriodEndPicker) },
            onConfirm = { viewModel.onEvent(StatsUiEvent.OnCustomPeriodEndSelected(it)) },
        )
    }

    val anchorMonth =
        uiState.periodStartDate?.let { java.time.YearMonth.from(it) }
            ?: java.time.YearMonth.now()
    HomeMonthPickerDialog(
        visible = uiState.showMonthPicker,
        pickerYear = uiState.monthPickerYear,
        selectedYear = anchorMonth.year,
        selectedMonth = anchorMonth.monthValue,
        onDismiss = { viewModel.onEvent(StatsUiEvent.OnDismissMonthPicker) },
        onPreviousYear = { viewModel.onEvent(StatsUiEvent.OnMonthPickerPreviousYear) },
        onNextYear = { viewModel.onEvent(StatsUiEvent.OnMonthPickerNextYear) },
        onMonthSelected = { viewModel.onEvent(StatsUiEvent.OnMonthPickerMonthSelected(it)) },
        onThisMonth = { viewModel.onEvent(StatsUiEvent.OnMonthPickerThisMonth) },
    )

    val periodSwipeEnabled = uiState.canNavigatePeriod && uiState.periodSwipeEnabled
    val pagerState =
        rememberPagerState(
            initialPage = if (uiState.ledgerTab == StatsLedgerTab.INCOME) 0 else 1,
            pageCount = { 2 },
        )

    LaunchedEffect(uiState.ledgerTab) {
        val target = if (uiState.ledgerTab == StatsLedgerTab.INCOME) 0 else 1
        if (pagerState.currentPage != target) {
            pagerState.animateScrollToPage(target)
        }
    }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }
            .distinctUntilChanged()
            .collect { page ->
                val tab = if (page == 0) StatsLedgerTab.INCOME else StatsLedgerTab.EXPENSE
                if (uiState.ledgerTab != tab) {
                    viewModel.onEvent(StatsUiEvent.OnLedgerTabSelected(tab))
                }
            }
    }

    Scaffold(
        containerColor = MmrBrandColors.ListBackground,
        contentWindowInsets = MmrNestedScaffoldInsets,
        topBar = {
            StatsTopBar(
                uiState = uiState,
                showBackNavigation = showBackNavigation,
                onNavigateBack = onNavigateBack,
                onPrevious = { viewModel.onEvent(StatsUiEvent.OnPreviousPeriod) },
                onNext = { viewModel.onEvent(StatsUiEvent.OnNextPeriod) },
                onModeSelected = { viewModel.onEvent(StatsUiEvent.OnPeriodModeSelected(it)) },
                onPeriodStartClick = { viewModel.onEvent(StatsUiEvent.OnPeriodStartFieldClick) },
                onPeriodEndClick = { viewModel.onEvent(StatsUiEvent.OnPeriodEndFieldClick) },
                onTitleClick =
                    if (uiState.periodMode != StatsPeriodMode.PERIOD) {
                        { viewModel.onEvent(StatsUiEvent.OnOpenMonthPicker) }
                    } else {
                        null
                    },
            )
        },
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
        ) {
            MmrStatsTabRow(
                incomeTabLabel = viewModel.ledgerTabLabel(StatsLedgerTab.INCOME, uiState.summary),
                expenseTabLabel = viewModel.ledgerTabLabel(StatsLedgerTab.EXPENSE, uiState.summary),
                selectedIndex = if (uiState.ledgerTab == StatsLedgerTab.INCOME) 0 else 1,
                onTabSelected = { index ->
                    viewModel.onEvent(
                        StatsUiEvent.OnLedgerTabSelected(
                            if (index == 0) StatsLedgerTab.INCOME else StatsLedgerTab.EXPENSE,
                        ),
                    )
                },
            )
            HorizontalDivider(
                color = MmrBrandColors.TransactionListBorder,
                thickness = com.mmr.app.ui.components.MmrDimens.transactionListDividerThickness,
            )

            MmrPeriodSwipeBox(
                periodKey = uiState.periodStartMillis,
                enabled = periodSwipeEnabled,
                onPrevious = { viewModel.onEvent(StatsUiEvent.OnPreviousPeriod) },
                onNext = { viewModel.onEvent(StatsUiEvent.OnNextPeriod) },
                modifier =
                    Modifier
                        .weight(1f)
                        .fillMaxWidth(),
            ) {
                when {
                    uiState.isLoading -> MmrLoadingState(modifier = Modifier.fillMaxSize())
                    uiState.errorMessage != null -> {
                        MmrErrorState(
                            message = uiState.errorMessage ?: "Something went wrong.",
                            onRetry = { viewModel.onEvent(StatsUiEvent.OnRetryClick) },
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                    else -> {
                        StatsPullToRefreshContainer(
                            isRefreshing = uiState.isRefreshing,
                            onRefresh = { viewModel.onEvent(StatsUiEvent.OnRefresh) },
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            HorizontalPager(
                                state = pagerState,
                                modifier = Modifier.fillMaxSize(),
                                beyondViewportPageCount = 1,
                                userScrollEnabled = true,
                            ) { page ->
                                val tab = if (page == 0) StatsLedgerTab.INCOME else StatsLedgerTab.EXPENSE
                                val breakdowns =
                                    if (tab == StatsLedgerTab.INCOME) {
                                        uiState.incomeBreakdowns
                                    } else {
                                        uiState.expenseBreakdowns
                                    }
                                val empty =
                                    if (tab == StatsLedgerTab.INCOME) {
                                        uiState.incomeTabEmpty
                                    } else {
                                        uiState.expenseTabEmpty
                                    }
                                if (empty) {
                                    MmrEmptyState(
                                        message = "No ${tab.name.lowercase()} in this period.",
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                } else {
                                    MmrAdaptiveWidthBox(modifier = Modifier.fillMaxSize()) {
                                        StatsCategoryList(
                                            breakdowns = breakdowns,
                                            showNativeAdPlaceholder = breakdowns.size > 3,
                                            headerContent = {
                                                StatsPieChartSection(
                                                    breakdowns = breakdowns,
                                                    modifier = Modifier.fillMaxWidth(),
                                                )
                                            },
                                            onCategoryClick = { item ->
                                                val drill =
                                                    viewModel.drillDownFor(item, tab)
                                                        ?: return@StatsCategoryList
                                                onNavigateToCategoryDetail(drill)
                                            },
                                            modifier = Modifier.fillMaxSize(),
                                            contentPadding =
                                                PaddingValues(
                                                    bottom = com.mmr.app.ui.components.MmrDimens.spaceSm,
                                                ),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StatsTopBar(
    uiState: StatsUiState,
    showBackNavigation: Boolean,
    onNavigateBack: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onModeSelected: (StatsPeriodMode) -> Unit,
    onPeriodStartClick: () -> Unit,
    onPeriodEndClick: () -> Unit,
    onTitleClick: (() -> Unit)?,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val slashDate = DateTimeFormatter.ofPattern("dd/MM/yy", Locale.getDefault())
    val periodTitleContent: (@Composable () -> Unit)? =
        if (uiState.periodMode == StatsPeriodMode.PERIOD) {
            {
                val startLabel = uiState.periodStartDate?.let { slashDate.format(it) } ?: "Start"
                val endLabel = uiState.periodEndDate?.let { slashDate.format(it) } ?: "End"
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = startLabel,
                        color = MmrBrandColors.HeaderIcon,
                        fontSize = 15.sp,
                        modifier =
                            Modifier.clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = onPeriodStartClick,
                            ),
                    )
                    Text(text = " ~ ", color = MmrBrandColors.HeaderIcon, fontSize = 15.sp)
                    Text(
                        text = endLabel,
                        color = MmrBrandColors.HeaderIcon,
                        fontSize = 15.sp,
                        modifier =
                            Modifier.clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = onPeriodEndClick,
                            ),
                    )
                }
            }
        } else {
            null
        }

    Column(Modifier.fillMaxWidth()) {
        MmrHeaderBar(
            title = if (uiState.periodMode == StatsPeriodMode.PERIOD) "" else uiState.navigationLabel,
            titleContent = periodTitleContent,
            onTitleClick = onTitleClick,
            showBack = showBackNavigation,
            onBack = onNavigateBack,
            onPrevious =
                if (uiState.periodMode != StatsPeriodMode.PERIOD && uiState.canNavigatePeriod) {
                    onPrevious
                } else {
                    null
                },
            onNext =
                if (uiState.periodMode != StatsPeriodMode.PERIOD && uiState.canNavigatePeriod) {
                    onNext
                } else {
                    null
                },
            trailing = {
                Box {
                    MmrPeriodChip(
                        label = StatsPeriodUtils.modeDropdownLabel(uiState.periodMode),
                        onClick = { menuExpanded = true },
                    )
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        StatsPeriodMode.entries.forEach { mode ->
                            DropdownMenuItem(
                                text = { Text(StatsPeriodUtils.modeDropdownLabel(mode)) },
                                onClick = {
                                    menuExpanded = false
                                    onModeSelected(mode)
                                },
                            )
                        }
                    }
                }
            },
        )
    }
}
