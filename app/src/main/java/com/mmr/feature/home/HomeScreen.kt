package com.mmr.feature.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mmr.app.ui.adaptive.MmrAdaptiveWidthBox
import com.mmr.app.ui.adaptive.mmrScreenHorizontalPadding
import com.mmr.app.ui.state.MmrEmptyState
import com.mmr.app.ui.state.MmrErrorState
import com.mmr.app.ui.state.MmrLoadingState
import com.mmr.feature.home.model.*
import com.mmr.feature.home.ui.HomeMonthPickerDialog
import com.mmr.feature.home.ui.HomeMonthTopBar
import com.mmr.feature.home.ui.HomePeriodSummaryStrip
import com.mmr.feature.home.ui.HomeViewTabRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToAddTransaction: () -> Unit,
    onNavigateToEditTransaction: (Long) -> Unit,
    onNavigateToApprovals: () -> Unit,
    onNavigateToSearch: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    HomeMonthPickerDialog(
        visible = uiState.showMonthPicker,
        pickerYear = uiState.monthPickerYear,
        selectedYear = uiState.selectedYear,
        selectedMonth = uiState.selectedMonth,
        onDismiss = { viewModel.onEvent(HomeUiEvent.OnDismissMonthPicker) },
        onPreviousYear = { viewModel.onEvent(HomeUiEvent.OnMonthPickerPreviousYear) },
        onNextYear = { viewModel.onEvent(HomeUiEvent.OnMonthPickerNextYear) },
        onMonthSelected = { viewModel.onEvent(HomeUiEvent.OnMonthPickerMonthSelected(it)) },
        onThisMonth = { viewModel.onEvent(HomeUiEvent.OnMonthPickerThisMonth) }
    )

    Scaffold(
        topBar = {
            HomeMonthTopBar(
                monthTitle = uiState.monthTitle,
                onPreviousMonth = { viewModel.onEvent(HomeUiEvent.OnPreviousMonth) },
                onNextMonth = { viewModel.onEvent(HomeUiEvent.OnNextMonth) },
                onMonthTitleClick = { viewModel.onEvent(HomeUiEvent.OnOpenMonthPicker) },
                onSearchClick = onNavigateToSearch,
                pendingApprovalCount = uiState.pendingApprovalCount,
                onApprovalsClick = onNavigateToApprovals
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNavigateToAddTransaction) {
                Text("+", style = MaterialTheme.typography.titleLarge)
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            HomeViewTabRow(
                selectedTab = uiState.selectedViewTab,
                onTabSelected = { viewModel.onEvent(HomeUiEvent.OnViewTabSelected(it)) }
            )

            HomePeriodSummaryStrip(summary = uiState.summary)

            if (uiState.selectedViewTab == HomeViewTab.DAILY || uiState.selectedViewTab == HomeViewTab.TOTAL) {
                HomeFilterChipsRow(uiState = uiState, onEvent = viewModel::onEvent)
            }

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when {
                    uiState.isLoading -> MmrLoadingState()

                    uiState.errorMessage != null -> {
                        MmrErrorState(
                            message = uiState.errorMessage ?: "Something went wrong.",
                            onRetry = { viewModel.onEvent(HomeUiEvent.OnRetryClick) }
                        )
                    }

                    uiState.isEmpty -> {
                        MmrEmptyState(message = emptyMessage(uiState))
                    }

                    else -> {
                        MmrAdaptiveWidthBox(modifier = Modifier.fillMaxSize()) {
                            when (uiState.selectedViewTab) {
                                HomeViewTab.CALENDAR -> HomeCalendarContent(uiState.calendarDays)
                                HomeViewTab.MONTHLY -> HomeMonthlyContent(uiState.monthlyCategoryRows)
                                HomeViewTab.NOTE -> HomeNoteListContent(
                                    items = uiState.noteTransactions,
                                    onItemClick = onNavigateToEditTransaction
                                )
                                else -> HomeGroupedTransactionList(
                                    groups = uiState.groupedTransactions,
                                    onItemClick = onNavigateToEditTransaction
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
private fun HomeFilterChipsRow(
    uiState: HomeUiState,
    onEvent: (HomeUiEvent) -> Unit
) {
    val horizontalPadding = mmrScreenHorizontalPadding()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = horizontalPadding, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = uiState.selectedTypeFilter == HomeTypeFilter.ALL,
            onClick = { onEvent(HomeUiEvent.OnTypeFilterSelected(HomeTypeFilter.ALL)) },
            label = { Text("All") }
        )
        FilterChip(
            selected = uiState.selectedTypeFilter == HomeTypeFilter.EXPENSE,
            onClick = { onEvent(HomeUiEvent.OnTypeFilterSelected(HomeTypeFilter.EXPENSE)) },
            label = { Text("Expenses") }
        )
        FilterChip(
            selected = uiState.selectedTypeFilter == HomeTypeFilter.INCOME,
            onClick = { onEvent(HomeUiEvent.OnTypeFilterSelected(HomeTypeFilter.INCOME)) },
            label = { Text("Income") }
        )
        uiState.categories.forEach { category ->
            FilterChip(
                selected = uiState.selectedCategoryFilter == category,
                onClick = { onEvent(HomeUiEvent.OnCategoryFilterSelected(category)) },
                label = { Text(category) }
            )
        }
    }
}

@Composable
private fun HomeGroupedTransactionList(
    groups: List<TransactionGroupUi>,
    onItemClick: (Long) -> Unit
) {
    val horizontalPadding = mmrScreenHorizontalPadding()
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = horizontalPadding),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 80.dp, top = 4.dp)
    ) {
        groups.forEach { group ->
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = group.dateLabel,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    val dayNetFormatted =
                        "${if (group.dayTotal >= 0) "+₹" else "-₹"}${"%.2f".format(kotlin.math.abs(group.dayTotal))}"
                    Text(
                        text = dayNetFormatted,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
            items(group.items, key = { it.id }) { item ->
                TransactionRowCard(item = item, onClick = { onItemClick(item.id) })
            }
        }
    }
}

@Composable
private fun HomeNoteListContent(
    items: List<TransactionRowUi>,
    onItemClick: (Long) -> Unit
) {
    val horizontalPadding = mmrScreenHorizontalPadding()
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = horizontalPadding),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 80.dp, top = 8.dp)
    ) {
        items(items, key = { it.id }) { item ->
            TransactionRowCard(item = item, onClick = { onItemClick(item.id) })
        }
    }
}

@Composable
private fun HomeCalendarContent(days: List<CalendarDayUi>) {
    val horizontalPadding = mmrScreenHorizontalPadding()
    LazyVerticalGrid(
        columns = GridCells.Fixed(7),
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = horizontalPadding),
        contentPadding = PaddingValues(bottom = 80.dp, top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(days, key = { it.dayOfMonth }) { day ->
            val hasActivity = day.transactionCount > 0
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (hasActivity) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    }
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = day.label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (hasActivity) {
                        Text(
                            text = "${if (day.netTotal >= 0) "+" else ""}${"%.0f".format(day.netTotal)}",
                            style = MaterialTheme.typography.labelSmall,
                            textAlign = TextAlign.Center,
                            color = if (day.netTotal >= 0) Color(0xFF2563EB) else Color(0xFFEF4444)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeMonthlyContent(rows: List<com.mmr.core.database.model.CategorySpending>) {
    val horizontalPadding = mmrScreenHorizontalPadding()
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = horizontalPadding),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 80.dp, top = 8.dp)
    ) {
        items(
            rows,
            key = { row -> row.categoryId ?: row.categoryName?.hashCode()?.toLong() ?: row.hashCode().toLong() }
        ) { row ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = row.categoryName ?: "Uncategorized",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${row.transactionCount} transactions",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                    Text(
                        text = "₹${"%.2f".format(row.totalAmount)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFEF4444)
                    )
                }
            }
        }
    }
}

@Composable
private fun TransactionRowCard(
    item: TransactionRowUi,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${item.subtitle} • ${item.timeLabel}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
            Text(
                text = item.amountFormatted,
                style = MaterialTheme.typography.titleMedium,
                color = if (item.isIncome) Color(0xFF10B981) else Color(0xFFEF4444),
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private fun emptyMessage(uiState: HomeUiState): String {
    val filtered = uiState.selectedCategoryFilter != null ||
        uiState.selectedTypeFilter != HomeTypeFilter.ALL
    return when (uiState.selectedViewTab) {
        HomeViewTab.NOTE -> if (filtered) {
            "No notes match your search or filters this month."
        } else {
            "No transactions with notes this month."
        }
        HomeViewTab.CALENDAR -> "No activity this month."
        HomeViewTab.MONTHLY -> "No category spending this month."
        HomeViewTab.TOTAL -> if (filtered) {
            "No transactions match your search or filters."
        } else {
            "No transactions yet.\nTap '+' to add your first transaction."
        }
        else -> if (filtered) {
            "No transactions match your search or filters this month."
        } else {
            "No transactions this month.\nTap '+' to add one."
        }
    }
}
