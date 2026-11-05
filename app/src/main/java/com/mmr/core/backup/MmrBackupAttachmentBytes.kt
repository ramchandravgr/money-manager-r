package com.mmr.core.backup

import android.util.Base64
import com.mmr.core.data.repository.AssetAttachmentRepositoryImpl
import com.mmr.core.data.repository.TransactionAttachmentRepositoryImpl

object MmrBackupAttachmentBytes {
    const val MAX_TOTAL_RESTORE_BYTES = 120 * 1024 * 1024

    fun decodeBase64Content(
        contentBase64: String,
        maxBytes: Int,
    ): ByteArray? {
        if (contentBase64.isBlank()) return null
        val maxEncodedLength = ((maxBytes.toLong() * 4) / 3) + 4
        if (contentBase64.length > maxEncodedLength) return null
        val bytes =
            runCatching { Base64.decode(contentBase64, Base64.DEFAULT) }.getOrNull()
                ?: return null
        if (bytes.size > maxBytes) return null
        return bytes
    }

    fun maxBytesForTransactionAttachment(): Int = TransactionAttachmentRepositoryImpl.MAX_ATTACHMENT_BYTES

    fun maxBytesForAssetAttachment(): Int = AssetAttachmentRepositoryImpl.MAX_ATTACHMENT_BYTES
}
