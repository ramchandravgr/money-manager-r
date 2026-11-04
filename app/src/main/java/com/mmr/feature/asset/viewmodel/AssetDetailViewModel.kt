package com.mmr.feature.asset.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mmr.app.ui.state.MmrUiMessages
import com.mmr.core.asset.AssetValuationFormat
import com.mmr.core.common.display.MmrMoneyFormat
import com.mmr.core.common.validation.MmrValidation
import com.mmr.core.domain.repository.AssetAttachmentRepository
import com.mmr.core.domain.repository.AssetRepository
import com.mmr.core.domain.repository.LoanRepository
import com.mmr.core.reminder.MmrReminderPresets
import com.mmr.feature.asset.model.AssetDetailUiEvent
import com.mmr.feature.asset.model.AssetDetailUiState
import com.mmr.feature.asset.model.AssetValuationItemUi
import com.mmr.feature.asset.model.assetTypeDisplayLabel
import com.mmr.feature.asset.model.displayLabel
import com.mmr.feature.transaction.model.TransactionReceiptUi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class AssetDetailViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val assetRepository: AssetRepository,
        private val loanRepository: LoanRepository,
        private val attachmentRepository: AssetAttachmentRepository,
    ) : ViewModel() {
        private val assetId: Long = savedStateHandle.get<Long>("assetId") ?: 0L
        private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

        private val dialogStateFlow = MutableStateFlow(DialogState())
        private val detailStatusFlow = MutableStateFlow(DetailStatus())
        private val linkedLoanLabelFlow = MutableStateFlow<LinkedLoanUi?>(null)
        private val photosFlow = MutableStateFlow<List<TransactionReceiptUi>>(emptyList())

        init {
            viewModelScope.launch { refreshPhotos() }
            viewModelScope.launch {
                assetRepository.getAssetWithValuationsById(assetId).collect { relation ->
                    val loanId = relation?.asset?.linkedLoanId
                    linkedLoanLabelFlow.value =
                        if (loanId != null) {
                            val loan = loanRepository.getLoanById(loanId)
                            if (loan != null) {
                                LinkedLoanUi(loan.name, MmrMoneyFormat.formatInr(loan.outstandingAmount))
                            } else {
                                null
                            }
                        } else {
                            null
                        }
                }
            }
        }

        val uiState: StateFlow<AssetDetailUiState> =
            combine(
                assetRepository.getAssetWithValuationsById(assetId),
                dialogStateFlow,
                detailStatusFlow,
                linkedLoanLabelFlow,
                photosFlow,
            ) { relation, dialog, status, linkedLoan, photos ->
                if (relation == null) {
                    return@combine AssetDetailUiState(
                        isLoading = false,
                        assetId = assetId,
                        errorMessage = status.loadError ?: "Asset not found.",
                    )
                }
                val asset = relation.asset
                val sorted = relation.valuations.sortedByDescending { it.recordedAt }
                val latest = sorted.firstOrNull()
                val previous = sorted.drop(1).firstOrNull()
                val purchaseBasis = asset.purchasePrice ?: sorted.minByOrNull { it.recordedAt }?.amount
                val previewAmount = MmrValidation.parsePositiveAmount(dialog.amountInput)
                val previewChange =
                    previewAmount?.let {
                        AssetValuationFormat.formatPercentChange(
                            AssetValuationFormat.percentChangeFromPrevious(latest?.amount, it),
                        )
                    }
                AssetDetailUiState(
                    isLoading = false,
                    assetId = asset.id,
                    name = asset.name,
                    typeLabel = asset.assetType.assetTypeDisplayLabel().ifBlank { "—" },
                    status = asset.status,
                    statusLabel = asset.status.displayLabel(),
                    currentValueFormatted = MmrMoneyFormat.formatInr(asset.currentValue),
                    changeSinceLastLabel =
                        AssetValuationFormat.formatPercentChange(
                            AssetValuationFormat.percentChangeFromPrevious(
                                previous?.amount,
                                latest?.amount ?: asset.currentValue,
                            ),
                        ),
                    changeSincePurchaseLabel =
                        AssetValuationFormat.formatPercentChange(
                            AssetValuationFormat.percentChangeFromPrevious(purchaseBasis, asset.currentValue),
                        ),
                    purchaseDateFormatted = asset.purchaseDateMillis?.let { dateFormat.format(Date(it)) },
                    purchasePriceFormatted = asset.purchasePrice?.let { MmrMoneyFormat.formatInr(it) },
                    isInherited = asset.isInherited,
                    ownedByName = asset.ownedByName,
                    location = asset.location,
                    identifier = asset.identifier,
                    linkedLoanName = linkedLoan?.name,
                    linkedLoanOutstandingFormatted = linkedLoan?.outstandingFormatted,
                    nextValuationDueFormatted = asset.nextValuationDueMillis?.let { dateFormat.format(Date(it)) },
                    reminderLabel =
                        if (asset.valuationReminderDaysBefore > 0) {
                            MmrReminderPresets.loanEmiReminderLabel(asset.valuationReminderDaysBefore)
                        } else {
                            null
                        },
                    saleDateFormatted = asset.saleDateMillis?.let { dateFormat.format(Date(it)) },
                    salePriceFormatted = asset.salePrice?.let { MmrMoneyFormat.formatInr(it) },
                    notes = asset.notes,
                    photos = photos,
                    valuations =
                        sorted.map { v ->
                            val prior =
                                sorted
                                    .filter {
                                        it.recordedAt < v.recordedAt ||
                                            (it.recordedAt == v.recordedAt && it.id < v.id)
                                    }.maxByOrNull { it.recordedAt }
                            AssetValuationItemUi(
                                id = v.id,
                                amountFormatted = MmrMoneyFormat.formatInr(v.amount),
                                dateFormatted = dateFormat.format(Date(v.recordedAt)),
                                changePercentLabel =
                                    AssetValuationFormat.formatPercentChange(
                                        AssetValuationFormat.percentChangeFromPrevious(prior?.amount, v.amount),
                                    ),
                                notes = v.notes,
                            )
                        },
                    errorMessage = status.loadError,
                    isValuationSuccess = status.valuationSuccess,
                    isAssetDeleted = status.deleted,
                    showValuationDialog = dialog.visible,
                    valuationAmountInput = dialog.amountInput,
                    valuationDateMillis = dialog.dateMillis,
                    valuationNoteInput = dialog.noteInput,
                    valuationPreviewChangeLabel = previewChange,
                )
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = AssetDetailUiState(isLoading = true, assetId = assetId),
            )

        fun onEvent(event: AssetDetailUiEvent) {
            when (event) {
                AssetDetailUiEvent.OnRetryClick -> detailStatusFlow.update { it.copy(loadError = null) }
                AssetDetailUiEvent.OnOpenValuationDialog ->
                    dialogStateFlow.update {
                        DialogState(visible = true, dateMillis = System.currentTimeMillis())
                    }
                AssetDetailUiEvent.OnDismissValuationDialog -> dialogStateFlow.value = DialogState()
                is AssetDetailUiEvent.OnValuationAmountChanged ->
                    dialogStateFlow.update { it.copy(amountInput = event.value) }
                is AssetDetailUiEvent.OnValuationDateChanged ->
                    dialogStateFlow.update { it.copy(dateMillis = event.millis) }
                is AssetDetailUiEvent.OnValuationNoteChanged ->
                    dialogStateFlow.update { it.copy(noteInput = event.value) }
                AssetDetailUiEvent.OnSubmitValuation -> submitValuation()
                AssetDetailUiEvent.OnClearValuationSuccess ->
                    detailStatusFlow.update { it.copy(valuationSuccess = false) }
                AssetDetailUiEvent.OnDeleteAsset -> deleteAsset()
            }
        }

        private fun submitValuation() {
            val dialog = dialogStateFlow.value
            val amount = MmrValidation.parsePositiveAmount(dialog.amountInput) ?: return
            viewModelScope.launch {
                try {
                    assetRepository.recordValuation(
                        assetId = assetId,
                        amount = amount,
                        recordedAt = dialog.dateMillis,
                        notes = dialog.noteInput.trim().takeIf { it.isNotEmpty() },
                    )
                    dialogStateFlow.value = DialogState()
                    detailStatusFlow.update { it.copy(valuationSuccess = true) }
                    refreshPhotos()
                } catch (e: Exception) {
                    detailStatusFlow.update {
                        it.copy(loadError = MmrUiMessages.friendly(e.message, MmrUiMessages.GENERIC_ACTION))
                    }
                }
            }
        }

        private fun deleteAsset() {
            viewModelScope.launch {
                try {
                    val asset = assetRepository.getAssetById(assetId) ?: return@launch
                    assetRepository.deleteAsset(asset)
                    detailStatusFlow.update { it.copy(deleted = true) }
                } catch (e: Exception) {
                    detailStatusFlow.update {
                        it.copy(loadError = MmrUiMessages.friendly(e.message, MmrUiMessages.GENERIC_ACTION))
                    }
                }
            }
        }

        private data class DialogState(
            val visible: Boolean = false,
            val amountInput: String = "",
            val dateMillis: Long = System.currentTimeMillis(),
            val noteInput: String = "",
        )

        private data class DetailStatus(
            val loadError: String? = null,
            val valuationSuccess: Boolean = false,
            val deleted: Boolean = false,
        )

        private data class LinkedLoanUi(
            val name: String,
            val outstandingFormatted: String,
        )

        private suspend fun refreshPhotos() {
            photosFlow.value =
                attachmentRepository.listForAsset(assetId).mapNotNull { entity ->
                    val path = attachmentRepository.localFile(entity)?.absolutePath ?: return@mapNotNull null
                    TransactionReceiptUi(id = entity.id, filePath = path)
                }
        }
    }
