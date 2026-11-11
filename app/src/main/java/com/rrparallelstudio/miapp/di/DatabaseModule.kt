package com.rrparallelstudio.miapp.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.rrparallelstudio.miapp.core.data.catalog.MmrBuiltInAccounts
import com.rrparallelstudio.miapp.core.data.catalog.MmrCategorizationRuleSeeder
import com.rrparallelstudio.miapp.core.data.catalog.MmrDefaultCatalogSeeder
import com.rrparallelstudio.miapp.core.database.MmrDatabase
import com.rrparallelstudio.miapp.core.database.MmrMigrations
import com.rrparallelstudio.miapp.core.database.dao.AccountDao
import com.rrparallelstudio.miapp.core.database.dao.BudgetDao
import com.rrparallelstudio.miapp.core.database.dao.CategoryDao
import com.rrparallelstudio.miapp.core.database.dao.TransactionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Provider
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
        databaseProvider: Provider<MmrDatabase>,
    ): MmrDatabase =
        Room
            .databaseBuilder(
                context,
                MmrDatabase::class.java,
                "mmr_database.db",
            ).addMigrations(
                MmrMigrations.MIGRATION_26_27,
                MmrMigrations.MIGRATION_27_28,
                MmrMigrations.MIGRATION_28_29,
            ).fallbackToDestructiveMigration(dropAllTables = true)
            // Pre-release: no shipped DBs; bump [MmrDatabase.SCHEMA_VERSION] without migration classes.
            .addCallback(
                object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        CoroutineScope(Dispatchers.IO).launch {
                            val database = databaseProvider.get()
                            MmrDefaultCatalogSeeder.seedIfMissing(database)
                            MmrCategorizationRuleSeeder.seedIfMissing(context, database.categorizationRuleDao())
                            database.accountDao().insertAccounts(
                                listOf(MmrBuiltInAccounts.defaultAccount()),
                            )
                        }
                    }
                },
            ).build()

    @Provides
    fun provideOwnerProfileDao(database: MmrDatabase): com.rrparallelstudio.miapp.core.database.dao.OwnerProfileDao =
        database.ownerProfileDao()

    @Provides
    fun provideTransactionDao(database: MmrDatabase): TransactionDao = database.transactionDao()

    @Provides
    fun provideTransactionAttachmentDao(
        database: MmrDatabase,
    ): com.rrparallelstudio.miapp.core.database.dao.TransactionAttachmentDao = database.transactionAttachmentDao()

    @Provides
    fun provideCategoryDao(database: MmrDatabase): CategoryDao = database.categoryDao()

    @Provides
    fun provideBudgetDao(database: MmrDatabase): BudgetDao = database.budgetDao()

    @Provides
    fun provideAccountDao(database: MmrDatabase): AccountDao = database.accountDao()

    @Provides
    fun provideSmsMessageRecordDao(
        database: MmrDatabase,
    ): com.rrparallelstudio.miapp.core.database.dao.SmsMessageRecordDao = database.smsMessageRecordDao()

    @Provides
    fun provideSmsAccountOverrideDao(
        database: MmrDatabase,
    ): com.rrparallelstudio.miapp.core.database.dao.SmsAccountOverrideDao = database.smsAccountOverrideDao()

    @Provides
    fun provideTransactionApprovalDao(
        database: MmrDatabase,
    ): com.rrparallelstudio.miapp.core.database.dao.TransactionApprovalDao = database.transactionApprovalDao()

    @Provides
    fun provideApprovalLedgerDao(
        database: MmrDatabase,
    ): com.rrparallelstudio.miapp.core.database.dao.ApprovalLedgerDao = database.approvalLedgerDao()

    @Provides
    fun provideLoanDao(database: MmrDatabase): com.rrparallelstudio.miapp.core.database.dao.LoanDao = database.loanDao()

    @Provides
    fun provideLoanPaymentDao(database: MmrDatabase): com.rrparallelstudio.miapp.core.database.dao.LoanPaymentDao =
        database.loanPaymentDao()

    @Provides
    fun provideLoanInterestRevisionDao(
        database: MmrDatabase,
    ): com.rrparallelstudio.miapp.core.database.dao.LoanInterestRevisionDao = database.loanInterestRevisionDao()

    @Provides
    fun provideLoanScheduleDao(database: MmrDatabase): com.rrparallelstudio.miapp.core.database.dao.LoanScheduleDao =
        database.loanScheduleDao()

    @Provides
    fun provideRecurringRuleDao(database: MmrDatabase): com.rrparallelstudio.miapp.core.database.dao.RecurringRuleDao =
        database.recurringRuleDao()

    @Provides
    fun provideNotificationRuleDao(
        database: MmrDatabase,
    ): com.rrparallelstudio.miapp.core.database.dao.NotificationRuleDao = database.notificationRuleDao()

    @Provides
    fun provideBackupDao(database: MmrDatabase): com.rrparallelstudio.miapp.core.database.dao.BackupDao =
        database.backupDao()

    @Provides
    fun provideCategorizationRuleDao(
        database: MmrDatabase,
    ): com.rrparallelstudio.miapp.core.database.dao.CategorizationRuleDao = database.categorizationRuleDao()

    @Provides
    fun provideLanSyncDao(database: MmrDatabase): com.rrparallelstudio.miapp.core.database.dao.LanSyncDao =
        database.lanSyncDao()

    @Provides
    fun provideSyncTombstoneDao(database: MmrDatabase): com.rrparallelstudio.miapp.core.database.dao.SyncTombstoneDao =
        database.syncTombstoneDao()

    @Provides
    fun provideLanSyncTombstoneAckDao(
        database: MmrDatabase,
    ): com.rrparallelstudio.miapp.core.database.dao.LanSyncTombstoneAckDao = database.lanSyncTombstoneAckDao()

    @Provides
    fun provideAssetDao(database: MmrDatabase): com.rrparallelstudio.miapp.core.database.dao.AssetDao =
        database.assetDao()

    @Provides
    fun provideAssetValuationDao(
        database: MmrDatabase,
    ): com.rrparallelstudio.miapp.core.database.dao.AssetValuationDao = database.assetValuationDao()

    @Provides
    fun provideAssetAttachmentDao(
        database: MmrDatabase,
    ): com.rrparallelstudio.miapp.core.database.dao.AssetAttachmentDao = database.assetAttachmentDao()
}
