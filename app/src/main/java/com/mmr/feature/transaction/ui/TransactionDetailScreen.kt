package com.mmr.feature.transaction.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mmr.app.testing.MmrTestTags
import com.mmr.app.ui.adaptive.MmrAdaptiveWidthBox
import com.mmr.app.ui.adaptive.MmrLabelValueRow
import com.mmr.app.ui.adaptive.mmrScreenHorizontalPadding
import com.mmr.app.ui.components.MmrConfirmDialog
import com.mmr.app.ui.components.MmrContentDialog
import com.mmr.app.ui.components.MmrDimens
import com.mmr.app.ui.components.MmrSecondaryActionButton
import com.mmr.app.ui.components.MmrTopAppBar
import com.mmr.app.ui.state.MmrErrorState
import com.mmr.app.ui.state.MmrLoadingState
import com.mmr.core.loan.LoanLinkedTransactionDeleteMode
import com.mmr.feature.transaction.model.LinkedSmsItemUi
import com.mmr.feature.transaction.model.TransactionDetailUiEvent
import com.mmr.feature.transaction.viewmodel.TransactionDetailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailScreen(
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (Long) -> Unit,
    onNavigateToDuplicate: (sourceTransactionId: Long, dateMillis: Long) -> Unit,
    viewModel: TransactionDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val horizontalPadding = mmrScreenHorizontalPadding()

    LaunchedEffect(Unit) {
        viewModel.deleted.collect {
            onNavigateBack()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.duplicateNavigation.collect { nav ->
            onNavigateToDuplicate(nav.sourceTransactionId, nav.dateMillis)
        }
    }

    if (state.showDeleteConfirmDialog) {
        MmrConfirmDialog(
            title = "Delete transaction?",
            message = "This cannot be undone. Linked loan payments are unlinked or reversed based on your choice.",
            confirmLabel = "Delete",
            onConfirm = { viewModel.onEvent(TransactionDetailUiEvent.OnConfirmDelete) },
            onDismiss = { viewModel.onEvent(TransactionDetailUiEvent.OnDismissDeleteConfirm) },
            confirmIsDestructive = true,
        )
    }

    if (state.showLoanDeleteDialog) {
        MmrContentDialog(
            title = "Delete linked loan payment?",
            message =
                "This transaction is tied to a loan repayment. Unlink keeps the loan payment record; " +
                    "Reverse removes the payment and restores the loan balance.",
            onDismiss = { viewModel.onEvent(TransactionDetailUiEvent.OnDismissLoanDeleteDialog) },
            showDismissButton = false,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                MmrSecondaryActionButton(
                    text = "Unlink from loan",
                    onClick = {
                        viewModel.onEvent(
                            TransactionDetailUiEvent.OnConfirmDeleteWithLoanPolicy(
                                LoanLinkedTransactionDeleteMode.UNLINK,
                            ),
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                MmrSecondaryActionButton(
                    text = "Reverse loan payment",
                    onClick = {
                        viewModel.onEvent(
                            TransactionDetailUiEvent.OnConfirmDeleteWithLoanPolicy(
                                LoanLinkedTransactionDeleteMode.REVERSE,
                            ),
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }

    if (state.showDuplicateDateDialog) {
        MmrContentDialog(
            title = "Duplicate transaction",
            message = "Use the same date as this transaction, or today's date?",
            onDismiss = { viewModel.onEvent(TransactionDetailUiEvent.OnDismissDuplicateDateDialog) },
            showDismissButton = true,
            dismissLabel = "Cancel",
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                MmrSecondaryActionButton(
                    text = "Same date",
                    onClick = { viewModel.onEvent(TransactionDetailUiEvent.OnDuplicateSameDate) },
                    modifier =
                        Modifier
                            .testTag(MmrTestTags.transactionDuplicateSameDate)
                            .fillMaxWidth(),
                )
                MmrSecondaryActionButton(
                    text = "Today",
                    onClick = { viewModel.onEvent(TransactionDetailUiEvent.OnDuplicateToday) },
                    modifier =
                        Modifier
                            .testTag(MmrTestTags.transactionDuplicateToday)
                            .fillMaxWidth(),
                )
            }
        }
    }

    Scaffold(
        topBar = {
            MmrTopAppBar(
                title = "Transaction",
                showBackNavigation = true,
                onNavigateBack = onNavigateBack,
                useMmHeaderStyle = true,
                actions = {
                    if (!state.isLoading && state.errorMessage == null && !state.isDeleting) {
                        IconButton(
                            onClick = { onNavigateToEdit(state.transactionId) },
                            modifier = Modifier.testTag(MmrTestTags.transactionDetailEdit),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Edit,
                                contentDescription = "Edit",
                            )
                        }
                        IconButton(
                            onClick = { viewModel.onEvent(TransactionDetailUiEvent.OnDuplicateClick) },
                            modifier = Modifier.testTag(MmrTestTags.transactionDetailDuplicate),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ContentCopy,
                                contentDescription = "Duplicate",
                            )
                        }
                        IconButton(
                            onClick = { viewModel.onEvent(TransactionDetailUiEvent.OnDeleteClick) },
                            modifier = Modifier.testTag(MmrTestTags.transactionDetailDelete),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.DeleteOutline,
                                contentDescription = "Delete",
                            )
                        }
                    }
                },
            )
        },
    ) { padding ->
        when {
            state.isLoading || state.isDeleting -> MmrLoadingState(Modifier.padding(padding))
            state.errorMessage != null ->
                MmrErrorState(
                    message = state.errorMessage ?: "Error",
                    onRetry = null,
                    modifier = Modifier.fillMaxSize().padding(padding),
                )
            else -> {
                MmrAdaptiveWidthBox(Modifier.fillMaxSize().padding(padding)) {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontalPadding, vertical = MmrDimens.spaceMd),
                        verticalArrangement = Arrangement.spacedBy(MmrDimens.spaceMd),
                    ) {
                        item {
                            Text(
                                text = state.title,
                                style = MaterialTheme.typography.titleLarge,
                            )
                            Text(
                                text = state.amountFormatted,
                                style = MaterialTheme.typography.headlineSmall,
                                color =
                                    if (state.amountFormatted.startsWith("+")) {
                                        Color(0xFF10B981)
                                    } else {
                                        Color(0xFFEF4444)
                                    },
                            )
                        }
                        item {
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(
                                    Modifier.padding(MmrDimens.cardPadding),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    MmrLabelValueRow("Type", state.typeLabel)
                                    MmrLabelValueRow("Date", state.dateTimeLabel)
                                    MmrLabelValueRow("Category", state.categoryLabel)
                                    MmrLabelValueRow("Account", state.accountLabel)
                                    state.note?.takeIf { it.isNotBlank() }?.let {
                                        MmrLabelValueRow("Note", it)
                                    }
                                    state.paymentChannelLabel?.let {
                                        MmrLabelValueRow("Channel", it)
                                    }
                                    state.paymentInstrumentLabel?.let {
                                        MmrLabelValueRow("Instrument", it)
                                    }
                                    state.cardMetadataLabel?.let {
                                        MmrLabelValueRow("Card", it)
                                    }
                                }
                            }
                        }
                        item {
                            Text(
                                "Linked SMS (${state.linkedSms.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            if (state.linkedSms.isEmpty()) {
                                Text(
                                    "No SMS messages are linked to this transaction.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        items(state.linkedSms, key = { it.id }) { sms ->
                            LinkedSmsCard(sms)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LinkedSmsCard(sms: LinkedSmsItemUi) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
    ) {
        Column(Modifier.padding(MmrDimens.cardPadding), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(sms.sender, fontWeight = FontWeight.SemiBold)
            Text(sms.receivedLabel, style = MaterialTheme.typography.labelSmall)
            sms.amountFormatted?.let {
                Text(it, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }
            Text(sms.snippet, style = MaterialTheme.typography.bodySmall)
        }
    }
}
