package com.mmr.core.data.repository

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.mmr.core.attachment.AssetAttachmentStore
import com.mmr.core.common.image.MmrReceiptImage
import com.mmr.core.common.sync.MmrSyncId
import com.mmr.core.database.MmrDatabase
import com.mmr.core.database.dao.AssetAttachmentDao
import com.mmr.core.database.entity.AssetAttachmentEntity
import com.mmr.core.domain.repository.AssetAttachmentRepository
import com.mmr.core.lansync.LanSyncTombstoneRecorder
import com.mmr.core.lansync.SyncEntityType
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AssetAttachmentRepositoryImpl
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val database: MmrDatabase,
        private val attachmentDao: AssetAttachmentDao,
        private val store: AssetAttachmentStore,
        private val tombstoneRecorder: LanSyncTombstoneRecorder,
    ) : AssetAttachmentRepository {
        override suspend fun listForAsset(assetId: Long): List<AssetAttachmentEntity> =
            if (assetId <= 0L) {
                emptyList()
            } else {
                attachmentDao.getByAssetId(assetId)
            }

        override suspend fun addFromUri(
            assetId: Long,
            uri: Uri,
        ): AssetAttachmentEntity {
            require(assetId > 0L) { "Asset id is required." }
            val existing = attachmentDao.getByAssetId(assetId)
            if (existing.size >= MAX_ATTACHMENTS_PER_ASSET) {
                throw IllegalStateException("At most $MAX_ATTACHMENTS_PER_ASSET photos per asset.")
            }
            val resolver = context.contentResolver
            val mimeType = resolver.getType(uri) ?: "image/jpeg"
            val rawBytes =
                resolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: throw IllegalArgumentException("Could not read image.")
            val bytes =
                if (mimeType.startsWith("image/")) {
                    MmrReceiptImage.normalizeUriBytes(rawBytes, MAX_ATTACHMENT_BYTES)
                } else {
                    rawBytes
                }
            if (bytes.size > MAX_ATTACHMENT_BYTES) {
                throw IllegalArgumentException("Attachment is too large.")
            }
            val syncId = MmrSyncId.newId()
            store.writeBytes(syncId, bytes)
            val entity =
                AssetAttachmentEntity(
                    syncId = syncId,
                    assetId = assetId,
                    mimeType = mimeType,
                    fileSizeBytes = bytes.size.toLong(),
                )
            val id = attachmentDao.insert(entity)
            return entity.copy(id = id)
        }

        override suspend fun addFromBytes(
            assetId: Long,
            mimeType: String,
            bytes: ByteArray,
            syncId: String?,
            createdAt: Long,
            updatedAt: Long,
        ): AssetAttachmentEntity {
            require(assetId > 0L) { "Asset id is required." }
            val storedBytes =
                if (mimeType.startsWith("image/") && bytes.size > MAX_ATTACHMENT_BYTES) {
                    MmrReceiptImage.normalizeUriBytes(bytes, MAX_ATTACHMENT_BYTES)
                } else {
                    bytes
                }
            if (storedBytes.size > MAX_ATTACHMENT_BYTES) {
                throw IllegalArgumentException("Attachment is too large.")
            }
            val normalizedSyncId = MmrSyncId.normalize(syncId ?: MmrSyncId.newId())
            store.writeBytes(normalizedSyncId, storedBytes)
            val entity =
                AssetAttachmentEntity(
                    syncId = normalizedSyncId,
                    assetId = assetId,
                    mimeType = mimeType,
                    fileSizeBytes = storedBytes.size.toLong(),
                    createdAt = createdAt,
                    updatedAt = updatedAt,
                )
            val id = attachmentDao.insert(entity)
            return entity.copy(id = id)
        }

        override suspend fun delete(attachmentId: Long) {
            val existing = attachmentDao.getById(attachmentId) ?: return
            tombstoneRecorder.recordDeletion(existing.syncId, SyncEntityType.ASSET_ATTACHMENT)
            database.withTransaction {
                attachmentDao.deleteById(attachmentId)
                store.delete(existing.syncId)
            }
        }

        override fun localFile(entity: AssetAttachmentEntity): File? {
            val file = store.fileForSyncId(entity.syncId)
            return if (file.isFile) file else null
        }

        override suspend fun importSyncedAttachment(
            assetId: Long,
            syncId: String,
            mimeType: String,
            bytes: ByteArray,
            createdAt: Long,
            updatedAt: Long,
        ) {
            val normalizedSyncId = MmrSyncId.normalize(syncId)
            val existing = attachmentDao.getBySyncId(normalizedSyncId)
            if (existing != null) {
                if (updatedAt > existing.updatedAt) {
                    store.writeBytes(normalizedSyncId, bytes)
                    attachmentDao.update(
                        existing.copy(
                            assetId = assetId,
                            mimeType = mimeType,
                            fileSizeBytes = bytes.size.toLong(),
                            updatedAt = updatedAt,
                        ),
                    )
                }
            } else {
                store.writeBytes(normalizedSyncId, bytes)
                attachmentDao.insert(
                    AssetAttachmentEntity(
                        syncId = normalizedSyncId,
                        assetId = assetId,
                        mimeType = mimeType,
                        fileSizeBytes = bytes.size.toLong(),
                        createdAt = createdAt,
                        updatedAt = updatedAt,
                    ),
                )
            }
        }

        companion object {
            const val MAX_ATTACHMENTS_PER_ASSET = 8
            const val MAX_ATTACHMENT_BYTES = 4 * 1024 * 1024
        }
    }
