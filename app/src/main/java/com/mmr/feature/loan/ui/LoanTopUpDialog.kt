package com.mmr.feature.loan.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mmr.app.ui.components.MmrContentDialog
import com.mmr.app.ui.components.MmrDialogAmountTextField
import com.mmr.app.ui.components.MmrDialogBodyNote
import com.mmr.app.ui.components.MmrDialogDetailLine
import com.mmr.app.ui.components.MmrDialogPickerField
import com.mmr.app.ui.components.MmrDialogTextField
import com.mmr.core.common.validation.MmrValidation
import com.mmr.core.model.enums.LoanRepaymentMode
import com.mmr.core.model.enums.LoanTopUpAdjustment
import com.mmr.feature.stats.ui.StatsDatePickerDialog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LoanTopUpDialog(
    outstandingFormatted: String,
    repaymentMode: LoanRepaymentMode,
    amountInput: String,
    notesInput: String,
    topUpDateMillis: Long,
    topUpAdjustment: LoanTopUpAdjustment,
    isSubmitting: Boolean,
    errorMessage: String?,
    onAmountChanged: (String) -> Unit,
    onNotesChanged: (String) -> Unit,
    onTopUpDateChanged: (Long) -> Unit,
    onTopUpAdjustmentSelected: (LoanTopUpAdjustment) -> Unit,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit,
) {
    var showDatePicker by remember { mutableStateOf(false) }
    val dateLabel =
        remember(topUpDateMillis) {
            SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(topUpDateMillis))
        }
    val amountValid =
        remember(amountInput) {
            MmrValidation.parsePositiveAmount(amountInput) != null
        }
    val extendTenure = topUpAdjustment == LoanTopUpAdjustment.EXTEND_TENURE

    MmrContentDialog(
        title = "Add top-up",
        message =
            "Increases principal and outstanding on this loan. A transfer is posted to your ledger when a primary account is set.",
        onDismiss = onDismiss,
        confirmLabel = if (isSubmitting) "Saving…" else "Confirm top-up",
        onConfirm = onSubmit,
        confirmEnabled = !isSubmitting && amountValid,
        dismissEnabled = !isSubmitting,
        dismissLabel = "Cancel",
    ) {
        MmrDialogDetailLine(label = "Outstanding now", value = outstandingFormatted)
        MmrDialogAmountTextField(
            value = amountInput,
            onValueChange = onAmountChanged,
            label = "Additional principal (₹)",
            enabled = !isSubmitting,
            isError = errorMessage != null,
            errorMessage = errorMessage,
        )
        MmrDialogPickerField(
            label = "Top-up date",
            value = dateLabel,
            onClick = { showDatePicker = true },
            enabled = !isSubmitting,
        )
        if (repaymentMode == LoanRepaymentMode.FIXED_EMI) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                MmrDialogBodyNote(
                    text =
                        if (extendTenure) {
                            "After top-up: keep EMI, extend tenure"
                        } else {
                            "After top-up: keep tenure, raise EMI"
                        },
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Tenure", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(
                            checked = !extendTenure,
                            onCheckedChange = { increaseEmi ->
                                onTopUpAdjustmentSelected(
                                    if (increaseEmi) {
                                        LoanTopUpAdjustment.INCREASE_EMI
                                    } else {
                                        LoanTopUpAdjustment.EXTEND_TENURE
                                    },
                                )
                            },
                            enabled = !isSubmitting,
                        )
                        Text("EMI", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
        MmrDialogTextField(
            value = notesInput,
            onValueChange = onNotesChanged,
            label = "Notes (optional)",
            singleLine = false,
            minLines = 1,
            maxLines = 3,
            enabled = !isSubmitting,
        )
    }

    if (showDatePicker) {
        StatsDatePickerDialog(
            initialMillis = topUpDateMillis,
            onDismiss = { showDatePicker = false },
            onConfirm = {
                onTopUpDateChanged(it)
                showDatePicker = false
            },
        )
    }
}
