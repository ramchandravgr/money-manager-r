package com.rrparallelstudio.miapp.core.backup

/**
 * Uncompressed and compressed size limits for signed `.mibxbac` archives (zip bomb mitigation).
 */
object BackupArchiveLimits {
    const val MAX_COMPRESSED_BYTES = 160 * 1024 * 1024

    const val MAX_MANIFEST_BYTES = 512 * 1024

    const val MAX_PAYLOAD_BYTES = 32 * 1024 * 1024

    const val MAX_ATTACHMENT_ENTRY_BYTES = 5 * 1024 * 1024

    val MAX_TOTAL_UNCOMPRESSED_BYTES: Long =
        MmrBackupAttachmentBytes.MAX_TOTAL_RESTORE_BYTES.toLong() +
            MAX_PAYLOAD_BYTES.toLong() +
            MAX_MANIFEST_BYTES.toLong()
}
