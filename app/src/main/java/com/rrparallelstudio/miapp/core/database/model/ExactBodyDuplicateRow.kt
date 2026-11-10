package com.rrparallelstudio.miapp.core.database.model

import androidx.room.ColumnInfo
import com.rrparallelstudio.miapp.core.model.enums.SmsApprovalState

data class ExactBodyDuplicateRow(
    @ColumnInfo(name = "approvalId")
    val approvalId: Long,
    @ColumnInfo(name = "approvalState")
    val approvalState: SmsApprovalState,
)
