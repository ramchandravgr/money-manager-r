package com.rrparallelstudio.miapp.feature.asset.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rrparallelstudio.miapp.core.asset.purchaseSplitMatchesTotal
import com.rrparallelstudio.miapp.core.asset.resolvedPurchasePaymentMode
import com.rrparallelstudio.miapp.core.common.display.MmrMoneyFormat
import com.rrparallelstudio.miapp.core.common.image.MmrReceiptImage
import com.rrparallelstudio.miapp.core.common.validation.MmrValidation
import com.rrparallelstudio.miapp.core.data.repository.AssetAttachmentRepositoryImpl
import com.rrparallelstudio.miapp.core.database.entity.AssetEntity
import com.rrparallelstudio.miapp.core.domain.repository.AssetAttachmentRepository
import com.rrparallelstudio.miapp.core.domain.repository.AssetRepository
import com.rrparallelstudio.miapp.core.domain.repository.LoanRepository
import com.rrparallelstudio.miapp.core.model.enums.AssetPurchasePaymentMode
import com.rrparallelstudio.miapp.core.model.enums.AssetStatus
import com.rrparallelstudio.miapp.core.model.enums.LoanStatus
import com.rrparallelstudio.miapp.core.model.enums.LoanType
import com.rrparallelstudio.miapp.core.reminder.MmrReminderPresets
import com.rrparallelstudio.miapp.feature.asset.model.AddEditAssetUiEvent
import com.rrparallelstudio.miapp.feature.asset.model.AddEditAssetUiState
import com.rrparallelstudio.miapp.feature.asset.model.assetTypeForEdit
import com.rrparallelstudio.miapp.feature.transaction.model.LoanPickerUi
import com.rrparallelstudio.miapp.feature.transaction.model.TransactionReceiptUi
import com.rrparallelstudio.miapp.ui.state.MmrUiMessages
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class AddEditAssetViewModel
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val assetRepository: AssetRepository,
        private val loanRepository: LoanRepository,
        private val attachmentRepository: AssetAttachmentRepository,
        savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        private val assetId: Long? = savedStateHandle.get<Long>("assetId")?.takeIf { it > 0 }

        private val _uiState = MutableStateFlow(AddEditAssetUiState(assetId = assetId))
        val uiState: StateFlow<AddEditAssetUiState> = _uiState.asStateFlow()

        private var hydratedId: Long? = null
        private val pendingDeletedAttachmentIds = mutableSetOf<Long>()

        init {
            viewModelScope.launch {
                loanRepository.getAllLoans().catch { emit(emptyList()) }.collectLatest { loans ->
                    val borrowedActive =
                        loans.filter { it.loanType == LoanType.BORROWED && it.status == LoanStatus.ACTIVE }
                    val picker = borrowedActive.map { LoanPickerUi(id = it.id, name = it.name) }
                    _uiState.update { it.copy(linkedLoans = picker) }
                }
            }
            if (assetId != null) {
                viewModelScope.launch {
                    val asset = assetRepository.getAssetById(assetId) ?: return@launch
                    if (hydratedId == assetId) return@launch
                    hydratedId = assetId
                    val photos = loadPhotos(assetId)
                    val paymentMode = asset.resolvedPurchasePaymentMode()
                    val priceText =
                        asset.purchasePrice?.let { MmrMoneyFormat.formatNumber(it) }
                            ?: MmrMoneyFormat.formatNumber(asset.currentValue)
                    val downText = asset.purchaseDownPayment?.let { MmrMoneyFormat.formatNumber(it) } ?: ""
                    val financedText =
                        asset.purchaseFinancedAmount?.let { MmrMoneyFormat.formatNumber(it) }
                            ?: if (
                                paymentMode == AssetPurchasePaymentMode.FULLY_FINANCED &&
                                asset.purchaseFinancedAmount == null &&
                                asset.purchaseDownPayment == null
                            ) {
                                asset.purchasePrice?.let { MmrMoneyFormat.formatNumber(it) } ?: ""
                            } else {
                                ""
                            }
                    _uiState.update { current ->
                        val hydrated =
                            current.copy(
                                name = asset.name,
                                assetType = asset.assetType.assetTypeForEdit(),
                                status = asset.status,
                                isInherited = asset.isInherited,
                                purchaseDateMillis = asset.purchaseDateMillis,
                                purchasePaymentMode = if (asset.isInherited) AssetPurchasePaymentMode.PAID_IN_FULL else paymentMode,
                                purchasePrice = priceText,
                                purchaseDownPayment = downText,
                                purchaseFinancedAmount = financedText,
                                ownedByName = asset.ownedByName ?: "",
                                location = asset.location ?: "",
                                identifier = asset.identifier ?: "",
                                linkedLoanId = asset.linkedLoanId,
                                nextValuationDueMillis = asset.nextValuationDueMillis,
                                valuationReminderDaysBefore = asset.valuationReminderDaysBefore,
                                saleDateMillis = asset.saleDateMillis,
                                salePrice = asset.salePrice?.let { amt -> MmrMoneyFormat.formatNumber(amt) } ?: "",
                                notes = asset.notes ?: "",
                                photos = photos,
                            )
                        hydrated.copy(canSave = validate(hydrated))
                    }
                }
            }
        }

        override fun onCleared() {
            discardStagedPhotos()
            super.onCleared()
        }

        fun onEvent(event: AddEditAssetUiEvent) {
            when (event) {
                is AddEditAssetUiEvent.OnNameChanged ->
                    _uiState.update { it.copy(name = event.value, canSave = validate(it.copy(name = event.value))) }

                is AddEditAssetUiEvent.OnAssetTypeChanged ->
                    _uiState.update { it.copy(assetType = event.value, canSave = validate(it)) }

                is AddEditAssetUiEvent.OnStatusSelected ->
                    _uiState.update {
                        val next =
                            it.copy(
                                status = event.status,
                                saleDateMillis =
                                    if (event.status == AssetStatus.SOLD) {
                                        it.saleDateMillis
                                    } else {
                                        null
                                    },
                                salePrice =
                                    if (event.status == AssetStatus.SOLD) {
                                        it.salePrice
                                    } else {
                                        ""
                                    },
                            )
                        next.copy(canSave = validate(next))
                    }

                is AddEditAssetUiEvent.OnInheritedChanged ->
                    _uiState.update {
                        val next =
                            if (event.value) {
                                it.copy(
                                    isInherited = true,
                                    purchasePaymentMode = AssetPurchasePaymentMode.PAID_IN_FULL,
                                    purchaseDownPayment = "",
                                    purchaseFinancedAmount = "",
                                    linkedLoanId = null,
                                )
                            } else {
                                it.copy(
                                    isInherited = false,
                                    purchasePaymentMode = AssetPurchasePaymentMode.PAID_IN_FULL,
                                    purchaseDownPayment = "",
                                    purchaseFinancedAmount = "",
                                    linkedLoanId = null,
                                )
                            }
                        next.copy(canSave = validate(next))
                    }

                is AddEditAssetUiEvent.OnPurchaseDateChanged ->
                    _uiState.update { it.copy(purchaseDateMillis = event.millis, canSave = validate(it)) }

                is AddEditAssetUiEvent.OnPurchasePriceChanged ->
                    _uiState.update {
                        val next = it.copy(purchasePrice = event.value)
                        next.copy(canSave = validate(next))
                    }

                is AddEditAssetUiEvent.OnPurchasePaymentModeSelected ->
                    _uiState.update {
                        val next =
                            when (event.mode) {
                                AssetPurchasePaymentMode.PAID_IN_FULL ->
                                    it.copy(
                                        purchasePaymentMode = event.mode,
                                        purchaseDownPayment = "",
                                        purchaseFinancedAmount = "",
                                        linkedLoanId = null,
                                    )
                                AssetPurchasePaymentMode.FULLY_FINANCED ->
                                    it.copy(
                                        purchasePaymentMode = event.mode,
                                        purchaseDownPayment = "",
                                        purchaseFinancedAmount = "",
                                    )
                                AssetPurchasePaymentMode.DOWN_PAYMENT_AND_EMI ->
                                    it.copy(
                                        purchasePaymentMode = event.mode,
                                        purchaseDownPayment = "",
                                        purchaseFinancedAmount = "",
                                    )
                            }
                        next.copy(canSave = validate(next))
                    }

                is AddEditAssetUiEvent.OnPurchaseDownPaymentChanged ->
                    _uiState.update {
                        val next = it.copy(purchaseDownPayment = event.value)
                        next.copy(canSave = validate(next))
                    }

                is AddEditAssetUiEvent.OnPurchaseFinancedAmountChanged ->
                    _uiState.update {
                        val next = it.copy(purchaseFinancedAmount = event.value)
                        next.copy(canSave = validate(next))
                    }

                is AddEditAssetUiEvent.OnOwnedByChanged -> _uiState.update { it.copy(ownedByName = event.value) }

                is AddEditAssetUiEvent.OnLocationChanged -> _uiState.update { it.copy(location = event.value) }

                is AddEditAssetUiEvent.OnIdentifierChanged -> _uiState.update { it.copy(identifier = event.value) }

                is AddEditAssetUiEvent.OnLinkedLoanSelected ->
                    _uiState.update {
                        val next = it.copy(linkedLoanId = event.loanId)
                        next.copy(canSave = validate(next))
                    }

                is AddEditAssetUiEvent.OnNextValuationDueChanged ->
                    _uiState.update { it.copy(nextValuationDueMillis = event.millis) }

                is AddEditAssetUiEvent.OnReminderDaysSelected ->
                    _uiState.update { it.copy(valuationReminderDaysBefore = event.days) }

                is AddEditAssetUiEvent.OnSaleDateChanged ->
                    _uiState.update { it.copy(saleDateMillis = event.millis, canSave = validate(it)) }

                is AddEditAssetUiEvent.OnSalePriceChanged ->
                    _uiState.update { it.copy(salePrice = event.value, canSave = validate(it)) }

                is AddEditAssetUiEvent.OnNotesChanged -> _uiState.update { it.copy(notes = event.value) }

                AddEditAssetUiEvent.OnSaveClick -> save()

                is AddEditAssetUiEvent.OnPhotoPicked -> {
                    viewModelScope.launch { handlePhotoPicked(event.uriString) }
                }

                is AddEditAssetUiEvent.OnRemovePhoto -> markSavedPhotoForRemoval(event.attachmentId)

                is AddEditAssetUiEvent.OnRemovePendingPhoto -> removePendingPhoto(event.filePath)
            }
        }

        private suspend fun loadPhotos(forAssetId: Long): List<TransactionReceiptUi> =
            attachmentRepository.listForAsset(forAssetId).mapNotNull { entity ->
                val path = attachmentRepository.localFile(entity)?.absolutePath ?: return@mapNotNull null
                TransactionReceiptUi(id = entity.id, filePath = path)
            }

        private suspend fun handlePhotoPicked(uriString: String) {
            val uri = Uri.parse(uriString)
            if (_uiState.value.photos.size >= AssetAttachmentRepositoryImpl.MAX_ATTACHMENTS_PER_ASSET) {
                _uiState.update {
                    it.copy(
                        errorMessage =
                            "At most ${AssetAttachmentRepositoryImpl.MAX_ATTACHMENTS_PER_ASSET} " +
                                "photos per asset.",
                    )
                }
                return
            }
            try {
                val staged = stagePendingPhotoFile(uri)
                _uiState.update {
                    it.copy(
                        photos = it.photos + TransactionReceiptUi(id = 0L, filePath = staged.absolutePath),
                        errorMessage = null,
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = MmrUiMessages.friendly(e.message, "Could not add photo."))
                }
            }
        }

        private fun markSavedPhotoForRemoval(attachmentId: Long) {
            pendingDeletedAttachmentIds.add(attachmentId)
            _uiState.update { state ->
                state.copy(photos = state.photos.filterNot { it.id == attachmentId })
            }
        }

        private fun removePendingPhoto(filePath: String) {
            runCatching { File(filePath).delete() }
            _uiState.update { state ->
                state.copy(photos = state.photos.filterNot { it.id == 0L && it.filePath == filePath })
            }
        }

        private fun discardStagedPhotos() {
            _uiState.value.photos.filter { it.id == 0L }.forEach { photo ->
                runCatching { File(photo.filePath).delete() }
            }
            pendingDeletedAttachmentIds.clear()
        }

        private suspend fun applyPendingPhotoDeletions() {
            val ids = pendingDeletedAttachmentIds.toList()
            for (id in ids) {
                attachmentRepository.delete(id)
            }
            pendingDeletedAttachmentIds.clear()
        }

        private suspend fun persistPendingPhotos(forAssetId: Long) {
            for (photo in _uiState.value.photos.filter { it.id == 0L }) {
                val bytes = File(photo.filePath).readBytes()
                val mimeType =
                    context.contentResolver.getType(Uri.fromFile(File(photo.filePath))) ?: "image/jpeg"
                attachmentRepository.addFromBytes(forAssetId, mimeType, bytes)
                runCatching { File(photo.filePath).delete() }
            }
        }

        private fun stagePendingPhotoFile(uri: Uri): File {
            val dir = File(context.cacheDir, "pending_asset_photos").apply { mkdirs() }
            val dest = File(dir, "pending_${UUID.randomUUID()}.jpg")
            val rawBytes =
                context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: throw IllegalArgumentException("Could not read image.")
            val normalized =
                MmrReceiptImage.normalizeUriBytes(
                    rawBytes,
                    AssetAttachmentRepositoryImpl.MAX_ATTACHMENT_BYTES,
                )
            dest.writeBytes(normalized)
            return dest
        }

        private fun validate(state: AddEditAssetUiState): Boolean {
            if (state.name.trim().isEmpty()) return false
            val price = MmrValidation.parsePositiveAmount(state.purchasePrice) ?: return false
            if (!state.isInherited && state.purchaseDateMillis == null) return false
            if (state.status == AssetStatus.SOLD) {
                if (state.saleDateMillis == null) return false
                MmrValidation.parseNonNegativeAmount(state.salePrice) ?: return false
            }
            if (state.nextValuationDueMillis != null &&
                state.valuationReminderDaysBefore > 0 &&
                !MmrReminderPresets.isValidLoanEmiReminderDays(state.valuationReminderDaysBefore)
            ) {
                return false
            }
            if (!state.isInherited && !validatePurchasePayment(state, price)) return false
            return true
        }

        private fun validatePurchasePayment(
            state: AddEditAssetUiState,
            totalPurchasePrice: Double,
        ): Boolean {
            when (state.purchasePaymentMode) {
                AssetPurchasePaymentMode.PAID_IN_FULL,
                AssetPurchasePaymentMode.FULLY_FINANCED,
                -> return true
                AssetPurchasePaymentMode.DOWN_PAYMENT_AND_EMI -> {
                    val down = MmrValidation.parseNonNegativeAmount(state.purchaseDownPayment) ?: return false
                    val financed = MmrValidation.parsePositiveAmount(state.purchaseFinancedAmount) ?: return false
                    return purchaseSplitMatchesTotal(totalPurchasePrice, down, financed)
                }
            }
        }

        private fun purchasePaymentFieldsForSave(
            state: AddEditAssetUiState,
            total: Double,
        ): Triple<String?, Double?, Double?> {
            if (state.isInherited) return Triple(null, null, null)
            return when (state.purchasePaymentMode) {
                AssetPurchasePaymentMode.PAID_IN_FULL -> Triple(state.purchasePaymentMode.name, null, null)
                AssetPurchasePaymentMode.FULLY_FINANCED ->
                    Triple(state.purchasePaymentMode.name, null, total)
                AssetPurchasePaymentMode.DOWN_PAYMENT_AND_EMI -> {
                    val down = MmrValidation.parseNonNegativeAmount(state.purchaseDownPayment)!!
                    val financed = MmrValidation.parsePositiveAmount(state.purchaseFinancedAmount)!!
                    Triple(state.purchasePaymentMode.name, down, financed)
                }
            }
        }

        private fun linkedLoanIdForSave(state: AddEditAssetUiState): Long? =
            when {
                state.isInherited -> state.linkedLoanId
                state.purchasePaymentMode == AssetPurchasePaymentMode.PAID_IN_FULL -> null
                else -> state.linkedLoanId
            }

        private fun save() {
            val state = _uiState.value
            if (!validate(state) || state.isSaving) return
            val amount = MmrValidation.parsePositiveAmount(state.purchasePrice)
            if (amount == null) {
                _uiState.update { it.copy(errorMessage = "Enter a valid purchase amount.") }
                return
            }
            if (!state.isInherited && !validatePurchasePayment(state, amount)) {
                val message =
                    when (state.purchasePaymentMode) {
                        AssetPurchasePaymentMode.DOWN_PAYMENT_AND_EMI ->
                            when {
                                MmrValidation.parseNonNegativeAmount(state.purchaseDownPayment) == null ->
                                    "Enter a valid down payment."
                                MmrValidation.parsePositiveAmount(state.purchaseFinancedAmount) == null ->
                                    "Enter a valid financed (EMI) amount."
                                else -> "Down payment and financed amount must add up to the total purchase price."
                            }
                        else -> "Check how you paid for this asset."
                    }
                _uiState.update { it.copy(errorMessage = message) }
                return
            }
            viewModelScope.launch {
                _uiState.update { it.copy(isSaving = true, errorMessage = null) }
                try {
                    val (modeStored, downStored, financedStored) = purchasePaymentFieldsForSave(state, amount)
                    val entity =
                        AssetEntity(
                            id = assetId ?: 0L,
                            name = state.name.trim(),
                            assetType = state.assetType.trim(),
                            status = state.status,
                            isInherited = state.isInherited,
                            purchaseDateMillis = state.purchaseDateMillis,
                            purchasePrice = amount,
                            purchasePaymentMode = modeStored,
                            purchaseDownPayment = downStored,
                            purchaseFinancedAmount = financedStored,
                            ownedByName = state.ownedByName.trim().takeIf { it.isNotEmpty() },
                            location = state.location.trim().takeIf { it.isNotEmpty() },
                            identifier = state.identifier.trim().takeIf { it.isNotEmpty() },
                            linkedLoanId = linkedLoanIdForSave(state),
                            nextValuationDueMillis = state.nextValuationDueMillis,
                            valuationReminderDaysBefore = state.valuationReminderDaysBefore,
                            saleDateMillis = state.saleDateMillis,
                            salePrice =
                                if (state.status == AssetStatus.SOLD) {
                                    MmrValidation.parseNonNegativeAmount(state.salePrice)
                                } else {
                                    null
                                },
                            notes = state.notes.trim().takeIf { it.isNotEmpty() },
                        )
                    val valuationDate =
                        state.purchaseDateMillis
                            ?: state.saleDateMillis
                            ?: System.currentTimeMillis()
                    val savedAssetId =
                        if (assetId == null) {
                            assetRepository.createAsset(
                                asset = entity,
                                initialValuationAmount = amount,
                                initialValuationDate = valuationDate,
                            )
                        } else {
                            val existing =
                                assetRepository.getAssetById(assetId) ?: throw IllegalStateException("Asset not found.")
                            assetRepository.updateAsset(
                                entity.copy(
                                    id = existing.id,
                                    syncId = existing.syncId,
                                    currentValue = existing.currentValue,
                                    createdAt = existing.createdAt,
                                ),
                            )
                            if (state.status == AssetStatus.SOLD && state.saleDateMillis != null) {
                                val saleAmount = MmrValidation.parseNonNegativeAmount(state.salePrice)
                                if (saleAmount != null) {
                                    assetRepository.recordValuation(
                                        assetId = assetId,
                                        amount = saleAmount,
                                        recordedAt = state.saleDateMillis,
                                        notes = "Sale",
                                    )
                                }
                            }
                            assetId
                        }
                    applyPendingPhotoDeletions()
                    persistPendingPhotos(savedAssetId)
                    _uiState.update { it.copy(isSaving = false, isSaved = true) }
                } catch (e: Exception) {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            errorMessage = MmrUiMessages.friendly(e.message, MmrUiMessages.GENERIC_ACTION),
                        )
                    }
                }
            }
        }
    }
