package com.mmr.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
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
}
