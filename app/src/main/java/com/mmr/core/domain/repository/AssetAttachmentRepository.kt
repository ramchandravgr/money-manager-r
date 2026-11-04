package com.mmr.core.domain.repository

import android.net.Uri
import com.mmr.core.database.entity.AssetAttachmentEntity
import java.io.File

interface AssetAttachmentRepository {
    suspend fun listForAsset(assetId: Long): List<AssetAttachmentEntity>

    suspend fun addFromUri(
        assetId: Long,
        uri: Uri,
    ): AssetAttachmentEntity

    suspend fun addFromBytes(
        assetId: Long,
        mimeType: String,
        bytes: ByteArray,
        syncId: String? = null,
        createdAt: Long = System.currentTimeMillis(),
        updatedAt: Long = System.currentTimeMillis(),
    ): AssetAttachmentEntity

    suspend fun delete(attachmentId: Long)

    suspend fun importSyncedAttachment(
        assetId: Long,
        syncId: String,
        mimeType: String,
        bytes: ByteArray,
        createdAt: Long,
        updatedAt: Long,
    )

    fun localFile(entity: AssetAttachmentEntity): File?
}
