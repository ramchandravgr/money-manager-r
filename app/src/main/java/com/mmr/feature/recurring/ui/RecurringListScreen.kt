package com.mmr.feature.recurring.ui

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mmr.feature.recurring.model.RecurringFilterType
import com.mmr.feature.recurring.model.RecurringListUiEvent
import com.mmr.feature.recurring.model.RecurringRuleItemUi
import com.mmr.feature.recurring.model.UpcomingReminderUi
import com.mmr.app.ui.state.MmrEmptyState
import com.mmr.app.ui.state.MmrErrorState
import com.mmr.app.ui.state.MmrLoadingState
import com.mmr.app.ui.state.MmrStatusBanner
import com.mmr.feature.recurring.viewmodel.RecurringListViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringListScreen(
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (Long) -> Unit,
    onNavigateToAddRule: () -> Unit,
    showBackNavigation: Boolean = true,
    viewModel: RecurringListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Recurring & Reminders", fontWeight = FontWeight.Bold) },
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
            FloatingActionButton(onClick = onNavigateToAddRule) {
                Text("+", style = MaterialTheme.typography.titleLarge)
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = uiState.selectedFilter == RecurringFilterType.ALL,
                    onClick = { viewModel.onEvent(RecurringListUiEvent.OnFilterSelected(RecurringFilterType.ALL)) },
                    label = { Text("All (${uiState.rules.size})") }
                )
                FilterChip(
                    selected = uiState.selectedFilter == RecurringFilterType.ACTIVE,
                    onClick = { viewModel.onEvent(RecurringListUiEvent.OnFilterSelected(RecurringFilterType.ACTIVE)) },
                    label = { Text("Active (${uiState.activeRulesCount})") }
                )
                FilterChip(
                    selected = uiState.selectedFilter == RecurringFilterType.PAUSED,
                    onClick = { viewModel.onEvent(RecurringListUiEvent.OnFilterSelected(RecurringFilterType.PAUSED)) },
                    label = { Text("Paused") }
                )
            }

            when {
                uiState.isLoading -> MmrLoadingState()

                uiState.errorMessage != null -> {
                    MmrErrorState(
                        message = uiState.errorMessage ?: "Failed to load recurring rules.",
                        onRetry = { viewModel.onEvent(RecurringListUiEvent.OnRetryClick) }
                    )
                }

                uiState.isEmpty -> {
                    MmrEmptyState(
                        message = "No recurring rules set.\nTap '+' to create recurring bills, salary, or subscriptions."
                    )
                }

                else -> {
                    uiState.statusMessage?.let { message ->
                        MmrStatusBanner(
                            message = message,
                            onDismiss = { viewModel.onEvent(RecurringListUiEvent.OnDismissStatus) },
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Reminders preview section
                        if (uiState.reminders.isNotEmpty()) {
                            item {
                                Text(
                                    text = "Scheduled Alerts & Reminders",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            item {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    uiState.reminders.forEach { reminder ->
                                        ReminderCard(reminder = reminder)
                                    }
                                }
                            }
                            item {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Recurring Schedules",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Recurring rules list
                        items(uiState.rules, key = { it.id }) { rule ->
                            RecurringRuleCard(
                                rule = rule,
                                onClick = { onNavigateToDetail(rule.id) },
                                onToggleEnabled = { viewModel.onEvent(RecurringListUiEvent.OnToggleRuleEnabled(rule.id, it)) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReminderCard(reminder: UpcomingReminderUi) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = RoundedCornerShape(10.dp)
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
                    text = reminder.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "${reminder.message} • ${reminder.formattedScheduleDate}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.primary
            ) {
                Text(
                    text = "Alert",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun RecurringRuleCard(
    rule: RecurringRuleItemUi,
    onClick: () -> Unit,
    onToggleEnabled: (Boolean) -> Unit
) {
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
                        text = rule.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${rule.categoryName} • ${rule.accountName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                Switch(
                    checked = rule.isEnabled,
                    onCheckedChange = onToggleEnabled
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = rule.amountFormatted,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (rule.isExpense) Color(0xFFEF4444) else Color(0xFF10B981)
                )

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        text = rule.intervalLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Next: ${rule.nextRunDateFormatted}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
                Text(
                    text = rule.reminderLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
