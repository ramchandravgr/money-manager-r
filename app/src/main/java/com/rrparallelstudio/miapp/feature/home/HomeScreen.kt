@file:Suppress("ktlint:standard:no-wildcard-imports")

package com.rrparallelstudio.miapp.feature.home

import android.widget.Toast
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.CallMerge
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.rrparallelstudio.miapp.R
import com.rrparallelstudio.miapp.ads.HomeAdPlacementContext
import com.rrparallelstudio.miapp.ads.MmrAdPlacementPolicy
import com.rrparallelstudio.miapp.ads.MmrAdSlot
import com.rrparallelstudio.miapp.feature.home.HomeCarryOverInjector
import com.rrparallelstudio.miapp.feature.home.model.*
import com.rrparallelstudio.miapp.feature.home.ui.GroupedTransactionList
import com.rrparallelstudio.miapp.feature.home.ui.HomeCalendarTabContent
import com.rrparallelstudio.miapp.feature.home.ui.HomeExcelExportPeriodDialog
import com.rrparallelstudio.miapp.feature.home.ui.HomeMonthPickerDialog
import com.rrparallelstudio.miapp.feature.home.ui.HomeMonthTopBar
import com.rrparallelstudio.miapp.feature.home.ui.HomeMonthlyTabContent
import com.rrparallelstudio.miapp.feature.home.ui.HomePeriodSummaryStrip
import com.rrparallelstudio.miapp.feature.home.ui.HomeTotalTabContent
import com.rrparallelstudio.miapp.feature.home.ui.HomeViewTabRow
import com.rrparallelstudio.miapp.testing.MmrTestTags
import com.rrparallelstudio.miapp.ui.adaptive.MmrAdaptiveWidthBox
import com.rrparallelstudio.miapp.ui.components.MmrAddListFab
import com.rrparallelstudio.miapp.ui.components.MmrConfirmDialog
import com.rrparallelstudio.miapp.ui.components.MmrDeleteIconButton
import com.rrparallelstudio.miapp.ui.components.MmrDimens
import com.rrparallelstudio.miapp.ui.components.MmrFilterChip
import com.rrparallelstudio.miapp.ui.components.MmrNestedScaffoldInsets
import com.rrparallelstudio.miapp.ui.components.MmrPeriodSwipeBox
import com.rrparallelstudio.miapp.ui.components.MmrSelectionModeBackHandler
import com.rrparallelstudio.miapp.ui.components.MmrTopAppBar
import com.rrparallelstudio.miapp.ui.components.MmrTransactionMergeDialog
import com.rrparallelstudio.miapp.ui.state.MmrEmptyState
import com.rrparallelstudio.miapp.ui.state.MmrErrorState
import com.rrparallelstudio.miapp.ui.state.MmrLoadingState
import com.rrparallelstudio.miapp.ui.theme.MmrBrandColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToAddTransaction: (prefillDateMillis: Long?) -> Unit,
    onNavigateToTransactionDetail: (Long) -> Unit,
    onNavigateToApprovals: () -> Unit,
    onNavigateToSearch: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val ledgerListState = rememberLazyListState()
    val ledgerTotalListState = rememberLazyListState()
    val ledgerScrollToTopNonce by viewModel.ledgerScrollToTopNonce.collectAsState()

    LaunchedEffect(ledgerScrollToTopNonce) {
        if (ledgerScrollToTopNonce > 0) {
            ledgerListState.animateScrollToItem(0)
            ledgerTotalListState.animateScrollToItem(0)
        }
    }

    MmrSelectionModeBackHandler(
        enabled = uiState.isSelectionMode,
        onExitSelectionMode = { viewModel.onEvent(HomeUiEvent.OnExitSelectionMode) },
    )

    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.openTransactionId.collect { id ->
            onNavigateToTransactionDetail(id)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.periodExportResult.collect { result ->
            Toast
                .makeText(
                    context,
                    context.getString(R.string.home_excel_saved_to_downloads, result.savedFileName),
                    Toast.LENGTH_LONG,
                ).show()
        }
    }

    HomeMonthPickerDialog(
        visible = uiState.showMonthPicker,
        selectedYear = uiState.selectedYear,
        selectedMonth = uiState.selectedMonth,
        onDismiss = { viewModel.onEvent(HomeUiEvent.OnDismissMonthPicker) },
        onPeriodSelected = { year, month ->
            viewModel.onEvent(HomeUiEvent.OnMonthPickerPeriodSelected(year, month))
        },
        onThisMonth = { viewModel.onEvent(HomeUiEvent.OnMonthPickerThisMonth) },
    )

    if (uiState.showMergeDialog) {
        val mergeMessage =
            buildString {
                append("Combined total: ${uiState.mergeCombinedAmountLabel}. ")
                append("Linked SMS stay attached to the merged transaction.")
                if (uiState.mergeDateOptions.size > 1) {
                    append(" Choose which date to keep.")
                }
            }
        MmrTransactionMergeDialog(
            title = "Merge transactions",
            message = mergeMessage,
            name = uiState.mergeNameInput,
            onNameChange = { viewModel.onEvent(HomeUiEvent.OnMergeNameChanged(it)) },
            nameLabel = "Name",
            namePlaceholder = "Merged transaction name",
            dateOptions = uiState.mergeDateOptions,
            selectedDateTimeMillis = uiState.mergeSelectedDateTimeMillis,
            onDateSelected = { viewModel.onEvent(HomeUiEvent.OnMergeDateSelected(it)) },
            onDismiss = { viewModel.onEvent(HomeUiEvent.OnDismissMergeDialog) },
            onConfirm = { viewModel.onEvent(HomeUiEvent.OnConfirmMerge) },
            errorMessage = uiState.mergeErrorMessage,
            confirmTestTag = MmrTestTags.homeSelectionMerge,
        )
    }

    HomeExcelExportPeriodDialog(
        visible = uiState.showExcelExportDialog,
        title = "${stringResource(R.string.app_name)} - Excel",
        options = uiState.excelExportOptions,
        onDismiss = { viewModel.onEvent(HomeUiEvent.OnDismissExcelExportDialog) },
        onOptionSelected = { option ->
            viewModel.onEvent(HomeUiEvent.OnExcelExportPeriodSelected(option.preset))
        },
    )

    if (uiState.isExcelExporting) {
        Dialog(onDismissRequest = {}) {
            Surface(shape = MaterialTheme.shapes.medium) {
                Box(
                    modifier = Modifier.padding(32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }

    if (uiState.showDeleteConfirmDialog) {
        val count = uiState.selectedTransactionIds.size
        MmrConfirmDialog(
            title = if (count == 1) "Delete transaction?" else "Delete $count transactions?",
            message = "This cannot be undone. Linked loan payments are unlinked, not reversed.",
            confirmLabel = "Delete",
            onConfirm = { viewModel.onEvent(HomeUiEvent.OnConfirmDeleteSelected) },
            onDismiss = { viewModel.onEvent(HomeUiEvent.OnDismissDeleteConfirmDialog) },
            confirmIsDestructive = true,
        )
    }

    val selectionListTab =
        uiState.selectedViewTab == HomeViewTab.DAILY ||
            uiState.selectedViewTab == HomeViewTab.TOTAL

    val periodSwipeKey = uiState.selectedYear * 100L + uiState.selectedMonth
    val periodSwipeEnabled = !uiState.isSelectionMode

    Scaffold(
        containerColor = MmrBrandColors.ScreenContainer,
        contentWindowInsets = MmrNestedScaffoldInsets,
        topBar = {
            if (uiState.isSelectionMode) {
                MmrTopAppBar(
                    title = "${uiState.selectedTransactionIds.size} selected",
                    showBackNavigation = true,
                    onNavigateBack = { viewModel.onEvent(HomeUiEvent.OnExitSelectionMode) },
                    useBrandHeaderStyle = true,
                    useCompactHeaderTitle = true,
                    actions = {
                        if (uiState.visibleSelectableCount > 0) {
                            IconButton(
                                onClick = { viewModel.onEvent(HomeUiEvent.OnToggleSelectAllVisible) },
                                enabled = uiState.visibleSelectableCount > 0,
                            ) {
                                Icon(
                                    imageVector =
                                        if (uiState.allVisibleSelected) {
                                            Icons.Filled.CheckBox
                                        } else {
                                            Icons.Outlined.SelectAll
                                        },
                                    contentDescription =
                                        if (uiState.allVisibleSelected) {
                                            "Clear selection"
                                        } else {
                                            "Select all"
                                        },
                                )
                            }
                        }
                        MmrDeleteIconButton(
                            onClick = { viewModel.onEvent(HomeUiEvent.OnOpenDeleteConfirmDialog) },
                            enabled = uiState.canDeleteSelection,
                            contentDescription = "Delete selected",
                            testTag = MmrTestTags.homeSelectionDelete,
                        )
                        IconButton(
                            onClick = { viewModel.onEvent(HomeUiEvent.OnOpenMergeDialog) },
                            enabled = uiState.canMergeSelection,
                            modifier = Modifier.testTag(MmrTestTags.homeSelectionMerge),
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.CallMerge,
                                contentDescription = "Merge selected",
                            )
                        }
                    },
                )
            } else {
                HomeMonthTopBar(
                    monthTitle = uiState.monthTitle,
                    onPreviousMonth = { viewModel.onEvent(HomeUiEvent.OnPreviousMonth) },
                    onNextMonth = { viewModel.onEvent(HomeUiEvent.OnNextMonth) },
                    onMonthTitleClick = { viewModel.onEvent(HomeUiEvent.OnOpenMonthPicker) },
                    onSearchClick = onNavigateToSearch,
                    pendingApprovalCount = uiState.pendingApprovalCount,
                    onApprovalsClick = onNavigateToApprovals,
                )
            }
        },
        floatingActionButton = {
            if (!uiState.isSelectionMode) {
                MmrAddListFab(
                    modifier = Modifier.testTag(MmrTestTags.homeFabAdd),
                    contentDescription = "Add transaction",
                    onClick = {
                        val prefill =
                            if (uiState.selectedViewTab == HomeViewTab.CALENDAR) {
                                viewModel.calendarAddTransactionMillis()
                            } else {
                                null
                            }
                        onNavigateToAddTransaction(prefill)
                    },
                )
            }
        },
        bottomBar = {
            if (!uiState.isSelectionMode) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    HorizontalDivider(
                        color = MmrBrandColors.TransactionListBorder,
                        thickness = MmrDimens.transactionListDividerThickness,
                    )
                    HomePeriodSummaryStrip(
                        summary = uiState.summary,
                        amountColorScheme = uiState.amountColorScheme,
                    )
                }
            }
        },
    ) { innerPadding ->
        val homeAdContext =
            remember(
                uiState.selectedViewTab,
                uiState.isSelectionMode,
                uiState.isLoading,
                uiState.errorMessage,
                uiState.groupedTransactions,
            ) {
                HomeAdPlacementContext(
                    viewTab = uiState.selectedViewTab,
                    isSelectionMode = uiState.isSelectionMode,
                    isLoading = uiState.isLoading,
                    errorMessage = uiState.errorMessage,
                    groupedTransactions = uiState.groupedTransactions,
                )
            }
        val homeBannerPlacement = remember(homeAdContext) { MmrAdPlacementPolicy.homeDailyBanner(homeAdContext) }
        val homeMrecPlacement = remember(homeAdContext) { MmrAdPlacementPolicy.homeDailyMrec(homeAdContext) }

        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                if (!uiState.isSelectionMode) {
                    HomeViewTabRow(
                        selectedTab = uiState.selectedViewTab,
                        onTabSelected = { viewModel.onEvent(HomeUiEvent.OnViewTabSelected(it)) },
                    )

                    if (homeBannerPlacement != null) {
                        MmrAdSlot(
                            placement = homeBannerPlacement,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    if (uiState.selectedViewTab == HomeViewTab.DAILY || uiState.selectedViewTab == HomeViewTab.TOTAL) {
                        HomeFilterChipsRow(uiState = uiState, onEvent = viewModel::onEvent)
                    }
                }

                MmrPeriodSwipeBox(
                    periodKey = periodSwipeKey,
                    enabled = periodSwipeEnabled,
                    onPrevious = { viewModel.onEvent(HomeUiEvent.OnPreviousMonth) },
                    onNext = { viewModel.onEvent(HomeUiEvent.OnNextMonth) },
                    modifier =
                        Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                ) {
                    when {
                        uiState.isLoading -> {
                            MmrLoadingState(modifier = Modifier.fillMaxSize())
                        }

                        uiState.errorMessage != null -> {
                            MmrErrorState(
                                message = uiState.errorMessage ?: "Something went wrong.",
                                onRetry = { viewModel.onEvent(HomeUiEvent.OnRetryClick) },
                                modifier = Modifier.fillMaxSize(),
                            )
                        }

                        homeMrecPlacement != null -> {
                            HomeDailyMrecPane(
                                uiState = uiState,
                                emptyMessage = emptyMessage(uiState),
                                selectionListTab = selectionListTab,
                                onNavigateToTransactionDetail = onNavigateToTransactionDetail,
                                onEvent = viewModel::onEvent,
                                ledgerListState = ledgerListState,
                            )
                        }

                        uiState.isEmpty -> {
                            MmrEmptyState(
                                message = emptyMessage(uiState),
                                modifier = Modifier.fillMaxSize(),
                            )
                        }

                        else -> {
                            MmrAdaptiveWidthBox(modifier = Modifier.fillMaxSize()) {
                                val selectionMode = uiState.isSelectionMode
                                val selectedIds = uiState.selectedTransactionIds
                                val onRowClick: (Long) -> Unit = { id ->
                                    if (!HomeCarryOverInjector.isCarryOverRow(id)) {
                                        if (selectionMode) {
                                            viewModel.onEvent(HomeUiEvent.OnToggleTransactionSelection(id))
                                        } else {
                                            onNavigateToTransactionDetail(id)
                                        }
                                    }
                                }
                                val onRowLongClick: (Long) -> Unit = { id ->
                                    if (!HomeCarryOverInjector.isCarryOverRow(id) &&
                                        !selectionMode &&
                                        selectionListTab
                                    ) {
                                        viewModel.onEvent(HomeUiEvent.OnEnterSelectionMode)
                                        viewModel.onEvent(HomeUiEvent.OnToggleTransactionSelection(id))
                                    }
                                }
                                when (uiState.selectedViewTab) {
                                    HomeViewTab.CALENDAR ->
                                        HomeCalendarTabContent(
                                            cells = uiState.calendarCells,
                                            weekStart = uiState.calendarWeekStart,
                                            selectedDate = uiState.calendarSelectedDate,
                                            selectedDayGroup = uiState.calendarSelectedDayGroup,
                                            onDateSelected = {
                                                viewModel.onEvent(HomeUiEvent.OnCalendarDateSelected(it))
                                            },
                                            onPreviousDay = {
                                                viewModel.onEvent(HomeUiEvent.OnCalendarPreviousDay)
                                            },
                                            onNextDay = {
                                                viewModel.onEvent(HomeUiEvent.OnCalendarNextDay)
                                            },
                                            onAddForDay = {
                                                viewModel.calendarAddTransactionMillis()?.let { millis ->
                                                    onNavigateToAddTransaction(millis)
                                                } ?: onNavigateToAddTransaction(null)
                                            },
                                            onTransactionClick = onNavigateToTransactionDetail,
                                        )
                                    HomeViewTab.MONTHLY ->
                                        HomeMonthlyTabContent(
                                            periods = uiState.monthlyPeriods,
                                            expandedMonths = uiState.monthlyExpandedMonths,
                                            onMonthToggle = {
                                                viewModel.onEvent(HomeUiEvent.OnMonthlyMonthToggle(it))
                                            },
                                        )
                                    HomeViewTab.TOTAL -> {
                                        val totalSummary = uiState.totalAccountsSummary
                                        if (totalSummary != null) {
                                            HomeTotalTabContent(
                                                summary = totalSummary,
                                                groups = uiState.groupedTransactions,
                                                selectionMode = selectionMode,
                                                selectedIds = selectedIds,
                                                onItemClick = onRowClick,
                                                onItemLongClick = onRowLongClick,
                                                onExportClick = {
                                                    viewModel.onEvent(HomeUiEvent.OnExportPeriodClick)
                                                },
                                                modifier = Modifier.fillMaxSize(),
                                                listState = ledgerTotalListState,
                                            )
                                        } else {
                                            GroupedTransactionList(
                                                groups = uiState.groupedTransactions,
                                                selectionMode = selectionMode,
                                                selectedIds = selectedIds,
                                                onItemClick = onRowClick,
                                                onItemLongClick = onRowLongClick,
                                                listState = ledgerTotalListState,
                                            )
                                        }
                                    }
                                    else ->
                                        GroupedTransactionList(
                                            groups = uiState.groupedTransactions,
                                            selectionMode = selectionMode,
                                            selectedIds = selectedIds,
                                            onItemClick = onRowClick,
                                            onItemLongClick = onRowLongClick,
                                            listState = ledgerListState,
                                        )
                                }
                            }
                        }
                    }
                }
            }
            if (!uiState.isSelectionMode) {
                MmrAdSlot(
                    placement = homeMrecPlacement,
                    modifier =
                        Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = MmrDimens.homeDailyCentreBannerBottomAboveFab),
                )
            }
        }
    }
}

@Composable
private fun HomeFilterChipsRow(
    uiState: HomeUiState,
    onEvent: (HomeUiEvent) -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = MmrDimens.listItemPaddingHorizontal, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(MmrDimens.spaceXs),
    ) {
        MmrFilterChip(
            label = "All",
            selected = uiState.selectedTypeFilter == HomeTypeFilter.ALL,
            onClick = { onEvent(HomeUiEvent.OnTypeFilterSelected(HomeTypeFilter.ALL)) },
        )
        MmrFilterChip(
            label = "Expenses",
            selected = uiState.selectedTypeFilter == HomeTypeFilter.EXPENSE,
            onClick = { onEvent(HomeUiEvent.OnTypeFilterSelected(HomeTypeFilter.EXPENSE)) },
        )
        MmrFilterChip(
            label = "Income",
            selected = uiState.selectedTypeFilter == HomeTypeFilter.INCOME,
            onClick = { onEvent(HomeUiEvent.OnTypeFilterSelected(HomeTypeFilter.INCOME)) },
        )
        uiState.categories.forEach { category ->
            MmrFilterChip(
                label = category,
                selected = uiState.selectedCategoryFilter == category,
                onClick = { onEvent(HomeUiEvent.OnCategoryFilterSelected(category)) },
            )
        }
    }
}

@Composable
private fun HomeDailyMrecPane(
    uiState: HomeUiState,
    emptyMessage: String,
    selectionListTab: Boolean,
    onNavigateToTransactionDetail: (Long) -> Unit,
    onEvent: (HomeUiEvent) -> Unit,
    ledgerListState: LazyListState,
) {
    MmrAdaptiveWidthBox(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (uiState.groupedTransactions.isEmpty()) {
                MmrEmptyState(
                    message = emptyMessage,
                    modifier =
                        Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                )
            } else {
                val selectionMode = uiState.isSelectionMode
                val selectedIds = uiState.selectedTransactionIds
                GroupedTransactionList(
                    groups = uiState.groupedTransactions,
                    selectionMode = selectionMode,
                    selectedIds = selectedIds,
                    listState = ledgerListState,
                    onItemClick = { id ->
                        if (!HomeCarryOverInjector.isCarryOverRow(id)) {
                            if (selectionMode) {
                                onEvent(HomeUiEvent.OnToggleTransactionSelection(id))
                            } else {
                                onNavigateToTransactionDetail(id)
                            }
                        }
                    },
                    onItemLongClick = { id ->
                        if (!HomeCarryOverInjector.isCarryOverRow(id) &&
                            !selectionMode &&
                            selectionListTab
                        ) {
                            onEvent(HomeUiEvent.OnEnterSelectionMode)
                            onEvent(HomeUiEvent.OnToggleTransactionSelection(id))
                        }
                    },
                    contentBottomPadding = MmrDimens.homeDailyCentreBannerListBottomPadding,
                    modifier =
                        Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                )
            }
        }
    }
}

private fun emptyMessage(uiState: HomeUiState): String {
    val filtered =
        uiState.selectedCategoryFilter != null ||
            uiState.selectedTypeFilter != HomeTypeFilter.ALL
    return when (uiState.selectedViewTab) {
        HomeViewTab.CALENDAR -> "No activity this month."
        HomeViewTab.MONTHLY -> "No transactions in the last 12 months."
        HomeViewTab.TOTAL ->
            if (filtered) {
                "No transactions match your search or filters."
            } else {
                "No transactions yet.\nTap '+' to add your first transaction."
            }
        else ->
            if (filtered) {
                "No transactions match your search or filters this month."
            } else {
                "No transactions this month.\nTap '+' to add one."
            }
    }
}
