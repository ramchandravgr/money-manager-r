package com.mmr.core.lansync

import com.mmr.core.attachment.TransactionAttachmentStore
import com.mmr.core.database.dao.TransactionAttachmentDao
import com.mmr.core.domain.repository.TransactionAttachmentRepository
import com.mmr.core.lansync.model.LanAttachmentRecord
import com.mmr.core.lansync.transport.LanSyncHttpClient
import com.mmr.core.lansync.transport.LanSyncHttpConstants
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LanSyncAttachmentPuller
    @Inject
    constructor(
        private val attachmentDao: TransactionAttachmentDao,
        private val attachmentRepository: TransactionAttachmentRepository,
        private val store: TransactionAttachmentStore,
    ) {
        suspend fun pullMissing(
            host: String,
            port: Int,
            deviceId: String,
            authToken: String,
            records: List<LanAttachmentRecord>,
        ): Int {
            var pulled = 0
            for (record in records) {
                val row = attachmentDao.getBySyncId(record.syncId) ?: continue
                val localBytes = store.readBytes(record.syncId)
                if (localBytes != null && localBytes.size.toLong() == record.fileSizeBytes) {
                    continue
                }
                val txnId = row.transactionId
                val path = LanSyncHttpConstants.pathAttachment(record.syncId)
                val response =
                    runCatching {
                        LanSyncHttpClient.getBytes(
                            host = host,
                            port = port,
                            path = path,
                            deviceId = deviceId,
                            authToken = authToken,
                        )
                    }.getOrNull() ?: continue
                if (response.statusCode != 200) continue
                attachmentRepository.importSyncedAttachment(
                    transactionId = txnId,
                    syncId = record.syncId,
                    mimeType = record.mimeType,
                    bytes = response.body,
                    createdAt = record.createdAt,
                    updatedAt = record.updatedAt,
                )
                pulled++
            }
            return pulled
        }
    }
