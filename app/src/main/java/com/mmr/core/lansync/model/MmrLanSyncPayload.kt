package com.mmr.core.lansync.model

import com.mmr.core.backup.model.MmrAppSettingsBackup
import com.mmr.core.data.preferences.AppDisplaySettingsSnapshot

data class MmrLanSyncPayload(
    val formatVersion: Int,
    val exportedAtEpochMillis: Long,
    val sourceDeviceId: String,
    val categories: List<LanCategoryRecord>,
    val subcategories: List<LanSubcategoryRecord>,
    val accounts: List<LanAccountRecord>,
    val loans: List<LanLoanRecord>,
    val transactions: List<LanTransactionRecord>,
    val appSettings: MmrAppSettingsBackup? = null,
    val displaySettings: AppDisplaySettingsSnapshot? = null,
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
)
