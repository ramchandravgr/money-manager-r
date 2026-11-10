package com.rrparallelstudio.miapp.core.backup.model

import com.rrparallelstudio.miapp.core.database.entity.AccountEntity
import com.rrparallelstudio.miapp.core.database.entity.AssetEntity
import com.rrparallelstudio.miapp.core.database.entity.AssetValuationEntity
import com.rrparallelstudio.miapp.core.database.entity.BudgetEntity
import com.rrparallelstudio.miapp.core.database.entity.CategorizationRuleEntity
import com.rrparallelstudio.miapp.core.database.entity.CategoryEntity
import com.rrparallelstudio.miapp.core.database.entity.LoanEntity
import com.rrparallelstudio.miapp.core.database.entity.LoanInterestRevisionEntity
import com.rrparallelstudio.miapp.core.database.entity.LoanPaymentEntity
import com.rrparallelstudio.miapp.core.database.entity.LoanScheduleEntryEntity
import com.rrparallelstudio.miapp.core.database.entity.NotificationRuleEntity
import com.rrparallelstudio.miapp.core.database.entity.RecurringRuleEntity
import com.rrparallelstudio.miapp.core.database.entity.SmsMessageRecordEntity
import com.rrparallelstudio.miapp.core.database.entity.SubcategoryEntity
import com.rrparallelstudio.miapp.core.database.entity.SyncTombstoneEntity
import com.rrparallelstudio.miapp.core.database.entity.TransactionApprovalEntity
import com.rrparallelstudio.miapp.core.database.entity.TransactionEntity
import com.rrparallelstudio.miapp.core.database.entity.TrustedLanDeviceEntity

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
    val budgets: List<BudgetEntity> = emptyList(),
    val assets: List<AssetEntity> = emptyList(),
    val assetValuations: List<AssetValuationEntity> = emptyList(),
    val notificationRules: List<NotificationRuleEntity>,
    val smsMessageRecords: List<SmsMessageRecordEntity>,
    val transactionApprovals: List<TransactionApprovalEntity>,
    val appSettings: MmrAppSettingsBackup? = null,
    val tombstones: List<SyncTombstoneEntity> = emptyList(),
    val categorizationRules: List<CategorizationRuleEntity> = emptyList(),
    val trustedLanDevices: List<TrustedLanDeviceEntity> = emptyList(),
    val transactionAttachments: List<MmrBackupAttachmentRecord> = emptyList(),
    val assetAttachments: List<MmrBackupAssetAttachmentRecord> = emptyList(),
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
    val budgets: Int = 0,
    val assets: Int = 0,
    val assetValuations: Int = 0,
    val assetAttachments: Int = 0,
    val notificationRules: Int,
    val smsMessageRecords: Int,
    val transactionApprovals: Int,
    val appSettingsRestored: Boolean = false,
) {
    val totalRecords: Int
        get() =
            categories + subcategories + accounts + loans + loanInterestRevisions +
                loanScheduleEntries + transactions + loanPayments +
                recurringRules + budgets + assets + assetValuations + assetAttachments +
                notificationRules + smsMessageRecords + transactionApprovals
}
