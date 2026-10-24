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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mmr.app.testing.MmrTestTags
import com.mmr.app.ui.adaptive.mmrScreenHorizontalPadding
import com.mmr.app.ui.components.MmrAddListFab
import com.mmr.app.ui.components.MmrDimens
import com.mmr.app.ui.components.MmrDualSummaryStrip
import com.mmr.app.ui.components.MmrHeaderBar
import com.mmr.app.ui.state.MmrEmptyState
import com.mmr.app.ui.state.MmrErrorState
import com.mmr.app.ui.state.MmrLoadingState
import com.mmr.app.ui.state.MmrStatusBanner
import com.mmr.app.ui.theme.MmrBrandColors
import com.mmr.feature.recurring.model.RecurringFilterType
import com.mmr.feature.recurring.model.RecurringListUiEvent
import com.mmr.feature.recurring.model.RecurringRuleItemUi
import com.mmr.feature.recurring.model.UpcomingReminderUi
import com.mmr.feature.recurring.viewmodel.RecurringListViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringListScreen(
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (Long) -> Unit,
    onNavigateToAddRule: () -> Unit,
    showBackNavigation: Boolean = true,
    viewModel: RecurringListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val horizontalPadding = mmrScreenHorizontalPadding()

    Scaffold(
        containerColor = MmrBrandColors.ListBackground,
        contentWindowInsets = com.mmr.app.ui.components.MmrNestedScaffoldInsets,
        topBar = {
            Column(modifier = Modifier.fillMaxWidth()) {
                MmrHeaderBar(
                    title = "Recurring",
                    showBack = showBackNavigation,
                    onBack = onNavigateBack,
                )
                MmrDualSummaryStrip(
                    leftLabel = "Active rules",
                    leftAmount = uiState.activeRulesCount.toString(),
                    leftAmountColor = MmrBrandColors.Active,
                    rightLabel = "Reminders",
                    rightAmount = uiState.reminders.size.toString(),
                    rightAmountColor = MmrBrandColors.TextStrong,
                )
            }
        },
        floatingActionButton = {
            MmrAddListFab(
                modifier = Modifier.testTag(MmrTestTags.recurringFabAdd),
                contentDescription = "Add recurring rule",
                onClick = onNavigateToAddRule,
            )
        },
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
        ) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = horizontalPadding, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(MmrDimens.spaceSm),
            ) {
                FilterChip(
                    selected = uiState.selectedFilter == RecurringFilterType.ALL,
                    onClick = { viewModel.onEvent(RecurringListUiEvent.OnFilterSelected(RecurringFilterType.ALL)) },
                    label = { Text("All (${uiState.totalRulesCount})") },
                )
                FilterChip(
                    selected = uiState.selectedFilter == RecurringFilterType.ACTIVE,
                    onClick = { viewModel.onEvent(RecurringListUiEvent.OnFilterSelected(RecurringFilterType.ACTIVE)) },
                    label = { Text("Active (${uiState.activeRulesCount})") },
                )
                FilterChip(
                    selected = uiState.selectedFilter == RecurringFilterType.PAUSED,
                    onClick = { viewModel.onEvent(RecurringListUiEvent.OnFilterSelected(RecurringFilterType.PAUSED)) },
                    label = { Text("Paused (${uiState.pausedRulesCount})") },
                )
            }

            when {
                uiState.isLoading -> MmrLoadingState()

                uiState.errorMessage != null -> {
                    MmrErrorState(
                        message = uiState.errorMessage ?: "Failed to load recurring rules.",
                        onRetry = { viewModel.onEvent(RecurringListUiEvent.OnRetryClick) },
                    )
                }

                uiState.isFilteredEmpty -> {
                    Column(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .padding(horizontal = horizontalPadding),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        MmrEmptyState(
                            message =
                                when (uiState.selectedFilter) {
                                    RecurringFilterType.ACTIVE ->
                                        "No active recurring rules.\nTurn a rule on with the switch, or tap Paused to see paused rules."
                                    RecurringFilterType.PAUSED ->
                                        "No paused recurring rules.\nTurn a rule off with the switch to pause auto-posting."
                                    RecurringFilterType.ALL -> "No rules match this filter."
                                },
                        )
                        TextButton(
                            onClick = { viewModel.onEvent(RecurringListUiEvent.OnResetFilters) },
                        ) {
                            Text("Show all rules")
                        }
                    }
                }

                uiState.isEmpty -> {
                    MmrEmptyState(
                        message = "No recurring rules set.\nTap '+' to create recurring bills, salary, or subscriptions.",
                    )
                }

                else -> {
                    uiState.statusMessage?.let { message ->
                        MmrStatusBanner(
                            message = message,
                            onDismiss = { viewModel.onEvent(RecurringListUiEvent.OnDismissStatus) },
                            modifier = Modifier.padding(horizontal = horizontalPadding, vertical = MmrDimens.spaceSm),
                        )
                    }
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontalPadding, vertical = MmrDimens.spaceLg),
                        verticalArrangement = Arrangement.spacedBy(MmrDimens.spaceMd),
                    ) {
                        // Reminders preview section
                        if (uiState.reminders.isNotEmpty()) {
                            item {
                                Text(
                                    text = "Scheduled Alerts & Reminders",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                            item {
                                Column(verticalArrangement = Arrangement.spacedBy(MmrDimens.spaceSm)) {
                                    uiState.reminders.forEach { reminder ->
                                        ReminderCard(reminder = reminder)
                                    }
                                }
                            }
                            item {
                                Spacer(modifier = Modifier.height(MmrDimens.spaceSm))
                                Text(
                                    text = "Recurring Schedules",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }

                        // Recurring rules list
                        items(uiState.rules, key = { it.id }) { rule ->
                            RecurringRuleCard(
                                rule = rule,
                                onClick = { onNavigateToDetail(rule.id) },
                                onToggleEnabled = {
                                    viewModel.onEvent(
                                        RecurringListUiEvent.OnToggleRuleEnabled(rule.id, it),
                                    )
                                },
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
        shape = RoundedCornerShape(MmrDimens.spaceMd),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(MmrDimens.spaceMd),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f).padding(end = MmrDimens.spaceSm),
                verticalArrangement = Arrangement.spacedBy(MmrDimens.spaceXs),
            ) {
                Text(
                    text = reminder.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    text = "${reminder.message} • ${reminder.formattedScheduleDate}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                )
            }
            Surface(
                shape = RoundedCornerShape(MmrDimens.spaceSm),
                color = MaterialTheme.colorScheme.primary,
            ) {
                Text(
                    text = "Alert",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.padding(horizontal = MmrDimens.spaceSm, vertical = MmrDimens.spaceXs),
                )
            }
        }
    }
}

@Composable
private fun RecurringRuleCard(
    rule: RecurringRuleItemUi,
    onClick: () -> Unit,
    onToggleEnabled: (Boolean) -> Unit,
) {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(MmrDimens.spaceMd),
    ) {
        Column(modifier = Modifier.padding(MmrDimens.cardPadding)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f).padding(end = MmrDimens.spaceSm),
                    verticalArrangement = Arrangement.spacedBy(MmrDimens.spaceXs),
                ) {
                    Text(
                        text = rule.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "${rule.categoryName} • ${rule.accountName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Switch(
                        checked = rule.isEnabled,
                        onCheckedChange = onToggleEnabled,
                    )
                    if (!rule.isEnabled) {
                        Text(
                            text = "Paused",
                            style = MaterialTheme.typography.labelSmall,
                            color = MmrBrandColors.TextMedium,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(MmrDimens.spaceMd))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = rule.amountFormatted,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (rule.isExpense) MmrBrandColors.AmountNegative else MmrBrandColors.AmountPositive,
                )

                Surface(
                    shape = RoundedCornerShape(MmrDimens.spaceSm),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                ) {
                    Text(
                        text = rule.intervalLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = MmrDimens.spaceSm, vertical = MmrDimens.spaceXs),
                    )
                }
            }

            Spacer(modifier = Modifier.height(MmrDimens.spaceSm))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Next: ${rule.nextRunDateFormatted}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                )
                Text(
                    text = rule.reminderLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
