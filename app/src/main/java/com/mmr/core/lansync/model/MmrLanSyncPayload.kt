package com.mmr.core.lansync.model

import com.mmr.core.backup.model.MmrAppSettingsBackup
import com.mmr.core.data.preferences.AppDisplaySettingsSnapshot

data class MmrLanSyncPayload(
    val formatVersion: Int,
    val exportedAtEpochMillis: Long,
    val sourceDeviceId: String,
    val sourceDeviceName: String? = null,
    val categories: List<LanCategoryRecord>,
    val subcategories: List<LanSubcategoryRecord>,
    val accounts: List<LanAccountRecord>,
    val loans: List<LanLoanRecord>,
    val transactions: List<LanTransactionRecord>,
    val loanPayments: List<LanLoanPaymentRecord> = emptyList(),
    val loanInterestRevisions: List<LanLoanInterestRevisionRecord> = emptyList(),
    val loanScheduleEntries: List<LanLoanScheduleEntryRecord> = emptyList(),
    val recurringRules: List<LanRecurringRuleRecord> = emptyList(),
    val categorizationRules: List<LanCategorizationRuleRecord> = emptyList(),
    val notificationRules: List<LanNotificationRuleRecord> = emptyList(),
    val appSettings: MmrAppSettingsBackup? = null,
    val displaySettings: AppDisplaySettingsSnapshot? = null,
    val attachments: List<LanAttachmentRecord> = emptyList(),
    val tombstones: List<LanSyncTombstoneRecord> = emptyList(),
)

data class LanAttachmentRecord(
    val syncId: String,
    val transactionSyncId: String,
    val mimeType: String,
    val fileSizeBytes: Long,
    val createdAt: Long,
    val updatedAt: Long,
)

data class LanCategoryRecord(
    val syncId: String,
    val name: String,
    val iconName: String,
    val colorHex: String,
    val type: String,
    val isDefault: Boolean,
    val isActive: Boolean,
    val orderIndex: Int,
)

data class LanSubcategoryRecord(
    val syncId: String,
    val categorySyncId: String,
    val name: String,
    val isActive: Boolean,
    val orderIndex: Int,
)

data class LanAccountRecord(
    val syncId: String,
    val name: String,
    val type: String,
    val openingBalance: Double,
    val currentBalance: Double,
    val currencyCode: String,
    val isActive: Boolean,
    val isDefault: Boolean,
    val cardLastFour: String?,
    val createdAt: Long,
    val updatedAt: Long,
)

data class LanLoanRecord(
    val syncId: String,
    val name: String,
    val lenderName: String,
    val principalAmount: Double,
    val interestRate: Double,
    val outstandingAmount: Double,
    val openingDate: Long,
    val dueDate: Long?,
    val autoTransferDate: Long?,
    val linkedDebitAccountSyncId: String?,
    val linkedLoanAccountSyncId: String?,
    val loanType: String,
    val status: String,
    val repaymentMode: String?,
    val tenureMonths: Int?,
    val remainingTenureMonths: Int?,
    val fixedEmiAmount: Double?,
    val emiDayOfMonth: Int?,
    val fixedEmiInputMode: String?,
    val notes: String?,
    val createdAt: Long,
    val updatedAt: Long,
)

data class LanTransactionRecord(
    val syncId: String,
    val dateTime: Long,
    val amount: Double,
    val type: String,
    val categorySyncId: String?,
    val subcategorySyncId: String?,
    val note: String?,
    val accountSyncId: String,
    val relatedAccountSyncId: String?,
    val loanSyncId: String?,
    val sourceType: String,
    val sourceReferenceId: String?,
    val attachmentCount: Int,
    val createdAt: Long,
    val updatedAt: Long,
    val isEdited: Boolean,
    val paymentChannel: String?,
    val paymentInstrument: String?,
    val cardLastFour: String?,
    val cardNetwork: String?,
    val syncedFromDeviceId: String? = null,
    val syncedFromDeviceName: String? = null,
)
