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
    val state: SmsApprovalState
)

data class PendingManualSmsDraft(
    val sender: String,
    val body: String,
    val timestampMillis: Long,
    val duplicateMatch: ManualSmsDuplicateMatch
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
    val pendingManualDuplicate: PendingManualSmsDraft? = null
)

sealed interface ApprovalCenterUiEvent {
    data class OnTabSelected(val tab: SmsApprovalState) : ApprovalCenterUiEvent
    data class OnEditClick(val id: Long) : ApprovalCenterUiEvent
    data class OnApproveQuick(val id: Long) : ApprovalCenterUiEvent
    data class OnRejectQuick(val id: Long) : ApprovalCenterUiEvent
    data class OnMarkDuplicateQuick(val id: Long) : ApprovalCenterUiEvent
    data class OnUpdateCandidate(
        val id: Long,
        val amount: Double,
        val type: TransactionType,
        val categoryId: Long?,
        val accountId: Long?,
        val note: String?
    ) : ApprovalCenterUiEvent
    object OnScanInboxClick : ApprovalCenterUiEvent
    object OnClearScanMessage : ApprovalCenterUiEvent
    object OnRetryClick : ApprovalCenterUiEvent
    object OnAddManualSmsClick : ApprovalCenterUiEvent
    object OnDismissManualEntry : ApprovalCenterUiEvent
    data class OnSubmitManualSms(
        val sender: String,
        val body: String,
        val timestampMillis: Long
    ) : ApprovalCenterUiEvent
    object OnDismissManualDuplicate : ApprovalCenterUiEvent
    object OnManualDuplicateRevertPrior : ApprovalCenterUiEvent
    object OnManualDuplicateAddAsNew : ApprovalCenterUiEvent
}
