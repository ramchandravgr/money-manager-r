package com.rrparallelstudio.miapp.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.rrparallelstudio.miapp.core.database.converter.EnumConverters
import com.rrparallelstudio.miapp.core.database.dao.AccountDao
import com.rrparallelstudio.miapp.core.database.dao.ApprovalLedgerDao
import com.rrparallelstudio.miapp.core.database.dao.BackupDao
import com.rrparallelstudio.miapp.core.database.dao.BudgetDao
import com.rrparallelstudio.miapp.core.database.dao.CategorizationRuleDao
import com.rrparallelstudio.miapp.core.database.dao.CategoryDao
import com.rrparallelstudio.miapp.core.database.dao.LanSyncDao
import com.rrparallelstudio.miapp.core.database.dao.LanSyncTombstoneAckDao
import com.rrparallelstudio.miapp.core.database.dao.LoanDao
import com.rrparallelstudio.miapp.core.database.dao.LoanInterestRevisionDao
import com.rrparallelstudio.miapp.core.database.dao.LoanPaymentDao
import com.rrparallelstudio.miapp.core.database.dao.LoanScheduleDao
import com.rrparallelstudio.miapp.core.database.dao.NotificationRuleDao
import com.rrparallelstudio.miapp.core.database.dao.RecurringRuleDao
import com.rrparallelstudio.miapp.core.database.dao.SmsAccountOverrideDao
import com.rrparallelstudio.miapp.core.database.dao.SmsMessageRecordDao
import com.rrparallelstudio.miapp.core.database.dao.SyncTombstoneDao
import com.rrparallelstudio.miapp.core.database.dao.TransactionApprovalDao
import com.rrparallelstudio.miapp.core.database.dao.TransactionAttachmentDao
import com.rrparallelstudio.miapp.core.database.dao.TransactionDao
import com.rrparallelstudio.miapp.core.database.entity.AccountEntity
import com.rrparallelstudio.miapp.core.database.entity.AssetAttachmentEntity
import com.rrparallelstudio.miapp.core.database.entity.AssetEntity
import com.rrparallelstudio.miapp.core.database.entity.AssetValuationEntity
import com.rrparallelstudio.miapp.core.database.entity.BudgetEntity
import com.rrparallelstudio.miapp.core.database.entity.CategorizationRuleEntity
import com.rrparallelstudio.miapp.core.database.entity.CategoryEntity
import com.rrparallelstudio.miapp.core.database.entity.LanSyncLogEntryEntity
import com.rrparallelstudio.miapp.core.database.entity.LanSyncTombstonePeerAckEntity
import com.rrparallelstudio.miapp.core.database.entity.LoanEntity
import com.rrparallelstudio.miapp.core.database.entity.LoanInterestRevisionEntity
import com.rrparallelstudio.miapp.core.database.entity.LoanPaymentEntity
import com.rrparallelstudio.miapp.core.database.entity.LoanScheduleEntryEntity
import com.rrparallelstudio.miapp.core.database.entity.NotificationRuleEntity
import com.rrparallelstudio.miapp.core.database.entity.OwnerProfileEntity
import com.rrparallelstudio.miapp.core.database.entity.PendingLanPairRequestEntity
import com.rrparallelstudio.miapp.core.database.entity.RecurringRuleEntity
import com.rrparallelstudio.miapp.core.database.entity.SmsAccountOverrideEntity
import com.rrparallelstudio.miapp.core.database.entity.SmsMessageRecordEntity
import com.rrparallelstudio.miapp.core.database.entity.SubcategoryEntity
import com.rrparallelstudio.miapp.core.database.entity.SyncTombstoneEntity
import com.rrparallelstudio.miapp.core.database.entity.TransactionApprovalEntity
import com.rrparallelstudio.miapp.core.database.entity.TransactionAttachmentEntity
import com.rrparallelstudio.miapp.core.database.entity.TransactionEntity
import com.rrparallelstudio.miapp.core.database.entity.TrustedLanDeviceEntity

@Database(
    entities = [
        CategoryEntity::class,
        SubcategoryEntity::class,
        AccountEntity::class,
        LoanEntity::class,
        LoanPaymentEntity::class,
        LoanInterestRevisionEntity::class,
        LoanScheduleEntryEntity::class,
        RecurringRuleEntity::class,
        NotificationRuleEntity::class,
        TransactionEntity::class,
        TransactionAttachmentEntity::class,
        SmsMessageRecordEntity::class,
        TransactionApprovalEntity::class,
        CategorizationRuleEntity::class,
        TrustedLanDeviceEntity::class,
        PendingLanPairRequestEntity::class,
        LanSyncLogEntryEntity::class,
        SyncTombstoneEntity::class,
        LanSyncTombstonePeerAckEntity::class,
        BudgetEntity::class,
        AssetEntity::class,
        AssetValuationEntity::class,
        AssetAttachmentEntity::class,
        OwnerProfileEntity::class,
        SmsAccountOverrideEntity::class,
    ],
    version = 29,
    exportSchema = false,
)
@TypeConverters(EnumConverters::class)
abstract class MmrDatabase : RoomDatabase() {
    companion object {
        const val SCHEMA_VERSION = 29
    }

    abstract fun transactionDao(): TransactionDao

    abstract fun transactionAttachmentDao(): TransactionAttachmentDao

    abstract fun categoryDao(): CategoryDao

    abstract fun accountDao(): AccountDao

    abstract fun loanDao(): LoanDao

    abstract fun loanPaymentDao(): LoanPaymentDao

    abstract fun loanInterestRevisionDao(): LoanInterestRevisionDao

    abstract fun loanScheduleDao(): LoanScheduleDao

    abstract fun recurringRuleDao(): RecurringRuleDao

    abstract fun notificationRuleDao(): NotificationRuleDao

    abstract fun smsMessageRecordDao(): SmsMessageRecordDao

    abstract fun transactionApprovalDao(): TransactionApprovalDao

    abstract fun approvalLedgerDao(): ApprovalLedgerDao

    abstract fun backupDao(): BackupDao

    abstract fun categorizationRuleDao(): CategorizationRuleDao

    abstract fun lanSyncDao(): LanSyncDao

    abstract fun syncTombstoneDao(): SyncTombstoneDao

    abstract fun lanSyncTombstoneAckDao(): LanSyncTombstoneAckDao

    abstract fun budgetDao(): BudgetDao

    abstract fun assetDao(): com.rrparallelstudio.miapp.core.database.dao.AssetDao

    abstract fun assetValuationDao(): com.rrparallelstudio.miapp.core.database.dao.AssetValuationDao

    abstract fun assetAttachmentDao(): com.rrparallelstudio.miapp.core.database.dao.AssetAttachmentDao

    abstract fun ownerProfileDao(): com.rrparallelstudio.miapp.core.database.dao.OwnerProfileDao

    abstract fun smsAccountOverrideDao(): SmsAccountOverrideDao
}
