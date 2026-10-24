package com.mmr.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "lan_sync_log_entries")
data class LanSyncLogEntryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val completedAtMillis: Long,
    val peerName: String,
    val success: Boolean,
    val summaryText: String,
    val warningsText: String? = null,
)
