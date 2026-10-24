package com.mmr.feature.approval.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mmr.app.ui.components.MmrDimens
import com.mmr.app.ui.components.MmrFormCompactSearchField
import com.mmr.app.ui.components.MmrPeriodSwipeBox
import com.mmr.app.ui.state.MmrEmptyState
import com.mmr.app.ui.state.MmrLoadingState
import com.mmr.app.ui.theme.MmrBrandColors
import com.mmr.feature.approval.model.CardRefundApplyStep
import com.mmr.feature.approval.model.PendingCardRefundApplyPrompt
import com.mmr.feature.home.ui.GroupedTransactionList

@Composable
fun ApplyCardRefundDialog(
    prompt: PendingCardRefundApplyPrompt,
    onDismiss: () -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onNoteQueryChange: (String) -> Unit,
    onSelectPurchase: (Long) -> Unit,
    onBackToPicker: () -> Unit,
    onToggleFullRefund: (Boolean) -> Unit,
    onPartialAmountChange: (String) -> Unit,
    onConfirmApply: () -> Unit,
) {
    val periodKey = "${prompt.pickerYear}-${prompt.pickerMonth}-${prompt.noteQuery}-${prompt.step}"
    val title =
        when (prompt.step) {
            CardRefundApplyStep.PICK_PURCHASE -> "Apply refund · ${prompt.refundAmountLabel}"
            CardRefundApplyStep.CONFIRM_AMOUNT -> "Refund amount"
        }

    Dialog(
        onDismissRequest = onDismiss,
        properties =
            DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = true,
                dismissOnClickOutside = false,
            ),
    ) {
        Surface(
            modifier =
                Modifier
                    .fillMaxWidth(0.96f)
                    .fillMaxHeight(0.92f),
            shape = RoundedCornerShape(16.dp),
            color = MmrBrandColors.ListBackground,
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .background(MmrBrandColors.Header)
                            .padding(horizontal = MmrDimens.spaceSm, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onDismiss, modifier = Modifier.size(MmrDimens.topBarIconButton)) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = "Close",
                            tint = MmrBrandColors.HeaderIcon,
                            modifier = Modifier.size(MmrDimens.iconMd),
                        )
                    }
                    Text(
                        text = title,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MmrBrandColors.HeaderIcon,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    IconButton(onClick = {}, enabled = false, modifier = Modifier.size(MmrDimens.topBarIconButton)) {}
                }

                if (prompt.step == CardRefundApplyStep.PICK_PURCHASE) {
                    Column(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .background(MmrBrandColors.SurfaceWhite)
                                .padding(horizontal = MmrDimens.spaceMd, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = prompt.smsSnippet,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            IconButton(onClick = onPreviousMonth, modifier = Modifier.size(36.dp)) {
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Previous month")
                            }
                            Text(
                                text = prompt.monthTitle,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                            )
                            IconButton(onClick = onNextMonth, modifier = Modifier.size(36.dp)) {
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Next month")
                            }
                        }
                        MmrFormCompactSearchField(
                            value = prompt.noteQuery,
                            onValueChange = onNoteQueryChange,
                            placeholder = "Search note",
                        )
                    }
                    HorizontalDivider(color = MmrBrandColors.BorderMain, thickness = 0.5.dp)
                    MmrPeriodSwipeBox(
                        periodKey = periodKey,
                        enabled = !prompt.isLoadingCandidates,
                        onPrevious = onPreviousMonth,
                        onNext = onNextMonth,
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                    ) {
                        when {
                            prompt.isLoadingCandidates ->
                                MmrLoadingState(modifier = Modifier.fillMaxSize())
                            prompt.isEmptyMonth ->
                                MmrEmptyState(
                                    message = "No matching purchases this month.",
                                    modifier = Modifier.fillMaxSize(),
                                )
                            else ->
                                GroupedTransactionList(
                                    groups = prompt.groupedTransactions,
                                    onItemClick = onSelectPurchase,
                                    modifier = Modifier.fillMaxSize(),
                                    contentBottomPadding = MmrDimens.spaceLg,
                                )
                        }
                    }
                } else {
                    Column(
                        modifier =
                            Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(MmrDimens.spaceMd),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            text = "Refund from SMS: ${prompt.refundAmountLabel}",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = prompt.applyFullRefund,
                                onClick = { onToggleFullRefund(true) },
                                label = { Text("Full refund") },
                            )
                            FilterChip(
                                selected = !prompt.applyFullRefund,
                                onClick = { onToggleFullRefund(false) },
                                label = { Text("Partial") },
                            )
                        }
                        if (!prompt.applyFullRefund) {
                            OutlinedTextField(
                                value = prompt.partialRefundAmountText,
                                onValueChange = onPartialAmountChange,
                                label = { Text("Refund amount (₹)") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                            )
                        }
                    }
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = MmrDimens.spaceMd, vertical = MmrDimens.spaceSm),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        TextButton(onClick = onBackToPicker, modifier = Modifier.weight(1f)) {
                            Text("Back")
                        }
                        Button(onClick = onConfirmApply, modifier = Modifier.weight(1f)) {
                            Text("Apply refund")
                        }
                    }
                }
            }
        }
    }
}
