package com.mmr.feature.transaction.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mmr.app.testing.MmrTestTags
import com.mmr.app.ui.components.MmrAppBarDeleteIcon
import com.mmr.app.ui.components.MmrAppBarSaveIcon
import com.mmr.app.ui.components.MmrContentDialog
import com.mmr.app.ui.components.MmrSecondaryActionButton
import com.mmr.app.ui.components.MmrTopAppBar
import com.mmr.app.ui.state.MmrErrorState
import com.mmr.app.ui.state.MmrLoadingState
import com.mmr.core.loan.LoanLinkedTransactionDeleteMode
import com.mmr.core.model.enums.TransactionType
import com.mmr.feature.transaction.model.AddEditTransactionUiEvent
import com.mmr.feature.transaction.viewmodel.AddEditTransactionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTransactionRoute(
    onNavigateBack: () -> Unit,
    viewModel: AddEditTransactionViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val categorySheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val accountSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val transferToSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(state.isSaved) {
        if (state.isSaved) {
            onNavigateBack()
        }
    }

    if (state.showLoanDeleteDialog) {
        MmrContentDialog(
            title = "Delete linked loan payment?",
            message =
                "This transaction is tied to a loan repayment. Unlink keeps the loan payment record; " +
                    "Reverse removes the payment and restores the loan balance.",
            onDismiss = { viewModel.onEvent(AddEditTransactionUiEvent.OnDismissLoanDeleteDialog) },
            showDismissButton = false,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                MmrSecondaryActionButton(
                    text = "Reverse payment & restore balance",
                    onClick = {
                        viewModel.onEvent(
                            AddEditTransactionUiEvent.OnConfirmDeleteWithLoanPolicy(
                                LoanLinkedTransactionDeleteMode.REVERSE,
                            ),
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                MmrSecondaryActionButton(
                    text = "Unlink only",
                    onClick = {
                        viewModel.onEvent(
                            AddEditTransactionUiEvent.OnConfirmDeleteWithLoanPolicy(
                                LoanLinkedTransactionDeleteMode.UNLINK,
                            ),
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                TextButton(
                    onClick = { viewModel.onEvent(AddEditTransactionUiEvent.OnDismissLoanDeleteDialog) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Cancel")
                }
            }
        }
    }

    if (state.categoryPickerVisible) {
        CategorySubcategoryPickerSheet(
            state = state,
            onEvent = viewModel::onEvent,
            sheetState = categorySheetState,
            onDismiss = { viewModel.onEvent(AddEditTransactionUiEvent.OnDismissCategoryPicker) },
        )
    }

    if (state.accountPickerVisible) {
        val title =
            when (state.accountPickerTarget) {
                com.mmr.feature.transaction.model.AccountPickerTarget.SINGLE_ACCOUNT -> "Account"
                com.mmr.feature.transaction.model.AccountPickerTarget.TRANSFER_FROM -> "Transfer from"
            }
        AccountPickerSheet(
            title = title,
            accounts = state.accounts,
            selectedAccountId = state.selectedAccountId,
            onAccountSelected = { id ->
                viewModel.onEvent(AddEditTransactionUiEvent.OnAccountSelected(id))
                viewModel.onEvent(AddEditTransactionUiEvent.OnDismissAccountPicker)
            },
            sheetState = accountSheetState,
            onDismiss = { viewModel.onEvent(AddEditTransactionUiEvent.OnDismissAccountPicker) },
            mode = com.mmr.feature.transaction.model.AccountPickerMode.AccountsGrouped,
        )
    }

    if (state.transferToPickerVisible) {
        AccountPickerSheet(
            title = "Transfer to",
            accounts = state.accounts,
            selectedAccountId = state.selectedRelatedAccountId,
            onAccountSelected = { id ->
                viewModel.onEvent(AddEditTransactionUiEvent.OnTransferToAccountSelected(id))
                viewModel.onEvent(AddEditTransactionUiEvent.OnDismissTransferToPicker)
            },
            sheetState = transferToSheetState,
            onDismiss = { viewModel.onEvent(AddEditTransactionUiEvent.OnDismissTransferToPicker) },
            mode = com.mmr.feature.transaction.model.AccountPickerMode.TransferTo,
            loans = state.loans,
            selectedLoanId = state.selectedTransferLoanId,
            onLoanSelected = { id ->
                viewModel.onEvent(AddEditTransactionUiEvent.OnTransferToLoanSelected(id))
                viewModel.onEvent(AddEditTransactionUiEvent.OnDismissTransferToPicker)
            },
            excludedAccountId = state.selectedAccountId,
        )
    }

    Scaffold(
        topBar = {
            MmrTopAppBar(
                title =
                    when (state.selectedType) {
                        TransactionType.INCOME -> "Income"
                        TransactionType.TRANSFER -> "Transfer"
                        else -> "Expense"
                    },
                showBackNavigation = true,
                onNavigateBack = onNavigateBack,
                useMmHeaderStyle = true,
                actions = {
                    if (state.showDeleteButton) {
                        MmrAppBarDeleteIcon(
                            onClick = { viewModel.onEvent(AddEditTransactionUiEvent.OnDeleteClick) },
                        )
                    }
                    MmrAppBarSaveIcon(
                        onClick = { viewModel.onEvent(AddEditTransactionUiEvent.OnSaveClick) },
                        enabled = state.canSave,
                    )
                },
            )
        },
    ) { padding ->
        when {
            state.isLoading -> {
                MmrLoadingState(modifier = Modifier.fillMaxSize().padding(padding))
            }

            state.loadErrorMessage != null -> {
                MmrErrorState(
                    message = state.loadErrorMessage ?: "Unable to open this screen.",
                    onRetry = null,
                    modifier = Modifier.fillMaxSize().padding(padding),
                )
            }

            else -> {
                Column(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .padding(horizontal = 16.dp)
                            .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    SingleChoiceSegmentedButtonRow(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                    ) {
                        val types =
                            listOf(
                                TransactionType.INCOME,
                                TransactionType.EXPENSE,
                                TransactionType.TRANSFER,
                            )
                        types.forEachIndexed { index, type ->
                            val typeTag =
                                when (type) {
                                    TransactionType.INCOME -> MmrTestTags.transactionTypeIncome
                                    TransactionType.EXPENSE -> MmrTestTags.transactionTypeExpense
                                    TransactionType.TRANSFER -> MmrTestTags.transactionTypeTransfer
                                    else -> "transaction.type.${type.name.lowercase()}"
                                }
                            SegmentedButton(
                                modifier = Modifier.testTag(typeTag),
                                shape = SegmentedButtonDefaults.itemShape(index = index, count = types.size),
                                onClick = { viewModel.onEvent(AddEditTransactionUiEvent.OnTypeSelected(type)) },
                                selected = state.selectedType == type,
                            ) {
                                Text(
                                    text = type.name.lowercase().replaceFirstChar { it.uppercase() },
                                    style = MaterialTheme.typography.labelMedium,
                                )
                            }
                        }
                    }

                    TransactionDateTimeField(
                        dateTimeMillis = state.dateTimeMillis,
                        onDateTimeMillisChange = {
                            viewModel.onEvent(AddEditTransactionUiEvent.OnDateTimeChanged(it))
                        },
                    )

                    TransactionAmountField(
                        amount = state.amount,
                        onAmountChange = { viewModel.onEvent(AddEditTransactionUiEvent.OnAmountChanged(it)) },
                    )

                    val selectedCategory = state.categories.firstOrNull { it.id == state.selectedCategoryId }
                    val selectedSub =
                        selectedCategory?.subcategories?.firstOrNull {
                            it.id == state.selectedSubcategoryId
                        }
                    val categoryDisplay =
                        when {
                            selectedCategory == null -> ""
                            selectedSub != null -> "${selectedCategory.name} · ${selectedSub.name}"
                            else -> selectedCategory.name
                        }

                    TransactionFormPickerField(
                        label = "Category",
                        value = categoryDisplay.ifEmpty { "Select category" },
                        onClick =
                            if (state.categories.isNotEmpty()) {
                                { viewModel.onEvent(AddEditTransactionUiEvent.OnCategoryRowClick) }
                            } else {
                                null
                            },
                        enabled = state.categories.isNotEmpty(),
                    )

                    if (state.categories.isEmpty()) {
                        Text(
                            text = "Add expense or income categories in Settings → Manage categories.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    val isTransfer = state.selectedType == TransactionType.TRANSFER
                    val selectedAccountName =
                        state.accounts
                            .firstOrNull { it.id == state.selectedAccountId }
                            ?.name
                            ?: ""

                    val transferToLabel =
                        when {
                            state.selectedTransferLoanId != null -> {
                                state.loans.firstOrNull { it.id == state.selectedTransferLoanId }?.name
                                    ?: ""
                            }
                            state.selectedRelatedAccountId != null -> {
                                state.accounts.firstOrNull { it.id == state.selectedRelatedAccountId }?.name
                                    ?: ""
                            }
                            else -> ""
                        }

                    if (isTransfer) {
                        TransactionFormPickerField(
                            label = "From account",
                            value = selectedAccountName.ifEmpty { "Select account" },
                            onClick =
                                if (state.accounts.isNotEmpty()) {
                                    { viewModel.onEvent(AddEditTransactionUiEvent.OnFromAccountRowClick) }
                                } else {
                                    null
                                },
                            enabled = state.accounts.isNotEmpty(),
                        )
                        TransactionFormPickerField(
                            label = "To",
                            value = transferToLabel.ifEmpty { "Select destination" },
                            onClick =
                                if (state.accounts.isNotEmpty() || state.loans.isNotEmpty()) {
                                    { viewModel.onEvent(AddEditTransactionUiEvent.OnToAccountRowClick) }
                                } else {
                                    null
                                },
                            enabled = state.accounts.isNotEmpty() || state.loans.isNotEmpty(),
                        )
                    } else {
                        TransactionFormPickerField(
                            label = "Account",
                            value = selectedAccountName.ifEmpty { "Select account" },
                            onClick =
                                if (state.accounts.isNotEmpty()) {
                                    { viewModel.onEvent(AddEditTransactionUiEvent.OnAccountRowClick) }
                                } else {
                                    null
                                },
                            enabled = state.accounts.isNotEmpty(),
                        )
                    }

                    if (state.accounts.isEmpty()) {
                        Text(
                            text = "Add an account in Settings before saving transactions.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    TransactionNoteField(
                        note = state.note,
                        onNoteChange = { viewModel.onEvent(AddEditTransactionUiEvent.OnNoteChanged(it)) },
                        suggestions = if (state.autocompleteEnabled) state.noteSuggestions else emptyList(),
                    )

                    state.errorMessage?.let { error ->
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        }
    }
}
