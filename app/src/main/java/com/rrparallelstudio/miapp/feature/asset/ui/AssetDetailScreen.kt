package com.rrparallelstudio.miapp.feature.asset.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.rrparallelstudio.miapp.core.model.enums.AssetStatus
import com.rrparallelstudio.miapp.feature.asset.model.AssetDetailUiEvent
import com.rrparallelstudio.miapp.feature.asset.viewmodel.AssetDetailViewModel
import com.rrparallelstudio.miapp.feature.stats.ui.StatsDatePickerDialog
import com.rrparallelstudio.miapp.feature.transaction.ui.TransactionReceiptSection
import com.rrparallelstudio.miapp.feature.transaction.ui.TransactionReceiptViewerDialog
import com.rrparallelstudio.miapp.ui.components.MmrConfirmDialog
import com.rrparallelstudio.miapp.ui.components.MmrContentDialog
import com.rrparallelstudio.miapp.ui.components.MmrDeleteIconButton
import com.rrparallelstudio.miapp.ui.components.MmrDetailHeroBlock
import com.rrparallelstudio.miapp.ui.components.MmrDetailKeyValueRow
import com.rrparallelstudio.miapp.ui.components.MmrDetailListItemCard
import com.rrparallelstudio.miapp.ui.components.MmrDetailOverviewCard
import com.rrparallelstudio.miapp.ui.components.MmrDetailScreenScaffold
import com.rrparallelstudio.miapp.ui.components.MmrDetailSectionHint
import com.rrparallelstudio.miapp.ui.components.MmrDetailSectionTitle
import com.rrparallelstudio.miapp.ui.components.MmrDialogDetailLine
import com.rrparallelstudio.miapp.ui.components.MmrDimens
import com.rrparallelstudio.miapp.ui.components.MmrPrimaryActionButton
import com.rrparallelstudio.miapp.ui.components.MmrTransactionFormSectionLabel
import com.rrparallelstudio.miapp.ui.components.MmrTransactionFormTextRow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssetDetailScreen(
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (Long) -> Unit,
    viewModel: AssetDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var viewingPhotoPath by remember { mutableStateOf<String?>(null) }
    val valuationDateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    viewingPhotoPath?.let { path ->
        TransactionReceiptViewerDialog(filePath = path, onDismiss = { viewingPhotoPath = null })
    }

    LaunchedEffect(uiState.isAssetDeleted) {
        if (uiState.isAssetDeleted) onNavigateBack()
    }

    LaunchedEffect(uiState.isValuationSuccess) {
        if (uiState.isValuationSuccess) {
            viewModel.onEvent(AssetDetailUiEvent.OnClearValuationSuccess)
        }
    }

    val showFooter = uiState.status == AssetStatus.ACTIVE && !uiState.isLoading
    val changeMeta =
        buildList {
            uiState.changeSinceLastLabel?.let { add("Since last: $it") }
            uiState.changeSincePurchaseLabel?.let { add("Since purchase: $it") }
        }.joinToString(" · ").takeIf { it.isNotEmpty() }

    MmrDetailScreenScaffold(
        title = uiState.name.ifBlank { "Asset details" },
        onNavigateBack = onNavigateBack,
        useCompactHeaderTitle = true,
        isLoading = uiState.isLoading,
        errorMessage = uiState.errorMessage,
        onRetry = { viewModel.onEvent(AssetDetailUiEvent.OnRetryClick) },
        footerPrimaryLabel = if (showFooter) "Update value" else null,
        onFooterPrimary =
            if (showFooter) {
                { viewModel.onEvent(AssetDetailUiEvent.OnOpenValuationDialog) }
            } else {
                null
            },
        footerSecondaryLabel = "Edit",
        onFooterSecondary = { onNavigateToEdit(uiState.assetId) },
        topBarActions = {
            MmrDeleteIconButton(
                onClick = { showDeleteDialog = true },
                contentDescription = "Delete asset",
            )
        },
    ) {
        item {
            MmrDetailHeroBlock(
                title = uiState.name,
                amountFormatted = uiState.currentValueFormatted,
                subtitle = changeMeta,
            )
        }
        item {
            MmrDetailOverviewCard {
                MmrDetailKeyValueRow("Type", uiState.typeLabel)
                MmrDetailKeyValueRow("Status", uiState.statusLabel)
                if (uiState.isInherited) {
                    MmrDetailKeyValueRow("Inherited", "Yes")
                }
                uiState.purchaseDateFormatted?.let { MmrDetailKeyValueRow("Purchase date", it) }
                uiState.purchasePriceFormatted?.let { label ->
                    val rowLabel = if (uiState.isInherited) "Purchase price" else "Total purchase price"
                    MmrDetailKeyValueRow(rowLabel, label)
                }
                uiState.purchasePaymentLabel?.let { MmrDetailKeyValueRow("How you paid", it) }
                uiState.purchaseDownPaymentFormatted?.let { MmrDetailKeyValueRow("Down payment", it) }
                uiState.purchaseFinancedAmountFormatted?.let { MmrDetailKeyValueRow("Amount financed", it) }
                uiState.ownedByName?.let { MmrDetailKeyValueRow("Owned by", it) }
                uiState.location?.let { MmrDetailKeyValueRow("Location", it) }
                uiState.identifier?.let { MmrDetailKeyValueRow("Identifier", it) }
                uiState.linkedLoanName?.let { name ->
                    val liability =
                        uiState.linkedLoanOutstandingFormatted?.let { "Outstanding $it" }
                            ?: ""
                    MmrDetailKeyValueRow("Linked loan", "$name $liability".trim())
                }
                uiState.nextValuationDueFormatted?.let { MmrDetailKeyValueRow("Next valuation due", it) }
                uiState.reminderLabel?.let { MmrDetailKeyValueRow("Reminder", it) }
                uiState.saleDateFormatted?.let { MmrDetailKeyValueRow("Sale date", it) }
                uiState.salePriceFormatted?.let { MmrDetailKeyValueRow("Sale price", it) }
            }
        }
        if (uiState.photos.isNotEmpty()) {
            item {
                TransactionReceiptSection(
                    receipts = uiState.photos,
                    onAddClick = {},
                    onRemoveSaved = {},
                    onRemovePending = {},
                    readOnly = true,
                    sectionLabel = "Photos",
                    onReceiptClick = { viewingPhotoPath = it },
                )
            }
        }
        uiState.notes?.let { note ->
            item {
                MmrDetailSectionTitle("Notes")
            }
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Text(note, modifier = Modifier.padding(MmrDimens.cardPadding))
                }
            }
        }
        item {
            MmrDetailSectionTitle("Valuation history")
        }
        if (uiState.valuations.isEmpty()) {
            item {
                MmrDetailSectionHint("No valuations yet. Tap Update value to add one.")
            }
        } else {
            items(uiState.valuations, key = { it.id }) { entry ->
                MmrDetailListItemCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column {
                            Text(entry.amountFormatted, fontWeight = FontWeight.SemiBold)
                            Text(entry.dateFormatted, style = MaterialTheme.typography.bodySmall)
                            entry.notes?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                        }
                        entry.changePercentLabel?.let {
                            Text(it, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }
    }

    if (uiState.showValuationDialog) {
        MmrContentDialog(
            title = "Update value",
            onDismiss = { viewModel.onEvent(AssetDetailUiEvent.OnDismissValuationDialog) },
            confirmLabel = "Save",
            onConfirm = { viewModel.onEvent(AssetDetailUiEvent.OnSubmitValuation) },
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                MmrDialogDetailLine("Change vs last", uiState.valuationPreviewChangeLabel ?: "—")
                MmrTransactionFormSectionLabel(text = "Value")
                MmrTransactionFormTextRow(
                    value = uiState.valuationAmountInput,
                    onValueChange = { viewModel.onEvent(AssetDetailUiEvent.OnValuationAmountChanged(it)) },
                    placeholder = "Amount",
                    keyboardType = KeyboardType.Decimal,
                )
                MmrDialogDetailLine(
                    "Date",
                    valuationDateFormat.format(Date(uiState.valuationDateMillis)),
                )
                Text(
                    "Change date",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(top = 4.dp),
                    fontWeight = FontWeight.Medium,
                )
                MmrPrimaryActionButton(
                    text = "Pick date",
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth(),
                )
                MmrTransactionFormSectionLabel(text = "Note")
                MmrTransactionFormTextRow(
                    value = uiState.valuationNoteInput,
                    onValueChange = { viewModel.onEvent(AssetDetailUiEvent.OnValuationNoteChanged(it)) },
                    placeholder = "Optional note",
                    singleLine = false,
                )
            }
        }
    }

    if (showDatePicker) {
        StatsDatePickerDialog(
            initialMillis = uiState.valuationDateMillis,
            onDismiss = { showDatePicker = false },
            onConfirm = {
                viewModel.onEvent(AssetDetailUiEvent.OnValuationDateChanged(it))
                showDatePicker = false
            },
        )
    }

    if (showDeleteDialog) {
        MmrConfirmDialog(
            title = "Delete asset?",
            message = "This removes the asset, valuation history, and photos.",
            confirmLabel = "Delete",
            onConfirm = {
                showDeleteDialog = false
                viewModel.onEvent(AssetDetailUiEvent.OnDeleteAsset)
            },
            onDismiss = { showDeleteDialog = false },
            confirmIsDestructive = true,
        )
    }
}
