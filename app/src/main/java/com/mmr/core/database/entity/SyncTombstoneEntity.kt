package com.mmr.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sync_tombstones",
    indices = [Index(value = ["deletedAt"])],
)
data class SyncTombstoneEntity(
    @PrimaryKey
    val syncId: String,
    val entityType: String,
    val deletedAt: Long,
    val sourceDeviceId: String,
)
