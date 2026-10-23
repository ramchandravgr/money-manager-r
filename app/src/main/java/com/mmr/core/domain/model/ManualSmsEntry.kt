package com.mmr.core.domain.model

import com.mmr.core.model.enums.SmsApprovalState

data class ManualSmsDuplicateMatch(
    val approvalId: Long,
    val priorState: SmsApprovalState,
    val priorActionLabel: String,
)

enum class ManualSmsDuplicateResolution {
    REVERT_PRIOR_AND_USE_NEW,
    ADD_AS_NEW_TRANSACTION,
}

sealed interface ManualSmsSubmitResult {
    data class Added(
        val approvalId: Long,
    ) : ManualSmsSubmitResult

    data class NeedsDuplicateDecision(
        val match: ManualSmsDuplicateMatch,
    ) : ManualSmsSubmitResult

    data class ParseFailed(
        val reason: String,
    ) : ManualSmsSubmitResult
}

fun SmsApprovalState.toManualPriorActionLabel(): String =
    when (this) {
        SmsApprovalState.PENDING -> "pending approval"
        SmsApprovalState.REJECTED -> "rejected"
        SmsApprovalState.APPROVED -> "approved"
        SmsApprovalState.DUPLICATE -> "marked as duplicate"
        SmsApprovalState.IGNORED -> "ignored"
    }
