package com.mmr.feature.loan.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mmr.core.model.enums.LoanPaymentType
import com.mmr.core.model.enums.LoanStatus
import com.mmr.core.model.enums.LoanType
import com.mmr.feature.loan.model.*
import com.mmr.app.ui.state.MmrErrorState
import com.mmr.app.ui.state.MmrLoadingState
import com.mmr.app.ui.state.MmrStatusBanner
import com.mmr.feature.loan.viewmodel.LoanDetailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoanDetailScreen(
    onNavigateBack: () -> Unit,
    viewModel: LoanDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showRepaymentDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.isRepaymentSuccess) {
        if (uiState.isRepaymentSuccess) {
            showRepaymentDialog = false
            viewModel.onEvent(LoanDetailUiEvent.OnClearRepaymentState)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.loan?.name ?: "Loan Details", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    TextButton(onClick = onNavigateBack) {
                        Text("Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    uiState.loan?.let { loan ->
                        if (loan.status == LoanStatus.ACTIVE) {
                            TextButton(onClick = { viewModel.onEvent(LoanDetailUiEvent.OnStatusChanged(LoanStatus.PAUSED)) }) {
                                Text("Pause")
                            }
                        } else if (loan.status == LoanStatus.PAUSED) {
                            TextButton(onClick = { viewModel.onEvent(LoanDetailUiEvent.OnStatusChanged(LoanStatus.ACTIVE)) }) {
                                Text("Resume")
                            }
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            uiState.loan?.let { loan ->
                if (loan.status != LoanStatus.CLOSED && loan.outstandingAmount > 0.0) {
                    ExtendedFloatingActionButton(
                        onClick = { showRepaymentDialog = true },
                        text = { Text("Record Payment") },
                        icon = { Text("+") }
                    )
                }
            }
        }
    ) { innerPadding ->
        when {
            uiState.isLoading -> {
                MmrLoadingState(modifier = Modifier.fillMaxSize().padding(innerPadding))
            }

            uiState.errorMessage != null && uiState.loan == null -> {
                MmrErrorState(
                    message = uiState.errorMessage ?: "Failed to load loan details.",
                    onRetry = { viewModel.onEvent(LoanDetailUiEvent.OnRetryClick) },
                    modifier = Modifier.fillMaxSize().padding(innerPadding)
                )
            }

            uiState.loan != null -> {
                val loan = uiState.loan!!
                val isBorrowed = loan.loanType == LoanType.BORROWED
                val directionColor = if (isBorrowed) Color(0xFFEF4444) else Color(0xFF10B981)

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    uiState.statusMessage?.let { message ->
                        item {
                            MmrStatusBanner(
                                message = message,
                                onDismiss = { viewModel.onEvent(LoanDetailUiEvent.OnDismissStatus) }
                            )
                        }
                    }

                    // 1. Overview Card
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = if (isBorrowed) "I Owe (Borrowed)" else "Owed to Me (Lent)",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = directionColor,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = loan.lenderName,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer
                                    ) {
                                        Text(
                                            text = loan.statusLabel,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }
                                }

                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 12.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("Outstanding", style = MaterialTheme.typography.labelSmall)
                                        Text(
                                            text = loan.outstandingFormatted,
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = directionColor
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("Principal", style = MaterialTheme.typography.labelSmall)
                                        Text(
                                            text = loan.principalFormatted,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                LinearProgressIndicator(
                                    progress = { loan.progressPercentage },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = directionColor,
                                    trackColor = MaterialTheme.colorScheme.surface
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${loan.progressFormatted} repaid",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = loan.dueDateFormatted?.let { "Due: $it" } ?: "No due date set",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                        }
                    }

                    // 2. Repayment History Section Header
                    item {
                        Text(
                            text = "Repayment History",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // 3. Payment history list or empty
                    if (uiState.payments.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Text(
                                    text = "No repayments recorded yet.\nTap 'Record Payment' to add one.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        }
                    } else {
                        items(uiState.payments, key = { it.id }) { payment ->
                            PaymentHistoryCard(payment = payment)
                        }
                    }
                }
            }
        }
    }

    // Repayment Entry Dialog
    if (showRepaymentDialog) {
        RepaymentEntryDialog(
            amountInput = uiState.repaymentAmountInput,
            notesInput = uiState.repaymentNotesInput,
            selectedType = uiState.repaymentType,
            isSubmitting = uiState.isRepaymentSubmitting,
            errorMessage = uiState.errorMessage,
            onAmountChanged = { viewModel.onEvent(LoanDetailUiEvent.OnRepaymentAmountChanged(it)) },
            onNotesChanged = { viewModel.onEvent(LoanDetailUiEvent.OnRepaymentNotesChanged(it)) },
            onTypeSelected = { viewModel.onEvent(LoanDetailUiEvent.OnRepaymentTypeSelected(it)) },
            onSubmit = { viewModel.onEvent(LoanDetailUiEvent.OnSubmitRepayment) },
            onDismiss = {
                showRepaymentDialog = false
                viewModel.onEvent(LoanDetailUiEvent.OnClearRepaymentState)
            }
        )
    }
}

@Composable
private fun PaymentHistoryCard(payment: LoanPaymentItemUi) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = payment.paymentAmountFormatted,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF10B981)
                    )
                    Text(
                        text = payment.paymentDateFormatted,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        text = payment.paymentTypeLabel,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Remaining balance: ${payment.outstandingAfterFormatted}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            payment.notes?.takeIf { it.isNotBlank() }?.let { note ->
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Note: $note",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

@Composable
private fun RepaymentEntryDialog(
    amountInput: String,
    notesInput: String,
    selectedType: LoanPaymentType,
    isSubmitting: Boolean,
    errorMessage: String?,
    onAmountChanged: (String) -> Unit,
    onNotesChanged: (String) -> Unit,
    onTypeSelected: (LoanPaymentType) -> Unit,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Record Loan Payment", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = amountInput,
                    onValueChange = onAmountChanged,
                    label = { Text("Payment Amount (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Repayment Type
                Text("Payment Type", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedType == LoanPaymentType.PART_PAYMENT,
                        onClick = { onTypeSelected(LoanPaymentType.PART_PAYMENT) },
                        label = { Text("Part Payment") }
                    )
                    FilterChip(
                        selected = selectedType == LoanPaymentType.CLOSING_PAYMENT,
                        onClick = { onTypeSelected(LoanPaymentType.CLOSING_PAYMENT) },
                        label = { Text("Full Payoff") }
                    )
                }

                OutlinedTextField(
                    value = notesInput,
                    onValueChange = onNotesChanged,
                    label = { Text("Notes (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )

                errorMessage?.let { err ->
                    Text(text = err, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onSubmit,
                enabled = !isSubmitting && amountInput.isNotBlank()
            ) {
                Text(if (isSubmitting) "Saving..." else "Confirm Payment")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSubmitting) {
                Text("Cancel")
            }
        }
    )
}
