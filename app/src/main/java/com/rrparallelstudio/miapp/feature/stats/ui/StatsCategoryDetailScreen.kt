package com.rrparallelstudio.miapp.feature.stats.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.rrparallelstudio.miapp.R
import com.rrparallelstudio.miapp.feature.home.ui.HomeMonthPickerDialog
import com.rrparallelstudio.miapp.feature.home.ui.TransactionCompactRow
import com.rrparallelstudio.miapp.feature.home.ui.TransactionDayHeaderRow
import com.rrparallelstudio.miapp.feature.home.ui.TransactionSelectionRowCard
import com.rrparallelstudio.miapp.feature.stats.StatsPeriodUtils
import com.rrparallelstudio.miapp.feature.stats.model.StatsCategoryDetailUiEvent
import com.rrparallelstudio.miapp.feature.stats.model.StatsPeriodMode
import com.rrparallelstudio.miapp.feature.stats.viewmodel.StatsCategoryDetailViewModel
import com.rrparallelstudio.miapp.ui.components.MmrDeleteIconButton
import com.rrparallelstudio.miapp.ui.components.MmrDimens
import com.rrparallelstudio.miapp.ui.components.MmrHeaderBar
import com.rrparallelstudio.miapp.ui.components.MmrPeriodChip
import com.rrparallelstudio.miapp.ui.components.MmrPeriodSwipeBox
import com.rrparallelstudio.miapp.ui.state.MmrEmptyState
import com.rrparallelstudio.miapp.ui.state.MmrErrorState
import com.rrparallelstudio.miapp.ui.state.MmrLoadingState
import com.rrparallelstudio.miapp.ui.theme.MmrBrandColors
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
    val periodSwipeKey by viewModel.periodSwipeKey.collectAsState()
    com.rrparallelstudio.miapp.ui.components.MmrSelectionModeBackHandler(
        enabled = uiState.isSelectionMode,
        onExitSelectionMode = { viewModel.onEvent(StatsCategoryDetailUiEvent.OnExitSelectionMode) },
    )
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
        selectedYear = anchorMonth.year,
        selectedMonth = anchorMonth.monthValue,
        onDismiss = { viewModel.onEvent(StatsCategoryDetailUiEvent.OnDismissMonthPicker) },
        onPeriodSelected = { year, month ->
            viewModel.onEvent(StatsCategoryDetailUiEvent.OnMonthPickerPeriodSelected(year, month))
        },
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
                        color = MmrBrandColors.HeaderIcon,
                        fontSize = 15.sp,
                        modifier =
                            Modifier.clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { viewModel.onEvent(StatsCategoryDetailUiEvent.OnPeriodStartFieldClick) },
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
                                onClick = { viewModel.onEvent(StatsCategoryDetailUiEvent.OnPeriodEndFieldClick) },
                            ),
                    )
                }
            }
        } else {
            null
        }

    Scaffold(
        containerColor = MmrBrandColors.ScreenContainer,
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
                        MmrDeleteIconButton(
                            onClick = { viewModel.onEvent(StatsCategoryDetailUiEvent.OnRequestDeleteSelected) },
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MmrBrandColors.Header),
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
            } else if (!uiState.isSelectionMode && !uiState.isEmpty) {
                StatsCategoryDetailBottomChrome(
                    categoryName = uiState.categoryName,
                    categoryColor = categoryColor,
                    siblingCategories = uiState.siblingCategories,
                    selectedCategoryId = uiState.categoryId,
                    onCategorySelected = { id ->
                        if (id != uiState.categoryId) {
                            onSwitchCategory(id)
                        }
                    },
                    groupedTransactions = uiState.groupedTransactions,
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
            MmrPeriodSwipeBox(
                periodKey = periodSwipeKey,
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
                            val emptyMessage = stringResource(R.string.stats_no_transactions_in_period)
                            if (uiState.isEmpty && !uiState.isSelectionMode) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    MmrEmptyState(message = emptyMessage)
                                }
                            } else {
                                val txCount =
                                    uiState.groupedTransactions.sumOf { it.items.size }
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(bottom = MmrDimens.spaceLg),
                                    verticalArrangement = Arrangement.spacedBy(MmrDimens.spaceSm),
                                ) {
                                    if (!uiState.isSelectionMode) {
                                        item(key = "hero") {
                                            StatsCategoryDetailHero(
                                                categoryName = uiState.categoryName,
                                                categoryColor = categoryColor,
                                                totalFormatted = uiState.totalFormatted,
                                            )
                                        }
                                    }
                                    item(key = "subcats") {
                                        StatsSubcategoryBreakdownCard(
                                            rows = uiState.subcategoryBreakdowns,
                                            accentColor = categoryColor,
                                            selectedSubcategoryId = uiState.selectedSubcategoryId,
                                            onSubcategoryClick = { id ->
                                                viewModel.onEvent(StatsCategoryDetailUiEvent.OnSubcategorySelected(id))
                                            },
                                        )
                                    }
                                    item(key = "trend") {
                                        StatsCategorySpendingBarChart(
                                            points = uiState.chartPoints,
                                            accentColor = categoryColor,
                                            modifier = Modifier.padding(top = MmrDimens.spaceXs),
                                        )
                                    }
                                    item(key = "tx-header") {
                                        StatsCategoryTransactionsSectionHeader(recordCount = txCount)
                                    }
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
                                        items(
                                            count = group.items.size,
                                            key = { group.items[it].id },
                                        ) { index ->
                                            val txn = group.items[index]
                                            val isLastInDay = index == group.items.lastIndex
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
                                                    isLastInDayGroup = isLastInDay,
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
                                                    isLastInDayGroup = isLastInDay,
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
    uiState: com.rrparallelstudio.miapp.feature.stats.model.StatsCategoryDetailUiState,
    periodTitleContent: (@Composable () -> Unit)?,
    onNavigateBack: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onPeriodModeSelected: (StatsPeriodMode) -> Unit,
    onTitleClick: (() -> Unit)?,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    MmrHeaderBar(
        title = if (uiState.periodMode == StatsPeriodMode.PERIOD) "" else uiState.navigationLabel,
        titleContent = periodTitleContent,
        onTitleClick = onTitleClick,
        centerPeriodChrome = true,
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
                                onPeriodModeSelected(mode)
                            },
                        )
                    }
                }
            }
        },
    )
}

@Composable
private fun rememberSlashDateFormatter(): DateTimeFormatter =
    DateTimeFormatter.ofPattern("dd/MM/yy", Locale.getDefault())
