package com.mmr.core.lansync.model

data class LanSyncTombstoneRecord(
    val syncId: String,
    val entityType: String,
    val deletedAt: Long,
    val sourceDeviceId: String,
)
