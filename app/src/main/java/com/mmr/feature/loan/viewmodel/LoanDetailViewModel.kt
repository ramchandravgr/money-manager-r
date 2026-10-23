package com.mmr.feature.loan.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mmr.app.ui.state.MmrUiMessages
import com.mmr.core.common.validation.MmrValidation
import com.mmr.core.database.entity.LoanInterestRevisionEntity
import com.mmr.core.database.entity.LoanPaymentEntity
import com.mmr.core.domain.repository.LoanRepository
import com.mmr.core.loan.LoanAmortizationEngine
import com.mmr.core.loan.LoanDateUtils
import com.mmr.core.loan.LoanDisplayFormat
import com.mmr.core.loan.LoanFlexibleInterest
import com.mmr.core.loan.repaymentModeEnum
import com.mmr.core.model.enums.LoanPartPaymentAdjustment
import com.mmr.core.model.enums.LoanPaymentType
import com.mmr.core.model.enums.LoanScheduleStatus
import com.mmr.core.model.enums.LoanStatus
import com.mmr.core.model.enums.LoanTopUpAdjustment
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
class LoanDetailViewModel
    @Inject
    constructor(
        private val loanRepository: LoanRepository,
        savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        private val loanId: Long = checkNotNull(savedStateHandle["loanId"])

        private val _isSubmitting = MutableStateFlow(false)
        private val _isRepaymentSuccess = MutableStateFlow(false)
        private val _repaymentAmountInput = MutableStateFlow("")
        private val _repaymentNotesInput = MutableStateFlow("")
        private val _repaymentType = MutableStateFlow(LoanPaymentType.PART_PAYMENT)
        private val _repaymentPaymentDateMillis = MutableStateFlow(System.currentTimeMillis())
        private val _partPaymentAdjustment = MutableStateFlow(LoanPartPaymentAdjustment.REDUCE_TENURE)
        private val _showRateChangeDialog = MutableStateFlow(false)
        private val _showEmiChangeDialog = MutableStateFlow(false)
        private val _rateChangeInput = MutableStateFlow("")
        private val _emiChangeInput = MutableStateFlow("")
        private val _errorMessage = MutableStateFlow<String?>(null)
        private val _loadErrorMessage = MutableStateFlow<String?>(null)
        private val _statusMessage = MutableStateFlow<String?>(null)
        private val _retryKey = MutableStateFlow(0)
        private val _interestRevisions = MutableStateFlow<List<LoanInterestRevisionEntity>>(emptyList())
        private val _revisionDetail = MutableStateFlow<LoanRevisionDetailUi?>(null)
        private val _loanDeleted = MutableStateFlow(false)
        private val _showTopUpDialog = MutableStateFlow(false)
        private val _topUpAmountInput = MutableStateFlow("")
        private val _topUpNotesInput = MutableStateFlow("")
        private val _topUpDateMillis = MutableStateFlow(System.currentTimeMillis())
        private val _topUpAdjustment = MutableStateFlow(LoanTopUpAdjustment.EXTEND_TENURE)
        private val _isTopUpSubmitting = MutableStateFlow(false)

        init {
            viewModelScope.launch {
                loanRepository.getInterestRevisions(loanId).collect { _interestRevisions.value = it }
            }
        }

        private val loanWithPaymentsFlow =
            _retryKey.flatMapLatest {
                loanRepository
                    .getLoanWithPaymentsById(loanId)
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
            val paymentDateMillis: Long,
            val errorMessage: String?,
        )

        private val repaymentFormFlow =
            combine(
                combine(
                    _isSubmitting,
                    _isRepaymentSuccess,
                    _repaymentAmountInput,
                    _repaymentNotesInput,
                    _repaymentType,
                ) { submitting, success, amount, notes, repType ->
                    submitting to success to Triple(amount, notes, repType)
                },
                _repaymentPaymentDateMillis,
                _errorMessage,
            ) { statusAndInputs, paymentDate, error ->
                val (statusPair, inputs) = statusAndInputs
                val (submitting, success) = statusPair
                val (amount, notes, repType) = inputs
                RepaymentFormState(
                    isSubmitting = submitting,
                    isSuccess = success,
                    amountInput = amount,
                    notesInput = notes,
                    repaymentType = repType,
                    paymentDateMillis = paymentDate,
                    errorMessage = error,
                )
            }

        private data class TopUpFormState(
            val showDialog: Boolean,
            val amountInput: String,
            val notesInput: String,
            val dateMillis: Long,
            val adjustment: LoanTopUpAdjustment,
            val isSubmitting: Boolean,
        )

        private val topUpFormFlow =
            combine(
                combine(
                    _showTopUpDialog,
                    _topUpAmountInput,
                    _topUpNotesInput,
                    _topUpDateMillis,
                    _topUpAdjustment,
                ) { show, amount, notes, date, adjustment ->
                    TopUpFormState(show, amount, notes, date, adjustment, isSubmitting = false)
                },
                _isTopUpSubmitting,
            ) { form, submitting ->
                form.copy(isSubmitting = submitting)
            }

        private val scheduleFlow = loanRepository.getScheduleEntries(loanId, LoanScheduleStatus.PLANNED)

        private data class LoanDetailMeta(
            val loadError: String?,
            val status: String?,
            val partAdj: LoanPartPaymentAdjustment,
            val showRate: Boolean,
            val showEmi: Boolean,
            val rateIn: String,
            val emiIn: String,
        )

        private data class LoanDetailAuxiliary(
            val meta: LoanDetailMeta,
            val revisionEntities: List<LoanInterestRevisionEntity>,
            val revisionDetail: LoanRevisionDetailUi?,
            val loanDeleted: Boolean,
        )

        private val auxiliaryFlow =
            combine(
                combine(
                    _loadErrorMessage,
                    _statusMessage,
                    _partPaymentAdjustment,
                    _showRateChangeDialog,
                    _showEmiChangeDialog,
                ) { loadError, status, partAdj, showRate, showEmi ->
                    LoanDetailMeta(loadError, status, partAdj, showRate, showEmi, "", "")
                },
                combine(_rateChangeInput, _emiChangeInput) { rateIn, emiIn -> rateIn to emiIn },
                combine(_interestRevisions, _revisionDetail) { revisions, detail -> revisions to detail },
                _loanDeleted,
            ) { metaBase, rateEmi, revisionData, loanDeleted ->
                LoanDetailAuxiliary(
                    meta = metaBase.copy(rateIn = rateEmi.first, emiIn = rateEmi.second),
                    revisionEntities = revisionData.first,
                    revisionDetail = revisionData.second,
                    loanDeleted = loanDeleted,
                )
            }

        val uiState: StateFlow<LoanDetailUiState> =
            combine(
                loanWithPaymentsFlow,
                scheduleFlow,
                repaymentFormFlow,
                topUpFormFlow,
                auxiliaryFlow,
            ) { loanWithPayments, schedule, form, topUp, auxiliary ->
                val meta = auxiliary.meta
                val revisionEntities = auxiliary.revisionEntities
                val revisionDetail = auxiliary.revisionDetail
                val loanDeleted = auxiliary.loanDeleted
                if (loanWithPayments == null) {
                    LoanDetailUiState(
                        isLoading = false,
                        errorMessage = meta.loadError ?: "Loan not found.",
                    )
                } else {
                    val entity = loanWithPayments.loan
                    val loanUi = entity.toItemUi()
                    val paymentUis =
                        loanWithPayments.payments
                            .sortedByDescending { it.paymentDate }
                            .map { it.toPaymentUi() }
                    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                    val scheduleUi =
                        schedule.take(12).map {
                            LoanScheduleItemUi(
                                dueDateLabel = dateFormat.format(Date(it.dueDateMillis)),
                                emiFormatted = "₹${"%.2f".format(it.emiAmount)}",
                                principalFormatted = "₹${"%.2f".format(it.principalPlanned)}",
                                interestFormatted = "₹${"%.2f".format(it.interestPlanned)}",
                            )
                        }
                    val revisionUi =
                        revisionEntities.map { revision ->
                            LoanInterestRevisionItemUi(
                                id = revision.id,
                                effectiveDateLabel = dateFormat.format(Date(revision.effectiveFromMillis)),
                                rateLabel = LoanDisplayFormat.annualRatePerAnnumLabel(revision.annualRatePercent),
                                note = revision.note,
                            )
                        }
                    val flexibleInterestPendingFormatted: String?
                    val flexibleInterestAccrualFromLabel: String?
                    val flexibleRepaymentAccruedInterestHint: String?
                    if (
                        entity.repaymentModeEnum() == com.mmr.core.model.enums.LoanRepaymentMode.FLEXIBLE &&
                        entity.outstandingAmount > 0.0 &&
                        entity.status != LoanStatus.CLOSED
                    ) {
                        val accrualStart =
                            LoanFlexibleInterest.accrualStartMillis(
                                entity,
                                loanWithPayments.payments,
                            )
                        val pendingNow =
                            LoanFlexibleInterest.pendingInterest(
                                entity.outstandingAmount,
                                entity.interestRate,
                                accrualStart,
                                System.currentTimeMillis(),
                            )
                        val pendingAtPaymentDate =
                            LoanFlexibleInterest.pendingInterest(
                                entity.outstandingAmount,
                                entity.interestRate,
                                accrualStart,
                                form.paymentDateMillis,
                            )
                        flexibleInterestPendingFormatted = "₹${"%.2f".format(pendingNow)}"
                        flexibleInterestAccrualFromLabel = dateFormat.format(Date(accrualStart))
                        flexibleRepaymentAccruedInterestHint = "₹${"%.2f".format(pendingAtPaymentDate)}"
                    } else {
                        flexibleInterestPendingFormatted = null
                        flexibleInterestAccrualFromLabel = null
                        flexibleRepaymentAccruedInterestHint = null
                    }

                    LoanDetailUiState(
                        isLoading = false,
                        loan = loanUi,
                        repaymentMode = entity.repaymentModeEnum(),
                        fixedEmiFormatted = entity.fixedEmiAmount?.let { "₹${"%.2f".format(it)}" },
                        remainingTenureMonths = entity.remainingTenureMonths,
                        upcomingSchedule = scheduleUi,
                        payments = paymentUis,
                        isRepaymentSubmitting = form.isSubmitting,
                        isRepaymentSuccess = form.isSuccess,
                        repaymentAmountInput = form.amountInput,
                        repaymentNotesInput = form.notesInput,
                        repaymentPaymentDateMillis = form.paymentDateMillis,
                        repaymentType = form.repaymentType,
                        partPaymentAdjustment = meta.partAdj,
                        showRateChangeDialog = meta.showRate,
                        showEmiChangeDialog = meta.showEmi,
                        rateChangeInput = meta.rateIn,
                        emiChangeInput = meta.emiIn,
                        errorMessage = form.errorMessage,
                        statusMessage = meta.status,
                        interestRevisions = revisionUi,
                        revisionDetail = revisionDetail,
                        isLoanDeleted = loanDeleted,
                        showTopUpDialog = topUp.showDialog,
                        topUpAmountInput = topUp.amountInput,
                        topUpNotesInput = topUp.notesInput,
                        topUpDateMillis = topUp.dateMillis,
                        topUpAdjustment = topUp.adjustment,
                        isTopUpSubmitting = topUp.isSubmitting,
                        flexibleInterestPendingFormatted = flexibleInterestPendingFormatted,
                        flexibleInterestAccrualFromLabel = flexibleInterestAccrualFromLabel,
                        flexibleRepaymentAccruedInterestHint = flexibleRepaymentAccruedInterestHint,
                    )
                }
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = LoanDetailUiState(isLoading = true),
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

                is LoanDetailUiEvent.OnRepaymentPaymentDateChanged -> {
                    _repaymentPaymentDateMillis.value =
                        LoanDateUtils.normalizeLoanCalendarDay(event.paymentDateMillis)
                }

                is LoanDetailUiEvent.OnRepaymentTypeSelected -> {
                    _repaymentType.value = event.type
                    if (event.type == LoanPaymentType.CLOSING_PAYMENT) {
                        uiState.value.loan?.outstandingAmount?.let { outstanding ->
                            _repaymentAmountInput.value = "%.2f".format(outstanding)
                        }
                    }
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
                    _repaymentType.value = LoanPaymentType.PART_PAYMENT
                    _repaymentPaymentDateMillis.value = System.currentTimeMillis()
                    _errorMessage.value = null
                }

                LoanDetailUiEvent.OnDeleteLoan -> {
                    viewModelScope.launch {
                        try {
                            val loan = loanRepository.getLoanById(loanId)
                            if (loan != null) {
                                loanRepository.deleteLoan(loan)
                                _loanDeleted.value = true
                            } else {
                                _statusMessage.value = "Loan not found."
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

                is LoanDetailUiEvent.OnPartPaymentAdjustmentSelected -> {
                    _partPaymentAdjustment.value = event.adjustment
                }

                LoanDetailUiEvent.OnOpenRateChangeDialog -> {
                    val rate = uiState.value.loan?.interestRate
                    _rateChangeInput.value = rate?.let {
                        LoanDisplayFormat.annualRatePercent(it).removeSuffix("%")
                    } ?: ""
                    _showRateChangeDialog.value = true
                }

                LoanDetailUiEvent.OnDismissRateChangeDialog -> {
                    _showRateChangeDialog.value = false
                }

                is LoanDetailUiEvent.OnRateChangeInputChanged -> {
                    _rateChangeInput.value = event.value.filter { it.isDigit() || it == '.' }
                }

                LoanDetailUiEvent.OnSubmitRateChange -> submitRateChange()

                LoanDetailUiEvent.OnOpenEmiChangeDialog -> {
                    _emiChangeInput.value = uiState.value.fixedEmiFormatted
                        ?.removePrefix("₹")
                        ?.trim()
                        ?: ""
                    _showEmiChangeDialog.value = true
                }

                LoanDetailUiEvent.OnDismissEmiChangeDialog -> {
                    _showEmiChangeDialog.value = false
                }

                is LoanDetailUiEvent.OnEmiChangeInputChanged -> {
                    _emiChangeInput.value = event.value.filter { it.isDigit() || it == '.' }
                }

                LoanDetailUiEvent.OnSubmitEmiChange -> submitEmiChange()

                is LoanDetailUiEvent.OnInterestRevisionClick -> showRevisionDetail(event.revisionId)

                LoanDetailUiEvent.OnDismissRevisionDetail -> {
                    _revisionDetail.value = null
                }

                LoanDetailUiEvent.OnOpenTopUpDialog -> {
                    _topUpAmountInput.value = ""
                    _topUpNotesInput.value = ""
                    _topUpDateMillis.value = System.currentTimeMillis()
                    _topUpAdjustment.value = LoanTopUpAdjustment.EXTEND_TENURE
                    _errorMessage.value = null
                    _showTopUpDialog.value = true
                }

                LoanDetailUiEvent.OnDismissTopUpDialog -> {
                    _showTopUpDialog.value = false
                    _topUpAmountInput.value = ""
                    _topUpNotesInput.value = ""
                    _errorMessage.value = null
                }

                is LoanDetailUiEvent.OnTopUpAmountChanged -> {
                    _topUpAmountInput.value = event.amount.filter { it.isDigit() || it == '.' }
                }

                is LoanDetailUiEvent.OnTopUpNotesChanged -> {
                    _topUpNotesInput.value = event.notes
                }

                is LoanDetailUiEvent.OnTopUpDateChanged -> {
                    _topUpDateMillis.value =
                        LoanDateUtils.normalizeLoanCalendarDay(event.dateMillis)
                }

                is LoanDetailUiEvent.OnTopUpAdjustmentSelected -> {
                    _topUpAdjustment.value = event.adjustment
                }

                LoanDetailUiEvent.OnSubmitTopUp -> submitTopUp()
            }
        }

        private fun submitTopUp() {
            val amount = MmrValidation.parsePositiveAmount(_topUpAmountInput.value)
            if (amount == null) {
                _errorMessage.value = "Enter a valid top-up amount."
                return
            }
            viewModelScope.launch {
                _isTopUpSubmitting.value = true
                _errorMessage.value = null
                try {
                    loanRepository.recordLoanTopUp(
                        loanId = loanId,
                        topUpAmount = amount,
                        topUpDate = _topUpDateMillis.value,
                        topUpAdjustment = _topUpAdjustment.value,
                        notes = _topUpNotesInput.value.ifBlank { null },
                    )
                    _showTopUpDialog.value = false
                    _topUpAmountInput.value = ""
                    _topUpNotesInput.value = ""
                    _statusMessage.value = "Top-up recorded."
                } catch (e: Exception) {
                    _errorMessage.value = MmrUiMessages.friendly(e.message, MmrUiMessages.GENERIC_SAVE)
                } finally {
                    _isTopUpSubmitting.value = false
                }
            }
        }

        private fun showRevisionDetail(revisionId: Long) {
            val loan = uiState.value.loan ?: return
            val ascending = _interestRevisions.value.sortedBy { it.effectiveFromMillis }
            val currentIndex = ascending.indexOfFirst { it.id == revisionId }
            if (currentIndex < 0) return
            val current = ascending[currentIndex]
            val previousRate =
                if (currentIndex > 0) {
                    ascending[currentIndex - 1].annualRatePercent
                } else {
                    current.annualRatePercent
                }
            val tenure = uiState.value.remainingTenureMonths
            val outstanding = loan.outstandingAmount
            val emiBefore =
                if (tenure != null && tenure > 0 && outstanding > 0) {
                    LoanAmortizationEngine.computeEmi(outstanding, previousRate, tenure)
                } else {
                    null
                }
            val emiAfter =
                if (tenure != null && tenure > 0 && outstanding > 0) {
                    LoanAmortizationEngine.computeEmi(outstanding, current.annualRatePercent, tenure)
                } else {
                    null
                }
            _revisionDetail.value =
                LoanRevisionDetailUi(
                    beforeRateLabel = LoanDisplayFormat.annualRatePerAnnumLabel(previousRate),
                    afterRateLabel = LoanDisplayFormat.annualRatePerAnnumLabel(current.annualRatePercent),
                    emiBeforeFormatted = emiBefore?.let { "₹${"%.2f".format(it)}" },
                    emiAfterFormatted = emiAfter?.let { "₹${"%.2f".format(it)}" },
                    note = current.note,
                )
        }

        private fun submitRateChange() {
            val rate = _rateChangeInput.value.toDoubleOrNull()
            if (rate == null || rate < 0.0) {
                _errorMessage.value = "Enter a valid interest rate."
                return
            }
            viewModelScope.launch {
                try {
                    loanRepository.changeInterestRate(loanId, rate)
                    _showRateChangeDialog.value = false
                    _statusMessage.value = "Interest rate updated. Future EMIs recalculated."
                } catch (e: Exception) {
                    _errorMessage.value = MmrUiMessages.friendly(e.message, MmrUiMessages.GENERIC_SAVE)
                }
            }
        }

        private fun submitEmiChange() {
            val emi = _emiChangeInput.value.toDoubleOrNull()
            if (emi == null || emi <= 0.0) {
                _errorMessage.value = "Enter a valid EMI amount."
                return
            }
            viewModelScope.launch {
                try {
                    loanRepository.updateFutureEmi(loanId, emi)
                    _showEmiChangeDialog.value = false
                    _statusMessage.value = "Future EMI updated."
                } catch (e: Exception) {
                    _errorMessage.value = MmrUiMessages.friendly(e.message, MmrUiMessages.GENERIC_SAVE)
                }
            }
        }

        private fun submitRepayment() {
            val amount = MmrValidation.parsePositiveAmount(_repaymentAmountInput.value)
            if (amount == null) {
                _errorMessage.value = "Enter a valid repayment amount greater than zero."
                return
            }
            val paymentDate = LoanDateUtils.normalizeLoanCalendarDay(_repaymentPaymentDateMillis.value)

            viewModelScope.launch {
                _isSubmitting.value = true
                _errorMessage.value = null
                try {
                    val adjustment =
                        if (
                            uiState.value.repaymentMode == com.mmr.core.model.enums.LoanRepaymentMode.FIXED_EMI &&
                            _repaymentType.value == LoanPaymentType.PART_PAYMENT
                        ) {
                            _partPaymentAdjustment.value
                        } else {
                            null
                        }
                    loanRepository.recordLoanPayment(
                        loanId = loanId,
                        paymentAmount = amount,
                        paymentType = _repaymentType.value,
                        paymentDate = paymentDate,
                        partPaymentAdjustment = adjustment,
                        notes = _repaymentNotesInput.value.ifBlank { null },
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
    val typeLabel =
        when (paymentType) {
            LoanPaymentType.TOP_UP -> "Top-up"
            else ->
                paymentType.name
                    .lowercase(Locale.getDefault())
                    .replace('_', ' ')
                    .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        }
    val amountFormatted =
        if (paymentType == LoanPaymentType.TOP_UP) {
            "+₹${"%.2f".format(paymentAmount)}"
        } else {
            "₹${"%.2f".format(paymentAmount)}"
        }

    return LoanPaymentItemUi(
        id = id,
        paymentAmount = paymentAmount,
        paymentAmountFormatted = amountFormatted,
        paymentDateFormatted = dateFormat.format(Date(paymentDate)),
        paymentType = paymentType,
        paymentTypeLabel = typeLabel,
        principalComponentFormatted = principalComponent?.let { "₹${"%.2f".format(it)}" },
        interestComponentFormatted = interestComponent?.let { "₹${"%.2f".format(it)}" },
        outstandingAfterFormatted = "₹${"%.2f".format(outstandingAfterPayment)}",
        notes = notes,
        linkedTransactionId = transactionId,
    )
}
