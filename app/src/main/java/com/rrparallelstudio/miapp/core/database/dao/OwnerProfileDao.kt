package com.rrparallelstudio.miapp.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.rrparallelstudio.miapp.core.database.entity.OwnerProfileEntity

@Dao
interface OwnerProfileDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: OwnerProfileEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<OwnerProfileEntity>)

    @Query("SELECT * FROM owner_profiles WHERE profileId = :profileId LIMIT 1")
    suspend fun getById(profileId: String): OwnerProfileEntity?

    @Query("SELECT * FROM owner_profiles")
    suspend fun getAllOnce(): List<OwnerProfileEntity>

    @Query("SELECT COUNT(*) FROM owner_profiles")
    suspend fun countOnce(): Int
}
