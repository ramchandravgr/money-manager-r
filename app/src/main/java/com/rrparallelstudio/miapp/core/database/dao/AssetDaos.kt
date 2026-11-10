package com.rrparallelstudio.miapp.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.rrparallelstudio.miapp.core.database.entity.AssetAttachmentEntity
import com.rrparallelstudio.miapp.core.database.entity.AssetEntity
import com.rrparallelstudio.miapp.core.database.entity.AssetValuationEntity
import com.rrparallelstudio.miapp.core.database.relation.AssetWithValuations
import kotlinx.coroutines.flow.Flow

@Dao
interface AssetDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(asset: AssetEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAssets(assets: List<AssetEntity>)

    @Update
    suspend fun update(asset: AssetEntity)

    @Delete
    suspend fun delete(asset: AssetEntity)

    @Query("SELECT * FROM assets WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): AssetEntity?

    @Query("SELECT * FROM assets WHERE syncId = :syncId LIMIT 1")
    suspend fun getBySyncId(syncId: String): AssetEntity?

    @Query("SELECT * FROM assets WHERE id = :id LIMIT 1")
    fun getByIdFlow(id: Long): Flow<AssetEntity?>

    @Query("SELECT * FROM assets ORDER BY updatedAt DESC")
    fun getAllAssets(): Flow<List<AssetEntity>>

    @Query("SELECT * FROM assets ORDER BY updatedAt DESC")
    suspend fun getAllAssetsList(): List<AssetEntity>

    @Transaction
    @Query("SELECT * FROM assets WHERE id = :id LIMIT 1")
    fun getAssetWithValuationsById(id: Long): Flow<AssetWithValuations?>

    @Transaction
    @Query("SELECT * FROM assets ORDER BY updatedAt DESC")
    fun getAssetsWithValuations(): Flow<List<AssetWithValuations>>
}

@Dao
interface AssetValuationDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(valuation: AssetValuationEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertValuations(valuations: List<AssetValuationEntity>)

    @Query("SELECT * FROM asset_valuations WHERE assetId = :assetId ORDER BY recordedAt DESC, id DESC")
    fun getForAsset(assetId: Long): Flow<List<AssetValuationEntity>>

    @Query("SELECT * FROM asset_valuations WHERE assetId = :assetId ORDER BY recordedAt DESC, id DESC LIMIT 1")
    suspend fun getLatestForAsset(assetId: Long): AssetValuationEntity?

    @Query("DELETE FROM asset_valuations WHERE assetId = :assetId")
    suspend fun deleteForAsset(assetId: Long)

    @Query("SELECT * FROM asset_valuations WHERE syncId = :syncId LIMIT 1")
    suspend fun getBySyncId(syncId: String): AssetValuationEntity?

    @Delete
    suspend fun delete(valuation: AssetValuationEntity)
}

@Dao
interface AssetAttachmentDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(attachment: AssetAttachmentEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttachments(attachments: List<AssetAttachmentEntity>)

    @Query("SELECT * FROM asset_attachments WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): AssetAttachmentEntity?

    @Query("SELECT * FROM asset_attachments WHERE assetId = :assetId ORDER BY createdAt ASC")
    suspend fun getByAssetId(assetId: Long): List<AssetAttachmentEntity>

    @Query("DELETE FROM asset_attachments WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM asset_attachments WHERE assetId = :assetId")
    suspend fun deleteByAssetId(assetId: Long)

    @Query("SELECT * FROM asset_attachments WHERE syncId = :syncId LIMIT 1")
    suspend fun getBySyncId(syncId: String): AssetAttachmentEntity?

    @Update
    suspend fun update(attachment: AssetAttachmentEntity)
}
