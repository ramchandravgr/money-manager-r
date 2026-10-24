package com.mmr.feature.transaction.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mmr.app.testing.MmrTestTags
import com.mmr.app.ui.components.MmrAppBarDeleteIcon
import com.mmr.app.ui.components.MmrAppBarSaveIcon
import com.mmr.app.ui.components.MmrContentDialog
import com.mmr.app.ui.components.MmrDialogOptionButton
import com.mmr.app.ui.components.MmrImeAwareScrollColumn
import com.mmr.app.ui.components.MmrTopAppBar
import com.mmr.app.ui.components.MmrTransactionFormIcons
import com.mmr.app.ui.components.MmrTransactionFormScreenBackground
import com.mmr.app.ui.components.MmrTransactionTypeSegment
import com.mmr.app.ui.state.MmrErrorState
import com.mmr.app.ui.state.MmrLoadingState
import com.mmr.core.data.repository.TransactionAttachmentRepositoryImpl
import com.mmr.core.loan.LoanLinkedTransactionDeleteMode
import com.mmr.core.model.enums.TransactionType
import com.mmr.feature.transaction.model.AccountPickerMode
import com.mmr.feature.transaction.model.AddEditTransactionUiEvent
import com.mmr.feature.transaction.viewmodel.AddEditTransactionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTransactionRoute(
    onNavigateBack: () -> Unit,
    viewModel: AddEditTransactionViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val receiptPicker =
        rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            uri?.let { viewModel.onEvent(AddEditTransactionUiEvent.OnReceiptPicked(it.toString())) }
        }
    var expandedField by remember { mutableStateOf(TransactionFormExpandedField.None) }

    LaunchedEffect(state.selectedType) {
        expandedField = TransactionFormExpandedField.None
    }

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
            showDismissButton = true,
            dismissLabel = "Cancel",
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                MmrDialogOptionButton(
                    text = "Reverse payment & restore balance",
                    onClick = {
                        viewModel.onEvent(
                            AddEditTransactionUiEvent.OnConfirmDeleteWithLoanPolicy(
                                LoanLinkedTransactionDeleteMode.REVERSE,
                            ),
                        )
                    },
                )
                MmrDialogOptionButton(
                    text = "Unlink only",
                    onClick = {
                        viewModel.onEvent(
                            AddEditTransactionUiEvent.OnConfirmDeleteWithLoanPolicy(
                                LoanLinkedTransactionDeleteMode.UNLINK,
                            ),
                        )
                    },
                )
            }
        }
    }

    state.pickerNameEditor?.let { editor ->
        TransactionPickerNameEditorDialog(
            editor = editor,
            onNameChange = { viewModel.onEvent(AddEditTransactionUiEvent.OnPickerNameEditorChanged(it)) },
            onSave = { viewModel.onEvent(AddEditTransactionUiEvent.OnPickerNameEditorSave) },
            onDismiss = { viewModel.onEvent(AddEditTransactionUiEvent.OnPickerNameEditorDismiss) },
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
                useBrandHeaderStyle = true,
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
                MmrTransactionFormScreenBackground(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(padding),
                ) {
                    MmrImeAwareScrollColumn(
                        horizontalPadding = 16.dp,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        MmrTransactionTypeSegment(
                            selected = state.selectedType,
                            onTypeSelected = { viewModel.onEvent(AddEditTransactionUiEvent.OnTypeSelected(it)) },
                            modifier = Modifier.padding(vertical = 4.dp),
                            testTagForType = { type ->
                                when (type) {
                                    TransactionType.INCOME -> MmrTestTags.transactionTypeIncome
                                    TransactionType.EXPENSE -> MmrTestTags.transactionTypeExpense
                                    TransactionType.TRANSFER -> MmrTestTags.transactionTypeTransfer
                                    else -> null
                                }
                            },
                        )

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

                        TransactionExpandableCategoryField(
                            state = state,
                            displayValue = categoryDisplay,
                            expanded = expandedField == TransactionFormExpandedField.Category,
                            onExpandedChange = { open ->
                                if (open) {
                                    viewModel.onEvent(AddEditTransactionUiEvent.OnCategoryRowClick)
                                    expandedField = TransactionFormExpandedField.Category
                                } else {
                                    expandedField = TransactionFormExpandedField.None
                                }
                            },
                            onEvent = viewModel::onEvent,
                            enabled = state.categories.isNotEmpty(),
                        )

                        if (state.categories.isEmpty()) {
                            Text(
                                text = "Add expense or income categories in Settings → Categories.",
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
                            TransactionExpandableAccountField(
                                label = "From account",
                                displayValue = selectedAccountName,
                                emptyHint = "Select account",
                                icon = MmrTransactionFormIcons.transferFrom,
                                accounts = state.accounts,
                                selectedAccountId = state.selectedAccountId,
                                mode = AccountPickerMode.AccountsGrouped,
                                expanded = expandedField == TransactionFormExpandedField.TransferFrom,
                                onExpandedChange = { open ->
                                    if (open) {
                                        viewModel.onEvent(AddEditTransactionUiEvent.OnFromAccountRowClick)
                                        expandedField = TransactionFormExpandedField.TransferFrom
                                    } else {
                                        expandedField = TransactionFormExpandedField.None
                                    }
                                },
                                onAccountSelected = {
                                    viewModel.onEvent(AddEditTransactionUiEvent.OnAccountSelected(it))
                                },
                                enabled = state.accounts.isNotEmpty(),
                            )
                            TransactionExpandableAccountField(
                                label = "To",
                                displayValue = transferToLabel,
                                emptyHint = "Select destination",
                                icon = MmrTransactionFormIcons.transferTo,
                                accounts = state.accounts,
                                selectedAccountId = state.selectedRelatedAccountId,
                                mode = AccountPickerMode.TransferTo,
                                expanded = expandedField == TransactionFormExpandedField.TransferTo,
                                onExpandedChange = { open ->
                                    if (open) {
                                        viewModel.onEvent(AddEditTransactionUiEvent.OnToAccountRowClick)
                                        expandedField = TransactionFormExpandedField.TransferTo
                                    } else {
                                        expandedField = TransactionFormExpandedField.None
                                    }
                                },
                                onAccountSelected = {
                                    viewModel.onEvent(AddEditTransactionUiEvent.OnTransferToAccountSelected(it))
                                },
                                loans = state.loans,
                                selectedLoanId = state.selectedTransferLoanId,
                                onLoanSelected = {
                                    viewModel.onEvent(AddEditTransactionUiEvent.OnTransferToLoanSelected(it))
                                },
                                excludedAccountId = state.selectedAccountId,
                                enabled = state.accounts.isNotEmpty() || state.loans.isNotEmpty(),
                            )
                        } else {
                            TransactionExpandableAccountField(
                                label = "Account",
                                displayValue = selectedAccountName,
                                emptyHint = "Select account",
                                icon = MmrTransactionFormIcons.account,
                                accounts = state.accounts,
                                selectedAccountId = state.selectedAccountId,
                                mode = AccountPickerMode.AccountsGrouped,
                                expanded = expandedField == TransactionFormExpandedField.Account,
                                onExpandedChange = { open ->
                                    if (open) {
                                        viewModel.onEvent(AddEditTransactionUiEvent.OnAccountRowClick)
                                        expandedField = TransactionFormExpandedField.Account
                                    } else {
                                        expandedField = TransactionFormExpandedField.None
                                    }
                                },
                                onAccountSelected = {
                                    viewModel.onEvent(AddEditTransactionUiEvent.OnAccountSelected(it))
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
                            onAttachPhotoClick = {
                                receiptPicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                                )
                            },
                            canAttachMore =
                                state.receipts.size <
                                    TransactionAttachmentRepositoryImpl.MAX_ATTACHMENTS_PER_TRANSACTION,
                        )

                        TransactionReceiptSection(
                            receipts = state.receipts,
                            onAddClick = {
                                receiptPicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                                )
                            },
                            onRemoveSaved = { viewModel.onEvent(AddEditTransactionUiEvent.OnRemoveReceipt(it)) },
                            onRemovePending = {
                                viewModel.onEvent(
                                    AddEditTransactionUiEvent.OnRemovePendingReceipt(it),
                                )
                            },
                            canAddMore =
                                state.receipts.size <
                                    TransactionAttachmentRepositoryImpl.MAX_ATTACHMENTS_PER_TRANSACTION,
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
}
