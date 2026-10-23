package com.mmr.app.di

import android.content.Context
import androidx.room.Room
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
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Singleton

/**
 * In-memory Room for instrumented tests. Replaces production [DatabaseModule] on @HiltAndroidTest classes.
 */
@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [DatabaseModule::class],
)
object TestDatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
    ): MmrDatabase {
        val database =
            Room
                .inMemoryDatabaseBuilder(context, MmrDatabase::class.java)
                .allowMainThreadQueries()
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
        runBlocking {
            MmrDefaultCatalogSeeder.seedIfMissing(database)
            MmrCategorizationRuleSeeder.seedIfMissing(context, database.categorizationRuleDao())
            if (database
                    .accountDao()
                    .getAccounts()
                    .first()
                    .isEmpty()
            ) {
                database.accountDao().insertAccounts(
                    listOf(
                        AccountEntity(name = "Cash", type = AccountType.CASH, isDefault = true),
                        AccountEntity(name = "Main Bank Account", type = AccountType.BANK, isDefault = false),
                    ),
                )
            }
        }
        return database
    }

    @Provides
    fun provideTransactionDao(database: MmrDatabase): TransactionDao = database.transactionDao()

    @Provides
    fun provideCategoryDao(database: MmrDatabase): CategoryDao = database.categoryDao()

    @Provides
    fun provideAccountDao(database: MmrDatabase): AccountDao = database.accountDao()

    @Provides
    fun provideSmsMessageRecordDao(database: MmrDatabase) = database.smsMessageRecordDao()

    @Provides
    fun provideTransactionApprovalDao(database: MmrDatabase) = database.transactionApprovalDao()

    @Provides
    fun provideApprovalLedgerDao(database: MmrDatabase) = database.approvalLedgerDao()

    @Provides
    fun provideLoanDao(database: MmrDatabase) = database.loanDao()

    @Provides
    fun provideLoanPaymentDao(database: MmrDatabase) = database.loanPaymentDao()

    @Provides
    fun provideLoanInterestRevisionDao(database: MmrDatabase) = database.loanInterestRevisionDao()

    @Provides
    fun provideLoanScheduleDao(database: MmrDatabase) = database.loanScheduleDao()

    @Provides
    fun provideRecurringRuleDao(database: MmrDatabase) = database.recurringRuleDao()

    @Provides
    fun provideNotificationRuleDao(database: MmrDatabase) = database.notificationRuleDao()

    @Provides
    fun provideBackupDao(database: MmrDatabase) = database.backupDao()

    @Provides
    fun provideCategorizationRuleDao(database: MmrDatabase) = database.categorizationRuleDao()

    @Provides
    fun provideLanSyncDao(database: MmrDatabase) = database.lanSyncDao()
}
