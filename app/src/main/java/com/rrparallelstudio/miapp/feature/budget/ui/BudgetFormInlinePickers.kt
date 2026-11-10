package com.rrparallelstudio.miapp.feature.budget.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rrparallelstudio.miapp.feature.budget.model.AddEditBudgetUiEvent
import com.rrparallelstudio.miapp.feature.budget.model.AddEditBudgetUiState
import com.rrparallelstudio.miapp.feature.transaction.ui.CategoryPickerPanelState
import com.rrparallelstudio.miapp.feature.transaction.ui.CategorySubcategoryPickerPanel
import com.rrparallelstudio.miapp.feature.transaction.ui.budgetCategoryPickerCallbacks
import com.rrparallelstudio.miapp.ui.components.MmrTransactionFormExpandablePicker
import com.rrparallelstudio.miapp.ui.components.MmrTransactionFormIcons

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
