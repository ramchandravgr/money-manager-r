package com.rrparallelstudio.miapp.core.domain.model

import com.rrparallelstudio.miapp.core.database.entity.TransactionApprovalEntity

data class PendingApprovalWithSms(
    val approval: TransactionApprovalEntity,
    val smsBody: String,
)
