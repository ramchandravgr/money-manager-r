package com.mmr.feature.loan.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mmr.app.ui.state.MmrUiMessages
import com.mmr.core.database.entity.LoanPaymentEntity
import com.mmr.core.domain.repository.LoanRepository
import com.mmr.core.model.enums.LoanPaymentType
import com.mmr.core.model.enums.LoanStatus
import com.mmr.feature.loan.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class LoanDetailViewModel @Inject constructor(
    private val loanRepository: LoanRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val loanId: Long = checkNotNull(savedStateHandle["loanId"])

    private val _isSubmitting = MutableStateFlow(false)
    private val _isRepaymentSuccess = MutableStateFlow(false)
    private val _repaymentAmountInput = MutableStateFlow("")
    private val _repaymentNotesInput = MutableStateFlow("")
    private val _repaymentType = MutableStateFlow(LoanPaymentType.PART_PAYMENT)
    private val _errorMessage = MutableStateFlow<String?>(null)
    private val _loadErrorMessage = MutableStateFlow<String?>(null)
    private val _statusMessage = MutableStateFlow<String?>(null)
    private val _retryKey = MutableStateFlow(0)

    private val loanWithPaymentsFlow = _retryKey.flatMapLatest {
        loanRepository.getLoanWithPaymentsById(loanId)
            .catch { e ->
                _loadErrorMessage.value = MmrUiMessages.friendly(e.message, MmrUiMessages.GENERIC_LOAD)
                emit(null)
            }
    }

    private data class RepaymentFormState(
        val isSubmitting: Boolean,
        val isSuccess: Boolean,
        val amountInput: String,
        val notesInput: String,
        val repaymentType: LoanPaymentType,
        val errorMessage: String?
    )

    private val repaymentFormFlow = combine(
        _isSubmitting,
        _isRepaymentSuccess,
        _repaymentAmountInput,
        _repaymentNotesInput,
        _repaymentType
    ) { submitting, success, amount, notes, repType ->
        Pair(submitting, success) to Triple(amount, notes, repType)
    }.combine(_errorMessage) { (status, inputs), error ->
        RepaymentFormState(
            isSubmitting = status.first,
            isSuccess = status.second,
            amountInput = inputs.first,
            notesInput = inputs.second,
            repaymentType = inputs.third,
            errorMessage = error
        )
    }

    val uiState: StateFlow<LoanDetailUiState> = combine(
        loanWithPaymentsFlow,
        repaymentFormFlow,
        _loadErrorMessage,
        _statusMessage
    ) { loanWithPayments, form, loadError, status ->
        if (loanWithPayments == null) {
            LoanDetailUiState(
                isLoading = false,
                errorMessage = loadError ?: "Loan not found."
            )
        } else {
            val loanUi = loanWithPayments.loan.toItemUi()
            val paymentUis = loanWithPayments.payments
                .sortedByDescending { it.paymentDate }
                .map { it.toPaymentUi() }

            LoanDetailUiState(
                isLoading = false,
                loan = loanUi,
                payments = paymentUis,
                isRepaymentSubmitting = form.isSubmitting,
                isRepaymentSuccess = form.isSuccess,
                repaymentAmountInput = form.amountInput,
                repaymentNotesInput = form.notesInput,
                repaymentType = form.repaymentType,
                errorMessage = form.errorMessage,
                statusMessage = status
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = LoanDetailUiState(isLoading = true)
    )

    fun onEvent(event: LoanDetailUiEvent) {
        when (event) {
            is LoanDetailUiEvent.OnRepaymentAmountChanged -> {
                val clean = event.amount.filter { it.isDigit() || it == '.' }
                _repaymentAmountInput.value = clean
            }

            is LoanDetailUiEvent.OnRepaymentNotesChanged -> {
                _repaymentNotesInput.value = event.notes
            }

            is LoanDetailUiEvent.OnRepaymentTypeSelected -> {
                _repaymentType.value = event.type
            }

            LoanDetailUiEvent.OnSubmitRepayment -> {
                submitRepayment()
            }

            is LoanDetailUiEvent.OnStatusChanged -> {
                viewModelScope.launch {
                    try {
                        loanRepository.updateLoanStatus(loanId, event.status)
                    } catch (e: Exception) {
                        _statusMessage.value = MmrUiMessages.friendly(e.message, MmrUiMessages.GENERIC_ACTION)
                    }
                }
            }

            LoanDetailUiEvent.OnClearRepaymentState -> {
                _isRepaymentSuccess.value = false
                _repaymentAmountInput.value = ""
                _repaymentNotesInput.value = ""
                _errorMessage.value = null
            }

            LoanDetailUiEvent.OnDeleteLoan -> {
                viewModelScope.launch {
                    try {
                        val loan = loanRepository.getLoanById(loanId)
                        if (loan != null) {
                            loanRepository.deleteLoan(loan)
                        }
                    } catch (e: Exception) {
                        _statusMessage.value = MmrUiMessages.friendly(e.message, MmrUiMessages.GENERIC_ACTION)
                    }
                }
            }

            LoanDetailUiEvent.OnRetryClick -> {
                _loadErrorMessage.value = null
                _statusMessage.value = null
                _retryKey.value = _retryKey.value + 1
            }

            LoanDetailUiEvent.OnDismissStatus -> {
                _statusMessage.value = null
            }
        }
    }

    private fun submitRepayment() {
        val amount = _repaymentAmountInput.value.toDoubleOrNull()
        if (amount == null || amount <= 0.0) {
            _errorMessage.value = "Enter a valid repayment amount greater than zero."
            return
        }

        viewModelScope.launch {
            _isSubmitting.value = true
            _errorMessage.value = null
            try {
                loanRepository.recordLoanPayment(
                    loanId = loanId,
                    paymentAmount = amount,
                    paymentType = _repaymentType.value,
                    notes = _repaymentNotesInput.value.ifBlank { null }
                )
                _isRepaymentSuccess.value = true
                _repaymentAmountInput.value = ""
                _repaymentNotesInput.value = ""
            } catch (e: Exception) {
                _errorMessage.value = MmrUiMessages.friendly(e.message, MmrUiMessages.GENERIC_SAVE)
            } finally {
                _isSubmitting.value = false
            }
        }
    }
}

private fun LoanPaymentEntity.toPaymentUi(): LoanPaymentItemUi {
    val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
    val typeLabel = paymentType.name.lowercase(Locale.getDefault())
        .replace('_', ' ')
        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }

    return LoanPaymentItemUi(
        id = id,
        paymentAmount = paymentAmount,
        paymentAmountFormatted = "₹${"%.2f".format(paymentAmount)}",
        paymentDateFormatted = dateFormat.format(Date(paymentDate)),
        paymentType = paymentType,
        paymentTypeLabel = typeLabel,
        principalComponentFormatted = principalComponent?.let { "₹${"%.2f".format(it)}" },
        interestComponentFormatted = interestComponent?.let { "₹${"%.2f".format(it)}" },
        outstandingAfterFormatted = "₹${"%.2f".format(outstandingAfterPayment)}",
        notes = notes
    )
}
