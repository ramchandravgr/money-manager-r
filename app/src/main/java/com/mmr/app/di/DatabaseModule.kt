package com.mmr.app.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.mmr.core.database.MmrDatabase
import com.mmr.core.database.dao.AccountDao
import com.mmr.core.database.dao.CategoryDao
import com.mmr.core.database.dao.TransactionDao
import com.mmr.core.database.entity.AccountEntity
import com.mmr.core.database.entity.CategoryEntity
import com.mmr.core.model.enums.AccountType
import com.mmr.core.model.enums.CategoryType
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
        databaseProvider: Provider<MmrDatabase>
    ): MmrDatabase {
        return Room.databaseBuilder(
            context,
            MmrDatabase::class.java,
            "mmr_database.db"
        ).fallbackToDestructiveMigration(dropAllTables = true)
        .addCallback(object : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                CoroutineScope(Dispatchers.IO).launch {
                    val database = databaseProvider.get()
                    database.categoryDao().insertCategories(
                        listOf(
                            CategoryEntity(name = "Food & Dining", iconName = "restaurant", colorHex = "#EF4444", type = CategoryType.EXPENSE, isDefault = true),
                            CategoryEntity(name = "Groceries", iconName = "shopping_cart", colorHex = "#F59E0B", type = CategoryType.EXPENSE, isDefault = true),
                            CategoryEntity(name = "Transport & Fuel", iconName = "directions_car", colorHex = "#3B82F6", type = CategoryType.EXPENSE, isDefault = true),
                            CategoryEntity(name = "Shopping", iconName = "shopping_bag", colorHex = "#EC4899", type = CategoryType.EXPENSE, isDefault = true),
                            CategoryEntity(name = "Bills & Utilities", iconName = "receipt_long", colorHex = "#8B5CF6", type = CategoryType.EXPENSE, isDefault = true),
                            CategoryEntity(name = "Salary", iconName = "payments", colorHex = "#10B981", type = CategoryType.INCOME, isDefault = true),
                            CategoryEntity(name = "Investment / Return", iconName = "trending_up", colorHex = "#06B6D4", type = CategoryType.INCOME, isDefault = true)
                        )
                    )
                    database.accountDao().insertAccounts(
                        listOf(
                            AccountEntity(name = "Cash", type = AccountType.CASH, isDefault = true),
                            AccountEntity(name = "Main Bank Account", type = AccountType.BANK, isDefault = false)
                        )
                    )
                }
            }
        }).build()
    }

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
    fun provideLoanDao(database: MmrDatabase): com.mmr.core.database.dao.LoanDao =
        database.loanDao()

    @Provides
    fun provideLoanPaymentDao(database: MmrDatabase): com.mmr.core.database.dao.LoanPaymentDao =
        database.loanPaymentDao()

    @Provides
    fun provideRecurringRuleDao(database: MmrDatabase): com.mmr.core.database.dao.RecurringRuleDao =
        database.recurringRuleDao()

    @Provides
    fun provideNotificationRuleDao(database: MmrDatabase): com.mmr.core.database.dao.NotificationRuleDao =
        database.notificationRuleDao()

    @Provides
    fun provideBackupDao(database: MmrDatabase): com.mmr.core.database.dao.BackupDao =
        database.backupDao()
}
