package com.mmr.feature.transaction.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mmr.core.model.enums.TransactionType
import com.mmr.feature.transaction.model.AddEditTransactionUiEvent
import com.mmr.app.ui.state.MmrErrorState
import com.mmr.app.ui.state.MmrLoadingState
import com.mmr.feature.category.ui.parseColorHex
import com.mmr.feature.transaction.viewmodel.AddEditTransactionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTransactionRoute(
    onNavigateBack: () -> Unit,
    viewModel: AddEditTransactionViewModel = hiltViewModel()
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

    if (state.categoryPickerVisible) {
        CategorySubcategoryPickerSheet(
            state = state,
            onEvent = viewModel::onEvent,
            sheetState = categorySheetState,
            onDismiss = { viewModel.onEvent(AddEditTransactionUiEvent.OnDismissCategoryPicker) }
        )
    }

    if (state.accountPickerVisible) {
        val title = when (state.accountPickerTarget) {
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
            onDismiss = { viewModel.onEvent(AddEditTransactionUiEvent.OnDismissAccountPicker) }
        )
    }

    if (state.transferToPickerVisible) {
        TransferToPickerSheet(
            accounts = state.accounts,
            loans = state.loans,
            selectedAccountId = state.selectedRelatedAccountId,
            selectedLoanId = state.selectedTransferLoanId,
            fromAccountId = state.selectedAccountId,
            onAccountSelected = { viewModel.onEvent(AddEditTransactionUiEvent.OnTransferToAccountSelected(it)) },
            onLoanSelected = { viewModel.onEvent(AddEditTransactionUiEvent.OnTransferToLoanSelected(it)) },
            sheetState = transferToSheetState,
            onDismiss = { viewModel.onEvent(AddEditTransactionUiEvent.OnDismissTransferToPicker) }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when (state.selectedType) {
                            TransactionType.INCOME -> "Income"
                            TransactionType.TRANSFER -> "Transfer"
                            else -> "Expense"
                        },
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    TextButton(onClick = onNavigateBack) {
                        Text("Cancel")
                    }
                },
                actions = {
                    TextButton(
                        onClick = { viewModel.onEvent(AddEditTransactionUiEvent.OnSaveClick) },
                        enabled = state.canSave
                    ) {
                        Text("Save", fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    ) { padding ->
        when {
            state.isLoading -> {
                MmrLoadingState(modifier = Modifier.fillMaxSize().padding(padding))
            }

            state.loadErrorMessage != null -> {
                MmrErrorState(
                    message = state.loadErrorMessage ?: "Unable to open this screen.",
                    onRetry = null,
                    modifier = Modifier.fillMaxSize().padding(padding)
                )
            }

            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SingleChoiceSegmentedButtonRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        val types = listOf(
                            TransactionType.INCOME,
                            TransactionType.EXPENSE,
                            TransactionType.TRANSFER
                        )
                        types.forEachIndexed { index, type ->
                            SegmentedButton(
                                shape = SegmentedButtonDefaults.itemShape(index = index, count = types.size),
                                onClick = { viewModel.onEvent(AddEditTransactionUiEvent.OnTypeSelected(type)) },
                                selected = state.selectedType == type
                            ) {
                                Text(type.name.lowercase().replaceFirstChar { it.uppercase() })
                            }
                        }
                    }

                    OutlinedTextField(
                        value = state.amount,
                        onValueChange = { viewModel.onEvent(AddEditTransactionUiEvent.OnAmountChanged(it)) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Amount") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.headlineSmall
                    )

                    val selectedCategory = state.categories.firstOrNull { it.id == state.selectedCategoryId }
                    val selectedSub = selectedCategory?.subcategories?.firstOrNull {
                        it.id == state.selectedSubcategoryId
                    }
                    val categoryDisplay = when {
                        selectedCategory == null -> "Select category"
                        selectedSub != null -> "${selectedCategory.name} · ${selectedSub.name}"
                        else -> selectedCategory.name
                    }

                    TransactionFormRow(
                        label = "Category",
                        value = categoryDisplay,
                        onClick = if (state.categories.isNotEmpty()) {
                            { viewModel.onEvent(AddEditTransactionUiEvent.OnCategoryRowClick) }
                        } else {
                            null
                        },
                        valueContent = if (selectedCategory != null) {
                            {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clip(CircleShape)
                                            .background(parseColorHex(selectedCategory.colorHex))
                                    )
                                    Text(
                                        text = categoryDisplay,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                            }
                        } else {
                            null
                        }
                    )

                    if (state.categories.isEmpty()) {
                        Text(
                            text = "Add categories in Settings → Manage categories.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    val isTransfer = state.selectedType == TransactionType.TRANSFER
                    val selectedAccountName = state.accounts
                        .firstOrNull { it.id == state.selectedAccountId }
                        ?.name
                        ?: "Select account"

                    val transferToLabel = when {
                        state.selectedTransferLoanId != null -> {
                            state.loans.firstOrNull { it.id == state.selectedTransferLoanId }?.name
                                ?: "Loan"
                        }
                        state.selectedRelatedAccountId != null -> {
                            state.accounts.firstOrNull { it.id == state.selectedRelatedAccountId }?.name
                                ?: "Account"
                        }
                        else -> "Select destination"
                    }

                    if (isTransfer) {
                        TransactionFormRow(
                            label = "From",
                            value = selectedAccountName,
                            onClick = if (state.accounts.isNotEmpty()) {
                                { viewModel.onEvent(AddEditTransactionUiEvent.OnFromAccountRowClick) }
                            } else {
                                null
                            }
                        )
                        TransactionFormRow(
                            label = "To",
                            value = transferToLabel,
                            onClick = if (state.accounts.isNotEmpty() || state.loans.isNotEmpty()) {
                                { viewModel.onEvent(AddEditTransactionUiEvent.OnToAccountRowClick) }
                            } else {
                                null
                            }
                        )
                    } else {
                        TransactionFormRow(
                            label = "Account",
                            value = selectedAccountName,
                            onClick = if (state.accounts.isNotEmpty()) {
                                { viewModel.onEvent(AddEditTransactionUiEvent.OnAccountRowClick) }
                            } else {
                                null
                            }
                        )
                    }

                    if (state.accounts.isEmpty()) {
                        Text(
                            text = "Add an account in Settings before saving transactions.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    OutlinedTextField(
                        value = state.note,
                        onValueChange = { viewModel.onEvent(AddEditTransactionUiEvent.OnNoteChanged(it)) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Note") },
                        minLines = 2
                    )

                    state.errorMessage?.let { error ->
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    Button(
                        onClick = { viewModel.onEvent(AddEditTransactionUiEvent.OnSaveClick) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        enabled = state.canSave
                    ) {
                        Text(if (state.isEditMode) "Update" else "Save")
                    }
                }
            }
        }
    }
}
