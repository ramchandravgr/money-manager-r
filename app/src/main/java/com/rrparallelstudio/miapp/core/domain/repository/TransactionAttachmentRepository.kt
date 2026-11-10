package com.rrparallelstudio.miapp.core.domain.repository

import android.net.Uri
import com.rrparallelstudio.miapp.core.database.entity.TransactionAttachmentEntity
import java.io.File

interface TransactionAttachmentRepository {
    suspend fun listForTransaction(transactionId: Long): List<TransactionAttachmentEntity>

    suspend fun addFromUri(
        transactionId: Long,
        uri: Uri,
    ): TransactionAttachmentEntity

    suspend fun addFromBytes(
        transactionId: Long,
        mimeType: String,
        bytes: ByteArray,
        syncId: String? = null,
        createdAt: Long = System.currentTimeMillis(),
        updatedAt: Long = System.currentTimeMillis(),
    ): TransactionAttachmentEntity

    suspend fun delete(attachmentId: Long)

    suspend fun deleteAllForTransaction(transactionId: Long)

    fun localFile(entity: TransactionAttachmentEntity): File?

    suspend fun importSyncedAttachment(
        transactionId: Long,
        syncId: String,
        mimeType: String,
        bytes: ByteArray,
        createdAt: Long,
        updatedAt: Long,
    )

    suspend fun refreshAttachmentCount(transactionId: Long)
}
