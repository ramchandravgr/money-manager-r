package com.rrparallelstudio.miapp.core.database.model

import androidx.room.Embedded
import com.rrparallelstudio.miapp.core.database.entity.TransactionApprovalEntity

data class TransactionApprovalWithSmsBody(
    @Embedded val approval: TransactionApprovalEntity,
    val smsFullBody: String?,
)
