package com.rrparallelstudio.miapp.feature.asset.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.rrparallelstudio.miapp.R
import com.rrparallelstudio.miapp.core.asset.displayLabel
import com.rrparallelstudio.miapp.core.asset.showsOptionalLinkedLoanField
import com.rrparallelstudio.miapp.core.model.enums.AssetPurchasePaymentMode
import com.rrparallelstudio.miapp.core.model.enums.AssetStatus
import com.rrparallelstudio.miapp.core.reminder.MmrReminderPresets
import com.rrparallelstudio.miapp.feature.asset.model.AddEditAssetUiEvent
import com.rrparallelstudio.miapp.feature.asset.model.AddEditAssetUiState
import com.rrparallelstudio.miapp.feature.asset.model.addEditAssetSaveBlocker
import com.rrparallelstudio.miapp.feature.asset.model.displayLabel
import com.rrparallelstudio.miapp.feature.stats.ui.StatsDatePickerDialog
import com.rrparallelstudio.miapp.feature.transaction.model.LoanPickerUi
import com.rrparallelstudio.miapp.ui.components.MmrIosSettingsChoiceRow
import com.rrparallelstudio.miapp.ui.components.MmrIosSettingsSingleChoiceSheet
import com.rrparallelstudio.miapp.ui.components.MmrTransactionFormChipOption
import com.rrparallelstudio.miapp.ui.components.MmrTransactionFormChoiceChips
import com.rrparallelstudio.miapp.ui.components.MmrTransactionFormFilterChip
import com.rrparallelstudio.miapp.ui.components.MmrTransactionFormIcons
import com.rrparallelstudio.miapp.ui.components.MmrTransactionFormPickerRow
import com.rrparallelstudio.miapp.ui.components.MmrTransactionFormPickerRowLabeled
import com.rrparallelstudio.miapp.ui.components.MmrTransactionFormSectionLabel
import com.rrparallelstudio.miapp.ui.components.MmrTransactionFormTextRow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AssetFormFields(
    uiState: AddEditAssetUiState,
    onEvent: (AddEditAssetUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    var showPurchaseDatePicker by remember { mutableStateOf(false) }
    var showNextDuePicker by remember { mutableStateOf(false) }
    var showSaleDatePicker by remember { mutableStateOf(false) }
    var showPaymentModeSheet by remember { mutableStateOf(false) }
    val paymentModeSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        MmrTransactionFormSectionLabel(text = "Asset name")
        MmrTransactionFormTextRow(
            value = uiState.name,
            onValueChange = { onEvent(AddEditAssetUiEvent.OnNameChanged(it)) },
            placeholder = "e.g. Family gold",
            leadingIcon = MmrTransactionFormIcons.name,
        )

        MmrTransactionFormSectionLabel(text = "Type")
        MmrTransactionFormTextRow(
            value = uiState.assetType,
            onValueChange = { onEvent(AddEditAssetUiEvent.OnAssetTypeChanged(it)) },
            placeholder = stringResource(R.string.asset_type_placeholder),
        )

        MmrTransactionFormSectionLabel(text = "Status")
        MmrTransactionFormChoiceChips(
            options =
                listOf(
                    MmrTransactionFormChipOption(AssetStatus.ACTIVE, AssetStatus.ACTIVE.displayLabel()),
                    MmrTransactionFormChipOption(AssetStatus.SOLD, AssetStatus.SOLD.displayLabel()),
                ),
            selected = uiState.status,
            onSelected = { onEvent(AddEditAssetUiEvent.OnStatusSelected(it)) },
        )

        AnimatedVisibility(
            visible = uiState.status == AssetStatus.SOLD,
            enter = expandVertically(),
            exit = shrinkVertically(),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                MmrTransactionFormPickerRowLabeled(
                    label = "Sale date",
                    value = uiState.saleDateMillis?.let { dateFormat.format(Date(it)) } ?: "",
                    emptyHint = "Select date",
                    icon = MmrTransactionFormIcons.date,
                    onClick = { showSaleDatePicker = true },
                )
                MmrTransactionFormSectionLabel(text = "Sale price")
                MmrTransactionFormTextRow(
                    value = uiState.salePrice,
                    onValueChange = { onEvent(AddEditAssetUiEvent.OnSalePriceChanged(it)) },
                    placeholder = "0 if scrapped or gifted",
                    keyboardType = KeyboardType.Decimal,
                    groupedAmountDisplay = true,
                )
            }
        }

        MmrTransactionFormPickerRowLabeled(
            label = "Inherited asset",
            value = if (uiState.isInherited) "Yes" else "No",
            emptyHint = "No",
            icon = MmrTransactionFormIcons.note,
            onClick = { onEvent(AddEditAssetUiEvent.OnInheritedChanged(!uiState.isInherited)) },
        )

        val purchaseLabel = if (uiState.isInherited) "Purchase date (optional)" else "Purchase date"
        MmrTransactionFormPickerRowLabeled(
            label = purchaseLabel,
            value = uiState.purchaseDateMillis?.let { dateFormat.format(Date(it)) } ?: "",
            emptyHint = if (uiState.isInherited) "Not set" else "Select date",
            icon = MmrTransactionFormIcons.date,
            onClick = { showPurchaseDatePicker = true },
        )

        if (!uiState.isInherited) {
            MmrTransactionFormPickerRowLabeled(
                label = "How you paid",
                value = uiState.purchasePaymentMode.displayLabel(),
                emptyHint = "Select",
                icon = MmrTransactionFormIcons.amount,
                onClick = { showPaymentModeSheet = true },
            )
        }

        val purchasePriceLabel =
            when {
                uiState.isInherited ->
                    if (uiState.assetId == null) "Current / Purchase Value" else "Purchase / Cost Basis"
                else -> "Total purchase price"
            }
        MmrTransactionFormSectionLabel(text = purchasePriceLabel)
        MmrTransactionFormTextRow(
            value = uiState.purchasePrice,
            onValueChange = { onEvent(AddEditAssetUiEvent.OnPurchasePriceChanged(it)) },
            placeholder = "Amount in ₹",
            keyboardType = KeyboardType.Decimal,
            groupedAmountDisplay = true,
        )

        if (!uiState.isInherited && uiState.purchasePaymentMode == AssetPurchasePaymentMode.DOWN_PAYMENT_AND_EMI) {
            MmrTransactionFormSectionLabel(text = "Down payment")
            MmrTransactionFormTextRow(
                value = uiState.purchaseDownPayment,
                onValueChange = { onEvent(AddEditAssetUiEvent.OnPurchaseDownPaymentChanged(it)) },
                placeholder = "Paid upfront",
                keyboardType = KeyboardType.Decimal,
                groupedAmountDisplay = true,
            )
            MmrTransactionFormSectionLabel(text = "Amount financed (EMI)")
            MmrTransactionFormTextRow(
                value = uiState.purchaseFinancedAmount,
                onValueChange = { onEvent(AddEditAssetUiEvent.OnPurchaseFinancedAmountChanged(it)) },
                placeholder = "Loan principal",
                keyboardType = KeyboardType.Decimal,
                groupedAmountDisplay = true,
            )
            if (!uiState.canSave) {
                addEditAssetSaveBlocker(uiState)?.let { hint ->
                    Text(
                        text = hint,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }

        if (!uiState.isInherited && uiState.purchasePaymentMode.showsOptionalLinkedLoanField()) {
            AssetLinkedLoanPicker(
                linkedLoans = uiState.linkedLoans,
                linkedLoanId = uiState.linkedLoanId,
                onSelected = { onEvent(AddEditAssetUiEvent.OnLinkedLoanSelected(it)) },
            )
        }

        MmrTransactionFormSectionLabel(text = "Owned by")
        MmrTransactionFormTextRow(
            value = uiState.ownedByName,
            onValueChange = { onEvent(AddEditAssetUiEvent.OnOwnedByChanged(it)) },
            placeholder = "Optional",
        )
        MmrTransactionFormSectionLabel(text = "Location")
        MmrTransactionFormTextRow(
            value = uiState.location,
            onValueChange = { onEvent(AddEditAssetUiEvent.OnLocationChanged(it)) },
            placeholder = "City, locker, safe…",
        )
        MmrTransactionFormSectionLabel(text = "Identifier")
        MmrTransactionFormTextRow(
            value = uiState.identifier,
            onValueChange = { onEvent(AddEditAssetUiEvent.OnIdentifierChanged(it)) },
            placeholder = "Reg no, policy no…",
        )

        if (uiState.isInherited) {
            AssetLinkedLoanPicker(
                linkedLoans = uiState.linkedLoans,
                linkedLoanId = uiState.linkedLoanId,
                onSelected = { onEvent(AddEditAssetUiEvent.OnLinkedLoanSelected(it)) },
            )
        }

        MmrTransactionFormPickerRowLabeled(
            label = "Next valuation due",
            value = uiState.nextValuationDueMillis?.let { dateFormat.format(Date(it)) } ?: "",
            emptyHint = "Not set",
            icon = MmrTransactionFormIcons.date,
            onClick = { showNextDuePicker = true },
        )

        if (uiState.nextValuationDueMillis != null) {
            MmrTransactionFormSectionLabel(text = "Reminder before due date")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MmrTransactionFormFilterChip(
                    label = "None",
                    selected = uiState.valuationReminderDaysBefore == 0,
                    onClick = { onEvent(AddEditAssetUiEvent.OnReminderDaysSelected(0)) },
                )
                MmrReminderPresets.loanEmiReminderDaysOptions.forEach { (days, label) ->
                    MmrTransactionFormFilterChip(
                        label = label,
                        selected = uiState.valuationReminderDaysBefore == days,
                        onClick = { onEvent(AddEditAssetUiEvent.OnReminderDaysSelected(days)) },
                    )
                }
            }
        }

        MmrTransactionFormSectionLabel(text = "Notes")
        MmrTransactionFormTextRow(
            value = uiState.notes,
            onValueChange = { onEvent(AddEditAssetUiEvent.OnNotesChanged(it)) },
            placeholder = "Optional",
            singleLine = false,
            minLines = 2,
        )
    }

    if (showPurchaseDatePicker) {
        StatsDatePickerDialog(
            initialMillis = uiState.purchaseDateMillis,
            onDismiss = { showPurchaseDatePicker = false },
            onConfirm = {
                onEvent(AddEditAssetUiEvent.OnPurchaseDateChanged(it))
                showPurchaseDatePicker = false
            },
        )
    }
    if (showNextDuePicker) {
        StatsDatePickerDialog(
            initialMillis = uiState.nextValuationDueMillis,
            onDismiss = { showNextDuePicker = false },
            onConfirm = {
                onEvent(AddEditAssetUiEvent.OnNextValuationDueChanged(it))
                showNextDuePicker = false
            },
        )
    }
    if (showSaleDatePicker) {
        StatsDatePickerDialog(
            initialMillis = uiState.saleDateMillis,
            onDismiss = { showSaleDatePicker = false },
            onConfirm = {
                onEvent(AddEditAssetUiEvent.OnSaleDateChanged(it))
                showSaleDatePicker = false
            },
        )
    }
    if (showPaymentModeSheet && !uiState.isInherited) {
        val modes = AssetPurchasePaymentMode.entries
        MmrIosSettingsSingleChoiceSheet(
            title = "How you paid",
            onDismiss = { showPaymentModeSheet = false },
            sheetState = paymentModeSheetState,
        ) {
            modes.forEachIndexed { index, mode ->
                MmrIosSettingsChoiceRow(
                    label = mode.displayLabel(),
                    selected = uiState.purchasePaymentMode == mode,
                    onClick = {
                        onEvent(AddEditAssetUiEvent.OnPurchasePaymentModeSelected(mode))
                        showPaymentModeSheet = false
                    },
                    showDividerBelow = index < modes.lastIndex,
                )
            }
        }
    }
}

@Composable
private fun AssetLinkedLoanPicker(
    linkedLoans: List<LoanPickerUi>,
    linkedLoanId: Long?,
    onSelected: (Long?) -> Unit,
) {
    MmrTransactionFormSectionLabel(text = "Linked loan")
    val emptyHint =
        if (linkedLoans.isEmpty()) "No active borrowed loans" else "None"
    val selectedName = linkedLoans.firstOrNull { it.id == linkedLoanId }?.name ?: ""
    MmrTransactionFormPickerRow(
        icon = null,
        placeholder = "",
        value = selectedName,
        emptyHint = emptyHint,
        enabled = linkedLoans.isNotEmpty(),
        onClick =
            if (linkedLoans.isEmpty()) {
                null
            } else {
                {
                    val currentIndex = linkedLoans.indexOfFirst { it.id == linkedLoanId }
                    val next =
                        if (currentIndex < 0) {
                            linkedLoans.first().id
                        } else if (currentIndex >= linkedLoans.lastIndex) {
                            null
                        } else {
                            linkedLoans[currentIndex + 1].id
                        }
                    onSelected(next)
                }
            },
    )
}
