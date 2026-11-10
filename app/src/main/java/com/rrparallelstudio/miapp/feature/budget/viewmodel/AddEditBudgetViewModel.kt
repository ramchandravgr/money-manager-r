package com.rrparallelstudio.miapp.feature.budget.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rrparallelstudio.miapp.core.common.sync.MmrSyncId
import com.rrparallelstudio.miapp.core.common.validation.MmrValidation
import com.rrparallelstudio.miapp.core.database.entity.BudgetEntity
import com.rrparallelstudio.miapp.core.domain.repository.BudgetRepository
import com.rrparallelstudio.miapp.core.domain.repository.CategoryRepository
import com.rrparallelstudio.miapp.core.model.category.CategoryWithChildren
import com.rrparallelstudio.miapp.core.model.enums.BudgetPeriodType
import com.rrparallelstudio.miapp.core.model.enums.CategoryType
import com.rrparallelstudio.miapp.feature.budget.model.AddEditBudgetUiEvent
import com.rrparallelstudio.miapp.feature.budget.model.AddEditBudgetUiState
import com.rrparallelstudio.miapp.feature.transaction.model.CategoryUi
import com.rrparallelstudio.miapp.feature.transaction.model.SubcategoryUi
import com.rrparallelstudio.miapp.ui.state.MmrUiMessages
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddEditBudgetViewModel
    @Inject
    constructor(
        private val budgetRepository: BudgetRepository,
        private val categoryRepository: CategoryRepository,
        savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        private val budgetId: Long? = savedStateHandle.get<Long>("budgetId")?.takeIf { it > 0 }

        private val _uiState = MutableStateFlow(AddEditBudgetUiState(budgetId = budgetId))
        val uiState: StateFlow<AddEditBudgetUiState> = _uiState.asStateFlow()

        private var appliedEditBudgetId: Long? = null

        init {
            viewModelScope.launch {
                categoryRepository.observeActiveCategoryTree().catch { emit(emptyList()) }.collectLatest { tree ->
                    val categories = expenseCategories(tree)
                    val existing =
                        if (budgetId != null && appliedEditBudgetId != budgetId) {
                            budgetRepository.getById(budgetId)
                        } else {
                            null
                        }
                    val catId =
                        resolveCategorySelection(
                            categories,
                            existing?.categoryId ?: _uiState.value.categoryId,
                        )
                    val subId =
                        resolveSubcategorySelection(
                            categories,
                            catId,
                            existing?.subcategoryId ?: _uiState.value.subcategoryId,
                        )
                    _uiState.update { current ->
                        val hydrate = existing != null && appliedEditBudgetId != budgetId
                        if (hydrate) {
                            appliedEditBudgetId = budgetId
                        }
                        val base =
                            if (hydrate) {
                                current.copy(
                                    amount = existing.amountLimit.toString(),
                                    periodType = existing.periodType,
                                    customStartMillis = existing.customPeriodStartMillis,
                                    customEndMillis = existing.customPeriodEndMillis,
                                    alertOnExceeded = existing.alertOnExceeded,
                                    categoryId = catId,
                                    subcategoryId = subId,
                                )
                            } else {
                                current.copy(
                                    categoryId = catId,
                                    subcategoryId = subId,
                                )
                            }
                        base.copy(
                            categories = categories,
                            pickerHighlightedCategoryId = catId ?: base.pickerHighlightedCategoryId,
                            canSave =
                                validate(
                                    base.copy(categories = categories, categoryId = catId, subcategoryId = subId),
                                ),
                        )
                    }
                }
            }
        }

        fun onEvent(event: AddEditBudgetUiEvent) {
            when (event) {
                is AddEditBudgetUiEvent.OnAmountChanged -> {
                    _uiState.update {
                        val next = it.copy(amount = event.value)
                        next.copy(canSave = validate(next))
                    }
                }

                is AddEditBudgetUiEvent.OnPeriodTypeSelected -> {
                    _uiState.update {
                        val next = it.copy(periodType = event.type)
                        next.copy(canSave = validate(next))
                    }
                }

                is AddEditBudgetUiEvent.OnCustomStartSelected -> {
                    _uiState.update {
                        val next = it.copy(customStartMillis = event.millis)
                        next.copy(canSave = validate(next))
                    }
                }

                is AddEditBudgetUiEvent.OnCustomEndSelected -> {
                    _uiState.update {
                        val next = it.copy(customEndMillis = event.millis)
                        next.copy(canSave = validate(next))
                    }
                }

                is AddEditBudgetUiEvent.OnAlertToggled -> {
                    _uiState.update { it.copy(alertOnExceeded = event.enabled) }
                }

                is AddEditBudgetUiEvent.OnPickerParentSelected -> {
                    _uiState.update {
                        val next =
                            it.copy(
                                categoryId = event.categoryId,
                                subcategoryId = null,
                                pickerHighlightedCategoryId = event.categoryId,
                            )
                        next.copy(canSave = validate(next))
                    }
                }

                is AddEditBudgetUiEvent.OnPickerSubcategorySelected -> {
                    _uiState.update {
                        val next = it.copy(subcategoryId = event.subcategoryId)
                        next.copy(canSave = validate(next))
                    }
                }

                AddEditBudgetUiEvent.OnPickerConfirmParentOnly -> {
                    _uiState.update {
                        val next = it.copy(subcategoryId = null)
                        next.copy(canSave = validate(next))
                    }
                }

                AddEditBudgetUiEvent.OnSaveClick -> saveBudget()

                AddEditBudgetUiEvent.OnDeleteClick -> {
                    val id = budgetId ?: return
                    viewModelScope.launch {
                        try {
                            budgetRepository.delete(id)
                            _uiState.update { it.copy(isSaved = true) }
                        } catch (e: Exception) {
                            _uiState.update {
                                it.copy(errorMessage = MmrUiMessages.friendly(e.message, MmrUiMessages.GENERIC_ACTION))
                            }
                        }
                    }
                }
            }
        }

        private fun saveBudget() {
            val state = _uiState.value
            if (!validate(state)) {
                _uiState.update { it.copy(errorMessage = "Complete all required fields.") }
                return
            }
            val amount = MmrValidation.parsePositiveAmount(state.amount)!!
            val categoryId = state.categoryId!!
            val now = System.currentTimeMillis()

            viewModelScope.launch {
                _uiState.update { it.copy(isSaving = true, errorMessage = null) }
                try {
                    val existing = budgetId?.let { budgetRepository.getById(it) }
                    val entity =
                        BudgetEntity(
                            id = existing?.id ?: 0L,
                            syncId = existing?.syncId ?: MmrSyncId.newId(),
                            categoryId = categoryId,
                            subcategoryId = state.subcategoryId,
                            amountLimit = amount,
                            periodType = state.periodType,
                            customPeriodStartMillis = state.customStartMillis,
                            customPeriodEndMillis = state.customEndMillis,
                            alertOnExceeded = state.alertOnExceeded,
                            lastAlertPeriodKey = existing?.lastAlertPeriodKey,
                            isActive = true,
                            createdAt = existing?.createdAt ?: now,
                            updatedAt = now,
                        )
                    budgetRepository.upsert(entity)
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

        private fun validate(state: AddEditBudgetUiState): Boolean {
            val amount = MmrValidation.parsePositiveAmount(state.amount)
            if (amount == null || state.categoryId == null) return false
            if (state.periodType == BudgetPeriodType.CUSTOM) {
                val start = state.customStartMillis
                val end = state.customEndMillis
                if (start == null ||
                    end == null ||
                    !MmrValidation.isValidEpochMillis(start) ||
                    !MmrValidation.isValidEpochMillis(end) ||
                    end < start
                ) {
                    return false
                }
            }
            return true
        }

        private fun expenseCategories(tree: List<CategoryWithChildren>): List<CategoryUi> =
            tree
                .filter { it.category.type == CategoryType.EXPENSE }
                .map { node ->
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
