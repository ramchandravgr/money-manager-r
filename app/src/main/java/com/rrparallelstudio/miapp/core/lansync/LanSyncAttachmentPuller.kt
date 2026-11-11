package com.rrparallelstudio.miapp.core.lansync

import com.rrparallelstudio.miapp.core.attachment.AssetAttachmentStore
import com.rrparallelstudio.miapp.core.attachment.TransactionAttachmentStore
import com.rrparallelstudio.miapp.core.database.dao.AssetAttachmentDao
import com.rrparallelstudio.miapp.core.database.dao.TransactionAttachmentDao
import com.rrparallelstudio.miapp.core.domain.repository.AssetAttachmentRepository
import com.rrparallelstudio.miapp.core.domain.repository.TransactionAttachmentRepository
import com.rrparallelstudio.miapp.core.lansync.model.LanAssetAttachmentRecord
import com.rrparallelstudio.miapp.core.lansync.model.LanAttachmentRecord
import com.rrparallelstudio.miapp.core.lansync.transport.LanSyncHttpClient
import com.rrparallelstudio.miapp.core.lansync.transport.LanSyncHttpConstants
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LanSyncAttachmentPuller
    @Inject
    constructor(
        private val attachmentDao: TransactionAttachmentDao,
        private val attachmentRepository: TransactionAttachmentRepository,
        private val store: TransactionAttachmentStore,
        private val assetAttachmentDao: AssetAttachmentDao,
        private val assetAttachmentRepository: AssetAttachmentRepository,
        private val assetStore: AssetAttachmentStore,
        private val networkUtils: LanSyncNetworkUtils,
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
                            network = networkUtils.wifiNetwork(),
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

        suspend fun pullMissingAssetAttachments(
            host: String,
            port: Int,
            deviceId: String,
            authToken: String,
            records: List<LanAssetAttachmentRecord>,
        ): Int {
            var pulled = 0
            for (record in records) {
                val row = assetAttachmentDao.getBySyncId(record.syncId) ?: continue
                val localBytes = assetStore.readBytes(record.syncId)
                if (localBytes != null && localBytes.size.toLong() == record.fileSizeBytes) {
                    continue
                }
                val assetId = row.assetId
                val path = LanSyncHttpConstants.pathAttachment(record.syncId)
                val response =
                    runCatching {
                        LanSyncHttpClient.getBytes(
                            host = host,
                            port = port,
                            path = path,
                            deviceId = deviceId,
                            authToken = authToken,
                            network = networkUtils.wifiNetwork(),
                        )
                    }.getOrNull() ?: continue
                if (response.statusCode != 200) continue
                assetAttachmentRepository.importSyncedAttachment(
                    assetId = assetId,
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
