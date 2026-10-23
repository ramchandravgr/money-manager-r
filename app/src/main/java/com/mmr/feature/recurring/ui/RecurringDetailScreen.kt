package com.mmr.feature.recurring.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
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
import com.mmr.app.ui.components.MmrDimens
import com.mmr.app.ui.components.MmrSecondaryActionButton
import com.mmr.app.ui.components.MmrTopAppBar
import com.mmr.app.ui.state.MmrErrorState
import com.mmr.app.ui.state.MmrLoadingState
import com.mmr.app.ui.state.MmrStatusBanner
import com.mmr.app.ui.theme.MmrMmColors
import com.mmr.feature.recurring.viewmodel.RecurringDetailUiEvent
import com.mmr.feature.recurring.viewmodel.RecurringDetailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringDetailScreen(
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (Long) -> Unit,
    viewModel: RecurringDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            MmrTopAppBar(
                title = uiState.rule?.name ?: "Recurring Detail",
                showBackNavigation = true,
                onNavigateBack = onNavigateBack,
                useMmHeaderStyle = true,
                actions = {
                    uiState.rule?.let { rule ->
                        IconButton(onClick = { onNavigateToEdit(rule.id) }) {
                            Icon(
                                imageVector = Icons.Outlined.Edit,
                                contentDescription = "Edit",
                                modifier = Modifier.size(MmrDimens.iconMd),
                                tint = MmrMmColors.HeaderIcon,
                            )
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        when {
            uiState.isLoading -> {
                MmrLoadingState(modifier = Modifier.fillMaxSize().padding(innerPadding))
            }

            uiState.errorMessage != null && uiState.rule == null -> {
                MmrErrorState(
                    message = uiState.errorMessage ?: "Failed to load recurring detail.",
                    onRetry = { viewModel.onEvent(RecurringDetailUiEvent.OnRetryClick) },
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                )
            }

            uiState.rule != null -> {
                val rule = uiState.rule!!

                LazyColumn(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    uiState.statusMessage?.let { message ->
                        item {
                            MmrStatusBanner(
                                message = message,
                                onDismiss = { viewModel.onEvent(RecurringDetailUiEvent.OnDismissStatus) },
                            )
                        }
                    }

                    // Overview Summary Card
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(16.dp),
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column {
                                        Text(
                                            text = rule.amountFormatted,
                                            style = MaterialTheme.typography.headlineMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (rule.isExpense) Color(0xFFEF4444) else Color(0xFF10B981),
                                        )
                                        Text(
                                            text = "${rule.categoryName} • ${rule.accountName}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }

                                    Switch(
                                        checked = rule.isEnabled,
                                        onCheckedChange = {
                                            viewModel.onEvent(
                                                RecurringDetailUiEvent.OnToggleEnabled(it),
                                            )
                                        },
                                    )
                                }

                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 12.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant,
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Column {
                                        Text("Interval", style = MaterialTheme.typography.labelSmall)
                                        Text(
                                            rule.intervalLabel,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("Next Run", style = MaterialTheme.typography.labelSmall)
                                        Text(
                                            rule.nextRunDateFormatted,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Text("Reminder", style = MaterialTheme.typography.labelSmall)
                                Text(rule.reminderLabel, style = MaterialTheme.typography.bodyMedium)

                                rule.noteTemplate?.takeIf { it.isNotBlank() }?.let { note ->
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Template Note", style = MaterialTheme.typography.labelSmall)
                                    Text(note, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }

                    // Advance schedule action button
                    item {
                        MmrSecondaryActionButton(
                            text = "Skip to Next Cycle",
                            onClick = { viewModel.onEvent(RecurringDetailUiEvent.OnAdvanceSchedule) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    // Upcoming Schedule Timeline
                    item {
                        Text(
                            text = "Next 5 Scheduled Dates",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }

                    items(uiState.upcomingScheduleDates) { dateLabel ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        ) {
                            Row(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = dateLabel,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                )
                                Text(
                                    text = "Scheduled",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
