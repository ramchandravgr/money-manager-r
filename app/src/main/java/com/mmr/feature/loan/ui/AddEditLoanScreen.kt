package com.mmr.feature.loan.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mmr.app.testing.MmrTestTags
import com.mmr.app.ui.components.MmrAppBarSaveIcon
import com.mmr.app.ui.components.MmrDimens
import com.mmr.app.ui.components.MmrPrimaryActionButton
import com.mmr.app.ui.components.MmrTopAppBar
import com.mmr.core.common.validation.MmrValidation
import com.mmr.core.loan.LoanAmortizationEngine
import com.mmr.core.model.enums.LoanRepaymentMode
import com.mmr.core.model.enums.LoanType
import com.mmr.feature.loan.model.AddEditLoanUiEvent
import com.mmr.feature.loan.model.FixedEmiEntryMode
import com.mmr.feature.loan.viewmodel.AddEditLoanViewModel
import com.mmr.feature.stats.ui.StatsDatePickerDialog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddEditLoanScreen(
    onNavigateBack: () -> Unit,
    viewModel: AddEditLoanViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val editLoanId = uiState.loanId
    val isEdit = editLoanId != null && editLoanId > 0L

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            MmrTopAppBar(
                title = if (isEdit) "Edit loan" else "New Loan Entry",
                showBackNavigation = true,
                onNavigateBack = onNavigateBack,
                useMmHeaderStyle = true,
                actions = {
                    MmrAppBarSaveIcon(
                        onClick = { viewModel.onEvent(AddEditLoanUiEvent.OnSaveClick) },
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
                    .padding(MmrDimens.cardPadding)
                    .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(MmrDimens.spaceLg),
        ) {
            // Loan Type / Direction
            Text("Loan Direction", style = MaterialTheme.typography.titleMedium)
            if (!isEdit) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = uiState.loanType == LoanType.BORROWED,
                        onClick = { viewModel.onEvent(AddEditLoanUiEvent.OnLoanTypeSelected(LoanType.BORROWED)) },
                        label = { Text("Borrowed (I owe)") },
                    )
                    FilterChip(
                        selected = uiState.loanType == LoanType.LENT,
                        onClick = { viewModel.onEvent(AddEditLoanUiEvent.OnLoanTypeSelected(LoanType.LENT)) },
                        label = { Text("Lent (Owed to me)") },
                    )
                }
            }

            // Loan Name / Description
            OutlinedTextField(
                value = uiState.name,
                onValueChange = { viewModel.onEvent(AddEditLoanUiEvent.OnNameChanged(it)) },
                label = { Text("Loan Name (e.g. Car Loan, Friend Loan)") },
                modifier =
                    Modifier
                        .testTag(MmrTestTags.loanFieldName)
                        .fillMaxWidth(),
                singleLine = true,
            )

            // Lender / Borrower Name
            OutlinedTextField(
                value = uiState.lenderName,
                onValueChange = { viewModel.onEvent(AddEditLoanUiEvent.OnLenderNameChanged(it)) },
                label = {
                    Text(if (uiState.loanType == LoanType.BORROWED) "Lender / Bank Name" else "Borrower / Person Name")
                },
                modifier =
                    Modifier
                        .testTag(MmrTestTags.loanFieldLender)
                        .fillMaxWidth(),
                singleLine = true,
            )

            // Principal Amount
            OutlinedTextField(
                value = uiState.principalAmount,
                onValueChange = { viewModel.onEvent(AddEditLoanUiEvent.OnPrincipalChanged(it)) },
                label = { Text("Principal Amount (₹)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier =
                    Modifier
                        .testTag(MmrTestTags.loanFieldPrincipal)
                        .fillMaxWidth(),
                singleLine = true,
                enabled = !isEdit,
            )

            OutlinedTextField(
                value = uiState.interestRate,
                onValueChange = { viewModel.onEvent(AddEditLoanUiEvent.OnInterestRateChanged(it)) },
                label = { Text("Interest Rate % (optional)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            LoanDisbursalDateField(
                disbursalDateMillis = uiState.disbursalDateMillis,
                enabled = uiState.canEditDisbursalDate,
                onDateSelected = { viewModel.onEvent(AddEditLoanUiEvent.OnDisbursalDateChanged(it)) },
            )

            Text("Repayment style", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = uiState.repaymentMode == LoanRepaymentMode.FIXED_EMI,
                    onClick = {
                        viewModel.onEvent(
                            AddEditLoanUiEvent.OnRepaymentModeSelected(LoanRepaymentMode.FIXED_EMI),
                        )
                    },
                    label = { Text("Fixed monthly EMI") },
                )
                FilterChip(
                    selected = uiState.repaymentMode == LoanRepaymentMode.FLEXIBLE,
                    onClick = {
                        viewModel.onEvent(
                            AddEditLoanUiEvent.OnRepaymentModeSelected(LoanRepaymentMode.FLEXIBLE),
                        )
                    },
                    label = { Text("Flexible payments") },
                )
            }
            if (uiState.repaymentMode == LoanRepaymentMode.FIXED_EMI) {
                Text(
                    "EMI is due on the same day each month. Enter either tenure or EMI — not both.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        if (uiState.fixedEmiEntryMode == FixedEmiEntryMode.TENURE) "By tenure" else "By EMI",
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text("Tenure", style = MaterialTheme.typography.labelMedium)
                        Switch(
                            checked = uiState.fixedEmiEntryMode == FixedEmiEntryMode.EMI_AMOUNT,
                            onCheckedChange = { byEmi ->
                                viewModel.onEvent(
                                    AddEditLoanUiEvent.OnFixedEmiEntryModeSelected(
                                        if (byEmi) FixedEmiEntryMode.EMI_AMOUNT else FixedEmiEntryMode.TENURE,
                                    ),
                                )
                            },
                        )
                        Text("EMI", style = MaterialTheme.typography.labelMedium)
                    }
                }
                val principalForPreview = MmrValidation.parsePositiveAmount(uiState.principalAmount)
                val rateForPreview =
                    MmrValidation.sanitizeDecimalInput(uiState.interestRate).toDoubleOrNull()?.coerceAtLeast(0.0)
                        ?: 0.0
                when (uiState.fixedEmiEntryMode) {
                    FixedEmiEntryMode.TENURE -> {
                        OutlinedTextField(
                            value = uiState.tenureMonths,
                            onValueChange = { viewModel.onEvent(AddEditLoanUiEvent.OnTenureMonthsChanged(it)) },
                            label = { Text("Tenure (months)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                        )
                        uiState.tenureMonths.toIntOrNull()?.takeIf { it > 0 }?.let { months ->
                            principalForPreview?.let { principal ->
                                val emi = LoanAmortizationEngine.computeEmi(principal, rateForPreview, months)
                                Text(
                                    "Calculated monthly EMI: ₹${"%.2f".format(emi)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                    FixedEmiEntryMode.EMI_AMOUNT -> {
                        OutlinedTextField(
                            value = uiState.fixedEmiOverride,
                            onValueChange = { viewModel.onEvent(AddEditLoanUiEvent.OnFixedEmiOverrideChanged(it)) },
                            label = { Text("Monthly EMI (₹)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                        )
                        uiState.fixedEmiOverride.toDoubleOrNull()?.takeIf { it > 0.0 }?.let { emi ->
                            principalForPreview?.let { principal ->
                                val months =
                                    LoanAmortizationEngine.computeTenureMonths(principal, rateForPreview, emi)
                                when {
                                    months > 0 ->
                                        Text(
                                            "Calculated tenure: $months months",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary,
                                        )
                                    else -> {
                                        val floor =
                                            LoanAmortizationEngine.minimumEmiForAmortization(
                                                principal,
                                                rateForPreview,
                                            )
                                        Text(
                                            "EMI too low — need at least ₹1 above interest (≥ ₹${"%.2f".format(
                                                floor,
                                            )} / month).",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.error,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                OutlinedTextField(
                    value = uiState.emiDayOfMonth,
                    onValueChange = { viewModel.onEvent(AddEditLoanUiEvent.OnEmiDayChanged(it)) },
                    label = { Text("EMI day of month (1–28)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
            }

            if (uiState.loanLiabilityAccounts.isNotEmpty()) {
                Text("Loan liability account", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Ledger transfers for repayments use this account.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    uiState.loanLiabilityAccounts.forEach { account ->
                        FilterChip(
                            selected = uiState.linkedLoanAccountId == account.id,
                            onClick = {
                                viewModel.onEvent(AddEditLoanUiEvent.OnLinkedLoanAccountSelected(account.id))
                            },
                            label = { Text(account.name) },
                        )
                    }
                }
            }

            // Notes
            OutlinedTextField(
                value = uiState.notes,
                onValueChange = { viewModel.onEvent(AddEditLoanUiEvent.OnNotesChanged(it)) },
                label = { Text("Notes (optional)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
            )

            uiState.errorMessage?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            MmrPrimaryActionButton(
                text =
                    when {
                        uiState.isSaving -> "Saving…"
                        isEdit -> "Save changes"
                        else -> "Create Loan"
                    },
                onClick = { viewModel.onEvent(AddEditLoanUiEvent.OnSaveClick) },
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState.canSave && !uiState.isSaving,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LoanDisbursalDateField(
    disbursalDateMillis: Long,
    enabled: Boolean,
    onDateSelected: (Long) -> Unit,
) {
    var showPicker by remember { mutableStateOf(false) }
    val label =
        remember(disbursalDateMillis) {
            SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(disbursalDateMillis))
        }
    OutlinedCard(
        onClick = { if (enabled) showPicker = true },
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(MmrDimens.cardPadding),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text("Disbursal date", style = MaterialTheme.typography.labelLarge)
            Text(label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(
                "Interest accrues from this date through each EMI due date (defaults to today when you add the loan).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (!enabled) {
                Text(
                    "Locked after the first repayment is recorded.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
        }
    }
    if (showPicker) {
        StatsDatePickerDialog(
            initialMillis = disbursalDateMillis,
            onDismiss = { showPicker = false },
            onConfirm = {
                onDateSelected(it)
                showPicker = false
            },
        )
    }
}
