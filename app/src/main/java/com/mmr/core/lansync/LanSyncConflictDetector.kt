package com.mmr.core.lansync

import com.mmr.core.database.MmrDatabase
import com.mmr.core.lansync.model.LanRecurringRuleRecord
import com.mmr.core.lansync.model.MmrLanSyncPayload
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LanSyncConflictDetector
    @Inject
    constructor(
        private val database: MmrDatabase,
    ) {
        suspend fun detect(
            remote: MmrLanSyncPayload,
            options: LanSyncMergeOptions,
        ): LanSyncMergeConflicts {
            val recurring = detectRecurringConflicts(remote.recurringRules, options)
            return LanSyncMergeConflicts(recurring = recurring)
        }

        private suspend fun detectRecurringConflicts(
            remoteRules: List<LanRecurringRuleRecord>,
            options: LanSyncMergeOptions,
        ): List<LanSyncRecurringConflict> {
            val dao = database.recurringRuleDao()
            val conflicts = mutableListOf<LanSyncRecurringConflict>()
            for (remote in remoteRules) {
                if (options.recurringBySyncId.containsKey(remote.syncId)) continue
                val local = dao.getBySyncId(remote.syncId) ?: continue
                if (local.updatedAt == remote.updatedAt) continue
                conflicts +=
                    LanSyncRecurringConflict(
                        syncId = remote.syncId,
                        name = remote.name,
                        localUpdatedAt = local.updatedAt,
                        remoteUpdatedAt = remote.updatedAt,
                    )
            }
            return conflicts
        }
    }
