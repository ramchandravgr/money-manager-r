package com.mmr.feature.transaction.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mmr.app.ui.state.MmrUiMessages
import com.mmr.core.common.display.TransactionCategoryLabels
import com.mmr.core.domain.repository.TransactionRepository
import com.mmr.core.loan.LoanLinkedTransactionDeleteMode
import com.mmr.core.model.enums.CardNetwork
import com.mmr.core.model.enums.TransactionType
import com.mmr.core.parser.model.SmsInstrument
import com.mmr.core.parser.model.SmsPaymentChannel
import com.mmr.feature.transaction.model.LinkedSmsItemUi
import com.mmr.feature.transaction.model.TransactionDetailUiEvent
import com.mmr.feature.transaction.model.TransactionDetailUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class TransactionDetailViewModel
    @Inject
    constructor(
        private val transactionRepository: TransactionRepository,
        savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        private val transactionId: Long = checkNotNull(savedStateHandle["transactionId"])

        private data class DetailOverlays(
            val showDeleteConfirmDialog: Boolean = false,
            val showLoanDeleteDialog: Boolean = false,
            val showDuplicateDateDialog: Boolean = false,
            val isDeleting: Boolean = false,
        )

        private val _overlays = MutableStateFlow(DetailOverlays())
        private val _deleted = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
        val deleted = _deleted.asSharedFlow()

        private val _duplicateNavigation = MutableSharedFlow<DuplicateNavigation>(extraBufferCapacity = 1)
        val duplicateNavigation = _duplicateNavigation.asSharedFlow()

        data class DuplicateNavigation(
            val sourceTransactionId: Long,
            val dateMillis: Long,
        )

        private fun formatPaymentChannel(raw: String?): String? {
            raw ?: return null
            return runCatching { SmsPaymentChannel.valueOf(raw) }
                .getOrNull()
                ?.name
                ?.replace('_', ' ')
                ?.lowercase(Locale.getDefault())
                ?.replaceFirstChar { it.titlecase(Locale.getDefault()) }
                ?: raw
        }

        private fun formatPaymentInstrument(raw: String?): String? {
            raw ?: return null
            return runCatching { SmsInstrument.valueOf(raw) }
                .getOrNull()
                ?.name
                ?.replace('_', ' ')
                ?.lowercase(Locale.getDefault())
                ?.replaceFirstChar { it.titlecase(Locale.getDefault()) }
                ?: raw
        }

        private fun formatCardMetadata(
            lastFour: String?,
            network: CardNetwork?,
        ): String? {
            val parts = mutableListOf<String>()
            network?.let { parts.add(it.name) }
            lastFour?.takeIf { it.length == 4 }?.let { parts.add("•••• $it") }
            return parts.takeIf { it.isNotEmpty() }?.joinToString(" · ")
        }

        val uiState: StateFlow<TransactionDetailUiState> =
            combine(
                transactionRepository.getApprovedTransactionsDetailed().map { list ->
                    list.find { it.transaction.id == transactionId }
                },
                transactionRepository.getLinkedSmsForTransaction(transactionId),
                _overlays,
            ) { details, smsRecords, overlays ->
                if (details == null) {
                    TransactionDetailUiState(
                        isLoading = false,
                        transactionId = transactionId,
                        errorMessage = "Transaction not found.",
                        showDeleteConfirmDialog = overlays.showDeleteConfirmDialog,
                        showLoanDeleteDialog = overlays.showLoanDeleteDialog,
                        showDuplicateDateDialog = overlays.showDuplicateDateDialog,
                        isDeleting = overlays.isDeleting,
                    )
                } else {
                    val txn = details.transaction
                    val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
                    val isIncome = txn.type == TransactionType.INCOME
                    val sign = if (isIncome) "+" else "-"
                    val typeLabel =
                        txn.type.name
                            .lowercase(Locale.getDefault())
                            .replaceFirstChar { it.titlecase(Locale.getDefault()) }
                    val smsUi =
                        smsRecords.map { sms ->
                            LinkedSmsItemUi(
                                id = sms.id,
                                sender = sms.sender,
                                snippet = sms.messageBodySnippet,
                                receivedLabel = dateFormat.format(Date(sms.receivedTime)),
                                amountFormatted = sms.parsedAmount?.let { "₹${"%.2f".format(it)}" },
                            )
                        }
                    TransactionDetailUiState(
                        isLoading = false,
                        transactionId = txn.id,
                        title =
                            txn.note?.trim()?.takeIf { it.isNotEmpty() }
                                ?: details.category?.name ?: "Transaction",
                        amountFormatted = "$sign₹${"%.2f".format(txn.amount)}",
                        typeLabel = typeLabel,
                        dateTimeLabel = dateFormat.format(Date(txn.dateTime)),
                        dateTimeMillis = txn.dateTime,
                        categoryLabel =
                            TransactionCategoryLabels.format(
                                details.category?.name,
                                details.subcategory?.name,
                            ),
                        accountLabel = details.account?.name ?: "Account",
                        note = txn.note,
                        paymentChannelLabel = formatPaymentChannel(txn.paymentChannel),
                        paymentInstrumentLabel = formatPaymentInstrument(txn.paymentInstrument),
                        cardMetadataLabel = formatCardMetadata(txn.cardLastFour, txn.cardNetwork),
                        linkedSms = smsUi,
                        showDeleteConfirmDialog = overlays.showDeleteConfirmDialog,
                        showLoanDeleteDialog = overlays.showLoanDeleteDialog,
                        showDuplicateDateDialog = overlays.showDuplicateDateDialog,
                        isDeleting = overlays.isDeleting,
                    )
                }
            }.catch {
                emit(
                    TransactionDetailUiState(
                        isLoading = false,
                        transactionId = transactionId,
                        errorMessage = MmrUiMessages.friendly(it.message, MmrUiMessages.GENERIC_LOAD),
                    ),
                )
            }.stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                TransactionDetailUiState(isLoading = true, transactionId = transactionId),
            )

        fun onEvent(event: TransactionDetailUiEvent) {
            when (event) {
                TransactionDetailUiEvent.OnDeleteClick -> {
                    _overlays.update { it.copy(showDeleteConfirmDialog = true) }
                }

                TransactionDetailUiEvent.OnDismissDeleteConfirm -> {
                    _overlays.update { it.copy(showDeleteConfirmDialog = false) }
                }

                TransactionDetailUiEvent.OnConfirmDelete -> {
                    _overlays.update { it.copy(showDeleteConfirmDialog = false) }
                    viewModelScope.launch {
                        val linked = transactionRepository.hasLinkedLoanPayment(transactionId)
                        if (linked) {
                            _overlays.update { it.copy(showLoanDeleteDialog = true) }
                        } else {
                            deleteTransaction(LoanLinkedTransactionDeleteMode.UNLINK)
                        }
                    }
                }

                TransactionDetailUiEvent.OnDismissLoanDeleteDialog -> {
                    _overlays.update { it.copy(showLoanDeleteDialog = false) }
                }

                is TransactionDetailUiEvent.OnConfirmDeleteWithLoanPolicy -> {
                    _overlays.update { it.copy(showLoanDeleteDialog = false) }
                    deleteTransaction(event.mode)
                }

                TransactionDetailUiEvent.OnDuplicateClick -> {
                    _overlays.update { it.copy(showDuplicateDateDialog = true) }
                }

                TransactionDetailUiEvent.OnDismissDuplicateDateDialog -> {
                    _overlays.update { it.copy(showDuplicateDateDialog = false) }
                }

                TransactionDetailUiEvent.OnDuplicateSameDate -> {
                    _overlays.update { it.copy(showDuplicateDateDialog = false) }
                    val millis = uiState.value.dateTimeMillis
                    _duplicateNavigation.tryEmit(
                        DuplicateNavigation(
                            sourceTransactionId = transactionId,
                            dateMillis = millis,
                        ),
                    )
                }

                TransactionDetailUiEvent.OnDuplicateToday -> {
                    _overlays.update { it.copy(showDuplicateDateDialog = false) }
                    _duplicateNavigation.tryEmit(
                        DuplicateNavigation(
                            sourceTransactionId = transactionId,
                            dateMillis = System.currentTimeMillis(),
                        ),
                    )
                }
            }
        }

        private fun deleteTransaction(mode: LoanLinkedTransactionDeleteMode) {
            viewModelScope.launch {
                _overlays.update { it.copy(isDeleting = true) }
                try {
                    val existing =
                        transactionRepository.getTransactionDetailedById(transactionId)
                            ?: throw IllegalArgumentException("Transaction not found.")
                    transactionRepository.deleteTransactionWithLoanPolicy(existing.transaction, mode)
                    _deleted.tryEmit(Unit)
                } catch (e: Exception) {
                    _overlays.update { it.copy(isDeleting = false) }
                }
            }
        }
    }
