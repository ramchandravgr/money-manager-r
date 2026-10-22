package com.mmr.feature.recurring.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mmr.core.model.enums.RecurrenceInterval
import com.mmr.core.model.enums.TransactionType
import com.mmr.feature.recurring.model.AddEditRecurringRuleUiEvent
import com.mmr.feature.recurring.viewmodel.AddEditRecurringRuleViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditRecurringRuleScreen(
    onNavigateBack: () -> Unit,
    viewModel: AddEditRecurringRuleViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val accounts by viewModel.accounts.collectAsState()

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (uiState.ruleId != null) "Edit Recurring Rule" else "New Recurring Rule",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    TextButton(onClick = onNavigateBack) {
                        Text("Cancel")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    TextButton(
                        onClick = { viewModel.onEvent(AddEditRecurringRuleUiEvent.OnSaveClick) },
                        enabled = uiState.canSave && !uiState.isSaving
                    ) {
                        Text(if (uiState.isSaving) "Saving..." else "Save", fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Transaction Type (Expense vs Income)
            Text("Rule Type", style = MaterialTheme.typography.titleMedium)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = uiState.transactionType == TransactionType.EXPENSE,
                    onClick = { viewModel.onEvent(AddEditRecurringRuleUiEvent.OnTypeSelected(TransactionType.EXPENSE)) },
                    label = { Text("Expense (e.g. Rent, Bill)") }
                )
                FilterChip(
                    selected = uiState.transactionType == TransactionType.INCOME,
                    onClick = { viewModel.onEvent(AddEditRecurringRuleUiEvent.OnTypeSelected(TransactionType.INCOME)) },
                    label = { Text("Income (e.g. Salary)") }
                )
            }

            // Name
            OutlinedTextField(
                value = uiState.name,
                onValueChange = { viewModel.onEvent(AddEditRecurringRuleUiEvent.OnNameChanged(it)) },
                label = { Text("Rule Name (e.g. House Rent, Netflix, Salary)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Amount
            OutlinedTextField(
                value = uiState.amount,
                onValueChange = { viewModel.onEvent(AddEditRecurringRuleUiEvent.OnAmountChanged(it)) },
                label = { Text("Amount (₹)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Recurrence Interval
            Text("Recurrence Frequency", style = MaterialTheme.typography.titleMedium)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    RecurrenceInterval.DAILY to "Daily",
                    RecurrenceInterval.WEEKLY to "Weekly",
                    RecurrenceInterval.MONTHLY to "Monthly",
                    RecurrenceInterval.YEARLY to "Yearly"
                ).forEach { (interval, label) ->
                    FilterChip(
                        selected = uiState.interval == interval,
                        onClick = { viewModel.onEvent(AddEditRecurringRuleUiEvent.OnIntervalSelected(interval)) },
                        label = { Text(label) }
                    )
                }
            }

            // Category picker
            if (categories.isNotEmpty()) {
                Text("Category", style = MaterialTheme.typography.titleMedium)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { cat ->
                        FilterChip(
                            selected = uiState.categoryId == cat.id,
                            onClick = { viewModel.onEvent(AddEditRecurringRuleUiEvent.OnCategorySelected(cat.id)) },
                            label = { Text(cat.name) }
                        )
                    }
                }
            }

            // Account picker
            if (accounts.isNotEmpty()) {
                Text("Account", style = MaterialTheme.typography.titleMedium)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    accounts.forEach { acc ->
                        FilterChip(
                            selected = uiState.accountId == acc.id,
                            onClick = { viewModel.onEvent(AddEditRecurringRuleUiEvent.OnAccountSelected(acc.id)) },
                            label = { Text(acc.name) }
                        )
                    }
                }
            }

            // Reminder timing
            Text("Reminder Alert", style = MaterialTheme.typography.titleMedium)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    0 to "None",
                    60 to "1 hour before",
                    1440 to "1 day before",
                    2880 to "2 days before"
                ).forEach { (mins, label) ->
                    FilterChip(
                        selected = uiState.reminderMinutesBefore == mins,
                        onClick = { viewModel.onEvent(AddEditRecurringRuleUiEvent.OnReminderMinutesChanged(mins)) },
                        label = { Text(label) }
                    )
                }
            }

            // Notes
            OutlinedTextField(
                value = uiState.notes,
                onValueChange = { viewModel.onEvent(AddEditRecurringRuleUiEvent.OnNotesChanged(it)) },
                label = { Text("Note template (optional)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )

            uiState.errorMessage?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = { viewModel.onEvent(AddEditRecurringRuleUiEvent.OnSaveClick) },
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState.canSave && !uiState.isSaving
            ) {
                Text(if (uiState.isSaving) "Saving..." else "Save Recurring Rule")
            }
        }
    }
}
