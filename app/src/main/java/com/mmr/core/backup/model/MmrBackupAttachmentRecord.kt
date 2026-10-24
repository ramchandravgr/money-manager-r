package com.mmr.core.backup.model

data class MmrBackupAttachmentRecord(
    val id: Long,
    val syncId: String,
    val transactionId: Long,
    val mimeType: String,
    val fileSizeBytes: Long,
    val createdAt: Long,
    val updatedAt: Long,
    val contentBase64: String,
)
