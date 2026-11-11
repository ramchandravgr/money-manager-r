package com.rrparallelstudio.miapp.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * User correction: this SMS pattern should use [accountId] instead of the parser's account.
 * Pattern is template id and/or instrument plus the last four digits from the SMS.
 */
@Entity(
    tableName = "sms_account_overrides",
    indices = [
        Index(value = ["templateId", "smsLastFour"], unique = true),
        Index(value = ["instrument", "smsLastFour"]),
    ],
)
data class SmsAccountOverrideEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val templateId: String = "",
    val instrument: String = "",
    val smsLastFour: String,
    val accountId: Long,
    val updatedAt: Long = System.currentTimeMillis(),
)
