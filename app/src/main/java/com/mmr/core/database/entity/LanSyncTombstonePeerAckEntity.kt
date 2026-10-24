package com.mmr.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "lan_sync_tombstone_acks")
data class LanSyncTombstonePeerAckEntity(
    @PrimaryKey
    val peerDeviceId: String,
    val lastAckDeletedAt: Long,
)
