package com.mmr.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mmr.core.database.entity.LanSyncTombstonePeerAckEntity

@Dao
interface LanSyncTombstoneAckDao {
    @Query("SELECT lastAckDeletedAt FROM lan_sync_tombstone_acks WHERE peerDeviceId = :peerDeviceId LIMIT 1")
    suspend fun getWatermark(peerDeviceId: String): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: LanSyncTombstonePeerAckEntity)

    @Query("SELECT * FROM lan_sync_tombstone_acks WHERE peerDeviceId IN (:peerDeviceIds)")
    suspend fun getForPeers(peerDeviceIds: List<String>): List<LanSyncTombstonePeerAckEntity>
}
