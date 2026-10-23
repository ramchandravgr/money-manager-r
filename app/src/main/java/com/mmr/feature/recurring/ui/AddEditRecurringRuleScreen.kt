package com.mmr.feature.recurring.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mmr.app.testing.MmrTestTags
import com.mmr.app.ui.components.MmrAppBarSaveIcon
import com.mmr.app.ui.components.MmrPrimaryActionButton
import com.mmr.app.ui.components.MmrTopAppBar
import com.mmr.core.database.entity.CategoryEntity
import com.mmr.core.model.enums.CategoryType
import com.mmr.core.model.enums.RecurrenceInterval
import com.mmr.core.model.enums.TransactionType
import com.mmr.feature.recurring.model.AddEditRecurringRuleUiEvent
import com.mmr.feature.recurring.model.AddEditRecurringRuleUiState
import com.mmr.feature.recurring.viewmodel.AddEditRecurringRuleViewModel
import com.mmr.feature.transaction.model.toAccountUi
import com.mmr.feature.transaction.ui.AccountPickerSheet
import com.mmr.feature.transaction.ui.TransactionAmountField
import com.mmr.feature.transaction.ui.TransactionDateTimeField
import com.mmr.feature.transaction.ui.TransactionFormPickerField
import com.mmr.feature.transaction.ui.TransactionNoteField

private val reminderOptions =
    listOf(
        0 to "None",
        60 to "1 hour before",
        1440 to "1 day before",
        2880 to "2 days before",
    )

private val intervalOptions =
    listOf(
        RecurrenceInterval.DAILY to "Daily",
        RecurrenceInterval.WEEKLY to "Weekly",
        RecurrenceInterval.MONTHLY to "Monthly",
        RecurrenceInterval.YEARLY to "Yearly",
    )

private fun reminderLabel(minutes: Int): String =
    reminderOptions
        .firstOrNull {
            it.first == minutes
        }?.second ?: "Custom"

private fun categoriesForType(
    categories: List<CategoryEntity>,
    transactionType: TransactionType,
): List<CategoryEntity> {
    val target =
        when (transactionType) {
            TransactionType.INCOME -> CategoryType.INCOME
            TransactionType.EXPENSE -> CategoryType.EXPENSE
            else -> return emptyList()
        }
    return categories.filter { it.type == target && it.isActive }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditRecurringRuleScreen(
    onNavigateBack: () -> Unit,
    viewModel: AddEditRecurringRuleViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val accounts by viewModel.accounts.collectAsState()

    val categorySheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val accountSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val reminderSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            onNavigateBack()
        }
    }

    val filteredCategories = categoriesForType(categories, uiState.transactionType)
    val categoryName = filteredCategories.firstOrNull { it.id == uiState.categoryId }?.name ?: ""
    val accountName = accounts.firstOrNull { it.id == uiState.accountId }?.name ?: ""
    val accountChoices = accounts.map { it.toAccountUi() }

    if (uiState.categoryPickerVisible) {
        RecurringCategoryPickerSheet(
            categories = filteredCategories,
            selectedCategoryId = uiState.categoryId,
            onCategorySelected = { viewModel.onEvent(AddEditRecurringRuleUiEvent.OnCategorySelected(it)) },
            sheetState = categorySheetState,
            onDismiss = { viewModel.onEvent(AddEditRecurringRuleUiEvent.OnDismissCategoryPicker) },
        )
    }

    if (uiState.accountPickerVisible) {
        AccountPickerSheet(
            title = "Account",
            accounts = accountChoices,
            selectedAccountId = uiState.accountId,
            onAccountSelected = { viewModel.onEvent(AddEditRecurringRuleUiEvent.OnAccountSelected(it)) },
            sheetState = accountSheetState,
            onDismiss = { viewModel.onEvent(AddEditRecurringRuleUiEvent.OnDismissAccountPicker) },
        )
    }

    if (uiState.reminderPickerVisible) {
        RecurringReminderPickerSheet(
            selectedMinutes = uiState.reminderMinutesBefore,
            onSelected = { viewModel.onEvent(AddEditRecurringRuleUiEvent.OnReminderMinutesChanged(it)) },
            sheetState = reminderSheetState,
            onDismiss = { viewModel.onEvent(AddEditRecurringRuleUiEvent.OnDismissReminderPicker) },
        )
    }

    Scaffold(
        topBar = {
            MmrTopAppBar(
                title =
                    when (uiState.transactionType) {
                        TransactionType.INCOME -> "Income"
                        else -> "Expense"
                    },
                showBackNavigation = true,
                onNavigateBack = onNavigateBack,
                useMmHeaderStyle = true,
                actions = {
                    MmrAppBarSaveIcon(
                        onClick = { viewModel.onEvent(AddEditRecurringRuleUiEvent.OnSaveClick) },
                        enabled = uiState.canSave && !uiState.isSaving,
                    )
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            RecurringRuleFormFields(
                uiState = uiState,
                categoryName = categoryName,
                accountName = accountName,
                categoriesEmpty = filteredCategories.isEmpty(),
                accountsEmpty = accounts.isEmpty(),
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

@Composable
private fun RecurringRuleFormFields(
    uiState: AddEditRecurringRuleUiState,
    categoryName: String,
    accountName: String,
    categoriesEmpty: Boolean,
    accountsEmpty: Boolean,
    onEvent: (AddEditRecurringRuleUiEvent) -> Unit,
) {
    SingleChoiceSegmentedButtonRow(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
    ) {
        val types = listOf(TransactionType.EXPENSE, TransactionType.INCOME)
        types.forEachIndexed { index, type ->
            SegmentedButton(
                shape = SegmentedButtonDefaults.itemShape(index = index, count = types.size),
                onClick = { onEvent(AddEditRecurringRuleUiEvent.OnTypeSelected(type)) },
                selected = uiState.transactionType == type,
            ) {
                Text(type.name.lowercase().replaceFirstChar { it.uppercase() })
            }
        }
    }

    OutlinedTextField(
        value = uiState.name,
        onValueChange = { onEvent(AddEditRecurringRuleUiEvent.OnNameChanged(it)) },
        label = { Text("Name") },
        placeholder = { Text("House rent, Netflix, salary…") },
        modifier =
            Modifier
                .testTag(MmrTestTags.recurringFieldName)
                .fillMaxWidth(),
        singleLine = true,
    )

    TransactionDateTimeField(
        dateTimeMillis = uiState.startDate,
        onDateTimeMillisChange = { onEvent(AddEditRecurringRuleUiEvent.OnStartDateChanged(it)) },
    )

    TransactionAmountField(
        amount = uiState.amount,
        onAmountChange = { onEvent(AddEditRecurringRuleUiEvent.OnAmountChanged(it)) },
    )

    Text(
        text = "Frequency",
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        intervalOptions.forEachIndexed { index, (interval, label) ->
            SegmentedButton(
                shape = SegmentedButtonDefaults.itemShape(index = index, count = intervalOptions.size),
                onClick = { onEvent(AddEditRecurringRuleUiEvent.OnIntervalSelected(interval)) },
                selected = uiState.interval == interval,
            ) {
                Text(label)
            }
        }
    }

    TransactionFormPickerField(
        label = "Category",
        value = categoryName.ifEmpty { "Select category" },
        onClick =
            if (!categoriesEmpty) {
                { onEvent(AddEditRecurringRuleUiEvent.OnCategoryRowClick) }
            } else {
                null
            },
        enabled = !categoriesEmpty,
    )
    if (categoriesEmpty) {
        Text(
            text = "Add ${uiState.transactionType.name.lowercase()} categories in Settings → Manage categories.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    TransactionFormPickerField(
        label = "Account",
        value = accountName.ifEmpty { "Select account" },
        onClick =
            if (!accountsEmpty) {
                { onEvent(AddEditRecurringRuleUiEvent.OnAccountRowClick) }
            } else {
                null
            },
        enabled = !accountsEmpty,
    )
    if (accountsEmpty) {
        Text(
            text = "Add an account in Settings before saving.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    TransactionFormPickerField(
        label = "Reminder",
        value = reminderLabel(uiState.reminderMinutesBefore),
        onClick = { onEvent(AddEditRecurringRuleUiEvent.OnReminderRowClick) },
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecurringCategoryPickerSheet(
    categories: List<CategoryEntity>,
    selectedCategoryId: Long?,
    onCategorySelected: (Long?) -> Unit,
    sheetState: androidx.compose.material3.SheetState,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Category", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                TextButton(onClick = onDismiss) { Text("Close") }
            }
            HorizontalDivider()
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                item {
                    RecurringPickerRow(
                        label = "None",
                        selected = selectedCategoryId == null,
                        onClick = { onCategorySelected(null) },
                    )
                }
                items(categories, key = { it.id }) { category ->
                    RecurringPickerRow(
                        label = category.name,
                        selected = category.id == selectedCategoryId,
                        onClick = { onCategorySelected(category.id) },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecurringReminderPickerSheet(
    selectedMinutes: Int,
    onSelected: (Int) -> Unit,
    sheetState: androidx.compose.material3.SheetState,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Reminder", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                TextButton(onClick = onDismiss) { Text("Close") }
            }
            HorizontalDivider()
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(reminderOptions, key = { it.first }) { (minutes, label) ->
                    RecurringPickerRow(
                        label = label,
                        selected = minutes == selectedMinutes,
                        onClick = { onSelected(minutes) },
                    )
                }
            }
        }
    }
}

@Composable
private fun RecurringPickerRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        )
        if (selected) {
            Text(
                text = "✓",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
}
