package com.mmr.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.mmr.core.database.entity.SyncTombstoneEntity

@Dao
interface SyncTombstoneDao {
    @Query("SELECT * FROM sync_tombstones")
    suspend fun getAll(): List<SyncTombstoneEntity>

    @Query("SELECT * FROM sync_tombstones WHERE deletedAt > :afterExclusive ORDER BY deletedAt ASC")
    suspend fun getDeletedAfter(afterExclusive: Long): List<SyncTombstoneEntity>

    @Query("SELECT * FROM sync_tombstones WHERE syncId = :syncId LIMIT 1")
    suspend fun getBySyncId(syncId: String): SyncTombstoneEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: SyncTombstoneEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<SyncTombstoneEntity>)

    @Query("DELETE FROM sync_tombstones WHERE syncId = :syncId")
    suspend fun deleteBySyncId(syncId: String)

    @Query("DELETE FROM sync_tombstones WHERE deletedAt <= :cutoffInclusive")
    suspend fun deleteWhereDeletedAtLte(cutoffInclusive: Long)

    @Query("DELETE FROM sync_tombstones")
    suspend fun deleteAll()

    @Transaction
    suspend fun replaceAll(entities: List<SyncTombstoneEntity>) {
        deleteAll()
        if (entities.isNotEmpty()) {
            upsertAll(entities)
        }
    }
}
