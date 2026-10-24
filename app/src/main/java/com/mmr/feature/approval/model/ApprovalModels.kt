package com.mmr.feature.approval.model

import com.mmr.core.domain.model.ManualSmsDuplicateMatch
import com.mmr.core.model.enums.SmsApprovalState
import com.mmr.core.model.enums.TransactionType

data class ApprovalCandidateUi(
    val id: Long,
    val sender: String,
    val amountFormatted: String,
    val amount: Double,
    val type: TransactionType,
    val isCardRefund: Boolean = false,
    val typeLabel: String,
    val merchant: String?,
    val note: String,
    val dateLabel: String,
    val categoryName: String,
    val accountName: String,
    val statusLabel: String,
    val confidenceLabel: String,
    val parseSourceLabel: String? = null,
    val rawSnippet: String,
    /** Full inbox SMS text when stored on the linked record. */
    val smsFullBody: String,
    val state: SmsApprovalState,
)

data class PendingManualSmsDraft(
    val sender: String,
    val body: String,
    val timestampMillis: Long,
    val duplicateMatch: ManualSmsDuplicateMatch,
)

data class ApprovalCenterUiState(
    val isLoading: Boolean = false,
    val isScanning: Boolean = false,
    val candidates: List<ApprovalCandidateUi> = emptyList(),
    val selectedTab: SmsApprovalState = SmsApprovalState.PENDING,
    val pendingCount: Int = 0,
    val scanResultMessage: String? = null,
    val errorMessage: String? = null,
    val isEmpty: Boolean = false,
    val showManualEntryDialog: Boolean = false,
    val manualEntryError: String? = null,
    val pendingManualDuplicate: PendingManualSmsDraft? = null,
    val pendingSmsMerge: PendingSmsMergePrompt? = null,
    val pendingCardRefundApply: PendingCardRefundApplyPrompt? = null,
    val mergeCategoryOptions: List<ApprovalCategoryOptionUi> = emptyList(),
    val isSelectionMode: Boolean = false,
    val selectedApprovalIds: Set<Long> = emptySet(),
    val canLinkSelection: Boolean = false,
    val allPendingSelected: Boolean = false,
)

data class ApprovalCategoryOptionUi(
    val id: Long,
    val name: String,
)

data class PendingSmsMergePrompt(
    val approvalIds: List<Long>,
    val defaultCategoryId: Long?,
    val defaultCategoryName: String,
    val selectedCategoryId: Long? = null,
    val selectedSubcategoryId: Long? = null,
    val mergePickerHighlightedCategoryId: Long? = null,
    val mergeCategories: List<com.mmr.feature.transaction.model.CategoryUi> = emptyList(),
    val useLedgerCategory: Boolean = false,
    val userInitiatedLink: Boolean = false,
    val isLoadingCandidates: Boolean = false,
    val pickerYear: Int,
    val pickerMonth: Int,
    val monthTitle: String,
    val noteQuery: String = "",
    val groupedTransactions: List<com.mmr.feature.home.model.TransactionGroupUi> = emptyList(),
    val isEmptyMonth: Boolean = false,
) {
    val allowCreateNew: Boolean = approvalIds.size == 1
}

enum class CardRefundApplyStep {
    PICK_PURCHASE,
    CONFIRM_AMOUNT,
}

data class PendingCardRefundApplyPrompt(
    val approvalId: Long,
    val refundAmount: Double,
    val refundAmountLabel: String,
    val smsSnippet: String,
    val step: CardRefundApplyStep = CardRefundApplyStep.PICK_PURCHASE,
    val selectedPurchaseId: Long? = null,
    val isLoadingCandidates: Boolean = false,
    val pickerYear: Int,
    val pickerMonth: Int,
    val monthTitle: String,
    val noteQuery: String = "",
    val groupedTransactions: List<com.mmr.feature.home.model.TransactionGroupUi> = emptyList(),
    val isEmptyMonth: Boolean = false,
    val partialRefundAmountText: String = "",
    val applyFullRefund: Boolean = true,
)

sealed interface ApprovalCenterUiEvent {
    data class OnTabSelected(
        val tab: SmsApprovalState,
    ) : ApprovalCenterUiEvent

    data class OnEditClick(
        val id: Long,
    ) : ApprovalCenterUiEvent

    data class OnApproveQuick(
        val id: Long,
    ) : ApprovalCenterUiEvent

    data class OnRejectQuick(
        val id: Long,
    ) : ApprovalCenterUiEvent

    data class OnMarkDuplicateQuick(
        val id: Long,
    ) : ApprovalCenterUiEvent

    data class OnUpdateCandidate(
        val id: Long,
        val amount: Double,
        val type: TransactionType,
        val categoryId: Long?,
        val accountId: Long?,
        val note: String?,
    ) : ApprovalCenterUiEvent

    object OnScanInboxClick : ApprovalCenterUiEvent

    object OnClearScanMessage : ApprovalCenterUiEvent

    object OnRetryClick : ApprovalCenterUiEvent

    object OnAddManualSmsClick : ApprovalCenterUiEvent

    object OnDismissManualEntry : ApprovalCenterUiEvent

    data class OnSubmitManualSms(
        val sender: String,
        val body: String,
        val timestampMillis: Long,
    ) : ApprovalCenterUiEvent

    object OnDismissManualDuplicate : ApprovalCenterUiEvent

    object OnManualDuplicateRevertPrior : ApprovalCenterUiEvent

    object OnManualDuplicateAddAsNew : ApprovalCenterUiEvent

    object OnDismissSmsMerge : ApprovalCenterUiEvent

    object OnApproveAsNewTransaction : ApprovalCenterUiEvent

    data class OnMergeSmsIntoTransaction(
        val transactionId: Long,
    ) : ApprovalCenterUiEvent

    data class OnMergePickerParentSelected(
        val categoryId: Long,
    ) : ApprovalCenterUiEvent

    data class OnMergePickerSubcategorySelected(
        val subcategoryId: Long,
    ) : ApprovalCenterUiEvent

    object OnMergePickerConfirmParentOnly : ApprovalCenterUiEvent

    object OnMergeUseLedgerCategory : ApprovalCenterUiEvent

    object OnLinkPickerPreviousMonth : ApprovalCenterUiEvent

    object OnLinkPickerNextMonth : ApprovalCenterUiEvent

    data class OnLinkPickerNoteQueryChanged(
        val query: String,
    ) : ApprovalCenterUiEvent

    data class OnLinkToExisting(
        val id: Long,
    ) : ApprovalCenterUiEvent

    data class OnApprovalLongPress(
        val id: Long,
    ) : ApprovalCenterUiEvent

    data class OnApprovalSelectionToggle(
        val id: Long,
    ) : ApprovalCenterUiEvent

    object OnExitApprovalSelection : ApprovalCenterUiEvent

    object OnApprovalSelectAll : ApprovalCenterUiEvent

    object OnLinkSelectedApprovals : ApprovalCenterUiEvent

    object OnDismissCardRefundApply : ApprovalCenterUiEvent

    data class OnCardRefundSelectPurchase(
        val transactionId: Long,
    ) : ApprovalCenterUiEvent

    object OnCardRefundBackToPicker : ApprovalCenterUiEvent

    data class OnCardRefundToggleFull(
        val full: Boolean,
    ) : ApprovalCenterUiEvent

    data class OnCardRefundPartialAmountChanged(
        val text: String,
    ) : ApprovalCenterUiEvent

    object OnCardRefundConfirmApply : ApprovalCenterUiEvent

    object OnCardRefundPickerPreviousMonth : ApprovalCenterUiEvent

    object OnCardRefundPickerNextMonth : ApprovalCenterUiEvent

    data class OnCardRefundPickerNoteQueryChanged(
        val query: String,
    ) : ApprovalCenterUiEvent

    data class OnApplyRefundQuick(
        val id: Long,
    ) : ApprovalCenterUiEvent
}
