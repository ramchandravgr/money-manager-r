package com.mmr.feature.loan.ui

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
import com.mmr.core.model.enums.LoanType
import com.mmr.feature.loan.model.AddEditLoanUiEvent
import com.mmr.feature.loan.viewmodel.AddEditLoanViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditLoanScreen(
    onNavigateBack: () -> Unit,
    viewModel: AddEditLoanViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New Loan Entry", fontWeight = FontWeight.Bold) },
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
                        onClick = { viewModel.onEvent(AddEditLoanUiEvent.OnSaveClick) },
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
            // Loan Type / Direction
            Text("Loan Direction", style = MaterialTheme.typography.titleMedium)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = uiState.loanType == LoanType.BORROWED,
                    onClick = { viewModel.onEvent(AddEditLoanUiEvent.OnLoanTypeSelected(LoanType.BORROWED)) },
                    label = { Text("Borrowed (I owe)") }
                )
                FilterChip(
                    selected = uiState.loanType == LoanType.LENT,
                    onClick = { viewModel.onEvent(AddEditLoanUiEvent.OnLoanTypeSelected(LoanType.LENT)) },
                    label = { Text("Lent (Owed to me)") }
                )
            }

            // Loan Name / Description
            OutlinedTextField(
                value = uiState.name,
                onValueChange = { viewModel.onEvent(AddEditLoanUiEvent.OnNameChanged(it)) },
                label = { Text("Loan Name (e.g. Car Loan, Friend Loan)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Lender / Borrower Name
            OutlinedTextField(
                value = uiState.lenderName,
                onValueChange = { viewModel.onEvent(AddEditLoanUiEvent.OnLenderNameChanged(it)) },
                label = {
                    Text(if (uiState.loanType == LoanType.BORROWED) "Lender / Bank Name" else "Borrower / Person Name")
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Principal Amount
            OutlinedTextField(
                value = uiState.principalAmount,
                onValueChange = { viewModel.onEvent(AddEditLoanUiEvent.OnPrincipalChanged(it)) },
                label = { Text("Principal Amount (₹)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Interest Rate
            OutlinedTextField(
                value = uiState.interestRate,
                onValueChange = { viewModel.onEvent(AddEditLoanUiEvent.OnInterestRateChanged(it)) },
                label = { Text("Interest Rate % (optional)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Notes
            OutlinedTextField(
                value = uiState.notes,
                onValueChange = { viewModel.onEvent(AddEditLoanUiEvent.OnNotesChanged(it)) },
                label = { Text("Notes (optional)") },
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
                onClick = { viewModel.onEvent(AddEditLoanUiEvent.OnSaveClick) },
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState.canSave && !uiState.isSaving
            ) {
                Text(if (uiState.isSaving) "Saving Loan..." else "Create Loan")
            }
        }
    }
}
