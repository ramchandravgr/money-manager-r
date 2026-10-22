package com.mmr.feature.loan.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import com.mmr.core.model.enums.LoanStatus
import com.mmr.core.model.enums.LoanType
import com.mmr.feature.loan.model.*
import com.mmr.app.ui.state.MmrEmptyState
import com.mmr.app.ui.state.MmrErrorState
import com.mmr.app.ui.state.MmrLoadingState
import com.mmr.app.ui.state.MmrStatusBanner
import com.mmr.feature.loan.viewmodel.LoanListViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoanListScreen(
    onNavigateBack: () -> Unit,
    onNavigateToLoanDetail: (Long) -> Unit,
    onNavigateToAddLoan: () -> Unit,
    showBackNavigation: Boolean = true,
    viewModel: LoanListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Loans Tracking", fontWeight = FontWeight.Bold) },
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
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNavigateToAddLoan) {
                Text("+", style = MaterialTheme.typography.titleLarge)
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 1. Summary Cards (Borrowed vs Lent)
            LoanSummarySection(summary = uiState.summary)

            // 2. Filter Chips (Type: All, Borrowed, Lent)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = uiState.selectedFilterType == LoanFilterType.ALL,
                    onClick = { viewModel.onEvent(LoanListUiEvent.OnFilterTypeSelected(LoanFilterType.ALL)) },
                    label = { Text("All Direction") }
                )
                FilterChip(
                    selected = uiState.selectedFilterType == LoanFilterType.BORROWED,
                    onClick = { viewModel.onEvent(LoanListUiEvent.OnFilterTypeSelected(LoanFilterType.BORROWED)) },
                    label = { Text("Borrowed (I owe)") }
                )
                FilterChip(
                    selected = uiState.selectedFilterType == LoanFilterType.LENT,
                    onClick = { viewModel.onEvent(LoanListUiEvent.OnFilterTypeSelected(LoanFilterType.LENT)) },
                    label = { Text("Lent (Owed to me)") }
                )
            }

            // 3. Status Filters (All, Active, Closed, Overdue, Paused)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = uiState.selectedStatusFilter == LoanStatusFilter.ALL,
                    onClick = { viewModel.onEvent(LoanListUiEvent.OnStatusFilterSelected(LoanStatusFilter.ALL)) },
                    label = { Text("All Status") }
                )
                FilterChip(
                    selected = uiState.selectedStatusFilter == LoanStatusFilter.ACTIVE,
                    onClick = { viewModel.onEvent(LoanListUiEvent.OnStatusFilterSelected(LoanStatusFilter.ACTIVE)) },
                    label = { Text("Active") }
                )
                FilterChip(
                    selected = uiState.selectedStatusFilter == LoanStatusFilter.CLOSED,
                    onClick = { viewModel.onEvent(LoanListUiEvent.OnStatusFilterSelected(LoanStatusFilter.CLOSED)) },
                    label = { Text("Closed") }
                )
                FilterChip(
                    selected = uiState.selectedStatusFilter == LoanStatusFilter.OVERDUE,
                    onClick = { viewModel.onEvent(LoanListUiEvent.OnStatusFilterSelected(LoanStatusFilter.OVERDUE)) },
                    label = { Text("Overdue") }
                )
                FilterChip(
                    selected = uiState.selectedStatusFilter == LoanStatusFilter.PAUSED,
                    onClick = { viewModel.onEvent(LoanListUiEvent.OnStatusFilterSelected(LoanStatusFilter.PAUSED)) },
                    label = { Text("Paused") }
                )
            }

            // 4. Loading, Error, Empty, or List
            when {
                uiState.isLoading -> MmrLoadingState()

                uiState.errorMessage != null -> {
                    MmrErrorState(
                        message = uiState.errorMessage ?: "An error occurred.",
                        onRetry = { viewModel.onEvent(LoanListUiEvent.OnRetryClick) }
                    )
                }

                uiState.isEmpty -> {
                    MmrEmptyState(
                        message = "No loans recorded.\nTap '+' to create your first loan."
                    )
                }

                else -> {
                    uiState.statusMessage?.let { message ->
                        MmrStatusBanner(
                            message = message,
                            onDismiss = { viewModel.onEvent(LoanListUiEvent.OnDismissStatus) },
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(uiState.loans, key = { it.id }) { loan ->
                            LoanCard(
                                loan = loan,
                                onClick = { onNavigateToLoanDetail(loan.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LoanSummarySection(summary: LoanSummaryUi) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Total Borrowed (Outstanding)",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = summary.totalBorrowedOutstandingFormatted,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFEF4444)
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Total Lent (Outstanding)",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = summary.totalLentOutstandingFormatted,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF10B981)
                    )
                }
            }
        }
    }
}

@Composable
private fun LoanCard(
    loan: LoanItemUi,
    onClick: () -> Unit
) {
    val isBorrowed = loan.loanType == LoanType.BORROWED
    val directionLabel = if (isBorrowed) "Borrowed" else "Lent"
    val directionColor = if (isBorrowed) Color(0xFFEF4444) else Color(0xFF10B981)

    val statusContainerColor = when (loan.status) {
        LoanStatus.ACTIVE -> MaterialTheme.colorScheme.primaryContainer
        LoanStatus.CLOSED -> MaterialTheme.colorScheme.surfaceVariant
        LoanStatus.OVERDUE -> MaterialTheme.colorScheme.errorContainer
        LoanStatus.PAUSED -> MaterialTheme.colorScheme.secondaryContainer
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = loan.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${loan.lenderName} • $directionLabel",
                        style = MaterialTheme.typography.bodySmall,
                        color = directionColor,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = statusContainerColor
                ) {
                    Text(
                        text = loan.statusLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Outstanding",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = loan.outstandingFormatted,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (loan.outstandingAmount > 0) directionColor else Color.Gray
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Principal",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = loan.principalFormatted,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Progress indicator
            LinearProgressIndicator(
                progress = { loan.progressPercentage },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = directionColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = loan.dueDateFormatted?.let { "Due: $it" } ?: "No due date",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
                Text(
                    text = "${loan.progressFormatted} repaid",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
