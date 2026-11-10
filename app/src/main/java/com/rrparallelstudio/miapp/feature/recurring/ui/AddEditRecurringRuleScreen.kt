package com.rrparallelstudio.miapp.feature.recurring.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.rrparallelstudio.miapp.core.model.enums.TransactionType
import com.rrparallelstudio.miapp.core.reminder.MmrReminderPresets
import com.rrparallelstudio.miapp.feature.recurring.model.AddEditRecurringRuleUiEvent
import com.rrparallelstudio.miapp.feature.recurring.model.AddEditRecurringRuleUiState
import com.rrparallelstudio.miapp.feature.recurring.viewmodel.AddEditRecurringRuleViewModel
import com.rrparallelstudio.miapp.feature.transaction.model.AccountPickerMode
import com.rrparallelstudio.miapp.feature.transaction.ui.TransactionAmountField
import com.rrparallelstudio.miapp.feature.transaction.ui.TransactionDateTimeField
import com.rrparallelstudio.miapp.feature.transaction.ui.TransactionExpandableAccountField
import com.rrparallelstudio.miapp.feature.transaction.ui.TransactionNameField
import com.rrparallelstudio.miapp.feature.transaction.ui.TransactionNoteField
import com.rrparallelstudio.miapp.feature.transaction.ui.TransactionRecurrenceIntervalChips
import com.rrparallelstudio.miapp.ui.components.MmrAppBarSaveIcon
import com.rrparallelstudio.miapp.ui.components.MmrImeAwareScrollColumn
import com.rrparallelstudio.miapp.ui.components.MmrPrimaryActionButton
import com.rrparallelstudio.miapp.ui.components.MmrTopAppBar
import com.rrparallelstudio.miapp.ui.components.MmrTransactionFormIcons
import com.rrparallelstudio.miapp.ui.components.MmrTransactionFormScreenBackground
import com.rrparallelstudio.miapp.ui.components.MmrTransactionFormSectionLabel
import com.rrparallelstudio.miapp.ui.components.MmrTransactionTypeSegment

private val reminderOptions = MmrReminderPresets.recurringReminderOptionsMinutes

private fun reminderLabel(minutes: Int): String = MmrReminderPresets.recurringReminderLabel(minutes)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditRecurringRuleScreen(
    onNavigateBack: () -> Unit,
    viewModel: AddEditRecurringRuleViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var expandedField by remember { mutableStateOf(RecurringFormExpandedField.None) }

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            onNavigateBack()
        }
    }

    LaunchedEffect(uiState.transactionType) {
        expandedField = RecurringFormExpandedField.None
    }

    Scaffold(
        topBar = {
            MmrTopAppBar(
                title =
                    when (uiState.transactionType) {
                        TransactionType.INCOME -> "Income"
                        TransactionType.TRANSFER -> "Transfer"
                        else -> "Expense"
                    },
                showBackNavigation = true,
                onNavigateBack = onNavigateBack,
                useBrandHeaderStyle = true,
                actions = {
                    MmrAppBarSaveIcon(
                        onClick = { viewModel.onEvent(AddEditRecurringRuleUiEvent.OnSaveClick) },
                        enabled = uiState.canSave && !uiState.isSaving,
                    )
                },
            )
        },
    ) { innerPadding ->
        MmrTransactionFormScreenBackground(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
        ) {
            MmrImeAwareScrollColumn(
                horizontalPadding = 16.dp,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                RecurringRuleFormFields(
                    uiState = uiState,
                    expandedField = expandedField,
                    onExpandedFieldChange = { expandedField = it },
                    onEvent = viewModel::onEvent,
                )
                MmrPrimaryActionButton(
                    text = if (uiState.ruleId != null) "Update rule" else "Save rule",
                    onClick = { viewModel.onEvent(AddEditRecurringRuleUiEvent.OnSaveClick) },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                    enabled = uiState.canSave && !uiState.isSaving,
                )
            }
        }
    }
}

@Composable
private fun RecurringRuleFormFields(
    uiState: AddEditRecurringRuleUiState,
    expandedField: RecurringFormExpandedField,
    onExpandedFieldChange: (RecurringFormExpandedField) -> Unit,
    onEvent: (AddEditRecurringRuleUiEvent) -> Unit,
) {
    MmrTransactionTypeSegment(
        selected = uiState.transactionType,
        onTypeSelected = { onEvent(AddEditRecurringRuleUiEvent.OnTypeSelected(it)) },
        modifier = Modifier.padding(vertical = 4.dp),
    )

    TransactionNameField(
        name = uiState.name,
        onNameChange = { onEvent(AddEditRecurringRuleUiEvent.OnNameChanged(it)) },
    )

    TransactionDateTimeField(
        dateTimeMillis = uiState.startDate,
        onDateTimeMillisChange = { onEvent(AddEditRecurringRuleUiEvent.OnStartDateChanged(it)) },
    )

    TransactionAmountField(
        amount = uiState.amount,
        onAmountChange = { onEvent(AddEditRecurringRuleUiEvent.OnAmountChanged(it)) },
    )

    MmrTransactionFormSectionLabel(text = "Frequency")
    TransactionRecurrenceIntervalChips(
        selected = uiState.interval,
        onSelected = { onEvent(AddEditRecurringRuleUiEvent.OnIntervalSelected(it)) },
    )

    val selectedCategory = uiState.categories.firstOrNull { it.id == uiState.categoryId }
    val selectedSub = selectedCategory?.subcategories?.firstOrNull { it.id == uiState.subcategoryId }
    val categoryDisplay =
        when {
            selectedCategory == null -> ""
            selectedSub != null -> "${selectedCategory.name} · ${selectedSub.name}"
            else -> selectedCategory.name
        }

    RecurringExpandableCategoryField(
        state = uiState,
        displayValue = categoryDisplay,
        expanded = expandedField == RecurringFormExpandedField.Category,
        onExpandedChange = { open ->
            if (open) {
                onEvent(AddEditRecurringRuleUiEvent.OnCategoryRowClick)
                onExpandedFieldChange(RecurringFormExpandedField.Category)
            } else {
                onExpandedFieldChange(RecurringFormExpandedField.None)
            }
        },
        onEvent = onEvent,
        enabled = uiState.categories.isNotEmpty(),
    )

    if (uiState.categories.isEmpty()) {
        Text(
            text = "Add expense or income categories in Settings → Categories.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    val isTransfer = uiState.transactionType == TransactionType.TRANSFER
    val selectedAccountName = uiState.accounts.firstOrNull { it.id == uiState.accountId }?.name ?: ""
    val transferToLabel =
        when {
            uiState.transferLoanId != null ->
                uiState.loans.firstOrNull { it.id == uiState.transferLoanId }?.name ?: ""
            uiState.relatedAccountId != null ->
                uiState.accounts.firstOrNull { it.id == uiState.relatedAccountId }?.name ?: ""
            else -> ""
        }

    if (isTransfer) {
        TransactionExpandableAccountField(
            label = "From account",
            displayValue = selectedAccountName,
            emptyHint = "Select account",
            icon = MmrTransactionFormIcons.transferFrom,
            accounts = uiState.accounts,
            selectedAccountId = uiState.accountId,
            mode = AccountPickerMode.AccountsGrouped,
            expanded = expandedField == RecurringFormExpandedField.TransferFrom,
            onExpandedChange = { open ->
                if (open) {
                    onEvent(AddEditRecurringRuleUiEvent.OnTransferFromRowClick)
                    onExpandedFieldChange(RecurringFormExpandedField.TransferFrom)
                } else {
                    onExpandedFieldChange(RecurringFormExpandedField.None)
                }
            },
            onAccountSelected = { onEvent(AddEditRecurringRuleUiEvent.OnAccountSelected(it)) },
            enabled = uiState.accounts.isNotEmpty(),
        )
        TransactionExpandableAccountField(
            label = "To",
            displayValue = transferToLabel,
            emptyHint = "Select account or loan",
            icon = MmrTransactionFormIcons.transferTo,
            accounts = uiState.accounts,
            selectedAccountId = uiState.relatedAccountId,
            mode = AccountPickerMode.TransferTo,
            expanded = expandedField == RecurringFormExpandedField.TransferTo,
            onExpandedChange = { open ->
                if (open) {
                    onEvent(AddEditRecurringRuleUiEvent.OnTransferToRowClick)
                    onExpandedFieldChange(RecurringFormExpandedField.TransferTo)
                } else {
                    onExpandedFieldChange(RecurringFormExpandedField.None)
                }
            },
            onAccountSelected = { onEvent(AddEditRecurringRuleUiEvent.OnTransferToAccountSelected(it)) },
            loans = uiState.loans,
            selectedLoanId = uiState.transferLoanId,
            onLoanSelected = { onEvent(AddEditRecurringRuleUiEvent.OnTransferToLoanSelected(it)) },
            excludedAccountId = uiState.accountId,
            enabled = uiState.accounts.isNotEmpty() || uiState.loans.isNotEmpty(),
        )
    } else {
        TransactionExpandableAccountField(
            label = "Account",
            displayValue = selectedAccountName,
            emptyHint = "Select account",
            icon = MmrTransactionFormIcons.account,
            accounts = uiState.accounts,
            selectedAccountId = uiState.accountId,
            mode = AccountPickerMode.AccountsGrouped,
            expanded = expandedField == RecurringFormExpandedField.Account,
            onExpandedChange = { open ->
                if (open) {
                    onEvent(AddEditRecurringRuleUiEvent.OnAccountRowClick)
                    onExpandedFieldChange(RecurringFormExpandedField.Account)
                } else {
                    onExpandedFieldChange(RecurringFormExpandedField.None)
                }
            },
            onAccountSelected = { onEvent(AddEditRecurringRuleUiEvent.OnAccountSelected(it)) },
            enabled = uiState.accounts.isNotEmpty(),
        )
    }

    if (uiState.accounts.isEmpty()) {
        Text(
            text = "Add an account in Settings before saving rules.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    Text(
        text = "Reminder is required — choose how many days before the due date to notify you.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    RecurringExpandableReminderField(
        displayValue = reminderLabel(uiState.reminderMinutesBefore),
        selectedMinutes = uiState.reminderMinutesBefore,
        expanded = expandedField == RecurringFormExpandedField.Reminder,
        onExpandedChange = { open ->
            onExpandedFieldChange(
                if (open) {
                    RecurringFormExpandedField.Reminder
                } else {
                    RecurringFormExpandedField.None
                },
            )
        },
        onSelected = { onEvent(AddEditRecurringRuleUiEvent.OnReminderMinutesChanged(it)) },
        reminderOptions = reminderOptions,
    )

    TransactionNoteField(
        note = uiState.notes,
        onNoteChange = { onEvent(AddEditRecurringRuleUiEvent.OnNotesChanged(it)) },
    )

    uiState.errorMessage?.let { error ->
        Text(
            text = error,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
