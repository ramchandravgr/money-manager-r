package com.mmr.feature.stats.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mmr.feature.stats.model.CategoryBreakdownUi
import com.mmr.feature.stats.model.DateRangeType
import com.mmr.feature.stats.model.StatsSummaryUi
import com.mmr.app.ui.adaptive.MmrAdaptiveWidthBox
import com.mmr.app.ui.adaptive.mmrScreenHorizontalPadding
import com.mmr.app.ui.state.MmrEmptyState
import com.mmr.app.ui.state.MmrErrorState
import com.mmr.app.ui.state.MmrLoadingState
import com.mmr.feature.stats.model.StatsUiEvent
import com.mmr.feature.stats.viewmodel.StatsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    onNavigateBack: () -> Unit,
    showBackNavigation: Boolean = true,
    viewModel: StatsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Stats & Aggregations", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    if (showBackNavigation) {
                        TextButton(onClick = onNavigateBack) {
                            Text("Back")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 1. Date Range Filter Chips
            DateRangeFilterRow(
                selectedType = uiState.selectedRangeType,
                onSelectRange = { viewModel.onEvent(StatsUiEvent.OnDateRangeTypeSelected(it)) }
            )

            // Current Active Range Label
            val horizontalPadding = mmrScreenHorizontalPadding()
            if (uiState.dateRangeLabel.isNotBlank()) {
                Text(
                    text = uiState.dateRangeLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(horizontal = horizontalPadding, vertical = 2.dp)
                )
            }

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when {
                    uiState.isLoading -> MmrLoadingState()

                    uiState.errorMessage != null -> {
                        MmrErrorState(
                            message = uiState.errorMessage ?: "Something went wrong.",
                            onRetry = { viewModel.onEvent(StatsUiEvent.OnRetryClick) }
                        )
                    }

                    uiState.isEmpty -> {
                        MmrEmptyState(message = "No approved transactions found for this period.")
                    }

                    else -> {
                        MmrAdaptiveWidthBox(modifier = Modifier.fillMaxSize()) {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(horizontalPadding, vertical = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                        // 2. Summary Cards (Total Income, Total Expense, Net Balance)
                        item {
                            StatsSummarySection(summary = uiState.summary)
                        }

                        // 3. Category Breakdown Header
                        item {
                            Text(
                                text = "Spending by Category",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Category items
                        if (uiState.categoryBreakdowns.isEmpty()) {
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Text(
                                        text = "No expenses recorded in this period.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.padding(16.dp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            items(uiState.categoryBreakdowns, key = { it.categoryId ?: -1L }) { item ->
                                CategoryBreakdownCard(breakdown = item)
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

@Composable
private fun DateRangeFilterRow(
    selectedType: DateRangeType,
    onSelectRange: (DateRangeType) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = mmrScreenHorizontalPadding(), vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = selectedType == DateRangeType.THIS_MONTH,
            onClick = { onSelectRange(DateRangeType.THIS_MONTH) },
            label = { Text("This Month") }
        )
        FilterChip(
            selected = selectedType == DateRangeType.LAST_MONTH,
            onClick = { onSelectRange(DateRangeType.LAST_MONTH) },
            label = { Text("Last Month") }
        )
        FilterChip(
            selected = selectedType == DateRangeType.THIS_YEAR,
            onClick = { onSelectRange(DateRangeType.THIS_YEAR) },
            label = { Text("This Year") }
        )
        FilterChip(
            selected = selectedType == DateRangeType.ALL_TIME,
            onClick = { onSelectRange(DateRangeType.ALL_TIME) },
            label = { Text("All Time") }
        )
    }
}

@Composable
private fun StatsSummarySection(summary: StatsSummaryUi) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Net balance headline
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Net Balance",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = summary.netBalanceFormatted,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (summary.netBalance >= 0) Color(0xFF10B981) else Color(0xFFEF4444)
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )

            // Income and Expense breakdown
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Total Income",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = summary.totalIncomeFormatted,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFF10B981),
                        fontWeight = FontWeight.Bold
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Total Expense",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = summary.totalExpenseFormatted,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFFEF4444),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryBreakdownCard(breakdown: CategoryBreakdownUi) {
    val categoryColor = parseHexColor(breakdown.colorHex)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(categoryColor)
                    )
                    Text(
                        text = breakdown.categoryName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Text(
                    text = breakdown.amountFormatted,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Percentage progress bar
            LinearProgressIndicator(
                progress = { breakdown.percentage.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = categoryColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${breakdown.transactionCount} transaction(s)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
                Text(
                    text = breakdown.percentageFormatted,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun parseHexColor(hex: String): Color {
    return try {
        val clean = hex.removePrefix("#")
        val colorInt = when (clean.length) {
            6 -> (0xFF000000 or clean.toLong(16)).toInt()
            8 -> clean.toLong(16).toInt()
            else -> 0xFF3B82F6.toInt()
        }
        Color(colorInt)
    } catch (_: Exception) {
        Color(0xFF3B82F6)
    }
}
