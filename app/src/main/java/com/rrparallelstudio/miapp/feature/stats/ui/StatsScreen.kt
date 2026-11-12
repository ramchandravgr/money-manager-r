package com.rrparallelstudio.miapp.feature.stats.ui

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
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.rrparallelstudio.miapp.R
import com.rrparallelstudio.miapp.ads.MmrAdPlacementPolicy
import com.rrparallelstudio.miapp.ads.MmrAdSlot
import com.rrparallelstudio.miapp.ads.StatsAdPlacementContext
import com.rrparallelstudio.miapp.feature.home.ui.HomeMonthPickerDialog
import com.rrparallelstudio.miapp.feature.stats.StatsPeriodUtils
import com.rrparallelstudio.miapp.feature.stats.model.StatsCategoryDrillDown
import com.rrparallelstudio.miapp.feature.stats.model.StatsLedgerTab
import com.rrparallelstudio.miapp.feature.stats.model.StatsPeriodMode
import com.rrparallelstudio.miapp.feature.stats.model.StatsUiEvent
import com.rrparallelstudio.miapp.feature.stats.model.StatsUiState
import com.rrparallelstudio.miapp.feature.stats.viewmodel.StatsViewModel
import com.rrparallelstudio.miapp.ui.adaptive.MmrAdaptiveWidthBox
import com.rrparallelstudio.miapp.ui.components.MmrHeaderBar
import com.rrparallelstudio.miapp.ui.components.MmrNestedScaffoldInsets
import com.rrparallelstudio.miapp.ui.components.MmrPeriodChip
import com.rrparallelstudio.miapp.ui.components.MmrPeriodSwipeBox
import com.rrparallelstudio.miapp.ui.components.MmrStatsTabRow
import com.rrparallelstudio.miapp.ui.state.MmrEmptyState
import com.rrparallelstudio.miapp.ui.state.MmrErrorState
import com.rrparallelstudio.miapp.ui.state.MmrLoadingState
import com.rrparallelstudio.miapp.ui.theme.MmrBrandColors
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
        selectedYear = anchorMonth.year,
        selectedMonth = anchorMonth.monthValue,
        onDismiss = { viewModel.onEvent(StatsUiEvent.OnDismissMonthPicker) },
        onPeriodSelected = { year, month ->
            viewModel.onEvent(StatsUiEvent.OnMonthPickerPeriodSelected(year, month))
        },
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
        containerColor = MmrBrandColors.ScreenContainer,
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
                                beyondViewportPageCount = 0,
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
                                val statsAdContext =
                                    remember(uiState.isLoading, uiState.errorMessage, empty) {
                                        StatsAdPlacementContext(
                                            isLoading = uiState.isLoading,
                                            errorMessage = uiState.errorMessage,
                                            tabHasData = !empty,
                                        )
                                    }
                                val statsAdPlacement =
                                    remember(statsAdContext) { MmrAdPlacementPolicy.statsNative(statsAdContext) }
                                if (empty) {
                                    MmrEmptyState(
                                        message = stringResource(R.string.stats_no_transactions_in_period),
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                } else {
                                    MmrAdaptiveWidthBox(modifier = Modifier.fillMaxSize()) {
                                        Column(modifier = Modifier.fillMaxSize()) {
                                            StatsCylinderInspectSection(
                                                breakdowns = breakdowns,
                                                onNavigateToCategoryDetail = { item ->
                                                    val drill =
                                                        viewModel.drillDownFor(item, tab)
                                                            ?: return@StatsCylinderInspectSection
                                                    onNavigateToCategoryDetail(drill)
                                                },
                                                modifier =
                                                    Modifier
                                                        .weight(1f)
                                                        .fillMaxWidth(),
                                            )
                                            MmrAdSlot(placement = statsAdPlacement)
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
                        style =
                            com.rrparallelstudio.miapp.ui.theme
                                .mmrTitleMediumFixedStyle(MmrBrandColors.HeaderIcon),
                        modifier =
                            Modifier.clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = onPeriodStartClick,
                            ),
                    )
                    Text(
                        text = " ~ ",
                        style =
                            com.rrparallelstudio.miapp.ui.theme
                                .mmrTitleMediumFixedStyle(MmrBrandColors.HeaderIcon),
                    )
                    Text(
                        text = endLabel,
                        style =
                            com.rrparallelstudio.miapp.ui.theme
                                .mmrTitleMediumFixedStyle(MmrBrandColors.HeaderIcon),
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
            centerPeriodChrome = true,
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
