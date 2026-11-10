package com.rrparallelstudio.miapp.feature.asset.model

import com.rrparallelstudio.miapp.core.model.enums.AssetPurchasePaymentMode
import com.rrparallelstudio.miapp.core.model.enums.AssetStatus
import com.rrparallelstudio.miapp.feature.transaction.model.TransactionReceiptUi

enum class AssetStatusFilter {
    ACTIVE,
    ALL,
    SOLD,
}

/** Display text for list/detail. */
fun String.assetTypeDisplayLabel(): String = trim()

/** Value shown in the add/edit type field when loading an existing asset. */
fun String.assetTypeForEdit(): String = assetTypeDisplayLabel()

fun AssetStatus.displayLabel(): String =
    when (this) {
        AssetStatus.ACTIVE -> "Active"
        AssetStatus.SOLD -> "Sold"
    }

data class AssetListItemUi(
    val id: Long,
    val name: String,
    val typeLabel: String,
    val status: AssetStatus,
    val statusLabel: String,
    val currentValueFormatted: String,
    val changePercentLabel: String?,
    val ownedByName: String?,
)

data class AssetListUiState(
    val isLoading: Boolean = true,
    val assets: List<AssetListItemUi> = emptyList(),
    val selectedStatusFilter: AssetStatusFilter = AssetStatusFilter.ACTIVE,
    val totalActiveValueFormatted: String = "₹0.00",
    val activeCount: Int = 0,
    val soldCount: Int = 0,
    val isEmpty: Boolean = false,
    val isFilteredEmpty: Boolean = false,
    val errorMessage: String? = null,
)

sealed interface AssetListUiEvent {
    data class OnStatusFilterSelected(
        val filter: AssetStatusFilter,
    ) : AssetListUiEvent

    object OnRetryClick : AssetListUiEvent
}

data class AssetValuationItemUi(
    val id: Long,
    val amountFormatted: String,
    val dateFormatted: String,
    val changePercentLabel: String?,
    val notes: String?,
)

data class AssetDetailUiState(
    val isLoading: Boolean = true,
    val assetId: Long = 0L,
    val name: String = "",
    val typeLabel: String = "",
    val status: AssetStatus = AssetStatus.ACTIVE,
    val statusLabel: String = "",
    val currentValueFormatted: String = "",
    val changeSinceLastLabel: String? = null,
    val changeSincePurchaseLabel: String? = null,
    val purchaseDateFormatted: String? = null,
    val purchasePriceFormatted: String? = null,
    val purchasePaymentLabel: String? = null,
    val purchaseDownPaymentFormatted: String? = null,
    val purchaseFinancedAmountFormatted: String? = null,
    val isInherited: Boolean = false,
    val ownedByName: String? = null,
    val location: String? = null,
    val identifier: String? = null,
    val linkedLoanName: String? = null,
    val linkedLoanOutstandingFormatted: String? = null,
    val nextValuationDueFormatted: String? = null,
    val reminderLabel: String? = null,
    val saleDateFormatted: String? = null,
    val salePriceFormatted: String? = null,
    val notes: String? = null,
    val valuations: List<AssetValuationItemUi> = emptyList(),
    val photos: List<TransactionReceiptUi> = emptyList(),
    val errorMessage: String? = null,
    val isValuationSuccess: Boolean = false,
    val isAssetDeleted: Boolean = false,
    val showValuationDialog: Boolean = false,
    val valuationAmountInput: String = "",
    val valuationDateMillis: Long = System.currentTimeMillis(),
    val valuationNoteInput: String = "",
    val valuationPreviewChangeLabel: String? = null,
)

sealed interface AssetDetailUiEvent {
    object OnRetryClick : AssetDetailUiEvent

    object OnOpenValuationDialog : AssetDetailUiEvent

    object OnDismissValuationDialog : AssetDetailUiEvent

    data class OnValuationAmountChanged(
        val value: String,
    ) : AssetDetailUiEvent

    data class OnValuationDateChanged(
        val millis: Long,
    ) : AssetDetailUiEvent

    data class OnValuationNoteChanged(
        val value: String,
    ) : AssetDetailUiEvent

    object OnSubmitValuation : AssetDetailUiEvent

    object OnClearValuationSuccess : AssetDetailUiEvent

    object OnDeleteAsset : AssetDetailUiEvent
}

data class AddEditAssetUiState(
    val assetId: Long? = null,
    val name: String = "",
    val assetType: String = "",
    val status: AssetStatus = AssetStatus.ACTIVE,
    val isInherited: Boolean = false,
    val purchaseDateMillis: Long? = null,
    val purchasePaymentMode: AssetPurchasePaymentMode = AssetPurchasePaymentMode.PAID_IN_FULL,
    val purchasePrice: String = "",
    val purchaseDownPayment: String = "",
    val purchaseFinancedAmount: String = "",
    val ownedByName: String = "",
    val location: String = "",
    val identifier: String = "",
    val linkedLoanId: Long? = null,
    val linkedLoans: List<com.rrparallelstudio.miapp.feature.transaction.model.LoanPickerUi> = emptyList(),
    val nextValuationDueMillis: Long? = null,
    val valuationReminderDaysBefore: Int = 0,
    val saleDateMillis: Long? = null,
    val salePrice: String = "",
    val notes: String = "",
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val canSave: Boolean = false,
    val errorMessage: String? = null,
    val photos: List<TransactionReceiptUi> = emptyList(),
)

sealed interface AddEditAssetUiEvent {
    data class OnNameChanged(
        val value: String,
    ) : AddEditAssetUiEvent

    data class OnAssetTypeChanged(
        val value: String,
    ) : AddEditAssetUiEvent

    data class OnStatusSelected(
        val status: AssetStatus,
    ) : AddEditAssetUiEvent

    data class OnInheritedChanged(
        val value: Boolean,
    ) : AddEditAssetUiEvent

    data class OnPurchaseDateChanged(
        val millis: Long?,
    ) : AddEditAssetUiEvent

    data class OnPurchasePriceChanged(
        val value: String,
    ) : AddEditAssetUiEvent

    data class OnPurchasePaymentModeSelected(
        val mode: AssetPurchasePaymentMode,
    ) : AddEditAssetUiEvent

    data class OnPurchaseDownPaymentChanged(
        val value: String,
    ) : AddEditAssetUiEvent

    data class OnPurchaseFinancedAmountChanged(
        val value: String,
    ) : AddEditAssetUiEvent

    data class OnOwnedByChanged(
        val value: String,
    ) : AddEditAssetUiEvent

    data class OnLocationChanged(
        val value: String,
    ) : AddEditAssetUiEvent

    data class OnIdentifierChanged(
        val value: String,
    ) : AddEditAssetUiEvent

    data class OnLinkedLoanSelected(
        val loanId: Long?,
    ) : AddEditAssetUiEvent

    data class OnNextValuationDueChanged(
        val millis: Long?,
    ) : AddEditAssetUiEvent

    data class OnReminderDaysSelected(
        val days: Int,
    ) : AddEditAssetUiEvent

    data class OnSaleDateChanged(
        val millis: Long?,
    ) : AddEditAssetUiEvent

    data class OnSalePriceChanged(
        val value: String,
    ) : AddEditAssetUiEvent

    data class OnNotesChanged(
        val value: String,
    ) : AddEditAssetUiEvent

    object OnSaveClick : AddEditAssetUiEvent

    data class OnPhotoPicked(
        val uriString: String,
    ) : AddEditAssetUiEvent

    data class OnRemovePhoto(
        val attachmentId: Long,
    ) : AddEditAssetUiEvent

    data class OnRemovePendingPhoto(
        val filePath: String,
    ) : AddEditAssetUiEvent
}
