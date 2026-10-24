package com.mmr.feature.transaction.ui

import com.mmr.feature.transaction.model.CategoryUi

data class CategoryPickerPanelState(
    val categories: List<CategoryUi>,
    val pickerHighlightedCategoryId: Long?,
)

data class CategoryPickerPanelCallbacks(
    val onParentSelected: (Long) -> Unit,
    val onSubcategorySelected: (Long) -> Unit,
    val onConfirmParentOnly: () -> Unit,
    val onAddCategory: (() -> Unit)? = null,
    val onAddSubcategory: (() -> Unit)? = null,
    val onEditCategory: ((Long) -> Unit)? = null,
    val onEditSubcategory: ((Long) -> Unit)? = null,
)

fun transactionCategoryPickerCallbacks(
    onEvent: (com.mmr.feature.transaction.model.AddEditTransactionUiEvent) -> Unit,
): CategoryPickerPanelCallbacks =
    CategoryPickerPanelCallbacks(
        onParentSelected = {
            onEvent(
                com.mmr.feature.transaction.model.AddEditTransactionUiEvent
                    .OnPickerParentSelected(it),
            )
        },
        onSubcategorySelected = {
            onEvent(
                com.mmr.feature.transaction.model.AddEditTransactionUiEvent
                    .OnPickerSubcategorySelected(it),
            )
        },
        onConfirmParentOnly = {
            onEvent(com.mmr.feature.transaction.model.AddEditTransactionUiEvent.OnPickerConfirmParentOnly)
        },
        onAddCategory = {
            onEvent(com.mmr.feature.transaction.model.AddEditTransactionUiEvent.OnPickerAddCategory)
        },
        onAddSubcategory = {
            onEvent(com.mmr.feature.transaction.model.AddEditTransactionUiEvent.OnPickerAddSubcategory)
        },
        onEditCategory = {
            onEvent(
                com.mmr.feature.transaction.model.AddEditTransactionUiEvent
                    .OnPickerEditCategory(it),
            )
        },
        onEditSubcategory = {
            onEvent(
                com.mmr.feature.transaction.model.AddEditTransactionUiEvent
                    .OnPickerEditSubcategory(it),
            )
        },
    )

fun recurringCategoryPickerCallbacks(
    onEvent: (com.mmr.feature.recurring.model.AddEditRecurringRuleUiEvent) -> Unit,
): CategoryPickerPanelCallbacks =
    CategoryPickerPanelCallbacks(
        onParentSelected = {
            onEvent(
                com.mmr.feature.recurring.model.AddEditRecurringRuleUiEvent
                    .OnPickerParentSelected(it),
            )
        },
        onSubcategorySelected = {
            onEvent(
                com.mmr.feature.recurring.model.AddEditRecurringRuleUiEvent
                    .OnPickerSubcategorySelected(it),
            )
        },
        onConfirmParentOnly = {
            onEvent(com.mmr.feature.recurring.model.AddEditRecurringRuleUiEvent.OnPickerConfirmParentOnly)
        },
    )

fun mergeCategoryPickerCallbacks(
    onEvent: (com.mmr.feature.approval.model.ApprovalCenterUiEvent) -> Unit,
): CategoryPickerPanelCallbacks =
    CategoryPickerPanelCallbacks(
        onParentSelected = {
            onEvent(
                com.mmr.feature.approval.model.ApprovalCenterUiEvent
                    .OnMergePickerParentSelected(it),
            )
        },
        onSubcategorySelected = {
            onEvent(
                com.mmr.feature.approval.model.ApprovalCenterUiEvent
                    .OnMergePickerSubcategorySelected(it),
            )
        },
        onConfirmParentOnly = {
            onEvent(com.mmr.feature.approval.model.ApprovalCenterUiEvent.OnMergePickerConfirmParentOnly)
        },
    )

fun searchCategoryPickerCallbacks(
    onEvent: (com.mmr.feature.search.model.TransactionSearchUiEvent) -> Unit,
): CategoryPickerPanelCallbacks =
    CategoryPickerPanelCallbacks(
        onParentSelected = {
            onEvent(
                com.mmr.feature.search.model.TransactionSearchUiEvent
                    .OnPickerParentSelected(it),
            )
        },
        onSubcategorySelected = {
            onEvent(
                com.mmr.feature.search.model.TransactionSearchUiEvent
                    .OnPickerSubcategorySelected(it),
            )
        },
        onConfirmParentOnly = {
            onEvent(com.mmr.feature.search.model.TransactionSearchUiEvent.OnPickerConfirmParentOnly)
        },
    )
