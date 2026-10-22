package com.mmr.feature.recurring.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mmr.app.ui.state.MmrUiMessages
import com.mmr.core.common.validation.MmrValidation
import com.mmr.core.database.entity.AccountEntity
import com.mmr.core.database.entity.CategoryEntity
import com.mmr.core.database.entity.RecurringRuleEntity
import com.mmr.core.domain.repository.AccountRepository
import com.mmr.core.domain.repository.CategoryRepository
import com.mmr.core.domain.repository.RecurringRuleRepository
import com.mmr.core.model.enums.RecurrenceInterval
import com.mmr.core.model.enums.TransactionType
import com.mmr.feature.recurring.model.AddEditRecurringRuleUiEvent
import com.mmr.feature.recurring.model.AddEditRecurringRuleUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddEditRecurringRuleViewModel @Inject constructor(
    private val recurringRuleRepository: RecurringRuleRepository,
    private val categoryRepository: CategoryRepository,
    private val accountRepository: AccountRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val ruleId: Long? = savedStateHandle.get<Long>("ruleId")?.takeIf { it > 0 }

    private val _uiState = MutableStateFlow(AddEditRecurringRuleUiState(ruleId = ruleId))
    val uiState: StateFlow<AddEditRecurringRuleUiState> = _uiState.asStateFlow()

    private val _categories = MutableStateFlow<List<CategoryEntity>>(emptyList())
    val categories: StateFlow<List<CategoryEntity>> = _categories.asStateFlow()

    private val _accounts = MutableStateFlow<List<AccountEntity>>(emptyList())
    val accounts: StateFlow<List<AccountEntity>> = _accounts.asStateFlow()

    init {
        loadPrerequisites()
    }

    private fun loadPrerequisites() {
        viewModelScope.launch {
            categoryRepository.getActiveCategories().collect { _categories.value = it }
        }
        viewModelScope.launch {
            accountRepository.getAccounts().collect { accs ->
                _accounts.value = accs
                if (_uiState.value.accountId == null && accs.isNotEmpty()) {
                    _uiState.update { it.copy(accountId = accs.first().id) }
                }
            }
        }

        if (ruleId != null) {
            viewModelScope.launch {
                val existing = recurringRuleRepository.getRuleById(ruleId)
                if (existing != null) {
                    _uiState.update {
                        it.copy(
                            name = existing.name,
                            amount = existing.amount.toString(),
                            transactionType = existing.transactionTemplateType,
                            categoryId = existing.categoryId,
                            accountId = existing.accountId,
                            interval = existing.recurrenceInterval,
                            dayOfMonth = existing.dayOfMonth,
                            startDate = existing.nextRunDate,
                            reminderMinutesBefore = existing.reminderMinutesBefore,
                            notes = existing.noteTemplate ?: "",
                            canSave = true
                        )
                    }
                }
            }
        }
    }

    fun onEvent(event: AddEditRecurringRuleUiEvent) {
        when (event) {
            is AddEditRecurringRuleUiEvent.OnNameChanged -> {
                _uiState.update { it.copy(name = event.name, canSave = validate(it.copy(name = event.name))) }
            }

            is AddEditRecurringRuleUiEvent.OnAmountChanged -> {
                val clean = event.amount.filter { it.isDigit() || it == '.' }
                _uiState.update { it.copy(amount = clean, canSave = validate(it.copy(amount = clean))) }
            }

            is AddEditRecurringRuleUiEvent.OnTypeSelected -> {
                _uiState.update { it.copy(transactionType = event.type) }
            }

            is AddEditRecurringRuleUiEvent.OnCategorySelected -> {
                _uiState.update { it.copy(categoryId = event.categoryId) }
            }

            is AddEditRecurringRuleUiEvent.OnAccountSelected -> {
                _uiState.update { it.copy(accountId = event.accountId, canSave = validate(it.copy(accountId = event.accountId))) }
            }

            is AddEditRecurringRuleUiEvent.OnIntervalSelected -> {
                _uiState.update { it.copy(interval = event.interval) }
            }

            is AddEditRecurringRuleUiEvent.OnDayOfMonthChanged -> {
                _uiState.update { it.copy(dayOfMonth = event.day) }
            }

            is AddEditRecurringRuleUiEvent.OnStartDateChanged -> {
                _uiState.update { it.copy(startDate = event.date) }
            }

            is AddEditRecurringRuleUiEvent.OnReminderMinutesChanged -> {
                _uiState.update { it.copy(reminderMinutesBefore = event.minutes) }
            }

            is AddEditRecurringRuleUiEvent.OnNotesChanged -> {
                _uiState.update { it.copy(notes = event.notes) }
            }

            AddEditRecurringRuleUiEvent.OnSaveClick -> {
                saveRule()
            }
        }
    }

    private fun validate(state: AddEditRecurringRuleUiState): Boolean {
        val amount = MmrValidation.parsePositiveAmount(state.amount)
        val hasValidStart = MmrValidation.isValidEpochMillis(state.startDate)
        return state.name.isNotBlank() && amount != null && state.accountId != null && hasValidStart
    }

    private fun saveRule() {
        val state = _uiState.value
        val amount = MmrValidation.parsePositiveAmount(state.amount) ?: run {
            _uiState.update { it.copy(errorMessage = "Enter a valid amount greater than zero.") }
            return
        }
        val accountId = state.accountId ?: run {
            _uiState.update { it.copy(errorMessage = "Select an account.") }
            return
        }
        if (!MmrValidation.isValidEpochMillis(state.startDate)) {
            _uiState.update { it.copy(errorMessage = "Choose a valid start date.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            try {
                if (ruleId != null) {
                    val existing = recurringRuleRepository.getRuleById(ruleId)
                    if (existing != null) {
                        val updated = existing.copy(
                            name = state.name.trim(),
                            amount = amount,
                            transactionTemplateType = state.transactionType,
                            categoryId = state.categoryId,
                            accountId = accountId,
                            recurrenceInterval = state.interval,
                            dayOfMonth = MmrValidation.clampDayOfMonth(state.dayOfMonth),
                            nextRunDate = state.startDate,
                            reminderMinutesBefore = state.reminderMinutesBefore,
                            noteTemplate = state.notes.ifBlank { null },
                            updatedAt = System.currentTimeMillis()
                        )
                        recurringRuleRepository.updateRecurringRule(updated)
                    } else {
                        _uiState.update {
                            it.copy(isSaving = false, errorMessage = "Recurring rule not found.")
                        }
                        return@launch
                    }
                } else {
                    val newRule = RecurringRuleEntity(
                        name = state.name.trim(),
                        amount = amount,
                        transactionTemplateType = state.transactionType,
                        categoryId = state.categoryId,
                        accountId = accountId,
                        recurrenceInterval = state.interval,
                        dayOfMonth = MmrValidation.clampDayOfMonth(state.dayOfMonth),
                        nextRunDate = state.startDate,
                        isEnabled = true,
                        reminderMinutesBefore = state.reminderMinutesBefore,
                        noteTemplate = state.notes.ifBlank { null }
                    )
                    recurringRuleRepository.createRecurringRule(newRule)
                }
                _uiState.update { it.copy(isSaving = false, isSaved = true) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isSaving = false, errorMessage = MmrUiMessages.friendly(e.message, MmrUiMessages.GENERIC_SAVE))
                }
            }
        }
    }
}
