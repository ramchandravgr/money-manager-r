package com.mmr.feature.recurring.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mmr.app.ui.components.MmrDetailKeyValueRow
import com.mmr.app.ui.components.MmrDetailListItemCard
import com.mmr.app.ui.components.MmrDetailOverviewCard
import com.mmr.app.ui.components.MmrDetailScreenScaffold
import com.mmr.app.ui.components.MmrDetailSectionTitle
import com.mmr.app.ui.state.MmrStatusBanner
import com.mmr.app.ui.theme.MmrBrandColors
import com.mmr.feature.recurring.viewmodel.RecurringDetailUiEvent
import com.mmr.feature.recurring.viewmodel.RecurringDetailViewModel

@Composable
fun RecurringDetailScreen(
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (Long) -> Unit,
    viewModel: RecurringDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val rule = uiState.rule
    val showFooter = rule != null && !uiState.isLoading

    MmrDetailScreenScaffold(
        title = rule?.name ?: "Recurring Detail",
        onNavigateBack = onNavigateBack,
        isLoading = uiState.isLoading,
        errorMessage = if (rule == null) uiState.errorMessage else null,
        onRetry = { viewModel.onEvent(RecurringDetailUiEvent.OnRetryClick) },
        footerPrimaryLabel = if (showFooter) "Edit rule" else null,
        onFooterPrimary =
            if (showFooter) {
                { onNavigateToEdit(rule!!.id) }
            } else {
                null
            },
        footerSecondaryLabel = if (showFooter) "Skip this occurrence" else null,
        onFooterSecondary =
            if (showFooter) {
                { viewModel.onEvent(RecurringDetailUiEvent.OnAdvanceSchedule) }
            } else {
                null
            },
    ) {
        uiState.statusMessage?.let { message ->
            item {
                MmrStatusBanner(
                    message = message,
                    onDismiss = { viewModel.onEvent(RecurringDetailUiEvent.OnDismissStatus) },
                )
            }
        }

        if (rule != null) {
            item {
                MmrDetailOverviewCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = rule.amountFormatted,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color =
                                    if (rule.isExpense) {
                                        MmrBrandColors.AmountNegative
                                    } else {
                                        MmrBrandColors.AmountPositive
                                    },
                            )
                            Text(
                                text = "${rule.categoryName} • ${rule.accountName}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MmrBrandColors.TextMedium,
                            )
                        }
                        Switch(
                            checked = rule.isEnabled,
                            onCheckedChange = {
                                viewModel.onEvent(RecurringDetailUiEvent.OnToggleEnabled(it))
                            },
                            colors =
                                SwitchDefaults.colors(
                                    checkedTrackColor = MmrBrandColors.Active,
                                ),
                        )
                    }
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 10.dp),
                        color = MmrBrandColors.BorderMain,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column {
                            Text(
                                "Interval",
                                style = MaterialTheme.typography.labelSmall,
                                color = MmrBrandColors.TextMedium,
                            )
                            Text(
                                rule.intervalLabel,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                "Next Run",
                                style = MaterialTheme.typography.labelSmall,
                                color = MmrBrandColors.TextMedium,
                            )
                            Text(
                                rule.nextRunDateFormatted,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    MmrDetailKeyValueRow(
                        label = "Reminder",
                        value = rule.reminderLabel,
                        valueFontWeight = FontWeight.Normal,
                    )
                    rule.noteTemplate?.takeIf { it.isNotBlank() }?.let { note ->
                        Spacer(modifier = Modifier.height(6.dp))
                        MmrDetailKeyValueRow(
                            label = "Template note",
                            value = note,
                            valueFontWeight = FontWeight.Normal,
                        )
                    }
                }
            }

            item {
                MmrDetailSectionTitle("Next 5 Scheduled Dates")
            }

            items(uiState.upcomingScheduleDates) { dateLabel ->
                MmrDetailListItemCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
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
                            color = MmrBrandColors.Active,
                        )
                    }
                }
            }
        }
    }
}
