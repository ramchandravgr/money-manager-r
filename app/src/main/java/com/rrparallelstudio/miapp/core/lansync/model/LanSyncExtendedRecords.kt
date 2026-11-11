package com.rrparallelstudio.miapp.core.lansync.model

data class LanLoanPaymentRecord(
    val syncId: String,
    val loanSyncId: String,
    val transactionSyncId: String?,
    val paymentAmount: Double,
    val paymentDate: Long,
    val paymentType: String,
    val principalComponent: Double?,
    val interestComponent: Double?,
    val outstandingAfterPayment: Double,
    val notes: String?,
)

data class LanLoanInterestRevisionRecord(
    val syncId: String,
    val loanSyncId: String,
    val effectiveFromMillis: Long,
    val annualRatePercent: Double,
    val note: String?,
    val createdAt: Long,
)

data class LanLoanScheduleEntryRecord(
    val syncId: String,
    val loanSyncId: String,
    val sequenceNumber: Int,
    val dueDateMillis: Long,
    val emiAmount: Double,
    val principalPlanned: Double,
    val interestPlanned: Double,
    val status: String,
    val linkedPaymentSyncId: String?,
    val annualRatePercentAtPlan: Double,
)

data class LanRecurringRuleRecord(
    val syncId: String,
    val name: String,
    val transactionTemplateType: String,
    val amount: Double,
    val categorySyncId: String?,
    val subcategorySyncId: String?,
    val accountSyncId: String,
    val relatedAccountSyncId: String? = null,
    val loanSyncId: String? = null,
    val dayOfMonth: Int?,
    val recurrenceInterval: String,
    val nextRunDate: Long,
    val isEnabled: Boolean,
    val lastGeneratedDate: Long?,
    val endDate: Long?,
    val noteTemplate: String?,
    val reminderMinutesBefore: Int,
    val createdAt: Long,
    val updatedAt: Long,
    val createdByProfileId: String? = null,
    val createdByDisplayName: String? = null,
)

data class LanBudgetRecord(
    val syncId: String,
    val categorySyncId: String,
    val subcategorySyncId: String?,
    val amountLimit: Double,
    val periodType: String,
    val customPeriodStartMillis: Long?,
    val customPeriodEndMillis: Long?,
    val alertOnExceeded: Boolean,
    val lastAlertPeriodKey: String?,
    val isActive: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
    val createdByProfileId: String? = null,
    val createdByDisplayName: String? = null,
)

data class LanSmsAccountOverrideRecord(
    val templateId: String,
    val instrument: String,
    val smsLastFour: String,
    val accountSyncId: String,
    val updatedAt: Long,
)

data class LanCategorizationRuleRecord(
    val syncId: String,
    val priority: Int,
    val matchType: String,
    val pattern: String,
    val categoryName: String,
    val subcategoryName: String?,
    val isEnabled: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
)

data class LanAssetRecord(
    val syncId: String,
    val name: String,
    val assetType: String,
    val status: String,
    val isInherited: Boolean,
    val purchaseDateMillis: Long?,
    val purchasePrice: Double?,
    val purchasePaymentMode: String? = null,
    val purchaseDownPayment: Double? = null,
    val purchaseFinancedAmount: Double? = null,
    val currentValue: Double,
    val ownedByName: String?,
    val location: String?,
    val identifier: String?,
    val linkedLoanSyncId: String?,
    val nextValuationDueMillis: Long?,
    val valuationReminderDaysBefore: Int,
    val saleDateMillis: Long?,
    val salePrice: Double?,
    val notes: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val createdByProfileId: String? = null,
    val createdByDisplayName: String? = null,
)

data class LanAssetValuationRecord(
    val syncId: String,
    val assetSyncId: String,
    val amount: Double,
    val recordedAt: Long,
    val notes: String?,
    val createdAt: Long,
)

data class LanAssetAttachmentRecord(
    val syncId: String,
    val assetSyncId: String,
    val mimeType: String,
    val fileSizeBytes: Long,
    val createdAt: Long,
    val updatedAt: Long,
)

data class LanNotificationRuleRecord(
    val syncId: String,
    val type: String,
    val enabled: Boolean,
    val scheduleTime: Long?,
    val targetLoanSyncId: String?,
    val targetRecurringRuleSyncId: String?,
    val targetAssetSyncId: String? = null,
    val thresholdValue: Double?,
    val title: String?,
    val message: String?,
    val isFired: Boolean,
    val createdAt: Long,
)
