package com.mmr.core.database.entity

import androidx.room.*
import com.mmr.core.model.enums.*

@Entity(
    tableName = "sms_message_records",
    indices = [
        Index(value = ["sender"]),
        Index(value = ["receivedTime"]),
        Index(value = ["approvalState"]),
        Index(value = ["messageBodyHash"], unique = true),
        Index(value = ["matchedTransactionId"]),
    ],
)
data class SmsMessageRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sender: String,
    val messageBodyHash: String,
    val messageBodySnippet: String,
    /** Full SMS body for exact duplicate checks (manual entry and inbox import). */
    val messageBodyFull: String? = null,
    val receivedTime: Long,
    val parsedStatus: SmsParsedStatus = SmsParsedStatus.MATCHED,
    val parsedAmount: Double? = null,
    val parsedDateTime: Long? = null,
    val matchedTransactionId: Long? = null,
    val approvalState: SmsApprovalState = SmsApprovalState.PENDING,
    val rawMessageId: String? = null,
    val processedAt: Long? = System.currentTimeMillis(),
)
