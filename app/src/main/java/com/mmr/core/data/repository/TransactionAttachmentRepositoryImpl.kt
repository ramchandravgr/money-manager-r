package com.mmr.core.data.repository

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.mmr.core.attachment.TransactionAttachmentStore
import com.mmr.core.common.sync.MmrSyncId
import com.mmr.core.database.MmrDatabase
import com.mmr.core.database.dao.TransactionAttachmentDao
import com.mmr.core.database.dao.TransactionDao
import com.mmr.core.database.entity.TransactionAttachmentEntity
import com.mmr.core.domain.repository.TransactionAttachmentRepository
import com.mmr.core.lansync.LanSyncTombstoneRecorder
import com.mmr.core.lansync.SyncEntityType
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionAttachmentRepositoryImpl
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val database: MmrDatabase,
        private val attachmentDao: TransactionAttachmentDao,
        private val transactionDao: TransactionDao,
        private val store: TransactionAttachmentStore,
        private val tombstoneRecorder: LanSyncTombstoneRecorder,
    ) : TransactionAttachmentRepository {
        override suspend fun listForTransaction(transactionId: Long): List<TransactionAttachmentEntity> =
            if (transactionId <= 0L) {
                emptyList()
            } else {
                attachmentDao.getByTransactionId(transactionId)
            }

        override suspend fun addFromUri(
            transactionId: Long,
            uri: Uri,
        ): TransactionAttachmentEntity {
            require(transactionId > 0L) { "Transaction id is required." }
            val existing = attachmentDao.getByTransactionId(transactionId)
            if (existing.size >= MAX_ATTACHMENTS_PER_TRANSACTION) {
                throw IllegalStateException("At most $MAX_ATTACHMENTS_PER_TRANSACTION receipts per transaction.")
            }
            val resolver = context.contentResolver
            val mimeType = resolver.getType(uri) ?: "image/jpeg"
            val bytes =
                resolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: throw IllegalArgumentException("Could not read image.")
            if (bytes.size > MAX_ATTACHMENT_BYTES) {
                throw IllegalArgumentException("Image is too large (max ${MAX_ATTACHMENT_BYTES / (1024 * 1024)} MB).")
            }
            return addFromBytes(transactionId, mimeType, bytes)
        }

        override suspend fun addFromBytes(
            transactionId: Long,
            mimeType: String,
            bytes: ByteArray,
            syncId: String?,
            createdAt: Long,
            updatedAt: Long,
        ): TransactionAttachmentEntity {
            require(transactionId > 0L) { "Transaction id is required." }
            if (bytes.size > MAX_ATTACHMENT_BYTES) {
                throw IllegalArgumentException("Attachment is too large.")
            }
            val normalizedSyncId = MmrSyncId.normalize(syncId ?: MmrSyncId.newId())
            store.writeBytes(normalizedSyncId, bytes)
            val entity =
                TransactionAttachmentEntity(
                    syncId = normalizedSyncId,
                    transactionId = transactionId,
                    mimeType = mimeType,
                    fileSizeBytes = bytes.size.toLong(),
                    createdAt = createdAt,
                    updatedAt = updatedAt,
                )
            val id = attachmentDao.insert(entity)
            refreshAttachmentCount(transactionId)
            return entity.copy(id = id)
        }

        override suspend fun delete(attachmentId: Long) {
            val existing = attachmentDao.getById(attachmentId) ?: return
            tombstoneRecorder.recordDeletion(existing.syncId, SyncEntityType.ATTACHMENT)
            database.withTransaction {
                attachmentDao.deleteById(attachmentId)
                store.delete(existing.syncId)
                refreshAttachmentCount(existing.transactionId)
            }
        }

        override suspend fun deleteAllForTransaction(transactionId: Long) {
            val items = attachmentDao.getByTransactionId(transactionId)
            database.withTransaction {
                attachmentDao.deleteByTransactionId(transactionId)
                items.forEach { store.delete(it.syncId) }
                refreshAttachmentCount(transactionId)
            }
        }

        override fun localFile(entity: TransactionAttachmentEntity): File? {
            val file = store.fileForSyncId(entity.syncId)
            return if (file.isFile) file else null
        }

        override suspend fun importSyncedAttachment(
            transactionId: Long,
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
                            transactionId = transactionId,
                            mimeType = mimeType,
                            fileSizeBytes = bytes.size.toLong(),
                            updatedAt = updatedAt,
                        ),
                    )
                }
            } else {
                store.writeBytes(normalizedSyncId, bytes)
                attachmentDao.insert(
                    TransactionAttachmentEntity(
                        syncId = normalizedSyncId,
                        transactionId = transactionId,
                        mimeType = mimeType,
                        fileSizeBytes = bytes.size.toLong(),
                        createdAt = createdAt,
                        updatedAt = updatedAt,
                    ),
                )
            }
            refreshAttachmentCount(transactionId)
        }

        override suspend fun refreshAttachmentCount(transactionId: Long) {
            val txn = transactionDao.getById(transactionId) ?: return
            val count = attachmentDao.getByTransactionId(transactionId).size
            if (txn.attachmentCount != count) {
                transactionDao.update(txn.copy(attachmentCount = count))
            }
        }

        companion object {
            const val MAX_ATTACHMENTS_PER_TRANSACTION = 5
            const val MAX_ATTACHMENT_BYTES = 5 * 1024 * 1024
        }
    }
