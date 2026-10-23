package com.mmr.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.mmr.core.database.converter.EnumConverters
import com.mmr.core.database.dao.AccountDao
import com.mmr.core.database.dao.ApprovalLedgerDao
import com.mmr.core.database.dao.BackupDao
import com.mmr.core.database.dao.CategorizationRuleDao
import com.mmr.core.database.dao.CategoryDao
import com.mmr.core.database.dao.LanSyncDao
import com.mmr.core.database.dao.LoanDao
import com.mmr.core.database.dao.LoanInterestRevisionDao
import com.mmr.core.database.dao.LoanPaymentDao
import com.mmr.core.database.dao.LoanScheduleDao
import com.mmr.core.database.dao.NotificationRuleDao
import com.mmr.core.database.dao.RecurringRuleDao
import com.mmr.core.database.dao.SmsMessageRecordDao
import com.mmr.core.database.dao.TransactionApprovalDao
import com.mmr.core.database.dao.TransactionDao
import com.mmr.core.database.entity.AccountEntity
import com.mmr.core.database.entity.CategorizationRuleEntity
import com.mmr.core.database.entity.CategoryEntity
import com.mmr.core.database.entity.LoanEntity
import com.mmr.core.database.entity.LoanInterestRevisionEntity
import com.mmr.core.database.entity.LoanPaymentEntity
import com.mmr.core.database.entity.LoanScheduleEntryEntity
import com.mmr.core.database.entity.NotificationRuleEntity
import com.mmr.core.database.entity.PendingLanPairRequestEntity
import com.mmr.core.database.entity.RecurringRuleEntity
import com.mmr.core.database.entity.SmsMessageRecordEntity
import com.mmr.core.database.entity.SubcategoryEntity
import com.mmr.core.database.entity.TransactionApprovalEntity
import com.mmr.core.database.entity.TransactionEntity
import com.mmr.core.database.entity.TrustedLanDeviceEntity

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
        SmsMessageRecordEntity::class,
        TransactionApprovalEntity::class,
        CategorizationRuleEntity::class,
        TrustedLanDeviceEntity::class,
        PendingLanPairRequestEntity::class,
    ],
    version = 11,
    exportSchema = false,
)
@TypeConverters(EnumConverters::class)
abstract class MmrDatabase : RoomDatabase() {
    companion object {
        const val SCHEMA_VERSION = 11
    }

    abstract fun transactionDao(): TransactionDao

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
}
