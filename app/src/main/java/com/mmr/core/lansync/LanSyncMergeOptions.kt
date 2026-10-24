package com.mmr.core.lansync

import com.mmr.core.data.preferences.LanSyncDuplicateEntityPolicy

enum class LanSyncRecurringResolution {
    USE_LOCAL,
    USE_REMOTE,
}

data class LanSyncMergeOptions(
    val recurringBySyncId: Map<String, LanSyncRecurringResolution> = emptyMap(),
    /** Same display name, different syncId → map to local row (no duplicate seed). */
    val duplicateAccountPolicy: LanSyncDuplicateEntityPolicy = LanSyncDuplicateEntityPolicy.MERGE_BY_NAME,
    val duplicateCategoryPolicy: LanSyncDuplicateEntityPolicy = LanSyncDuplicateEntityPolicy.MERGE_BY_NAME,
)

data class LanSyncRecurringConflict(
    val syncId: String,
    val name: String,
    val localUpdatedAt: Long,
    val remoteUpdatedAt: Long,
)

data class LanSyncMergeConflicts(
    val recurring: List<LanSyncRecurringConflict> = emptyList(),
) {
    val needsUserInput: Boolean
        get() = recurring.isNotEmpty()
}

class LanSyncNeedsResolutionException(
    val conflicts: LanSyncMergeConflicts,
) : IllegalStateException("Sync needs your choices before merging.")
