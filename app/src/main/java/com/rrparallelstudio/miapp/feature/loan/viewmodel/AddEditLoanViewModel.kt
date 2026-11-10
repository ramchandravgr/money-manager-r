package com.rrparallelstudio.miapp.feature.loan.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rrparallelstudio.miapp.core.common.display.MmrMoneyFormat
import com.rrparallelstudio.miapp.core.common.validation.MmrValidation
import com.rrparallelstudio.miapp.core.database.entity.LoanEntity
import com.rrparallelstudio.miapp.core.domain.repository.AccountRepository
import com.rrparallelstudio.miapp.core.domain.repository.LoanRepository
import com.rrparallelstudio.miapp.core.loan.LoanAmortizationEngine
import com.rrparallelstudio.miapp.core.loan.LoanDateUtils
import com.rrparallelstudio.miapp.core.loan.disbursalDateMillis
import com.rrparallelstudio.miapp.core.loan.repaymentModeEnum
import com.rrparallelstudio.miapp.core.model.enums.AccountType
import com.rrparallelstudio.miapp.core.model.enums.LoanRepaymentMode
import com.rrparallelstudio.miapp.core.model.enums.LoanStatus
import com.rrparallelstudio.miapp.core.reminder.MmrReminderPresets
import com.rrparallelstudio.miapp.feature.loan.model.AddEditLoanUiEvent
import com.rrparallelstudio.miapp.feature.loan.model.AddEditLoanUiState
import com.rrparallelstudio.miapp.feature.loan.model.FixedEmiEntryMode
import com.rrparallelstudio.miapp.feature.loan.model.LoanLiabilityAccountUi
import com.rrparallelstudio.miapp.ui.state.MmrUiMessages
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
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
                    val emiStored = loan.fixedEmiAmount?.let { MmrMoneyFormat.formatNumber(it) } ?: ""
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
                            emiDayOfMonth = (loan.emiDayOfMonth ?: 1).toString(),
                            firstEmiDueMillis = loan.firstEmiDueMillis,
                            emiReminderDaysBefore = loan.emiReminderDaysBefore,
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
                    val clean = MmrValidation.sanitizeDecimalInput(event.principal)
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
                                        emiFromTenure?.let { MmrMoneyFormat.formatNumber(it) }
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
                    val clean = MmrValidation.sanitizeDecimalInput(event.emi)
                    _uiState.update { it.copy(fixedEmiOverride = clean) }
                }
                is AddEditLoanUiEvent.OnEmiDayChanged -> {
                    val clean = event.day.filter { it.isDigit() }.take(2)
                    _uiState.update { it.copy(emiDayOfMonth = clean) }
                }
                is AddEditLoanUiEvent.OnFirstEmiDueDateChanged -> {
                    val normalized =
                        event.firstEmiDueMillis?.let { LoanDateUtils.normalizeLoanCalendarDay(it) }
                    val emiDay =
                        normalized?.let { millis ->
                            Calendar
                                .getInstance()
                                .apply { timeInMillis = millis }
                                .get(Calendar.DAY_OF_MONTH)
                                .coerceIn(1, 28)
                                .toString()
                        }
                    _uiState.update {
                        it.copy(
                            firstEmiDueMillis = normalized,
                            emiDayOfMonth = emiDay ?: it.emiDayOfMonth,
                        )
                    }
                }
                is AddEditLoanUiEvent.OnEmiReminderDaysSelected -> {
                    _uiState.update {
                        it.copy(
                            emiReminderDaysBefore = event.days,
                            canSave = validate(it.copy(emiReminderDaysBefore = event.days)),
                        )
                    }
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
            val emiReminderValid =
                state.repaymentMode != LoanRepaymentMode.FIXED_EMI ||
                    MmrReminderPresets.isValidLoanEmiReminderDays(state.emiReminderDaysBefore)
            return state.name.isNotBlank() &&
                state.lenderName.isNotBlank() &&
                amount != null &&
                dueDateValid &&
                emiReminderValid
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
            val emiDay = resolveEmiDayOfMonth(state)
            val firstEmiDue =
                state.firstEmiDueMillis?.let { LoanDateUtils.normalizeLoanCalendarDay(it) }
            if (state.repaymentMode == LoanRepaymentMode.FIXED_EMI &&
                !MmrReminderPresets.isValidLoanEmiReminderDays(state.emiReminderDaysBefore)
            ) {
                _uiState.update { it.copy(errorMessage = "Choose how many days before EMI to remind you.") }
                return
            }
            val tenureInput = state.tenureMonths.toIntOrNull()?.takeIf { it > 0 }
            val emiInput = MmrValidation.parsePositiveAmount(state.fixedEmiOverride)
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
                                    "Monthly EMI must be at least ₹1 above interest (≥ ${MmrMoneyFormat.formatInr(
                                        floor,
                                    )}; interest ≈ ${MmrMoneyFormat.formatInr(interestOnly)})."
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
                                firstEmiDueMillis = firstEmiDue,
                                emiReminderDaysBefore = state.emiReminderDaysBefore,
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
                        loanRepository.updateLoanFromEdit(
                            updated = updated,
                            previous = existing,
                            noPaymentsYet = priorPayments.isEmpty(),
                        )
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
                                firstEmiDueMillis = firstEmiDue,
                                emiReminderDaysBefore = state.emiReminderDaysBefore,
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

        private fun resolveEmiDayOfMonth(state: AddEditLoanUiState): Int {
            state.firstEmiDueMillis?.let { millis ->
                return Calendar
                    .getInstance()
                    .apply { timeInMillis = millis }
                    .get(Calendar.DAY_OF_MONTH)
                    .coerceIn(1, 28)
            }
            return state.emiDayOfMonth.toIntOrNull()?.coerceIn(1, 28) ?: 1
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
