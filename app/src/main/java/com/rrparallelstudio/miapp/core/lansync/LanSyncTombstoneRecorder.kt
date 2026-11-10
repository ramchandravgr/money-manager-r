package com.rrparallelstudio.miapp.core.lansync

import com.rrparallelstudio.miapp.core.data.preferences.LanSyncIdentityStore
import com.rrparallelstudio.miapp.core.database.dao.SyncTombstoneDao
import com.rrparallelstudio.miapp.core.database.entity.SyncTombstoneEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LanSyncTombstoneRecorder
    @Inject
    constructor(
        private val tombstoneDao: SyncTombstoneDao,
        private val identityStore: LanSyncIdentityStore,
    ) {
        suspend fun recordDeletion(
            syncId: String,
            entityType: String,
        ) {
            val normalized = syncId.trim()
            if (normalized.isEmpty()) return
            val now = System.currentTimeMillis()
            val existing = tombstoneDao.getBySyncId(normalized)
            if (existing != null && existing.deletedAt >= now) return
            tombstoneDao.upsert(
                SyncTombstoneEntity(
                    syncId = normalized,
                    entityType = entityType,
                    deletedAt = now,
                    sourceDeviceId = identityStore.deviceId,
                ),
            )
        }
    }
