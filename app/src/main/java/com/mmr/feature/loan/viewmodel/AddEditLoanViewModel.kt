package com.mmr.feature.loan.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mmr.app.ui.state.MmrUiMessages
import com.mmr.core.common.validation.MmrValidation
import com.mmr.core.database.entity.LoanEntity
import com.mmr.core.domain.repository.AccountRepository
import com.mmr.core.domain.repository.LoanRepository
import com.mmr.core.loan.LoanAmortizationEngine
import com.mmr.core.loan.LoanDateUtils
import com.mmr.core.loan.disbursalDateMillis
import com.mmr.core.loan.repaymentModeEnum
import com.mmr.core.model.enums.AccountType
import com.mmr.core.model.enums.LoanRepaymentMode
import com.mmr.core.model.enums.LoanStatus
import com.mmr.feature.loan.model.AddEditLoanUiEvent
import com.mmr.feature.loan.model.AddEditLoanUiState
import com.mmr.feature.loan.model.FixedEmiEntryMode
import com.mmr.feature.loan.model.LoanLiabilityAccountUi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddEditLoanViewModel
    @Inject
    constructor(
        private val loanRepository: LoanRepository,
        private val accountRepository: AccountRepository,
        savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        private val editLoanId: Long? = savedStateHandle.get<Long>("loanId")?.takeIf { it > 0L }

        private val _uiState = MutableStateFlow(AddEditLoanUiState(loanId = editLoanId))
        val uiState: StateFlow<AddEditLoanUiState> = _uiState.asStateFlow()

        init {
            viewModelScope.launch {
                val liabilityAccounts =
                    accountRepository
                        .getAccounts()
                        .first()
                        .filter { it.type == AccountType.LOAN && it.isActive }
                        .map { LoanLiabilityAccountUi(it.id, it.name) }
                _uiState.update { it.copy(loanLiabilityAccounts = liabilityAccounts) }
            }
            editLoanId?.let { loadLoan(it) }
        }

        private fun loadLoan(loanId: Long) {
            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true) }
                try {
                    val loan = loanRepository.getLoanById(loanId)
                    if (loan == null) {
                        _uiState.update { it.copy(isLoading = false, errorMessage = "Loan not found.") }
                        return@launch
                    }
                    val payments = loanRepository.getPaymentsForLoan(loanId).first()
                    val entryMode =
                        loan.fixedEmiInputMode
                            ?.let { runCatching { FixedEmiEntryMode.valueOf(it) }.getOrNull() }
                            ?: FixedEmiEntryMode.TENURE
                    val tenureStored =
                        loan.remainingTenureMonths?.toString()
                            ?: loan.tenureMonths?.toString()
                            ?: ""
                    val emiStored = loan.fixedEmiAmount?.let { "%.2f".format(it) } ?: ""
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            loanId = loan.id,
                            name = loan.name,
                            lenderName = loan.lenderName,
                            principalAmount = loan.principalAmount.toString(),
                            interestRate = loan.interestRate.toString(),
                            loanType = loan.loanType,
                            dueDate = loan.dueDate,
                            disbursalDateMillis = loan.disbursalDateMillis(),
                            canEditDisbursalDate = payments.isEmpty(),
                            linkedDebitAccountId = loan.linkedDebitAccountId,
                            linkedLoanAccountId = loan.linkedLoanAccountId,
                            repaymentMode = loan.repaymentModeEnum(),
                            originalRepaymentMode = loan.repaymentModeEnum(),
                            fixedEmiEntryMode = entryMode,
                            tenureMonths = if (entryMode == FixedEmiEntryMode.TENURE) tenureStored else "",
                            fixedEmiOverride = if (entryMode == FixedEmiEntryMode.EMI_AMOUNT) emiStored else "",
                            emiDayOfMonth = loan.emiDayOfMonth.toString(),
                            notes = loan.notes ?: "",
                            canSave = true,
                        )
                    }
                } catch (e: Exception) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = MmrUiMessages.friendly(e.message, MmrUiMessages.GENERIC_LOAD),
                        )
                    }
                }
            }
        }

        fun onEvent(event: AddEditLoanUiEvent) {
            when (event) {
                is AddEditLoanUiEvent.OnNameChanged -> {
                    _uiState.update { it.copy(name = event.name, canSave = validate(it.copy(name = event.name))) }
                }
                is AddEditLoanUiEvent.OnLenderNameChanged -> {
                    _uiState.update {
                        it.copy(
                            lenderName = event.lenderName,
                            canSave = validate(it.copy(lenderName = event.lenderName)),
                        )
                    }
                }
                is AddEditLoanUiEvent.OnPrincipalChanged -> {
                    if (_uiState.value.loanId != null) return
                    val clean = event.principal.filter { it.isDigit() || it == '.' }
                    _uiState.update {
                        it.copy(
                            principalAmount = clean,
                            canSave = validate(it.copy(principalAmount = clean)),
                        )
                    }
                }
                is AddEditLoanUiEvent.OnInterestRateChanged -> {
                    val clean = event.rate.filter { it.isDigit() || it == '.' }
                    _uiState.update { it.copy(interestRate = clean) }
                }
                is AddEditLoanUiEvent.OnLoanTypeSelected -> {
                    if (_uiState.value.loanId != null) return
                    _uiState.update { it.copy(loanType = event.type) }
                }
                is AddEditLoanUiEvent.OnDueDateChanged -> {
                    _uiState.update { it.copy(dueDate = event.dueDate) }
                }
                is AddEditLoanUiEvent.OnDisbursalDateChanged -> {
                    if (!_uiState.value.canEditDisbursalDate) return
                    val normalized = LoanDateUtils.normalizeLoanCalendarDay(event.disbursalDateMillis)
                    _uiState.update { it.copy(disbursalDateMillis = normalized) }
                }
                is AddEditLoanUiEvent.OnLinkedDebitAccountSelected -> {
                    _uiState.update { it.copy(linkedDebitAccountId = event.accountId) }
                }
                is AddEditLoanUiEvent.OnLinkedLoanAccountSelected -> {
                    _uiState.update { it.copy(linkedLoanAccountId = event.accountId) }
                }
                is AddEditLoanUiEvent.OnNotesChanged -> {
                    _uiState.update { it.copy(notes = event.notes) }
                }
                is AddEditLoanUiEvent.OnRepaymentModeSelected -> {
                    _uiState.update { it.copy(repaymentMode = event.mode) }
                }
                is AddEditLoanUiEvent.OnFixedEmiEntryModeSelected -> {
                    if (event.mode == _uiState.value.fixedEmiEntryMode) return
                    _uiState.update { current ->
                        val principal = MmrValidation.parsePositiveAmount(current.principalAmount)
                        val interest =
                            MmrValidation
                                .sanitizeDecimalInput(
                                    current.interestRate,
                                ).toDoubleOrNull()
                                ?.coerceAtLeast(0.0)
                                ?: 0.0
                        when (event.mode) {
                            FixedEmiEntryMode.TENURE -> {
                                val emi = current.fixedEmiOverride.toDoubleOrNull()
                                val tenureFromEmi =
                                    if (principal != null && emi != null) {
                                        LoanAmortizationEngine.computeTenureMonths(principal, interest, emi)
                                    } else {
                                        null
                                    }
                                current.copy(
                                    fixedEmiEntryMode = event.mode,
                                    fixedEmiOverride = "",
                                    tenureMonths = tenureFromEmi?.toString() ?: current.tenureMonths,
                                )
                            }
                            FixedEmiEntryMode.EMI_AMOUNT -> {
                                val tenure = current.tenureMonths.toIntOrNull()
                                val emiFromTenure =
                                    if (principal != null && tenure != null && tenure > 0) {
                                        LoanAmortizationEngine.computeEmi(principal, interest, tenure)
                                    } else {
                                        null
                                    }
                                current.copy(
                                    fixedEmiEntryMode = event.mode,
                                    tenureMonths = "",
                                    fixedEmiOverride =
                                        emiFromTenure?.let { "%.2f".format(it) }
                                            ?: current.fixedEmiOverride,
                                )
                            }
                        }
                    }
                }
                is AddEditLoanUiEvent.OnTenureMonthsChanged -> {
                    val clean = event.months.filter { it.isDigit() }
                    _uiState.update { it.copy(tenureMonths = clean) }
                }
                is AddEditLoanUiEvent.OnFixedEmiOverrideChanged -> {
                    val clean = event.emi.filter { it.isDigit() || it == '.' }
                    _uiState.update { it.copy(fixedEmiOverride = clean) }
                }
                is AddEditLoanUiEvent.OnEmiDayChanged -> {
                    val clean = event.day.filter { it.isDigit() }.take(2)
                    _uiState.update { it.copy(emiDayOfMonth = clean) }
                }
                AddEditLoanUiEvent.OnSaveClick -> {
                    saveLoan()
                }
            }
        }

        private fun validate(state: AddEditLoanUiState): Boolean {
            val amount =
                if (state.loanId != null) {
                    MmrValidation.parsePositiveAmount(state.principalAmount)
                } else {
                    MmrValidation.parsePositiveAmount(state.principalAmount)
                }
            val dueDateValid = state.dueDate == null || MmrValidation.isValidEpochMillis(state.dueDate)
            return state.name.isNotBlank() && state.lenderName.isNotBlank() && amount != null && dueDateValid
        }

        private fun saveLoan() {
            val state = _uiState.value
            val isEdit = state.loanId != null && state.loanId > 0L
            val principal =
                MmrValidation.parsePositiveAmount(state.principalAmount) ?: run {
                    _uiState.update { it.copy(errorMessage = "Enter a valid principal amount.") }
                    return
                }
            val interest =
                MmrValidation.sanitizeDecimalInput(state.interestRate).toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
            if (state.dueDate != null && !MmrValidation.isValidEpochMillis(state.dueDate)) {
                _uiState.update { it.copy(errorMessage = "Choose a valid due date.") }
                return
            }
            val emiDay = state.emiDayOfMonth.toIntOrNull()?.coerceIn(1, 28) ?: 1
            val tenureInput = state.tenureMonths.toIntOrNull()?.takeIf { it > 0 }
            val emiInput = state.fixedEmiOverride.toDoubleOrNull()?.takeIf { it > 0.0 }
            val (tenureMonths, fixedEmi) =
                if (state.repaymentMode == LoanRepaymentMode.FIXED_EMI) {
                    resolveFixedEmiAndTenure(
                        principal = principal,
                        interest = interest,
                        mode = state.fixedEmiEntryMode,
                        tenureInput = tenureInput,
                        emiInput = emiInput,
                    ) ?: run {
                        val message =
                            when {
                                state.fixedEmiEntryMode == FixedEmiEntryMode.EMI_AMOUNT &&
                                    emiInput != null &&
                                    LoanAmortizationEngine.computeTenureMonths(principal, interest, emiInput) <= 0 -> {
                                    val floor = LoanAmortizationEngine.minimumEmiForAmortization(principal, interest)
                                    val interestOnly = LoanAmortizationEngine.firstMonthInterest(principal, interest)
                                    "Monthly EMI must be at least ₹1 above interest (≥ ₹${"%.2f".format(
                                        floor,
                                    )}; interest ≈ ₹${"%.2f".format(interestOnly)})."
                                }
                                state.fixedEmiEntryMode == FixedEmiEntryMode.TENURE ->
                                    "Enter tenure in months."
                                else -> "Enter a valid monthly EMI amount."
                            }
                        _uiState.update { it.copy(errorMessage = message) }
                        return
                    }
                } else {
                    null to null
                }

            viewModelScope.launch {
                _uiState.update { it.copy(isSaving = true, errorMessage = null) }
                try {
                    if (isEdit) {
                        val existing =
                            loanRepository.getLoanById(state.loanId!!)
                                ?: throw IllegalArgumentException("Loan not found.")
                        if (existing.status == LoanStatus.CLOSED) {
                            throw IllegalStateException("Closed loans cannot be edited.")
                        }
                        val priorPayments = loanRepository.getPaymentsForLoan(state.loanId!!).first()
                        val disbursalMillis = LoanDateUtils.normalizeLoanCalendarDay(state.disbursalDateMillis)
                        val updated =
                            existing.copy(
                                name = state.name.trim(),
                                lenderName = state.lenderName.trim(),
                                interestRate = interest,
                                openingDate = if (priorPayments.isEmpty()) disbursalMillis else existing.openingDate,
                                dueDate = state.dueDate,
                                linkedDebitAccountId = state.linkedDebitAccountId,
                                linkedLoanAccountId = state.linkedLoanAccountId,
                                emiDayOfMonth = emiDay,
                                tenureMonths = tenureMonths ?: existing.tenureMonths,
                                remainingTenureMonths = tenureMonths ?: existing.remainingTenureMonths,
                                fixedEmiAmount = fixedEmi ?: existing.fixedEmiAmount,
                                fixedEmiInputMode =
                                    if (state.repaymentMode == LoanRepaymentMode.FIXED_EMI) {
                                        state.fixedEmiEntryMode.name
                                    } else {
                                        null
                                    },
                                notes = state.notes.ifBlank { null },
                                updatedAt = System.currentTimeMillis(),
                            )
                        loanRepository.updateLoan(updated)
                        if (state.repaymentMode == LoanRepaymentMode.FIXED_EMI &&
                            tenureMonths != null &&
                            fixedEmi != null
                        ) {
                            loanRepository.refreshPlannedEmiSchedule(state.loanId!!)
                        }
                        if (state.originalRepaymentMode != state.repaymentMode) {
                            loanRepository.changeRepaymentMode(
                                loanId = state.loanId,
                                newMode = state.repaymentMode,
                                fixedEmiOverride = fixedEmi,
                                tenureMonths = tenureMonths,
                            )
                        }
                    } else {
                        val disbursalMillis = LoanDateUtils.normalizeLoanCalendarDay(state.disbursalDateMillis)
                        val loan =
                            LoanEntity(
                                name = state.name.trim(),
                                lenderName = state.lenderName.trim(),
                                principalAmount = principal,
                                outstandingAmount = principal,
                                interestRate = interest,
                                openingDate = disbursalMillis,
                                dueDate = state.dueDate,
                                linkedDebitAccountId = state.linkedDebitAccountId,
                                loanType = state.loanType,
                                status = LoanStatus.ACTIVE,
                                repaymentMode = state.repaymentMode.name,
                                tenureMonths = tenureMonths,
                                remainingTenureMonths = tenureMonths,
                                fixedEmiAmount = fixedEmi,
                                emiDayOfMonth = emiDay,
                                fixedEmiInputMode =
                                    if (state.repaymentMode == LoanRepaymentMode.FIXED_EMI) {
                                        state.fixedEmiEntryMode.name
                                    } else {
                                        null
                                    },
                                notes = state.notes.ifBlank { null },
                            )
                        loanRepository.createLoan(loan)
                    }
                    _uiState.update { it.copy(isSaving = false, isSaved = true) }
                } catch (e: Exception) {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            errorMessage = MmrUiMessages.friendly(e.message, MmrUiMessages.GENERIC_SAVE),
                        )
                    }
                }
            }
        }

        private fun resolveFixedEmiAndTenure(
            principal: Double,
            interest: Double,
            mode: FixedEmiEntryMode,
            tenureInput: Int?,
            emiInput: Double?,
        ): Pair<Int, Double>? {
            return when (mode) {
                FixedEmiEntryMode.TENURE -> {
                    val tenure = tenureInput ?: return null
                    val emi = LoanAmortizationEngine.computeEmi(principal, interest, tenure)
                    tenure to emi
                }
                FixedEmiEntryMode.EMI_AMOUNT -> {
                    val emi = emiInput ?: return null
                    val months = LoanAmortizationEngine.computeTenureMonths(principal, interest, emi)
                    if (months <= 0) null else months to emi
                }
            }
        }
    }
