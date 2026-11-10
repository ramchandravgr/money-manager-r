package com.rrparallelstudio.miapp.core.backup.model

data class MmrBackupAssetAttachmentRecord(
    val id: Long,
    val syncId: String,
    val assetId: Long,
    val mimeType: String,
    val fileSizeBytes: Long,
    val createdAt: Long,
    val updatedAt: Long,
    val contentBase64: String,
)
