package com.mmr.feature.asset.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mmr.app.ui.state.MmrUiMessages
import com.mmr.core.common.image.MmrReceiptImage
import com.mmr.core.common.validation.MmrValidation
import com.mmr.core.data.repository.AssetAttachmentRepositoryImpl
import com.mmr.core.database.entity.AssetEntity
import com.mmr.core.domain.repository.AssetAttachmentRepository
import com.mmr.core.domain.repository.AssetRepository
import com.mmr.core.domain.repository.LoanRepository
import com.mmr.core.model.enums.AssetStatus
import com.mmr.core.model.enums.LoanStatus
import com.mmr.core.model.enums.LoanType
import com.mmr.core.reminder.MmrReminderPresets
import com.mmr.feature.asset.model.AddEditAssetUiEvent
import com.mmr.feature.asset.model.AddEditAssetUiState
import com.mmr.feature.asset.model.assetTypeForEdit
import com.mmr.feature.transaction.model.LoanPickerUi
import com.mmr.feature.transaction.model.TransactionReceiptUi
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
                    _uiState.update {
                        it.copy(
                            name = asset.name,
                            assetType = asset.assetType.assetTypeForEdit(),
                            status = asset.status,
                            isInherited = asset.isInherited,
                            purchaseDateMillis = asset.purchaseDateMillis,
                            purchasePrice = asset.purchasePrice?.toString() ?: asset.currentValue.toString(),
                            ownedByName = asset.ownedByName ?: "",
                            location = asset.location ?: "",
                            identifier = asset.identifier ?: "",
                            linkedLoanId = asset.linkedLoanId,
                            nextValuationDueMillis = asset.nextValuationDueMillis,
                            valuationReminderDaysBefore = asset.valuationReminderDaysBefore,
                            saleDateMillis = asset.saleDateMillis,
                            salePrice = asset.salePrice?.toString() ?: "",
                            notes = asset.notes ?: "",
                            photos = photos,
                            canSave =
                                validate(
                                    it.copy(name = asset.name, purchasePrice = asset.currentValue.toString()),
                                ),
                        )
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
                            it.copy(
                                isInherited = event.value,
                                purchaseDateMillis = if (event.value) it.purchaseDateMillis else it.purchaseDateMillis,
                            )
                        next.copy(canSave = validate(next))
                    }

                is AddEditAssetUiEvent.OnPurchaseDateChanged ->
                    _uiState.update { it.copy(purchaseDateMillis = event.millis, canSave = validate(it)) }

                is AddEditAssetUiEvent.OnPurchasePriceChanged ->
                    _uiState.update {
                        it.copy(
                            purchasePrice = event.value,
                            canSave = validate(it.copy(purchasePrice = event.value)),
                        )
                    }

                is AddEditAssetUiEvent.OnOwnedByChanged -> _uiState.update { it.copy(ownedByName = event.value) }

                is AddEditAssetUiEvent.OnLocationChanged -> _uiState.update { it.copy(location = event.value) }

                is AddEditAssetUiEvent.OnIdentifierChanged -> _uiState.update { it.copy(identifier = event.value) }

                is AddEditAssetUiEvent.OnLinkedLoanSelected ->
                    _uiState.update { it.copy(linkedLoanId = event.loanId) }

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
            return true
        }

        private fun save() {
            val state = _uiState.value
            if (!validate(state) || state.isSaving) return
            viewModelScope.launch {
                _uiState.update { it.copy(isSaving = true, errorMessage = null) }
                try {
                    val amount = MmrValidation.parsePositiveAmount(state.purchasePrice)!!
                    val entity =
                        AssetEntity(
                            id = assetId ?: 0L,
                            name = state.name.trim(),
                            assetType = state.assetType.trim(),
                            status = state.status,
                            isInherited = state.isInherited,
                            purchaseDateMillis = state.purchaseDateMillis,
                            purchasePrice = amount,
                            ownedByName = state.ownedByName.trim().takeIf { it.isNotEmpty() },
                            location = state.location.trim().takeIf { it.isNotEmpty() },
                            identifier = state.identifier.trim().takeIf { it.isNotEmpty() },
                            linkedLoanId = state.linkedLoanId,
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
