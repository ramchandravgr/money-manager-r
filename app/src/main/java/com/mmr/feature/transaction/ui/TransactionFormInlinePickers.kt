package com.mmr.feature.transaction.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mmr.app.ui.components.MmrTransactionFormExpandablePicker
import com.mmr.app.ui.components.MmrTransactionFormIcons
import com.mmr.feature.transaction.model.AccountPickerMode
import com.mmr.feature.transaction.model.AddEditTransactionUiEvent
import com.mmr.feature.transaction.model.AddEditTransactionUiState
import com.mmr.feature.transaction.model.LoanPickerUi

internal enum class TransactionFormExpandedField {
    None,
    Category,
    Account,
    TransferFrom,
    TransferTo,
}

@Composable
fun TransactionExpandableCategoryField(
    state: AddEditTransactionUiState,
    displayValue: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onEvent: (AddEditTransactionUiEvent) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    MmrTransactionFormExpandablePicker(
        label = "Category",
        value = displayValue,
        emptyHint = "Select category",
        icon = MmrTransactionFormIcons.category,
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        modifier = modifier,
        enabled = enabled,
    ) {
        CategorySubcategoryPickerPanel(
            state = state,
            onEvent = onEvent,
            maxPanelHeight = 300.dp,
            onPickerFinished = { onExpandedChange(false) },
        )
    }
}

@Composable
fun TransactionExpandableAccountField(
    label: String,
    displayValue: String,
    emptyHint: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accounts: List<com.mmr.feature.transaction.model.AccountUi>,
    selectedAccountId: Long?,
    mode: AccountPickerMode,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onAccountSelected: (Long) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loans: List<LoanPickerUi> = emptyList(),
    selectedLoanId: Long? = null,
    onLoanSelected: ((Long) -> Unit)? = null,
    excludedAccountId: Long? = null,
) {
    MmrTransactionFormExpandablePicker(
        label = label,
        value = displayValue,
        emptyHint = emptyHint,
        icon = icon,
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        modifier = modifier,
        enabled = enabled,
    ) {
        AccountPickerPanel(
            accounts = accounts,
            loans = loans,
            mode = mode,
            selectedAccountId = selectedAccountId,
            selectedLoanId = selectedLoanId,
            excludedAccountId = excludedAccountId,
            showAllOption = false,
            allOptionSelected = false,
            onAllSelected = null,
            onAccountSelected = { id ->
                onAccountSelected(id)
                onExpandedChange(false)
            },
            onLoanSelected =
                onLoanSelected?.let { handler ->
                    { loanId ->
                        handler(loanId)
                        onExpandedChange(false)
                    }
                },
            listMaxHeight = 280.dp,
        )
    }
}
