package com.mmr.feature.budget.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mmr.app.ui.components.MmrTransactionFormExpandablePicker
import com.mmr.app.ui.components.MmrTransactionFormIcons
import com.mmr.feature.budget.model.AddEditBudgetUiEvent
import com.mmr.feature.budget.model.AddEditBudgetUiState
import com.mmr.feature.transaction.ui.CategoryPickerPanelState
import com.mmr.feature.transaction.ui.CategorySubcategoryPickerPanel
import com.mmr.feature.transaction.ui.budgetCategoryPickerCallbacks

internal enum class BudgetFormExpandedField {
    None,
    Category,
}

@Composable
fun BudgetExpandableCategoryField(
    state: AddEditBudgetUiState,
    displayValue: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onEvent: (AddEditBudgetUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    MmrTransactionFormExpandablePicker(
        label = "Category",
        value = displayValue,
        emptyHint = "Select category or subcategory",
        icon = MmrTransactionFormIcons.category,
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        modifier = modifier,
    ) {
        CategorySubcategoryPickerPanel(
            panelState =
                CategoryPickerPanelState(
                    categories = state.categories,
                    pickerHighlightedCategoryId = state.pickerHighlightedCategoryId,
                ),
            callbacks = budgetCategoryPickerCallbacks(onEvent),
            maxPanelHeight = 300.dp,
            onPickerFinished = { onExpandedChange(false) },
        )
    }
}
