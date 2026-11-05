package com.mmr.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Known owner profiles for resolving "Created by" labels (local + peers from sync/backup). */
@Entity(tableName = "owner_profiles")
data class OwnerProfileEntity(
    @PrimaryKey
    val profileId: String,
    val displayName: String,
    val updatedAt: Long = System.currentTimeMillis(),
)
