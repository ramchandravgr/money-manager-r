package com.mmr.feature.loan.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mmr.app.ui.state.MmrUiMessages
import com.mmr.core.common.validation.MmrValidation
import com.mmr.core.database.entity.LoanEntity
import com.mmr.core.domain.repository.AccountRepository
import com.mmr.core.domain.repository.LoanRepository
import com.mmr.core.model.enums.LoanStatus
import com.mmr.core.model.enums.LoanType
import com.mmr.feature.loan.model.AddEditLoanUiEvent
import com.mmr.feature.loan.model.AddEditLoanUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddEditLoanViewModel @Inject constructor(
    private val loanRepository: LoanRepository,
    private val accountRepository: AccountRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddEditLoanUiState())
    val uiState: StateFlow<AddEditLoanUiState> = _uiState.asStateFlow()

    fun onEvent(event: AddEditLoanUiEvent) {
        when (event) {
            is AddEditLoanUiEvent.OnNameChanged -> {
                _uiState.update { it.copy(name = event.name, canSave = validate(it.copy(name = event.name))) }
            }
            is AddEditLoanUiEvent.OnLenderNameChanged -> {
                _uiState.update { it.copy(lenderName = event.lenderName, canSave = validate(it.copy(lenderName = event.lenderName))) }
            }
            is AddEditLoanUiEvent.OnPrincipalChanged -> {
                val clean = event.principal.filter { it.isDigit() || it == '.' }
                _uiState.update { it.copy(principalAmount = clean, canSave = validate(it.copy(principalAmount = clean))) }
            }
            is AddEditLoanUiEvent.OnInterestRateChanged -> {
                val clean = event.rate.filter { it.isDigit() || it == '.' }
                _uiState.update { it.copy(interestRate = clean) }
            }
            is AddEditLoanUiEvent.OnLoanTypeSelected -> {
                _uiState.update { it.copy(loanType = event.type) }
            }
            is AddEditLoanUiEvent.OnDueDateChanged -> {
                _uiState.update { it.copy(dueDate = event.dueDate) }
            }
            is AddEditLoanUiEvent.OnLinkedDebitAccountSelected -> {
                _uiState.update { it.copy(linkedDebitAccountId = event.accountId) }
            }
            is AddEditLoanUiEvent.OnNotesChanged -> {
                _uiState.update { it.copy(notes = event.notes) }
            }
            AddEditLoanUiEvent.OnSaveClick -> {
                saveLoan()
            }
        }
    }

    private fun validate(state: AddEditLoanUiState): Boolean {
        val amount = MmrValidation.parsePositiveAmount(state.principalAmount)
        val dueDateValid = state.dueDate == null || MmrValidation.isValidEpochMillis(state.dueDate)
        return state.name.isNotBlank() && state.lenderName.isNotBlank() && amount != null && dueDateValid
    }

    private fun saveLoan() {
        val state = _uiState.value
        val principal = MmrValidation.parsePositiveAmount(state.principalAmount) ?: run {
            _uiState.update { it.copy(errorMessage = "Enter a valid principal amount.") }
            return
        }
        val interest = MmrValidation.sanitizeDecimalInput(state.interestRate).toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
        if (state.dueDate != null && !MmrValidation.isValidEpochMillis(state.dueDate)) {
            _uiState.update { it.copy(errorMessage = "Choose a valid due date.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            try {
                val loan = LoanEntity(
                    name = state.name.trim(),
                    lenderName = state.lenderName.trim(),
                    principalAmount = principal,
                    outstandingAmount = principal,
                    interestRate = interest,
                    openingDate = System.currentTimeMillis(),
                    dueDate = state.dueDate,
                    linkedDebitAccountId = state.linkedDebitAccountId,
                    loanType = state.loanType,
                    status = LoanStatus.ACTIVE,
                    notes = state.notes.ifBlank { null }
                )
                loanRepository.createLoan(loan)
                _uiState.update { it.copy(isSaving = false, isSaved = true) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isSaving = false, errorMessage = MmrUiMessages.friendly(e.message, MmrUiMessages.GENERIC_SAVE))
                }
            }
        }
    }
}
