package com.mmr.feature.loan.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mmr.app.ui.components.MmrDimens
import com.mmr.app.ui.components.MmrPrimaryActionButton
import com.mmr.app.ui.theme.MmrMmColors
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

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MmrDimens.space2xl),
            shape = RoundedCornerShape(16.dp),
            color = MmrMmColors.SurfaceWhite,
            shadowElevation = 8.dp,
        ) {
            Column(
                modifier = Modifier.padding(MmrDimens.cardPadding),
                verticalArrangement = Arrangement.spacedBy(MmrDimens.spaceMd),
            ) {
                Text(
                    "Add top-up",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MmrMmColors.Reversal,
                )
                Text(
                    "Increases principal and outstanding on this loan. A transfer is posted to your ledger when a primary account is set.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("Outstanding now", style = MaterialTheme.typography.labelMedium)
                        Text(outstandingFormatted, fontWeight = FontWeight.Bold)
                    }
                }
                OutlinedCard(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("Top-up date", style = MaterialTheme.typography.labelLarge)
                        Text(dateLabel, fontWeight = FontWeight.Medium)
                    }
                }
                OutlinedTextField(
                    value = amountInput,
                    onValueChange = onAmountChanged,
                    label = { Text("Additional principal (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (repaymentMode == LoanRepaymentMode.FIXED_EMI) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("After top-up", style = MaterialTheme.typography.labelLarge)
                            Text(
                                if (extendTenure) "Keep EMI, extend tenure" else "Keep tenure, raise EMI",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Tenure", style = MaterialTheme.typography.labelMedium)
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
                            )
                            Text("EMI", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
                OutlinedTextField(
                    value = notesInput,
                    onValueChange = onNotesChanged,
                    label = { Text("Notes (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 1,
                    maxLines = 2,
                )
                errorMessage?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDismiss, enabled = !isSubmitting) { Text("Cancel") }
                    MmrPrimaryActionButton(
                        text = if (isSubmitting) "Saving…" else "Confirm",
                        onClick = onSubmit,
                        enabled = !isSubmitting && amountValid,
                        modifier = Modifier,
                    )
                }
            }
        }
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
