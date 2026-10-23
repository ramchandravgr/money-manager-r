package com.mmr.app.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.mmr.core.data.catalog.MmrCategorizationRuleSeeder
import com.mmr.core.data.catalog.MmrDefaultCatalogSeeder
import com.mmr.core.database.MmrDatabase
import com.mmr.core.database.dao.AccountDao
import com.mmr.core.database.dao.CategoryDao
import com.mmr.core.database.dao.TransactionDao
import com.mmr.core.database.entity.AccountEntity
import com.mmr.core.model.enums.AccountType
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
            ).fallbackToDestructiveMigration(dropAllTables = true)
            .addCallback(
                object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        CoroutineScope(Dispatchers.IO).launch {
                            val database = databaseProvider.get()
                            MmrDefaultCatalogSeeder.seedIfMissing(database)
                            MmrCategorizationRuleSeeder.seedIfMissing(context, database.categorizationRuleDao())
                            database.accountDao().insertAccounts(
                                listOf(
                                    AccountEntity(name = "Cash", type = AccountType.CASH, isDefault = true),
                                    AccountEntity(
                                        name = "Main Bank Account",
                                        type = AccountType.BANK,
                                        isDefault = false,
                                    ),
                                ),
                            )
                        }
                    }
                },
            ).build()

    @Provides
    fun provideTransactionDao(database: MmrDatabase): TransactionDao = database.transactionDao()

    @Provides
    fun provideCategoryDao(database: MmrDatabase): CategoryDao = database.categoryDao()

    @Provides
    fun provideAccountDao(database: MmrDatabase): AccountDao = database.accountDao()

    @Provides
    fun provideSmsMessageRecordDao(database: MmrDatabase): com.mmr.core.database.dao.SmsMessageRecordDao =
        database.smsMessageRecordDao()

    @Provides
    fun provideTransactionApprovalDao(database: MmrDatabase): com.mmr.core.database.dao.TransactionApprovalDao =
        database.transactionApprovalDao()

    @Provides
    fun provideApprovalLedgerDao(database: MmrDatabase): com.mmr.core.database.dao.ApprovalLedgerDao =
        database.approvalLedgerDao()

    @Provides
    fun provideLoanDao(database: MmrDatabase): com.mmr.core.database.dao.LoanDao = database.loanDao()

    @Provides
    fun provideLoanPaymentDao(database: MmrDatabase): com.mmr.core.database.dao.LoanPaymentDao =
        database.loanPaymentDao()

    @Provides
    fun provideLoanInterestRevisionDao(database: MmrDatabase): com.mmr.core.database.dao.LoanInterestRevisionDao =
        database.loanInterestRevisionDao()

    @Provides
    fun provideLoanScheduleDao(database: MmrDatabase): com.mmr.core.database.dao.LoanScheduleDao =
        database.loanScheduleDao()

    @Provides
    fun provideRecurringRuleDao(database: MmrDatabase): com.mmr.core.database.dao.RecurringRuleDao =
        database.recurringRuleDao()

    @Provides
    fun provideNotificationRuleDao(database: MmrDatabase): com.mmr.core.database.dao.NotificationRuleDao =
        database.notificationRuleDao()

    @Provides
    fun provideBackupDao(database: MmrDatabase): com.mmr.core.database.dao.BackupDao = database.backupDao()

    @Provides
    fun provideCategorizationRuleDao(database: MmrDatabase): com.mmr.core.database.dao.CategorizationRuleDao =
        database.categorizationRuleDao()

    @Provides
    fun provideLanSyncDao(database: MmrDatabase): com.mmr.core.database.dao.LanSyncDao = database.lanSyncDao()
}
