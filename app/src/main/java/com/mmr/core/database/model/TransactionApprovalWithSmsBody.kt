package com.mmr.core.database.model

import androidx.room.Embedded
import com.mmr.core.database.entity.TransactionApprovalEntity

data class TransactionApprovalWithSmsBody(
    @Embedded val approval: TransactionApprovalEntity,
    val smsFullBody: String?,
)
