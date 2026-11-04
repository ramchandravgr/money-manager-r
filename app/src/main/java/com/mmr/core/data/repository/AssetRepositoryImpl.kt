package com.mmr.core.data.repository

import androidx.room.withTransaction
import com.mmr.core.asset.AssetValuationReminderNotificationSync
import com.mmr.core.database.MmrDatabase
import com.mmr.core.database.dao.AssetDao
import com.mmr.core.database.dao.AssetValuationDao
import com.mmr.core.database.dao.NotificationRuleDao
import com.mmr.core.database.entity.AssetEntity
import com.mmr.core.database.entity.AssetValuationEntity
import com.mmr.core.database.relation.AssetWithValuations
import com.mmr.core.domain.repository.AssetRepository
import com.mmr.core.lansync.LanSyncTombstoneRecorder
import com.mmr.core.lansync.SyncEntityType
import com.mmr.core.model.enums.AssetStatus
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AssetRepositoryImpl
    @Inject
    constructor(
        private val database: MmrDatabase,
        private val assetDao: AssetDao,
        private val assetValuationDao: AssetValuationDao,
        private val notificationRuleDao: NotificationRuleDao,
        private val tombstoneRecorder: LanSyncTombstoneRecorder,
    ) : AssetRepository {
        override fun getAllAssets(): Flow<List<AssetEntity>> = assetDao.getAllAssets()

        override fun getAssetsWithValuations(): Flow<List<AssetWithValuations>> = assetDao.getAssetsWithValuations()

        override fun getAssetWithValuationsById(id: Long): Flow<AssetWithValuations?> =
            assetDao.getAssetWithValuationsById(id)

        override suspend fun getAssetById(id: Long): AssetEntity? = assetDao.getById(id)

        override suspend fun createAsset(
            asset: AssetEntity,
            initialValuationAmount: Double,
            initialValuationDate: Long,
            initialValuationNote: String?,
        ): Long {
            val assetId =
                database.withTransaction {
                    val now = System.currentTimeMillis()
                    val toInsert =
                        asset.copy(
                            currentValue = initialValuationAmount,
                            createdAt = now,
                            updatedAt = now,
                        )
                    val newId = assetDao.insert(toInsert)
                    assetValuationDao.insert(
                        AssetValuationEntity(
                            assetId = newId,
                            amount = initialValuationAmount,
                            recordedAt = initialValuationDate,
                            notes = initialValuationNote,
                            createdAt = now,
                        ),
                    )
                    newId
                }
            syncValuationReminder(assetId)
            return assetId
        }

        override suspend fun updateAsset(asset: AssetEntity) {
            val now = System.currentTimeMillis()
            assetDao.update(asset.copy(updatedAt = now))
            syncValuationReminder(asset.id)
        }

        override suspend fun deleteAsset(asset: AssetEntity) {
            notificationRuleDao.listForAsset(asset.id).forEach { notification ->
                tombstoneRecorder.recordDeletion(notification.syncId, SyncEntityType.NOTIFICATION_RULE)
            }
            notificationRuleDao.deleteForAsset(asset.id)
            tombstoneRecorder.recordDeletion(asset.syncId, SyncEntityType.ASSET)
            assetDao.delete(asset)
        }

        override fun getValuationsForAsset(assetId: Long): Flow<List<AssetValuationEntity>> =
            assetValuationDao.getForAsset(assetId)

        override suspend fun recordValuation(
            assetId: Long,
            amount: Double,
            recordedAt: Long,
            notes: String?,
        ): Long {
            val now = System.currentTimeMillis()
            val valuationId =
                database.withTransaction {
                    val id =
                        assetValuationDao.insert(
                            AssetValuationEntity(
                                assetId = assetId,
                                amount = amount,
                                recordedAt = recordedAt,
                                notes = notes,
                                createdAt = now,
                            ),
                        )
                    val existing = assetDao.getById(assetId) ?: return@withTransaction id
                    assetDao.update(
                        existing.copy(
                            currentValue = amount,
                            updatedAt = now,
                        ),
                    )
                    id
                }
            syncValuationReminder(assetId)
            return valuationId
        }

        override suspend fun syncValuationReminder(assetId: Long) {
            val asset = assetDao.getById(assetId) ?: return
            AssetValuationReminderNotificationSync.syncForAsset(
                asset,
                notificationRuleDao,
                tombstoneRecorder,
            )
        }

        override suspend fun reconcileAllAssetValuationReminders() {
            for (asset in assetDao.getAllAssetsList()) {
                if (asset.status == AssetStatus.ACTIVE) {
                    syncValuationReminder(asset.id)
                }
            }
        }
    }
