package com.rrparallelstudio.miapp.core.backup.model

/**
 * SMS account correction row in signed backup JSON (format v5+).
 * [accountSyncId] is canonical; [accountId] is optional legacy hint from the exporting device.
 */
data class MmrBackupSmsAccountOverrideRecord(
    val templateId: String,
    val instrument: String,
    val smsLastFour: String,
    val accountSyncId: String,
    val accountId: Long = 0L,
    val updatedAt: Long,
)
