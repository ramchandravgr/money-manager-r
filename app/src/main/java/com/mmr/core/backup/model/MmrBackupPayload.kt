package com.mmr.core.backup.model

import com.mmr.core.database.entity.AccountEntity
import com.mmr.core.database.entity.CategoryEntity
import com.mmr.core.database.entity.LoanEntity
import com.mmr.core.database.entity.LoanInterestRevisionEntity
import com.mmr.core.database.entity.LoanPaymentEntity
import com.mmr.core.database.entity.LoanScheduleEntryEntity
import com.mmr.core.database.entity.NotificationRuleEntity
import com.mmr.core.database.entity.RecurringRuleEntity
import com.mmr.core.database.entity.SmsMessageRecordEntity
import com.mmr.core.database.entity.SubcategoryEntity
import com.mmr.core.database.entity.TransactionApprovalEntity
import com.mmr.core.database.entity.TransactionEntity

data class MmrBackupPayload(
    val formatVersion: Int,
    val appId: String,
    val exportedAtEpochMillis: Long,
    val databaseVersion: Int,
    val categories: List<CategoryEntity>,
    val subcategories: List<SubcategoryEntity>,
    val accounts: List<AccountEntity>,
    val loans: List<LoanEntity>,
    val loanInterestRevisions: List<LoanInterestRevisionEntity> = emptyList(),
    val loanScheduleEntries: List<LoanScheduleEntryEntity> = emptyList(),
    val transactions: List<TransactionEntity>,
    val loanPayments: List<LoanPaymentEntity>,
    val recurringRules: List<RecurringRuleEntity>,
    val notificationRules: List<NotificationRuleEntity>,
    val smsMessageRecords: List<SmsMessageRecordEntity>,
    val transactionApprovals: List<TransactionApprovalEntity>,
    val appSettings: MmrAppSettingsBackup? = null,
)

data class BackupRestoreSummary(
    val categories: Int,
    val subcategories: Int,
    val accounts: Int,
    val loans: Int,
    val loanInterestRevisions: Int = 0,
    val loanScheduleEntries: Int = 0,
    val transactions: Int,
    val loanPayments: Int,
    val recurringRules: Int,
    val notificationRules: Int,
    val smsMessageRecords: Int,
    val transactionApprovals: Int,
    val appSettingsRestored: Boolean = false,
) {
    val totalRecords: Int
        get() =
            categories + subcategories + accounts + loans + loanInterestRevisions +
                loanScheduleEntries + transactions + loanPayments +
                recurringRules + notificationRules + smsMessageRecords + transactionApprovals
}
