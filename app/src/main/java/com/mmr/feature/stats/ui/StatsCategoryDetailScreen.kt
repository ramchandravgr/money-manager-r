package com.mmr.feature.stats.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mmr.app.ui.components.MmrDimens
import com.mmr.app.ui.components.MmrMmHeaderBar
import com.mmr.app.ui.components.MmrMmPeriodChip
import com.mmr.app.ui.components.MmrPeriodSwipeBox
import com.mmr.app.ui.state.MmrEmptyState
import com.mmr.app.ui.state.MmrErrorState
import com.mmr.app.ui.state.MmrLoadingState
import com.mmr.app.ui.theme.MmrMmColors
import com.mmr.feature.home.ui.HomeMonthPickerDialog
import com.mmr.feature.home.ui.TransactionCompactRow
import com.mmr.feature.home.ui.TransactionDayHeaderRow
import com.mmr.feature.home.ui.TransactionSelectionRowCard
import com.mmr.feature.stats.StatsPeriodUtils
import com.mmr.feature.stats.model.StatsCategoryDetailUiEvent
import com.mmr.feature.stats.model.StatsPeriodMode
import com.mmr.feature.stats.model.SubcategoryBreakdownUi
import com.mmr.feature.stats.viewmodel.StatsCategoryDetailViewModel
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun StatsCategoryDetailScreen(
    onNavigateBack: () -> Unit,
    onNavigateToTransaction: (Long) -> Unit,
    onSwitchCategory: (Long) -> Unit = {},
    viewModel: StatsCategoryDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val zoneId = ZoneId.systemDefault()
    val categoryColor = parseHexColor(uiState.categoryColorHex)
    val slashDate = rememberSlashDateFormatter()

    if (uiState.showPeriodStartPicker) {
        StatsDatePickerDialog(
            initialMillis =
                uiState.periodStartDate
                    ?.atStartOfDay(zoneId)
                    ?.toInstant()
                    ?.toEpochMilli(),
            onDismiss = { viewModel.onEvent(StatsCategoryDetailUiEvent.OnDismissPeriodStartPicker) },
            onConfirm = { viewModel.onEvent(StatsCategoryDetailUiEvent.OnCustomPeriodStartSelected(it)) },
        )
    }
    if (uiState.showPeriodEndPicker) {
        StatsDatePickerDialog(
            initialMillis =
                uiState.periodEndDate
                    ?.atStartOfDay(zoneId)
                    ?.toInstant()
                    ?.toEpochMilli(),
            onDismiss = { viewModel.onEvent(StatsCategoryDetailUiEvent.OnDismissPeriodEndPicker) },
            onConfirm = { viewModel.onEvent(StatsCategoryDetailUiEvent.OnCustomPeriodEndSelected(it)) },
        )
    }

    if (uiState.showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.onEvent(StatsCategoryDetailUiEvent.OnDismissDeleteConfirm) },
            title = { Text("Delete transactions?") },
            text = { Text("Delete ${uiState.selectedTransactionIds.size} selected transaction(s)?") },
            confirmButton = {
                TextButton(onClick = { viewModel.onEvent(StatsCategoryDetailUiEvent.OnConfirmDeleteSelected) }) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onEvent(StatsCategoryDetailUiEvent.OnDismissDeleteConfirm) }) {
                    Text("Cancel")
                }
            },
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
        onDismiss = { viewModel.onEvent(StatsCategoryDetailUiEvent.OnDismissMonthPicker) },
        onPreviousYear = { viewModel.onEvent(StatsCategoryDetailUiEvent.OnMonthPickerPreviousYear) },
        onNextYear = { viewModel.onEvent(StatsCategoryDetailUiEvent.OnMonthPickerNextYear) },
        onMonthSelected = { viewModel.onEvent(StatsCategoryDetailUiEvent.OnMonthPickerMonthSelected(it)) },
        onThisMonth = { viewModel.onEvent(StatsCategoryDetailUiEvent.OnMonthPickerThisMonth) },
    )

    val periodSwipeEnabled = uiState.canNavigatePeriod && uiState.periodSwipeEnabled

    val periodTitleContent: (@Composable () -> Unit)? =
        if (uiState.periodMode == StatsPeriodMode.PERIOD) {
            {
                val startLabel = uiState.periodStartDate?.let { slashDate.format(it) } ?: "Start"
                val endLabel = uiState.periodEndDate?.let { slashDate.format(it) } ?: "End"
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Text(
                        text = startLabel,
                        color = MmrMmColors.HeaderIcon,
                        fontSize = 15.sp,
                        modifier =
                            Modifier.clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { viewModel.onEvent(StatsCategoryDetailUiEvent.OnPeriodStartFieldClick) },
                            ),
                    )
                    Text(text = " ~ ", color = MmrMmColors.HeaderIcon, fontSize = 15.sp)
                    Text(
                        text = endLabel,
                        color = MmrMmColors.HeaderIcon,
                        fontSize = 15.sp,
                        modifier =
                            Modifier.clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { viewModel.onEvent(StatsCategoryDetailUiEvent.OnPeriodEndFieldClick) },
                            ),
                    )
                }
            }
        } else {
            null
        }

    Scaffold(
        containerColor = MmrMmColors.ListBackground,
        topBar = {
            if (uiState.isSelectionMode) {
                TopAppBar(
                    title = { Text("${uiState.selectedTransactionIds.size} selected") },
                    navigationIcon = {
                        TextButton(onClick = { viewModel.onEvent(StatsCategoryDetailUiEvent.OnExitSelectionMode) }) {
                            Text("Cancel")
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { viewModel.onEvent(StatsCategoryDetailUiEvent.OnSelectAllTransactions) },
                        ) {
                            Icon(Icons.Default.SelectAll, contentDescription = "Select all")
                        }
                        IconButton(
                            onClick = { viewModel.onEvent(StatsCategoryDetailUiEvent.OnRequestDeleteSelected) },
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MmrMmColors.Header),
                )
            } else {
                StatsCategoryDetailTopBar(
                    uiState = uiState,
                    periodTitleContent = periodTitleContent,
                    onNavigateBack = onNavigateBack,
                    onPrevious = { viewModel.onEvent(StatsCategoryDetailUiEvent.OnPreviousPeriod) },
                    onNext = { viewModel.onEvent(StatsCategoryDetailUiEvent.OnNextPeriod) },
                    onPeriodModeSelected = {
                        viewModel.onEvent(StatsCategoryDetailUiEvent.OnPeriodModeSelected(it))
                    },
                    onTitleClick =
                        if (uiState.periodMode != StatsPeriodMode.PERIOD) {
                            { viewModel.onEvent(StatsCategoryDetailUiEvent.OnOpenMonthPicker) }
                        } else {
                            null
                        },
                )
            }
        },
        bottomBar = {
            if (uiState.isSelectionMode && uiState.siblingCategories.isNotEmpty()) {
                StatsCategoryPickerBar(
                    categories = uiState.siblingCategories,
                    selectedCategoryId = uiState.categoryId,
                    onCategorySelected = { id ->
                        viewModel.onEvent(StatsCategoryDetailUiEvent.OnRecategorizeSelected(id))
                    },
                )
            } else if (!uiState.isSelectionMode) {
                StatsCategoryPickerBar(
                    categories = uiState.siblingCategories,
                    selectedCategoryId = uiState.categoryId,
                    onCategorySelected = { id ->
                        if (id != uiState.categoryId) {
                            onSwitchCategory(id)
                        }
                    },
                )
            }
        },
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
        ) {
            if (!uiState.isSelectionMode) {
                Text(
                    text = uiState.categoryName,
                    style = MaterialTheme.typography.titleMedium,
                    modifier =
                        Modifier.padding(
                            horizontal = MmrDimens.statsCategoryRowStartMargin,
                            vertical = MmrDimens.spaceSm,
                        ),
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
            }

            MmrPeriodSwipeBox(
                periodKey = uiState.periodStartMillis,
                enabled = periodSwipeEnabled,
                onPrevious = { viewModel.onEvent(StatsCategoryDetailUiEvent.OnPreviousPeriod) },
                onNext = { viewModel.onEvent(StatsCategoryDetailUiEvent.OnNextPeriod) },
                modifier =
                    Modifier
                        .weight(1f)
                        .fillMaxWidth(),
            ) {
                when {
                    uiState.isLoading -> MmrLoadingState(modifier = Modifier.fillMaxSize())
                    uiState.errorMessage != null ->
                        MmrErrorState(
                            message = uiState.errorMessage ?: "Something went wrong.",
                            onRetry = { viewModel.onEvent(StatsCategoryDetailUiEvent.OnRetryClick) },
                            modifier = Modifier.fillMaxSize(),
                        )
                    else -> {
                        StatsPullToRefreshContainer(
                            isRefreshing = uiState.isRefreshing,
                            onRefresh = { viewModel.onEvent(StatsCategoryDetailUiEvent.OnRefresh) },
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(bottom = MmrDimens.spaceLg),
                            ) {
                                item {
                                    StatsDetailTotalBlock(amountFormatted = uiState.totalFormatted)
                                }
                                items(uiState.subcategoryBreakdowns, key = { it.subcategoryId ?: "all" }) { row ->
                                    StatsMmSubcategoryRow(
                                        row = row,
                                        selected = isSubcategorySelected(row, uiState.selectedSubcategoryId),
                                        accentColor = categoryColor,
                                        onClick = {
                                            val id = if (row.name == "All") null else row.subcategoryId
                                            viewModel.onEvent(StatsCategoryDetailUiEvent.OnSubcategorySelected(id))
                                        },
                                    )
                                }
                                item {
                                    StatsWebLineChart(
                                        points = uiState.chartPoints,
                                        lineColor = categoryColor,
                                        modifier = Modifier.padding(vertical = MmrDimens.spaceXs),
                                    )
                                }
                                if (uiState.isEmpty) {
                                    item { MmrEmptyState(message = "No transactions in this period.") }
                                } else {
                                    uiState.groupedTransactions.forEach { group ->
                                        item(key = "header-${group.dateKey}") {
                                            TransactionDayHeaderRow(
                                                group = group,
                                                modifier =
                                                    Modifier.padding(
                                                        horizontal = MmrDimens.listItemPaddingHorizontal,
                                                    ),
                                            )
                                        }
                                        items(group.items, key = { it.id }) { txn ->
                                            if (uiState.isSelectionMode) {
                                                TransactionSelectionRowCard(
                                                    item = txn,
                                                    selected = txn.id in uiState.selectedTransactionIds,
                                                    onClick = {
                                                        viewModel.onEvent(
                                                            StatsCategoryDetailUiEvent.OnToggleTransactionSelection(
                                                                txn.id,
                                                            ),
                                                        )
                                                    },
                                                    onLongClick = {},
                                                    modifier =
                                                        Modifier.padding(
                                                            horizontal = MmrDimens.listItemPaddingHorizontal,
                                                        ),
                                                )
                                            } else {
                                                TransactionCompactRow(
                                                    item = txn,
                                                    onClick = { onNavigateToTransaction(txn.id) },
                                                    onLongClick = {
                                                        viewModel.onEvent(
                                                            StatsCategoryDetailUiEvent.OnEnterSelectionMode,
                                                        )
                                                        viewModel.onEvent(
                                                            StatsCategoryDetailUiEvent.OnToggleTransactionSelection(
                                                                txn.id,
                                                            ),
                                                        )
                                                    },
                                                    modifier =
                                                        Modifier.padding(
                                                            horizontal = MmrDimens.listItemPaddingHorizontal,
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
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StatsCategoryDetailTopBar(
    uiState: com.mmr.feature.stats.model.StatsCategoryDetailUiState,
    periodTitleContent: (@Composable () -> Unit)?,
    onNavigateBack: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onPeriodModeSelected: (StatsPeriodMode) -> Unit,
    onTitleClick: (() -> Unit)?,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    MmrMmHeaderBar(
        title = if (uiState.periodMode == StatsPeriodMode.PERIOD) "" else uiState.navigationLabel,
        titleContent = periodTitleContent,
        onTitleClick = onTitleClick,
        showBack = true,
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
                MmrMmPeriodChip(
                    label = StatsPeriodUtils.modeDropdownLabel(uiState.periodMode),
                    onClick = { menuExpanded = true },
                )
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    StatsPeriodMode.entries.forEach { mode ->
                        DropdownMenuItem(
                            text = { Text(StatsPeriodUtils.modeDropdownLabel(mode)) },
                            onClick = {
                                menuExpanded = false
                                onPeriodModeSelected(mode)
                            },
                        )
                    }
                }
            }
        },
    )
}

private fun isSubcategorySelected(
    row: SubcategoryBreakdownUi,
    selectedSubcategoryId: Long?,
): Boolean =
    if (row.name == "All") {
        selectedSubcategoryId == null
    } else {
        selectedSubcategoryId == row.subcategoryId
    }

@Composable
private fun rememberSlashDateFormatter(): DateTimeFormatter =
    DateTimeFormatter.ofPattern("dd/MM/yy", Locale.getDefault())
