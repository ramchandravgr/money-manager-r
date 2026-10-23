package com.mmr.core.domain.model

import com.mmr.core.database.entity.TransactionApprovalEntity

data class PendingApprovalWithSms(
    val approval: TransactionApprovalEntity,
    val smsBody: String,
)
