package com.mmr.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.mmr.core.model.enums.AssetStatus

@Entity(
    tableName = "assets",
    foreignKeys = [
        ForeignKey(
            entity = LoanEntity::class,
            parentColumns = ["id"],
            childColumns = ["linkedLoanId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index(value = ["status"]),
        Index(value = ["assetType"]),
        Index(value = ["linkedLoanId"]),
        Index(value = ["syncId"], unique = true),
    ],
)
data class AssetEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val syncId: String =
        com.mmr.core.common.sync.MmrSyncId
            .newId(),
    val name: String,
    /** User-defined label (e.g. Real Estate, Vehicle). Legacy rows may store old enum names. */
    val assetType: String = "",
    val status: AssetStatus = AssetStatus.ACTIVE,
    val isInherited: Boolean = false,
    val purchaseDateMillis: Long? = null,
    /** Cost basis at acquisition; may differ from first valuation when unknown. */
    val purchasePrice: Double? = null,
    val currentValue: Double = 0.0,
    val ownedByName: String? = null,
    val location: String? = null,
    val identifier: String? = null,
    val linkedLoanId: Long? = null,
    val nextValuationDueMillis: Long? = null,
    /** Days before [nextValuationDueMillis] to fire [com.mmr.core.model.enums.NotificationRuleType.ASSET_VALUATION_REMINDER]. */
    val valuationReminderDaysBefore: Int = 0,
    val saleDateMillis: Long? = null,
    val salePrice: Double? = null,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "asset_valuations",
    foreignKeys = [
        ForeignKey(
            entity = AssetEntity::class,
            parentColumns = ["id"],
            childColumns = ["assetId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["assetId"]),
        Index(value = ["recordedAt"]),
        Index(value = ["syncId"], unique = true),
    ],
)
data class AssetValuationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val syncId: String =
        com.mmr.core.common.sync.MmrSyncId
            .newId(),
    val assetId: Long,
    val amount: Double,
    val recordedAt: Long = System.currentTimeMillis(),
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "asset_attachments",
    foreignKeys = [
        ForeignKey(
            entity = AssetEntity::class,
            parentColumns = ["id"],
            childColumns = ["assetId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["assetId"]),
        Index(value = ["syncId"], unique = true),
    ],
)
data class AssetAttachmentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val syncId: String =
        com.mmr.core.common.sync.MmrSyncId
            .newId(),
    val assetId: Long,
    val mimeType: String,
    val fileSizeBytes: Long,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)
