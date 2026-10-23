package com.mmr.feature.home

import android.content.Intent
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.outlined.CallMerge
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.mmr.app.R
import com.mmr.app.testing.MmrTestTags
import com.mmr.app.ui.adaptive.MmrAdaptiveWidthBox
import com.mmr.app.ui.components.MmrConfirmDialog
import com.mmr.app.ui.components.MmrDimens
import com.mmr.app.ui.components.MmrInputDialog
import com.mmr.app.ui.components.MmrMmFilterChip
import com.mmr.app.ui.components.MmrNestedScaffoldInsets
import com.mmr.app.ui.components.MmrPeriodSwipeBox
import com.mmr.app.ui.components.MmrTopAppBar
import com.mmr.app.ui.state.MmrEmptyState
import com.mmr.app.ui.state.MmrErrorState
import com.mmr.app.ui.state.MmrLoadingState
import com.mmr.app.ui.theme.MmrMmColors
import com.mmr.feature.home.model.*
import com.mmr.feature.home.ui.HomeCalendarTabContent
import com.mmr.feature.home.ui.HomeExcelExportPeriodDialog
import com.mmr.feature.home.ui.HomeMonthPickerDialog
import com.mmr.feature.home.ui.HomeMonthTopBar
import com.mmr.feature.home.ui.HomeMonthlyTabContent
import com.mmr.feature.home.ui.HomePeriodSummaryStrip
import com.mmr.feature.home.ui.HomeTotalTabContent
import com.mmr.feature.home.ui.HomeViewTabRow
import com.mmr.feature.home.ui.TransactionCompactRow
import com.mmr.feature.home.ui.TransactionDayGroupSeparator
import com.mmr.feature.home.ui.TransactionDayHeaderRow
import com.mmr.feature.home.ui.TransactionSelectionRowCard

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

    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.openTransactionId.collect { id ->
            onNavigateToTransactionDetail(id)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.periodExportRequest.collect { request ->
            val share =
                Intent(Intent.ACTION_SEND).apply {
                    type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                    putExtra(Intent.EXTRA_STREAM, request.contentUri)
                    putExtra(Intent.EXTRA_SUBJECT, request.subject)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            context.startActivity(Intent.createChooser(share, "Export data to Excel"))
        }
    }

    HomeMonthPickerDialog(
        visible = uiState.showMonthPicker,
        pickerYear = uiState.monthPickerYear,
        selectedYear = uiState.selectedYear,
        selectedMonth = uiState.selectedMonth,
        onDismiss = { viewModel.onEvent(HomeUiEvent.OnDismissMonthPicker) },
        onPreviousYear = { viewModel.onEvent(HomeUiEvent.OnMonthPickerPreviousYear) },
        onNextYear = { viewModel.onEvent(HomeUiEvent.OnMonthPickerNextYear) },
        onMonthSelected = { viewModel.onEvent(HomeUiEvent.OnMonthPickerMonthSelected(it)) },
        onThisMonth = { viewModel.onEvent(HomeUiEvent.OnMonthPickerThisMonth) },
    )

    if (uiState.showMergeDialog) {
        HomeMergeDialog(
            name = uiState.mergeNameInput,
            errorMessage = uiState.mergeErrorMessage,
            onNameChange = { viewModel.onEvent(HomeUiEvent.OnMergeNameChanged(it)) },
            onDismiss = { viewModel.onEvent(HomeUiEvent.OnDismissMergeDialog) },
            onConfirm = { viewModel.onEvent(HomeUiEvent.OnConfirmMerge) },
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
    val periodSwipeEnabled = !uiState.isSelectionMode && uiState.monthSwipeEnabled

    Scaffold(
        containerColor = MmrMmColors.ListBackground,
        contentWindowInsets = MmrNestedScaffoldInsets,
        topBar = {
            if (uiState.isSelectionMode) {
                MmrTopAppBar(
                    title = "${uiState.selectedTransactionIds.size} selected",
                    showBackNavigation = true,
                    onNavigateBack = { viewModel.onEvent(HomeUiEvent.OnExitSelectionMode) },
                    useMmHeaderStyle = true,
                    useMmCompactTitle = true,
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
                        IconButton(
                            onClick = { viewModel.onEvent(HomeUiEvent.OnOpenDeleteConfirmDialog) },
                            enabled = uiState.canDeleteSelection,
                            modifier = Modifier.testTag(MmrTestTags.homeSelectionDelete),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.DeleteOutline,
                                contentDescription = "Delete selected",
                            )
                        }
                        IconButton(
                            onClick = { viewModel.onEvent(HomeUiEvent.OnOpenMergeDialog) },
                            enabled = uiState.canMergeSelection,
                            modifier = Modifier.testTag(MmrTestTags.homeSelectionMerge),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CallMerge,
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
                SmallFloatingActionButton(
                    modifier = Modifier.testTag(MmrTestTags.homeFabAdd),
                    onClick = {
                        val prefill =
                            if (uiState.selectedViewTab == HomeViewTab.CALENDAR) {
                                viewModel.calendarAddTransactionMillis()
                            } else {
                                null
                            }
                        onNavigateToAddTransaction(prefill)
                    },
                    containerColor = MmrMmColors.Active,
                    contentColor = Color.White,
                ) {
                    Text("+", style = MaterialTheme.typography.titleMedium)
                }
            }
        },
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            if (!uiState.isSelectionMode) {
                HomeViewTabRow(
                    selectedTab = uiState.selectedViewTab,
                    onTabSelected = { viewModel.onEvent(HomeUiEvent.OnViewTabSelected(it)) },
                )

                HomePeriodSummaryStrip(
                    summary = uiState.summary,
                    amountColorScheme = uiState.amountColorScheme,
                )

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
                                if (selectionMode) {
                                    viewModel.onEvent(HomeUiEvent.OnToggleTransactionSelection(id))
                                } else {
                                    onNavigateToTransactionDetail(id)
                                }
                            }
                            val onRowLongClick: (Long) -> Unit = { id ->
                                if (!selectionMode && selectionListTab) {
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
                                        )
                                    } else {
                                        HomeGroupedTransactionList(
                                            groups = uiState.groupedTransactions,
                                            selectionMode = selectionMode,
                                            selectedIds = selectedIds,
                                            onItemClick = onRowClick,
                                            onItemLongClick = onRowLongClick,
                                        )
                                    }
                                }
                                else ->
                                    HomeGroupedTransactionList(
                                        groups = uiState.groupedTransactions,
                                        selectionMode = selectionMode,
                                        selectedIds = selectedIds,
                                        onItemClick = onRowClick,
                                        onItemLongClick = onRowLongClick,
                                    )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeMergeDialog(
    name: String,
    errorMessage: String?,
    onNameChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    MmrInputDialog(
        title = "Merge transactions",
        message = "Combined amount and date come from the selected items. Linked SMS stay attached to the merged transaction.",
        value = name,
        onValueChange = onNameChange,
        onConfirm = onConfirm,
        onDismiss = onDismiss,
        label = "Name",
        placeholder = "Merged transaction name",
        errorMessage = errorMessage,
        confirmLabel = "Merge",
        confirmEnabled = name.trim().isNotEmpty(),
    )
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
        MmrMmFilterChip(
            label = "All",
            selected = uiState.selectedTypeFilter == HomeTypeFilter.ALL,
            onClick = { onEvent(HomeUiEvent.OnTypeFilterSelected(HomeTypeFilter.ALL)) },
        )
        MmrMmFilterChip(
            label = "Expenses",
            selected = uiState.selectedTypeFilter == HomeTypeFilter.EXPENSE,
            onClick = { onEvent(HomeUiEvent.OnTypeFilterSelected(HomeTypeFilter.EXPENSE)) },
        )
        MmrMmFilterChip(
            label = "Income",
            selected = uiState.selectedTypeFilter == HomeTypeFilter.INCOME,
            onClick = { onEvent(HomeUiEvent.OnTypeFilterSelected(HomeTypeFilter.INCOME)) },
        )
        uiState.categories.forEach { category ->
            MmrMmFilterChip(
                label = category,
                selected = uiState.selectedCategoryFilter == category,
                onClick = { onEvent(HomeUiEvent.OnCategoryFilterSelected(category)) },
            )
        }
    }
}

@Composable
private fun HomeGroupedTransactionList(
    groups: List<TransactionGroupUi>,
    selectionMode: Boolean,
    selectedIds: Set<Long>,
    onItemClick: (Long) -> Unit,
    onItemLongClick: (Long) -> Unit,
    listModifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = listModifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = MmrDimens.fabListBottomInset, top = 0.dp),
    ) {
        groups.forEachIndexed { groupIndex, group ->
            if (groupIndex > 0) {
                item(key = "day-sep-${group.dateKey}") {
                    TransactionDayGroupSeparator()
                }
            }
            item(key = "header-${group.dateKey}") {
                TransactionDayHeaderRow(group = group)
            }
            items(group.items, key = { it.id }) { item ->
                if (selectionMode) {
                    TransactionSelectionRowCard(
                        item = item,
                        selected = item.id in selectedIds,
                        onClick = { onItemClick(item.id) },
                        onLongClick = { onItemLongClick(item.id) },
                    )
                } else {
                    TransactionCompactRow(
                        item = item,
                        onClick = { onItemClick(item.id) },
                        onLongClick = { onItemLongClick(item.id) },
                    )
                }
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
