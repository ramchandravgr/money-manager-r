package com.mmr.feature.loan.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.mmr.app.testing.MmrTestTags
import com.mmr.app.ui.components.MmrTransactionFormChipOption
import com.mmr.app.ui.components.MmrTransactionFormChoiceChips
import com.mmr.app.ui.components.MmrTransactionFormFilterChip
import com.mmr.app.ui.components.MmrTransactionFormIcons
import com.mmr.app.ui.components.MmrTransactionFormPickerRow
import com.mmr.app.ui.components.MmrTransactionFormSectionLabel
import com.mmr.app.ui.components.MmrTransactionFormTextRow
import com.mmr.core.common.display.MmrMoneyFormat
import com.mmr.core.common.validation.MmrValidation
import com.mmr.core.loan.LoanAmortizationEngine
import com.mmr.core.model.enums.LoanRepaymentMode
import com.mmr.core.model.enums.LoanType
import com.mmr.feature.loan.model.AddEditLoanUiEvent
import com.mmr.feature.loan.model.AddEditLoanUiState
import com.mmr.feature.loan.model.FixedEmiEntryMode
import com.mmr.feature.loan.model.LoanLiabilityAccountUi
import com.mmr.feature.stats.ui.StatsDatePickerDialog
import com.mmr.feature.transaction.ui.TransactionFormPickerField
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LoanFormFields(
    uiState: AddEditLoanUiState,
    isEdit: Boolean,
    onEvent: (AddEditLoanUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (!isEdit) {
            MmrTransactionFormSectionLabel(text = "Loan direction")
            MmrTransactionFormChoiceChips(
                options =
                    listOf(
                        MmrTransactionFormChipOption(LoanType.BORROWED, "Borrowed"),
                        MmrTransactionFormChipOption(LoanType.LENT, "Lent"),
                    ),
                selected = uiState.loanType,
                onSelected = { onEvent(AddEditLoanUiEvent.OnLoanTypeSelected(it)) },
            )
        }

        LoanNameField(
            value = uiState.name,
            onValueChange = { onEvent(AddEditLoanUiEvent.OnNameChanged(it)) },
        )
        LoanLenderField(
            value = uiState.lenderName,
            loanType = uiState.loanType,
            onValueChange = { onEvent(AddEditLoanUiEvent.OnLenderNameChanged(it)) },
        )
        LoanPrincipalField(
            value = uiState.principalAmount,
            enabled = !isEdit,
            onValueChange = { onEvent(AddEditLoanUiEvent.OnPrincipalChanged(it)) },
        )
        LoanInterestRateField(
            value = uiState.interestRate,
            onValueChange = { onEvent(AddEditLoanUiEvent.OnInterestRateChanged(it)) },
        )
        LoanDisbursalDateField(
            disbursalDateMillis = uiState.disbursalDateMillis,
            enabled = uiState.canEditDisbursalDate,
            onDateSelected = { onEvent(AddEditLoanUiEvent.OnDisbursalDateChanged(it)) },
        )

        MmrTransactionFormSectionLabel(text = "Repayment style")
        MmrTransactionFormChoiceChips(
            options =
                listOf(
                    MmrTransactionFormChipOption(LoanRepaymentMode.FIXED_EMI, "Fixed EMI"),
                    MmrTransactionFormChipOption(LoanRepaymentMode.FLEXIBLE, "Flexible"),
                ),
            selected = uiState.repaymentMode,
            onSelected = { onEvent(AddEditLoanUiEvent.OnRepaymentModeSelected(it)) },
        )

        if (uiState.repaymentMode == LoanRepaymentMode.FIXED_EMI) {
            LoanFixedEmiFields(uiState = uiState, onEvent = onEvent)
        }

        if (uiState.loanLiabilityAccounts.isNotEmpty()) {
            LoanLiabilityAccountSection(
                accounts = uiState.loanLiabilityAccounts,
                selectedAccountId = uiState.linkedLoanAccountId,
                onAccountSelected = { onEvent(AddEditLoanUiEvent.OnLinkedLoanAccountSelected(it)) },
            )
        }

        LoanNotesField(
            value = uiState.notes,
            onValueChange = { onEvent(AddEditLoanUiEvent.OnNotesChanged(it)) },
        )

        uiState.errorMessage?.let { error ->
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
    }
}

@Composable
private fun LoanNameField(
    value: String,
    onValueChange: (String) -> Unit,
) {
    MmrTransactionFormTextRow(
        value = value,
        onValueChange = onValueChange,
        placeholder = "Loan name (e.g. Car loan)",
        leadingIcon = MmrTransactionFormIcons.name,
        testTag = MmrTestTags.loanFieldName,
    )
}

@Composable
private fun LoanLenderField(
    value: String,
    loanType: LoanType,
    onValueChange: (String) -> Unit,
) {
    val placeholder =
        if (loanType == LoanType.BORROWED) {
            "Lender / bank name"
        } else {
            "Borrower name"
        }
    MmrTransactionFormTextRow(
        value = value,
        onValueChange = onValueChange,
        placeholder = placeholder,
        leadingIcon = MmrTransactionFormIcons.lender,
        testTag = MmrTestTags.loanFieldLender,
    )
}

@Composable
private fun LoanPrincipalField(
    value: String,
    enabled: Boolean,
    onValueChange: (String) -> Unit,
) {
    if (!enabled) {
        MmrTransactionFormPickerRow(
            icon = MmrTransactionFormIcons.amount,
            placeholder = "Principal amount",
            value = value.ifBlank { "Principal amount" },
            onClick = null,
            enabled = false,
            modifier = Modifier.testTag(MmrTestTags.loanFieldPrincipal),
        )
        return
    }
    MmrTransactionFormTextRow(
        value = value,
        onValueChange = onValueChange,
        placeholder = "Principal amount",
        leadingIcon = MmrTransactionFormIcons.amount,
        testTag = MmrTestTags.loanFieldPrincipal,
        keyboardType = KeyboardType.Decimal,
        groupedAmountDisplay = true,
    )
}

@Composable
private fun LoanInterestRateField(
    value: String,
    onValueChange: (String) -> Unit,
) {
    MmrTransactionFormTextRow(
        value = value,
        onValueChange = onValueChange,
        placeholder = "Interest rate % (optional)",
        leadingIcon = MmrTransactionFormIcons.interestRate,
        keyboardType = KeyboardType.Decimal,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LoanDisbursalDateField(
    disbursalDateMillis: Long,
    enabled: Boolean,
    onDateSelected: (Long) -> Unit,
) {
    var showPicker by remember { mutableStateOf(false) }
    val formatted =
        remember(disbursalDateMillis) {
            SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(disbursalDateMillis))
        }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        TransactionFormPickerField(
            label = "Disbursal date",
            value = formatted,
            emptyHint = "",
            onClick =
                if (enabled) {
                    { showPicker = true }
                } else {
                    null
                },
            enabled = enabled,
            icon = MmrTransactionFormIcons.date,
        )
        Text(
            text =
                "Interest accrues from this date. Defaults to today when you create the loan.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        if (!enabled) {
            Text(
                text = "Locked after the first repayment is recorded.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
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

@Composable
private fun LoanFixedEmiFields(
    uiState: AddEditLoanUiState,
    onEvent: (AddEditLoanUiEvent) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "EMI is due on the same day each month. Enter tenure or EMI amount — not both.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        MmrTransactionFormSectionLabel(text = "EMI plan")
        MmrTransactionFormChoiceChips(
            options =
                listOf(
                    MmrTransactionFormChipOption(FixedEmiEntryMode.TENURE, "By tenure"),
                    MmrTransactionFormChipOption(FixedEmiEntryMode.EMI_AMOUNT, "By EMI"),
                ),
            selected = uiState.fixedEmiEntryMode,
            onSelected = { onEvent(AddEditLoanUiEvent.OnFixedEmiEntryModeSelected(it)) },
        )
        val principalForPreview = MmrValidation.parsePositiveAmount(uiState.principalAmount)
        val rateForPreview =
            MmrValidation.sanitizeDecimalInput(uiState.interestRate).toDoubleOrNull()?.coerceAtLeast(0.0)
                ?: 0.0
        when (uiState.fixedEmiEntryMode) {
            FixedEmiEntryMode.TENURE -> {
                MmrTransactionFormTextRow(
                    value = uiState.tenureMonths,
                    onValueChange = { onEvent(AddEditLoanUiEvent.OnTenureMonthsChanged(it)) },
                    placeholder = "Tenure (months)",
                    keyboardType = KeyboardType.Number,
                )
                uiState.tenureMonths.toIntOrNull()?.takeIf { it > 0 }?.let { months ->
                    principalForPreview?.let { principal ->
                        val emi = LoanAmortizationEngine.computeEmi(principal, rateForPreview, months)
                        Text(
                            text = "Calculated monthly EMI: ${MmrMoneyFormat.formatInr(emi)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 4.dp),
                        )
                    }
                }
            }
            FixedEmiEntryMode.EMI_AMOUNT -> {
                MmrTransactionFormTextRow(
                    value = uiState.fixedEmiOverride,
                    onValueChange = { onEvent(AddEditLoanUiEvent.OnFixedEmiOverrideChanged(it)) },
                    placeholder = "Monthly EMI",
                    leadingIcon = MmrTransactionFormIcons.amount,
                    keyboardType = KeyboardType.Decimal,
                    groupedAmountDisplay = true,
                )
                uiState.fixedEmiOverride.toDoubleOrNull()?.takeIf { it > 0.0 }?.let { emi ->
                    principalForPreview?.let { principal ->
                        val months =
                            LoanAmortizationEngine.computeTenureMonths(principal, rateForPreview, emi)
                        when {
                            months > 0 ->
                                Text(
                                    text = "Calculated tenure: $months months",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 4.dp),
                                )
                            else -> {
                                val floor =
                                    LoanAmortizationEngine.minimumEmiForAmortization(
                                        principal,
                                        rateForPreview,
                                    )
                                Text(
                                    text =
                                        "EMI too low — need at least ${MmrMoneyFormat.formatInr(floor)} / month.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(horizontal = 4.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
        MmrTransactionFormTextRow(
            value = uiState.emiDayOfMonth,
            onValueChange = { onEvent(AddEditLoanUiEvent.OnEmiDayChanged(it)) },
            placeholder = "EMI day of month (1–28)",
            keyboardType = KeyboardType.Number,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LoanLiabilityAccountSection(
    accounts: List<LoanLiabilityAccountUi>,
    selectedAccountId: Long?,
    onAccountSelected: (Long) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        MmrTransactionFormSectionLabel(text = "Loan liability account")
        Text(
            text = "Ledger transfers for repayments use this account.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            accounts.forEach { account ->
                MmrTransactionFormFilterChip(
                    label = account.name,
                    selected = selectedAccountId == account.id,
                    onClick = { onAccountSelected(account.id) },
                )
            }
        }
    }
}

@Composable
private fun LoanNotesField(
    value: String,
    onValueChange: (String) -> Unit,
) {
    MmrTransactionFormTextRow(
        value = value,
        onValueChange = onValueChange,
        placeholder = "Notes (optional)",
        leadingIcon = MmrTransactionFormIcons.note,
        singleLine = false,
        minLines = 2,
    )
}
