package com.mmr.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_lan_pair_requests")
data class PendingLanPairRequestEntity(
    @PrimaryKey
    val remoteDeviceId: String,
    val displayName: String,
    val theirAuthToken: String,
    val requestedAtMillis: Long = System.currentTimeMillis(),
)
