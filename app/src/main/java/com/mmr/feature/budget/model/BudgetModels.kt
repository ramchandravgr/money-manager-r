package com.mmr.feature.budget.model

import com.mmr.core.model.enums.BudgetPeriodType
import com.mmr.feature.transaction.model.CategoryUi

data class BudgetListItemUi(
    val budgetId: Long,
    val targetLabel: String,
    val periodLabel: String,
    val periodType: BudgetPeriodType,
    val limitFormatted: String,
    val spentFormatted: String,
    val percentUsed: Int,
    val progressFraction: Float,
    val isOverBudget: Boolean,
    val alertOnExceeded: Boolean,
)

data class BudgetListUiState(
    val isLoading: Boolean = true,
    val budgets: List<BudgetListItemUi> = emptyList(),
    val isEmpty: Boolean = false,
    val errorMessage: String? = null,
)

sealed interface BudgetListUiEvent {
    data object OnRetryClick : BudgetListUiEvent

    data class OnDeleteBudget(
        val budgetId: Long,
    ) : BudgetListUiEvent
}

data class AddEditBudgetUiState(
    val budgetId: Long? = null,
    val amount: String = "",
    val periodType: BudgetPeriodType = BudgetPeriodType.MONTHLY,
    val customStartMillis: Long? = null,
    val customEndMillis: Long? = null,
    val alertOnExceeded: Boolean = false,
    val categoryId: Long? = null,
    val subcategoryId: Long? = null,
    val categories: List<CategoryUi> = emptyList(),
    val pickerHighlightedCategoryId: Long? = null,
    val canSave: Boolean = false,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val errorMessage: String? = null,
)

sealed interface AddEditBudgetUiEvent {
    data class OnAmountChanged(
        val value: String,
    ) : AddEditBudgetUiEvent

    data class OnPeriodTypeSelected(
        val type: BudgetPeriodType,
    ) : AddEditBudgetUiEvent

    data class OnCustomStartSelected(
        val millis: Long,
    ) : AddEditBudgetUiEvent

    data class OnCustomEndSelected(
        val millis: Long,
    ) : AddEditBudgetUiEvent

    data class OnAlertToggled(
        val enabled: Boolean,
    ) : AddEditBudgetUiEvent

    data class OnPickerParentSelected(
        val categoryId: Long,
    ) : AddEditBudgetUiEvent

    data class OnPickerSubcategorySelected(
        val subcategoryId: Long,
    ) : AddEditBudgetUiEvent

    data object OnPickerConfirmParentOnly : AddEditBudgetUiEvent

    data object OnSaveClick : AddEditBudgetUiEvent

    data object OnDeleteClick : AddEditBudgetUiEvent
}
