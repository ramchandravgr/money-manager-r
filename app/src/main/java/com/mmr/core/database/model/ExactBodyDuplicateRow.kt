package com.mmr.core.database.model

import androidx.room.ColumnInfo
import com.mmr.core.model.enums.SmsApprovalState

data class ExactBodyDuplicateRow(
    @ColumnInfo(name = "approvalId")
    val approvalId: Long,
    @ColumnInfo(name = "approvalState")
    val approvalState: SmsApprovalState,
)
