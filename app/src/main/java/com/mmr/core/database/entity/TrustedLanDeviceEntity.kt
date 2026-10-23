package com.mmr.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "trusted_lan_devices",
    indices = [Index(value = ["displayName"])],
)
data class TrustedLanDeviceEntity(
    @PrimaryKey
    val remoteDeviceId: String,
    val displayName: String,
    /** Token the remote device must send when calling this device. */
    val theirAuthToken: String,
    val pairedAtMillis: Long = System.currentTimeMillis(),
)
