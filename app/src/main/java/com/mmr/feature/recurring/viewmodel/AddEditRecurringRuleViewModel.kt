package com.mmr.feature.recurring.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mmr.app.ui.state.MmrUiMessages
import com.mmr.core.common.validation.MmrValidation
import com.mmr.core.database.entity.RecurringRuleEntity
import com.mmr.core.domain.repository.AccountRepository
import com.mmr.core.domain.repository.CategoryRepository
import com.mmr.core.domain.repository.LoanRepository
import com.mmr.core.domain.repository.RecurringRuleRepository
import com.mmr.core.model.category.CategoryWithChildren
import com.mmr.core.model.enums.CategoryType
import com.mmr.core.model.enums.TransactionType
import com.mmr.feature.recurring.model.AddEditRecurringRuleUiEvent
import com.mmr.feature.recurring.model.AddEditRecurringRuleUiState
import com.mmr.feature.transaction.model.AccountPickerTarget
import com.mmr.feature.transaction.model.CategoryUi
import com.mmr.feature.transaction.model.LoanPickerUi
import com.mmr.feature.transaction.model.SubcategoryUi
import com.mmr.feature.transaction.model.toAccountUi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddEditRecurringRuleViewModel
    @Inject
    constructor(
        private val recurringRuleRepository: RecurringRuleRepository,
        private val categoryRepository: CategoryRepository,
        private val accountRepository: AccountRepository,
        private val loanRepository: LoanRepository,
        savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        private val ruleId: Long? = savedStateHandle.get<Long>("ruleId")?.takeIf { it > 0 }

        private val _uiState = MutableStateFlow(AddEditRecurringRuleUiState(ruleId = ruleId))
        val uiState: StateFlow<AddEditRecurringRuleUiState> = _uiState.asStateFlow()

        private val categoryTreeCache = MutableStateFlow<List<CategoryWithChildren>>(emptyList())
        private var appliedEditRuleId: Long? = null

        init {
            loadPrerequisites()
        }

        private fun loadPrerequisites() {
            viewModelScope.launch {
                combine(
                    categoryRepository.observeActiveCategoryTree().catch { emit(emptyList()) },
                    accountRepository.getAccounts().catch { emit(emptyList()) },
                    loanRepository.getActiveLoans().catch { emit(emptyList()) },
                ) { tree, accounts, loans ->
                    Triple(tree, accounts, loans)
                }.collectLatest { (tree, accounts, loans) ->
                    categoryTreeCache.value = tree
                    val rule =
                        if (ruleId != null && appliedEditRuleId != ruleId) {
                            recurringRuleRepository.getRuleById(ruleId)
                        } else {
                            null
                        }
                    val type = rule?.transactionTemplateType ?: _uiState.value.transactionType
                    val categories = categoriesForType(tree, type)
                    val accountUis = accounts.map { it.toAccountUi() }
                    val loanUis = loans.map { LoanPickerUi(id = it.id, name = it.name) }
                    val catId =
                        resolveCategorySelection(categories, rule?.categoryId ?: _uiState.value.categoryId)
                    val subId =
                        resolveSubcategorySelection(
                            categories,
                            catId,
                            rule?.subcategoryId ?: _uiState.value.subcategoryId,
                        )
                    _uiState.update { current ->
                        val hydrateFromRule = rule != null && appliedEditRuleId != ruleId
                        if (hydrateFromRule) {
                            appliedEditRuleId = ruleId
                        }
                        val base =
                            if (hydrateFromRule) {
                                current.copy(
                                    name = rule.name,
                                    amount = rule.amount.toString(),
                                    transactionType = rule.transactionTemplateType,
                                    categoryId = catId,
                                    subcategoryId = subId,
                                    accountId = rule.accountId,
                                    relatedAccountId = rule.relatedAccountId,
                                    transferLoanId = rule.loanId,
                                    interval = rule.recurrenceInterval,
                                    dayOfMonth = rule.dayOfMonth,
                                    startDate = rule.nextRunDate,
                                    reminderMinutesBefore = rule.reminderMinutesBefore,
                                    notes = rule.noteTemplate ?: "",
                                )
                            } else {
                                current.copy(
                                    categoryId = catId,
                                    subcategoryId = subId,
                                    accountId = current.accountId ?: accountUis.firstOrNull()?.id,
                                )
                            }
                        val withLists =
                            base.copy(
                                categories = categories,
                                accounts = accountUis,
                                loans = loanUis,
                                pickerHighlightedCategoryId = catId ?: base.pickerHighlightedCategoryId,
                            )
                        withLists.copy(canSave = validate(withLists))
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
                    val clean = MmrValidation.sanitizeDecimalInput(event.amount)
                    _uiState.update { it.copy(amount = clean, canSave = validate(it.copy(amount = clean))) }
                }

                is AddEditRecurringRuleUiEvent.OnTypeSelected -> {
                    _uiState.update { state ->
                        val categories = categoriesForType(categoryTreeCache.value, event.type)
                        val catId = resolveCategorySelection(categories, state.categoryId)
                        val subId = resolveSubcategorySelection(categories, catId, state.subcategoryId)
                        val next =
                            state.copy(
                                transactionType = event.type,
                                categories = categories,
                                categoryId = catId,
                                subcategoryId = subId,
                                pickerHighlightedCategoryId = catId,
                                relatedAccountId =
                                    if (event.type ==
                                        TransactionType.TRANSFER
                                    ) {
                                        state.relatedAccountId
                                    } else {
                                        null
                                    },
                                transferLoanId =
                                    if (event.type ==
                                        TransactionType.TRANSFER
                                    ) {
                                        state.transferLoanId
                                    } else {
                                        null
                                    },
                            )
                        next.copy(canSave = validate(next))
                    }
                }

                is AddEditRecurringRuleUiEvent.OnCategorySelected -> {
                    _uiState.update {
                        val next =
                            it.copy(
                                categoryId = event.categoryId,
                                subcategoryId = event.subcategoryId,
                                categoryPickerVisible = false,
                            )
                        next.copy(canSave = validate(next))
                    }
                }

                is AddEditRecurringRuleUiEvent.OnPickerParentSelected -> {
                    _uiState.update { it.copy(pickerHighlightedCategoryId = event.categoryId) }
                }

                is AddEditRecurringRuleUiEvent.OnPickerSubcategorySelected -> {
                    _uiState.update {
                        val next =
                            it.copy(
                                categoryId = it.pickerHighlightedCategoryId,
                                subcategoryId = event.subcategoryId,
                                categoryPickerVisible = false,
                            )
                        next.copy(canSave = validate(next))
                    }
                }

                AddEditRecurringRuleUiEvent.OnPickerConfirmParentOnly -> {
                    _uiState.update {
                        val next =
                            it.copy(
                                categoryId = it.pickerHighlightedCategoryId,
                                subcategoryId = null,
                                categoryPickerVisible = false,
                            )
                        next.copy(canSave = validate(next))
                    }
                }

                AddEditRecurringRuleUiEvent.OnCategoryRowClick -> {
                    _uiState.update {
                        it.copy(
                            pickerHighlightedCategoryId = it.categoryId ?: it.categories.firstOrNull()?.id,
                        )
                    }
                }

                AddEditRecurringRuleUiEvent.OnDismissCategoryPicker -> {
                    _uiState.update { it.copy(categoryPickerVisible = false) }
                }

                AddEditRecurringRuleUiEvent.OnAccountRowClick -> {
                    _uiState.update {
                        it.copy(accountPickerTarget = AccountPickerTarget.SINGLE_ACCOUNT)
                    }
                }

                AddEditRecurringRuleUiEvent.OnTransferFromRowClick -> {
                    _uiState.update {
                        it.copy(accountPickerTarget = AccountPickerTarget.TRANSFER_FROM)
                    }
                }

                AddEditRecurringRuleUiEvent.OnTransferToRowClick -> Unit

                AddEditRecurringRuleUiEvent.OnDismissAccountPicker -> {
                    _uiState.update { it.copy(accountPickerVisible = false) }
                }

                AddEditRecurringRuleUiEvent.OnDismissTransferToPicker -> {
                    _uiState.update { it.copy(transferToPickerVisible = false) }
                }

                AddEditRecurringRuleUiEvent.OnReminderRowClick -> {
                    _uiState.update { it.copy(reminderPickerVisible = true) }
                }

                AddEditRecurringRuleUiEvent.OnDismissReminderPicker -> {
                    _uiState.update { it.copy(reminderPickerVisible = false) }
                }

                is AddEditRecurringRuleUiEvent.OnAccountSelected -> {
                    _uiState.update {
                        val next = it.copy(accountId = event.accountId, accountPickerVisible = false)
                        next.copy(canSave = validate(next))
                    }
                }

                is AddEditRecurringRuleUiEvent.OnTransferToAccountSelected -> {
                    _uiState.update {
                        val next =
                            it.copy(
                                relatedAccountId = event.accountId,
                                transferLoanId = null,
                                transferToPickerVisible = false,
                            )
                        next.copy(canSave = validate(next))
                    }
                }

                is AddEditRecurringRuleUiEvent.OnTransferToLoanSelected -> {
                    _uiState.update {
                        val next =
                            it.copy(
                                transferLoanId = event.loanId,
                                relatedAccountId = null,
                                transferToPickerVisible = false,
                            )
                        next.copy(canSave = validate(next))
                    }
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
                    _uiState.update {
                        it.copy(reminderMinutesBefore = event.minutes, reminderPickerVisible = false)
                    }
                }

                is AddEditRecurringRuleUiEvent.OnNotesChanged -> {
                    _uiState.update { it.copy(notes = event.notes) }
                }

                AddEditRecurringRuleUiEvent.OnSaveClick -> saveRule()
            }
        }

        private fun validate(state: AddEditRecurringRuleUiState): Boolean {
            val amount = MmrValidation.parsePositiveAmount(state.amount)
            val hasValidStart = MmrValidation.isValidEpochMillis(state.startDate)
            if (state.name.isBlank() || amount == null || state.accountId == null || !hasValidStart) {
                return false
            }
            if (state.transactionType == TransactionType.TRANSFER) {
                val hasDest = state.relatedAccountId != null || state.transferLoanId != null
                if (!hasDest) return false
                if (state.relatedAccountId != null && state.relatedAccountId == state.accountId) return false
            }
            return true
        }

        private fun saveRule() {
            val state = _uiState.value
            if (!validate(state)) {
                _uiState.update { it.copy(errorMessage = "Complete all required fields.") }
                return
            }
            val amount = MmrValidation.parsePositiveAmount(state.amount)!!
            val accountId = state.accountId!!

            viewModelScope.launch {
                _uiState.update { it.copy(isSaving = true, errorMessage = null) }
                try {
                    val relatedAccountId =
                        if (state.transactionType == TransactionType.TRANSFER) state.relatedAccountId else null
                    val loanId =
                        if (state.transactionType == TransactionType.TRANSFER) state.transferLoanId else null
                    if (ruleId != null) {
                        val existing = recurringRuleRepository.getRuleById(ruleId)
                        if (existing != null) {
                            recurringRuleRepository.updateRecurringRule(
                                existing.copy(
                                    name = state.name.trim(),
                                    amount = amount,
                                    transactionTemplateType = state.transactionType,
                                    categoryId = state.categoryId,
                                    subcategoryId = state.subcategoryId,
                                    accountId = accountId,
                                    relatedAccountId = relatedAccountId,
                                    loanId = loanId,
                                    recurrenceInterval = state.interval,
                                    dayOfMonth = MmrValidation.clampDayOfMonth(state.dayOfMonth),
                                    nextRunDate = state.startDate,
                                    reminderMinutesBefore = state.reminderMinutesBefore,
                                    noteTemplate = state.notes.ifBlank { null },
                                    updatedAt = System.currentTimeMillis(),
                                ),
                            )
                        } else {
                            _uiState.update {
                                it.copy(isSaving = false, errorMessage = "Recurring rule not found.")
                            }
                            return@launch
                        }
                    } else {
                        recurringRuleRepository.createRecurringRule(
                            RecurringRuleEntity(
                                name = state.name.trim(),
                                amount = amount,
                                transactionTemplateType = state.transactionType,
                                categoryId = state.categoryId,
                                subcategoryId = state.subcategoryId,
                                accountId = accountId,
                                relatedAccountId = relatedAccountId,
                                loanId = loanId,
                                recurrenceInterval = state.interval,
                                dayOfMonth = MmrValidation.clampDayOfMonth(state.dayOfMonth),
                                nextRunDate = state.startDate,
                                isEnabled = true,
                                reminderMinutesBefore = state.reminderMinutesBefore,
                                noteTemplate = state.notes.ifBlank { null },
                            ),
                        )
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

        private fun categoriesForType(
            tree: List<CategoryWithChildren>,
            transactionType: TransactionType,
        ): List<CategoryUi> {
            val filtered =
                when (transactionType) {
                    TransactionType.EXPENSE -> tree.filter { it.category.type == CategoryType.EXPENSE }
                    TransactionType.INCOME -> tree.filter { it.category.type == CategoryType.INCOME }
                    TransactionType.TRANSFER -> tree.filter { it.category.type == CategoryType.EXPENSE }
                    else -> tree
                }
            return filtered.map { node ->
                CategoryUi(
                    id = node.category.id.value,
                    name = node.category.name,
                    colorHex = node.category.color.hex,
                    iconName = node.category.iconName,
                    subcategories =
                        node.subcategories.map { sub ->
                            SubcategoryUi(id = sub.id.value, name = sub.name)
                        },
                )
            }
        }

        private fun resolveCategorySelection(
            categories: List<CategoryUi>,
            preferredId: Long?,
        ): Long? {
            if (preferredId != null && categories.any { it.id == preferredId }) return preferredId
            return categories.firstOrNull()?.id
        }

        private fun resolveSubcategorySelection(
            categories: List<CategoryUi>,
            categoryId: Long?,
            preferredId: Long?,
        ): Long? {
            if (categoryId == null) return null
            val subs = categories.firstOrNull { it.id == categoryId }?.subcategories ?: emptyList()
            if (subs.isEmpty()) return null
            if (preferredId != null && subs.any { it.id == preferredId }) return preferredId
            return null
        }
    }
