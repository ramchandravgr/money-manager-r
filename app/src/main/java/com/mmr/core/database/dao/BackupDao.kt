package com.mmr.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.mmr.core.database.entity.AccountEntity
import com.mmr.core.database.entity.CategoryEntity
import com.mmr.core.database.entity.LoanEntity
import com.mmr.core.database.entity.LoanPaymentEntity
import com.mmr.core.database.entity.NotificationRuleEntity
import com.mmr.core.database.entity.RecurringRuleEntity
import com.mmr.core.database.entity.SmsMessageRecordEntity
import com.mmr.core.database.entity.SubcategoryEntity
import com.mmr.core.database.entity.TransactionApprovalEntity
import com.mmr.core.database.entity.TransactionEntity

@Dao
interface BackupDao {

    @Query("SELECT * FROM categories")
    suspend fun getAllCategories(): List<CategoryEntity>

    @Query("SELECT * FROM subcategories")
    suspend fun getAllSubcategories(): List<SubcategoryEntity>

    @Query("SELECT * FROM accounts")
    suspend fun getAllAccounts(): List<AccountEntity>

    @Query("SELECT * FROM loans")
    suspend fun getAllLoans(): List<LoanEntity>

    @Query("SELECT * FROM loan_payments")
    suspend fun getAllLoanPayments(): List<LoanPaymentEntity>

    @Query("SELECT * FROM recurring_rules")
    suspend fun getAllRecurringRules(): List<RecurringRuleEntity>

    @Query("SELECT * FROM notification_rules")
    suspend fun getAllNotificationRules(): List<NotificationRuleEntity>

    @Query("SELECT * FROM transactions")
    suspend fun getAllTransactions(): List<TransactionEntity>

    @Query("SELECT * FROM sms_message_records")
    suspend fun getAllSmsMessageRecords(): List<SmsMessageRecordEntity>

    @Query("SELECT * FROM transaction_approvals")
    suspend fun getAllTransactionApprovals(): List<TransactionApprovalEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<CategoryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubcategories(subcategories: List<SubcategoryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccounts(accounts: List<AccountEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLoans(loans: List<LoanEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLoanPayments(payments: List<LoanPaymentEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecurringRules(rules: List<RecurringRuleEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotificationRules(rules: List<NotificationRuleEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<TransactionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSmsMessageRecords(records: List<SmsMessageRecordEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactionApprovals(approvals: List<TransactionApprovalEntity>)

    @Transaction
    suspend fun clearAllUserData() {
        deleteAllTransactionApprovals()
        deleteAllNotificationRules()
        deleteAllLoanPayments()
        deleteAllTransactions()
        deleteAllSmsMessageRecords()
        deleteAllRecurringRules()
        deleteAllLoans()
        deleteAllSubcategories()
        deleteAllCategories()
        deleteAllAccounts()
    }

    @Transaction
    suspend fun restoreAllUserData(
        categories: List<CategoryEntity>,
        subcategories: List<SubcategoryEntity>,
        accounts: List<AccountEntity>,
        loans: List<LoanEntity>,
        transactions: List<TransactionEntity>,
        loanPayments: List<LoanPaymentEntity>,
        recurringRules: List<RecurringRuleEntity>,
        notificationRules: List<NotificationRuleEntity>,
        smsMessageRecords: List<SmsMessageRecordEntity>,
        transactionApprovals: List<TransactionApprovalEntity>
    ) {
        clearAllUserData()
        if (categories.isNotEmpty()) insertCategories(categories)
        if (subcategories.isNotEmpty()) insertSubcategories(subcategories)
        if (accounts.isNotEmpty()) insertAccounts(accounts)
        if (loans.isNotEmpty()) insertLoans(loans)
        if (recurringRules.isNotEmpty()) insertRecurringRules(recurringRules)
        if (transactions.isNotEmpty()) insertTransactions(transactions)
        if (loanPayments.isNotEmpty()) insertLoanPayments(loanPayments)
        if (notificationRules.isNotEmpty()) insertNotificationRules(notificationRules)
        if (smsMessageRecords.isNotEmpty()) insertSmsMessageRecords(smsMessageRecords)
        if (transactionApprovals.isNotEmpty()) insertTransactionApprovals(transactionApprovals)
    }

    @Query("DELETE FROM transaction_approvals")
    suspend fun deleteAllTransactionApprovals()

    @Query("DELETE FROM notification_rules")
    suspend fun deleteAllNotificationRules()

    @Query("DELETE FROM loan_payments")
    suspend fun deleteAllLoanPayments()

    @Query("DELETE FROM transactions")
    suspend fun deleteAllTransactions()

    @Query("DELETE FROM sms_message_records")
    suspend fun deleteAllSmsMessageRecords()

    @Query("DELETE FROM recurring_rules")
    suspend fun deleteAllRecurringRules()

    @Query("DELETE FROM loans")
    suspend fun deleteAllLoans()

    @Query("DELETE FROM subcategories")
    suspend fun deleteAllSubcategories()

    @Query("DELETE FROM categories")
    suspend fun deleteAllCategories()

    @Query("DELETE FROM accounts")
    suspend fun deleteAllAccounts()
}
