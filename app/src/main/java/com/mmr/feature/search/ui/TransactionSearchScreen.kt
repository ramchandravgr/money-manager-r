package com.mmr.feature.search.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mmr.app.ui.state.MmrEmptyState
import com.mmr.app.ui.state.MmrErrorState
import com.mmr.app.ui.state.MmrLoadingState
import com.mmr.feature.home.HomePeriodUtils
import com.mmr.feature.home.model.TransactionGroupUi
import com.mmr.feature.home.model.TransactionRowUi
import com.mmr.feature.search.model.TransactionSearchUiEvent
import com.mmr.feature.search.viewmodel.TransactionSearchViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionSearchScreen(
    onNavigateBack: () -> Unit,
    onNavigateToEditTransaction: (Long) -> Unit,
    viewModel: TransactionSearchViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val categorySheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    if (state.showDateRangePicker) {
        SearchDateRangeDialog(
            initialStart = state.periodStart,
            initialEnd = state.periodEnd,
            onConfirm = { start, end ->
                viewModel.onEvent(TransactionSearchUiEvent.OnDateRangeSelected(start, end))
            },
            onDismiss = { viewModel.onEvent(TransactionSearchUiEvent.OnDismissDateRangePicker) }
        )
    }

    if (state.showCategoryPicker) {
        SearchCategoryPickerSheet(
            state = state,
            onEvent = viewModel::onEvent,
            sheetState = categorySheetState,
            onDismiss = { viewModel.onEvent(TransactionSearchUiEvent.OnDismissCategoryPicker) }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Search", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = { viewModel.onEvent(TransactionSearchUiEvent.OnClearFilters) }) {
                        Text("Reset")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 380.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                OutlinedTextField(
                    value = state.noteQuery,
                    onValueChange = { viewModel.onEvent(TransactionSearchUiEvent.OnNoteQueryChanged(it)) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search in notes") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    singleLine = true
                )

                SearchFilterRow(
                    label = "Period",
                    value = periodLabel(state.periodStart, state.periodEnd),
                    onClick = { viewModel.onEvent(TransactionSearchUiEvent.OnPeriodClick) },
                    onClear = if (state.periodStart != null) {
                        { viewModel.onEvent(TransactionSearchUiEvent.OnClearPeriod) }
                    } else {
                        null
                    }
                )

                var accountExpanded by remember { mutableStateOf(false) }
                val selectedAccountName = state.accounts
                    .firstOrNull { it.id == state.selectedAccountId }
                    ?.name ?: "All accounts"
                SearchFilterRow(
                    label = "Account",
                    value = selectedAccountName,
                    onClick = { accountExpanded = true }
                )
                DropdownMenu(
                    expanded = accountExpanded,
                    onDismissRequest = { accountExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("All accounts") },
                        onClick = {
                            viewModel.onEvent(TransactionSearchUiEvent.OnAccountSelected(null))
                            accountExpanded = false
                        }
                    )
                    state.accounts.forEach { account ->
                        DropdownMenuItem(
                            text = { Text(account.name) },
                            onClick = {
                                viewModel.onEvent(TransactionSearchUiEvent.OnAccountSelected(account.id))
                                accountExpanded = false
                            }
                        )
                    }
                }

                SearchFilterRow(
                    label = "Category",
                    value = categoryLabel(state),
                    onClick = { viewModel.onEvent(TransactionSearchUiEvent.OnCategoryRowClick) },
                    onClear = if (state.selectedCategoryId != null) {
                        { viewModel.onEvent(TransactionSearchUiEvent.OnClearCategory) }
                    } else {
                        null
                    }
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Amount",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.width(100.dp)
                    )
                    OutlinedTextField(
                        value = state.amountMin,
                        onValueChange = { viewModel.onEvent(TransactionSearchUiEvent.OnAmountMinChanged(it)) },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Min.") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true
                    )
                    Text("~", modifier = Modifier.padding(horizontal = 8.dp))
                    OutlinedTextField(
                        value = state.amountMax,
                        onValueChange = { viewModel.onEvent(TransactionSearchUiEvent.OnAmountMaxChanged(it)) },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Max.") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true
                    )
                }
            }

            HorizontalDivider()

            Box(modifier = Modifier.weight(1f)) {
                when {
                    state.isLoading -> MmrLoadingState()
                    state.errorMessage != null -> MmrErrorState(
                        message = state.errorMessage ?: "Something went wrong.",
                        onRetry = { viewModel.onEvent(TransactionSearchUiEvent.OnClearFilters) }
                    )
                    state.isEmpty -> MmrEmptyState(message = "No transactions match your filters.")
                    else -> SearchGroupedResults(
                        groups = state.groupedResults,
                        onItemClick = onNavigateToEditTransaction,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchFilterRow(
    label: String,
    value: String,
    onClick: () -> Unit,
    onClear: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.width(100.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        if (onClear != null) {
            TextButton(onClick = onClear) {
                Text("Clear")
            }
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
}

@Composable
private fun SearchGroupedResults(
    groups: List<TransactionGroupUi>,
    onItemClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 24.dp, top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        groups.forEach { group ->
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = group.dateLabel,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    val dayNet =
                        "${if (group.dayTotal >= 0) "+₹" else "-₹"}${"%.2f".format(kotlin.math.abs(group.dayTotal))}"
                    Text(
                        text = dayNet,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
            items(group.items, key = { it.id }) { item ->
                SearchTransactionRowCard(item = item, onClick = { onItemClick(item.id) })
            }
        }
    }
}

@Composable
private fun SearchTransactionRowCard(item: TransactionRowUi, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
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
                Text(text = item.title, fontWeight = FontWeight.SemiBold)
                Text(
                    text = "${item.subtitle} • ${item.timeLabel}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
            Text(
                text = item.amountFormatted,
                fontWeight = FontWeight.Bold,
                color = if (item.isIncome) Color(0xFF10B981) else Color(0xFFEF4444)
            )
        }
    }
}

private fun periodLabel(start: java.time.LocalDate?, end: java.time.LocalDate?): String {
    if (start == null || end == null) return "Any time"
    val s = HomePeriodUtils.formatShortDate(minOf(start, end))
    val e = HomePeriodUtils.formatShortDate(maxOf(start, end))
    return "$s — $e"
}

private fun categoryLabel(state: com.mmr.feature.search.model.TransactionSearchUiState): String {
    val category = state.categories.firstOrNull { it.id == state.selectedCategoryId }
    if (category == null) return "All categories"
    val sub = category.subcategories.firstOrNull { it.id == state.selectedSubcategoryId }
    return if (sub != null) "${category.name} › ${sub.name}" else category.name
}
