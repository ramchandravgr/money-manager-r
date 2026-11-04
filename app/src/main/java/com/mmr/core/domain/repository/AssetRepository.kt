package com.mmr.core.domain.repository

import com.mmr.core.database.entity.AssetEntity
import com.mmr.core.database.entity.AssetValuationEntity
import com.mmr.core.database.relation.AssetWithValuations
import kotlinx.coroutines.flow.Flow

interface AssetRepository {
    fun getAllAssets(): Flow<List<AssetEntity>>

    fun getAssetsWithValuations(): Flow<List<AssetWithValuations>>

    fun getAssetWithValuationsById(id: Long): Flow<AssetWithValuations?>

    suspend fun getAssetById(id: Long): AssetEntity?

    suspend fun createAsset(
        asset: AssetEntity,
        initialValuationAmount: Double,
        initialValuationDate: Long,
        initialValuationNote: String? = null,
    ): Long

    suspend fun updateAsset(asset: AssetEntity)

    suspend fun deleteAsset(asset: AssetEntity)

    fun getValuationsForAsset(assetId: Long): Flow<List<AssetValuationEntity>>

    suspend fun recordValuation(
        assetId: Long,
        amount: Double,
        recordedAt: Long = System.currentTimeMillis(),
        notes: String? = null,
    ): Long

    suspend fun syncValuationReminder(assetId: Long)

    suspend fun reconcileAllAssetValuationReminders()
}
