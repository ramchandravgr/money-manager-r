package com.mmr.feature.transaction.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mmr.app.testing.MmrTestTags
import com.mmr.app.ui.components.MmrConfirmDialog
import com.mmr.app.ui.components.MmrContentDialog
import com.mmr.app.ui.components.MmrDetailHeroBlock
import com.mmr.app.ui.components.MmrDetailLinkedSmsCard
import com.mmr.app.ui.components.MmrDetailMetaCard
import com.mmr.app.ui.components.MmrDetailMetaRow
import com.mmr.app.ui.components.MmrDetailScreenScaffold
import com.mmr.app.ui.components.MmrDetailSectionHint
import com.mmr.app.ui.components.MmrDetailSectionTitle
import com.mmr.app.ui.components.MmrDialogOptionButton
import com.mmr.core.loan.LoanLinkedTransactionDeleteMode
import com.mmr.feature.transaction.model.LinkedSmsItemUi
import com.mmr.feature.transaction.model.TransactionDetailUiEvent
import com.mmr.feature.transaction.viewmodel.TransactionDetailViewModel

@Composable
fun TransactionDetailScreen(
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (Long) -> Unit,
    onNavigateToDuplicate: (sourceTransactionId: Long, dateMillis: Long) -> Unit,
    viewModel: TransactionDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

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
            showDismissButton = true,
            dismissLabel = "Cancel",
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                MmrDialogOptionButton(
                    text = "Unlink from loan",
                    onClick = {
                        viewModel.onEvent(
                            TransactionDetailUiEvent.OnConfirmDeleteWithLoanPolicy(
                                LoanLinkedTransactionDeleteMode.UNLINK,
                            ),
                        )
                    },
                )
                MmrDialogOptionButton(
                    text = "Reverse loan payment",
                    onClick = {
                        viewModel.onEvent(
                            TransactionDetailUiEvent.OnConfirmDeleteWithLoanPolicy(
                                LoanLinkedTransactionDeleteMode.REVERSE,
                            ),
                        )
                    },
                )
            }
        }
    }

    if (state.showDuplicateDateDialog) {
        MmrContentDialog(
            title = "Duplicate transaction",
            message = "Use the same date as this transaction, or today's date?",
            onDismiss = { viewModel.onEvent(TransactionDetailUiEvent.OnDismissDuplicateDateDialog) },
            confirmLabel = "Same date",
            onConfirm = { viewModel.onEvent(TransactionDetailUiEvent.OnDuplicateSameDate) },
            confirmTestTag = MmrTestTags.transactionDuplicateSameDate,
            secondaryConfirmLabel = "Today",
            onSecondaryConfirm = { viewModel.onEvent(TransactionDetailUiEvent.OnDuplicateToday) },
            secondaryConfirmTestTag = MmrTestTags.transactionDuplicateToday,
            showDismissButton = true,
            dismissLabel = "Cancel",
        )
    }

    val showContent = !state.isLoading && state.errorMessage == null && !state.isDeleting

    MmrDetailScreenScaffold(
        title = "Transaction",
        onNavigateBack = onNavigateBack,
        isLoading = state.isLoading || state.isDeleting,
        errorMessage = state.errorMessage,
        footerPrimaryLabel = if (showContent) "Edit" else null,
        onFooterPrimary =
            if (showContent) {
                { onNavigateToEdit(state.transactionId) }
            } else {
                null
            },
        footerPrimaryTestTag = MmrTestTags.transactionDetailEdit,
        footerSecondaryLabel = if (showContent) "Duplicate" else null,
        onFooterSecondary =
            if (showContent) {
                { viewModel.onEvent(TransactionDetailUiEvent.OnDuplicateClick) }
            } else {
                null
            },
        footerSecondaryTestTag = MmrTestTags.transactionDetailDuplicate,
        footerTextLabel = if (showContent) "Delete" else null,
        onFooterText =
            if (showContent) {
                { viewModel.onEvent(TransactionDetailUiEvent.OnDeleteClick) }
            } else {
                null
            },
        footerTextIsDestructive = true,
        footerTextTestTag = MmrTestTags.transactionDetailDelete,
    ) {
        item {
            MmrDetailHeroBlock(
                title = state.title,
                amountFormatted = state.amountFormatted,
                amountIsIncome = state.amountFormatted.startsWith("+"),
                subtitle = state.syncedFromLabel,
            )
        }
        item {
            MmrDetailMetaCard {
                MmrDetailMetaRow("Type", state.typeLabel)
                MmrDetailMetaRow("Date", state.dateTimeLabel)
                MmrDetailMetaRow("Category", state.categoryLabel)
                MmrDetailMetaRow("Account", state.accountLabel)
                state.note?.takeIf { it.isNotBlank() }?.let { MmrDetailMetaRow("Note", it) }
                state.paymentChannelLabel?.let { MmrDetailMetaRow("Channel", it) }
                state.paymentInstrumentLabel?.let { MmrDetailMetaRow("Instrument", it) }
                state.cardMetadataLabel?.let { MmrDetailMetaRow("Card", it) }
                state.refundSummaryLabel?.let { MmrDetailMetaRow("Refunds", it) }
            }
        }
        item {
            TransactionReceiptSection(
                receipts = state.receipts,
                onAddClick = {},
                onRemoveSaved = {},
                onRemovePending = {},
                readOnly = true,
            )
        }
        item {
            MmrDetailSectionTitle("Linked SMS (${state.linkedSms.size})")
            if (state.linkedSms.isEmpty()) {
                MmrDetailSectionHint("No SMS messages are linked to this transaction.")
            }
        }
        items(state.linkedSms, key = { it.id }) { sms ->
            TransactionDetailLinkedSmsCard(sms)
        }
    }
}

@Composable
private fun TransactionDetailLinkedSmsCard(sms: LinkedSmsItemUi) {
    MmrDetailLinkedSmsCard(
        sender = sms.roleLabel?.let { "$it · ${sms.sender}" } ?: sms.sender,
        receivedLabel = sms.receivedLabel,
        snippet = sms.snippet,
        amountFormatted = sms.amountFormatted,
    )
}
