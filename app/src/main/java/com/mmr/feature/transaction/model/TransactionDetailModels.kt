package com.mmr.feature.transaction.model

import com.mmr.core.loan.LoanLinkedTransactionDeleteMode

data class LinkedSmsItemUi(
    val id: Long,
    val sender: String,
    val snippet: String,
    val receivedLabel: String,
    val amountFormatted: String?,
    val roleLabel: String? = null,
)

data class TransactionDetailUiState(
    val isLoading: Boolean = true,
    val transactionId: Long = 0L,
    val title: String = "",
    val amountFormatted: String = "",
    val typeLabel: String = "",
    val dateTimeLabel: String = "",
    val categoryLabel: String = "",
    val accountLabel: String = "",
    val note: String? = null,
    val paymentChannelLabel: String? = null,
    val paymentInstrumentLabel: String? = null,
    val cardMetadataLabel: String? = null,
    val syncedFromLabel: String? = null,
    val refundSummaryLabel: String? = null,
    val linkedSms: List<LinkedSmsItemUi> = emptyList(),
    val receipts: List<TransactionReceiptUi> = emptyList(),
    val errorMessage: String? = null,
    val dateTimeMillis: Long = 0L,
    val showDeleteConfirmDialog: Boolean = false,
    val showLoanDeleteDialog: Boolean = false,
    val showDuplicateDateDialog: Boolean = false,
    val isDeleting: Boolean = false,
)

sealed interface TransactionDetailUiEvent {
    data object OnDeleteClick : TransactionDetailUiEvent

    data object OnDismissDeleteConfirm : TransactionDetailUiEvent

    data object OnConfirmDelete : TransactionDetailUiEvent

    data object OnDismissLoanDeleteDialog : TransactionDetailUiEvent

    data class OnConfirmDeleteWithLoanPolicy(
        val mode: LoanLinkedTransactionDeleteMode,
    ) : TransactionDetailUiEvent

    data object OnDuplicateClick : TransactionDetailUiEvent

    data object OnDismissDuplicateDateDialog : TransactionDetailUiEvent

    data object OnDuplicateSameDate : TransactionDetailUiEvent

    data object OnDuplicateToday : TransactionDetailUiEvent
}
