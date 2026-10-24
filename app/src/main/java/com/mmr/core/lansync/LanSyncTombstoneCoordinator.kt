package com.mmr.core.lansync

import com.mmr.core.database.dao.LanSyncDao
import com.mmr.core.database.dao.LanSyncTombstoneAckDao
import com.mmr.core.database.dao.SyncTombstoneDao
import com.mmr.core.database.entity.LanSyncTombstonePeerAckEntity
import com.mmr.core.database.entity.SyncTombstoneEntity
import com.mmr.core.lansync.model.LanSyncTombstoneRecord
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LanSyncTombstoneCoordinator
    @Inject
    constructor(
        private val tombstoneDao: SyncTombstoneDao,
        private val ackDao: LanSyncTombstoneAckDao,
        private val lanSyncDao: LanSyncDao,
    ) {
        suspend fun watermarkForPeer(peerDeviceId: String): Long = ackDao.getWatermark(peerDeviceId) ?: 0L

        suspend fun tombstonesForPeerExport(peerDeviceId: String): List<SyncTombstoneEntity> =
            tombstoneDao.getDeletedAfter(watermarkForPeer(peerDeviceId))

        suspend fun tombstonesForExport(peerDeviceId: String?): List<SyncTombstoneEntity> =
            if (peerDeviceId.isNullOrBlank()) {
                tombstoneDao.getAll()
            } else {
                tombstonesForPeerExport(peerDeviceId)
            }

        suspend fun onSuccessfulSyncToPeer(
            peerDeviceId: String,
            sentTombstones: List<LanSyncTombstoneRecord>,
        ) {
            if (sentTombstones.isEmpty()) return
            val maxDeleted = sentTombstones.maxOf { it.deletedAt }
            val current = ackDao.getWatermark(peerDeviceId) ?: 0L
            if (maxDeleted > current) {
                ackDao.upsert(
                    LanSyncTombstonePeerAckEntity(
                        peerDeviceId = peerDeviceId,
                        lastAckDeletedAt = maxDeleted,
                    ),
                )
            }
            pruneFullyAcknowledgedByAllTrustedPeers()
        }

        suspend fun pruneFullyAcknowledgedByAllTrustedPeers() {
            val trustedIds = lanSyncDao.getTrustedDevices().map { it.remoteDeviceId }
            if (trustedIds.isEmpty()) return
            val acks = ackDao.getForPeers(trustedIds)
            if (acks.size < trustedIds.size) return
            val cutoff = acks.minOf { it.lastAckDeletedAt }
            tombstoneDao.deleteWhereDeletedAtLte(cutoff)
        }
    }
