package com.mmr.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.mmr.core.database.entity.LanSyncLogEntryEntity
import com.mmr.core.database.entity.PendingLanPairRequestEntity
import com.mmr.core.database.entity.TrustedLanDeviceEntity

@Dao
interface LanSyncDao {
    @Query("SELECT * FROM trusted_lan_devices ORDER BY displayName")
    suspend fun getTrustedDevices(): List<TrustedLanDeviceEntity>

    @Query("SELECT * FROM trusted_lan_devices WHERE remoteDeviceId = :deviceId LIMIT 1")
    suspend fun getTrustedDevice(deviceId: String): TrustedLanDeviceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTrustedDevice(device: TrustedLanDeviceEntity)

    @Query("DELETE FROM trusted_lan_devices WHERE remoteDeviceId = :deviceId")
    suspend fun deleteTrustedDevice(deviceId: String)

    @Query("SELECT * FROM pending_lan_pair_requests ORDER BY requestedAtMillis DESC")
    suspend fun getPendingPairRequests(): List<PendingLanPairRequestEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPendingPairRequest(request: PendingLanPairRequestEntity)

    @Query("DELETE FROM pending_lan_pair_requests WHERE remoteDeviceId = :deviceId")
    suspend fun deletePendingPairRequest(deviceId: String)

    @Query("DELETE FROM pending_lan_pair_requests")
    suspend fun clearPendingPairRequests()

    @Transaction
    suspend fun approvePairRequest(request: PendingLanPairRequestEntity) {
        upsertTrustedDevice(
            TrustedLanDeviceEntity(
                remoteDeviceId = request.remoteDeviceId,
                displayName = request.displayName,
                theirAuthToken = request.theirAuthToken,
                pairedAtMillis = System.currentTimeMillis(),
            ),
        )
        deletePendingPairRequest(request.remoteDeviceId)
    }

    @Query("SELECT * FROM lan_sync_log_entries ORDER BY completedAtMillis DESC LIMIT 10")
    suspend fun getRecentSyncLog(): List<LanSyncLogEntryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSyncLogEntry(entry: LanSyncLogEntryEntity)

    @Query("DELETE FROM lan_sync_log_entries")
    suspend fun clearSyncLog()

    @Query("SELECT COUNT(*) FROM lan_sync_log_entries")
    suspend fun syncLogCount(): Int

    @Query(
        """
        DELETE FROM lan_sync_log_entries
        WHERE id IN (
            SELECT id FROM lan_sync_log_entries
            ORDER BY completedAtMillis ASC
            LIMIT :count
        )
        """,
    )
    suspend fun deleteOldestSyncLogEntries(count: Int)

    @Transaction
    suspend fun appendSyncLogEntry(entry: LanSyncLogEntryEntity) {
        insertSyncLogEntry(entry)
        val excess = syncLogCount() - 10
        if (excess > 0) {
            deleteOldestSyncLogEntries(excess)
        }
    }
}
