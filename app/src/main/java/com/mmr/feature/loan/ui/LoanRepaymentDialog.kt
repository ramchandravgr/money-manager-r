package com.mmr.feature.loan.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mmr.app.testing.MmrTestTags
import com.mmr.app.ui.components.MmrDimens
import com.mmr.app.ui.components.MmrPrimaryActionButton
import com.mmr.app.ui.theme.MmrMmColors
import com.mmr.core.common.validation.MmrValidation
import com.mmr.core.model.enums.LoanPartPaymentAdjustment
import com.mmr.core.model.enums.LoanPaymentType
import com.mmr.core.model.enums.LoanRepaymentMode
import com.mmr.feature.stats.ui.StatsDatePickerDialog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoanRepaymentDialog(
    outstandingFormatted: String,
    nextEmiHint: String?,
    flexibleAccruedInterestHint: String?,
    amountInput: String,
    notesInput: String,
    paymentDateMillis: Long,
    selectedType: LoanPaymentType,
    repaymentMode: LoanRepaymentMode,
    partPaymentAdjustment: LoanPartPaymentAdjustment,
    isSubmitting: Boolean,
    errorMessage: String?,
    onAmountChanged: (String) -> Unit,
    onNotesChanged: (String) -> Unit,
    onPaymentDateChanged: (Long) -> Unit,
    onTypeSelected: (LoanPaymentType) -> Unit,
    onPartPaymentAdjustmentSelected: (LoanPartPaymentAdjustment) -> Unit,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit,
) {
    var showDatePicker by remember { mutableStateOf(false) }
    val paymentDateLabel =
        remember(paymentDateMillis) {
            SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(paymentDateMillis))
        }
    val amountValid =
        remember(amountInput) {
            MmrValidation.parsePositiveAmount(amountInput) != null
        }
    val reduceEmiAfterPart =
        partPaymentAdjustment == LoanPartPaymentAdjustment.REDUCE_EMI

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
                    text = "Record payment",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MmrMmColors.Reversal,
                )

                OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "Outstanding",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            outstandingFormatted,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }

                OutlinedCard(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Payment date", style = MaterialTheme.typography.labelLarge)
                        Text(
                            paymentDateLabel,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }

                OutlinedTextField(
                    value = amountInput,
                    onValueChange = onAmountChanged,
                    label = { Text("Amount (₹)") },
                    supportingText = {
                        when {
                            flexibleAccruedInterestHint != null ->
                                Text(
                                    "Accrued interest ~$flexibleAccruedInterestHint is paid first; the rest reduces principal.",
                                )
                            selectedType == LoanPaymentType.CLOSING_PAYMENT ->
                                Text("Uses full outstanding balance.")
                            nextEmiHint != null ->
                                Text("Next planned EMI: $nextEmiHint")
                            else -> null
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier =
                        Modifier
                            .testTag(MmrTestTags.loanRepaymentAmount)
                            .fillMaxWidth(),
                    enabled = selectedType != LoanPaymentType.CLOSING_PAYMENT,
                )

                Text(
                    "Payment type",
                    style = MaterialTheme.typography.labelLarge,
                )
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = selectedType == LoanPaymentType.PART_PAYMENT,
                        onClick = { onTypeSelected(LoanPaymentType.PART_PAYMENT) },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                        modifier = Modifier.weight(1f),
                        icon = {},
                    ) {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("Part")
                        }
                    }
                    SegmentedButton(
                        selected = selectedType == LoanPaymentType.CLOSING_PAYMENT,
                        onClick = { onTypeSelected(LoanPaymentType.CLOSING_PAYMENT) },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        modifier = Modifier.weight(1f),
                        icon = {},
                    ) {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("Full")
                        }
                    }
                }

                if (
                    repaymentMode == LoanRepaymentMode.FIXED_EMI &&
                    selectedType == LoanPaymentType.PART_PAYMENT
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "After part payment",
                                style = MaterialTheme.typography.labelLarge,
                            )
                            Text(
                                if (reduceEmiAfterPart) "Lower monthly EMI" else "Fewer months left",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text("Tenure", style = MaterialTheme.typography.labelMedium)
                            Switch(
                                checked = reduceEmiAfterPart,
                                onCheckedChange = { reduceEmi ->
                                    onPartPaymentAdjustmentSelected(
                                        if (reduceEmi) {
                                            LoanPartPaymentAdjustment.REDUCE_EMI
                                        } else {
                                            LoanPartPaymentAdjustment.REDUCE_TENURE
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
                    maxLines = 3,
                )

                errorMessage?.let { err ->
                    Text(
                        text = err,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = onDismiss, enabled = !isSubmitting) {
                        Text("Cancel", color = MmrMmColors.TextMedium)
                    }
                    MmrPrimaryActionButton(
                        text = if (isSubmitting) "Saving…" else "Confirm",
                        onClick = onSubmit,
                        enabled = !isSubmitting && amountValid,
                        modifier = Modifier.testTag(MmrTestTags.loanRepaymentConfirm),
                    )
                }
            }
        }
    }

    if (showDatePicker) {
        StatsDatePickerDialog(
            initialMillis = paymentDateMillis,
            onDismiss = { showDatePicker = false },
            onConfirm = {
                onPaymentDateChanged(it)
                showDatePicker = false
            },
        )
    }
}
