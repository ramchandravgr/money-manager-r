package com.mmr.core.database.entity

import androidx.room.*
import com.mmr.core.model.enums.*
import com.mmr.core.model.enums.CardNetwork

@Entity(
    tableName = "accounts",
    indices = [
        Index(value = ["name"]),
        Index(value = ["isActive"]),
        Index(value = ["syncId"], unique = true),
    ],
)
data class AccountEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val syncId: String =
        com.mmr.core.common.sync.MmrSyncId
            .newId(),
    val name: String,
    val type: AccountType = AccountType.BANK,
    val openingBalance: Double = 0.0,
    val currentBalance: Double = 0.0,
    val currencyCode: String = "INR",
    val isActive: Boolean = true,
    val isDefault: Boolean = false,
    /** Last 4 digits for card / bank matching from SMS (optional). */
    val cardLastFour: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "loans",
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["linkedDebitAccountId"],
            onDelete = ForeignKey.SET_NULL,
        ),
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["linkedLoanAccountId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index(value = ["linkedDebitAccountId"]),
        Index(value = ["linkedLoanAccountId"]),
        Index(value = ["status"]),
        Index(value = ["dueDate"]),
        Index(value = ["syncId"], unique = true),
    ],
)
data class LoanEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val syncId: String =
        com.mmr.core.common.sync.MmrSyncId
            .newId(),
    val name: String,
    val lenderName: String,
    val principalAmount: Double,
    val interestRate: Double,
    val outstandingAmount: Double,
    val openingDate: Long,
    val dueDate: Long? = null,
    val autoTransferDate: Long? = null,
    val linkedDebitAccountId: Long? = null,
    val linkedLoanAccountId: Long? = null,
    val loanType: LoanType = LoanType.BORROWED,
    val status: LoanStatus = LoanStatus.ACTIVE,
    val repaymentMode: String? = null,
    val tenureMonths: Int? = null,
    val remainingTenureMonths: Int? = null,
    val fixedEmiAmount: Double? = null,
    val emiDayOfMonth: Int? = null,
    val fixedEmiInputMode: String? = null,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "loan_interest_revisions",
    foreignKeys = [
        ForeignKey(
            entity = LoanEntity::class,
            parentColumns = ["id"],
            childColumns = ["loanId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["loanId"]), Index(value = ["effectiveFromMillis"])],
)
data class LoanInterestRevisionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val loanId: Long,
    val effectiveFromMillis: Long,
    val annualRatePercent: Double,
    val note: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "loan_schedule_entries",
    foreignKeys = [
        ForeignKey(
            entity = LoanEntity::class,
            parentColumns = ["id"],
            childColumns = ["loanId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = LoanPaymentEntity::class,
            parentColumns = ["id"],
            childColumns = ["linkedPaymentId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index(value = ["loanId"]),
        Index(value = ["dueDateMillis"]),
        Index(value = ["status"]),
    ],
)
data class LoanScheduleEntryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val loanId: Long,
    val sequenceNumber: Int,
    val dueDateMillis: Long,
    val emiAmount: Double,
    val principalPlanned: Double,
    val interestPlanned: Double,
    val status: LoanScheduleStatus = LoanScheduleStatus.PLANNED,
    val linkedPaymentId: Long? = null,
    val annualRatePercentAtPlan: Double,
)

@Entity(
    tableName = "loan_payments",
    foreignKeys = [
        ForeignKey(
            entity = LoanEntity::class,
            parentColumns = ["id"],
            childColumns = ["loanId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = TransactionEntity::class,
            parentColumns = ["id"],
            childColumns = ["transactionId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index(value = ["loanId"]),
        Index(value = ["transactionId"]),
        Index(value = ["paymentDate"]),
    ],
)
data class LoanPaymentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val loanId: Long,
    val transactionId: Long? = null,
    val paymentAmount: Double,
    val paymentDate: Long = System.currentTimeMillis(),
    val paymentType: LoanPaymentType = LoanPaymentType.PART_PAYMENT,
    val principalComponent: Double? = null,
    val interestComponent: Double? = null,
    val outstandingAfterPayment: Double,
    val notes: String? = null,
)

@Entity(
    tableName = "recurring_rules",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL,
        ),
        ForeignKey(
            entity = SubcategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["subcategoryId"],
            onDelete = ForeignKey.SET_NULL,
        ),
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index(value = ["nextRunDate"]),
        Index(value = ["isEnabled"]),
        Index(value = ["accountId"]),
        Index(value = ["categoryId"]),
        Index(value = ["subcategoryId"]),
    ],
)
data class RecurringRuleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val transactionTemplateType: TransactionType = TransactionType.EXPENSE,
    val amount: Double,
    val categoryId: Long? = null,
    val subcategoryId: Long? = null,
    val accountId: Long,
    val dayOfMonth: Int? = null,
    val recurrenceInterval: RecurrenceInterval = RecurrenceInterval.MONTHLY,
    val nextRunDate: Long,
    val isEnabled: Boolean = true,
    val lastGeneratedDate: Long? = null,
    val endDate: Long? = null,
    val noteTemplate: String? = null,
    val reminderMinutesBefore: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "notification_rules",
    foreignKeys = [
        ForeignKey(
            entity = LoanEntity::class,
            parentColumns = ["id"],
            childColumns = ["targetLoanId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = RecurringRuleEntity::class,
            parentColumns = ["id"],
            childColumns = ["targetRecurringRuleId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["type"]),
        Index(value = ["enabled"]),
        Index(value = ["targetLoanId"]),
        Index(value = ["targetRecurringRuleId"]),
        Index(value = ["scheduleTime"]),
    ],
)
data class NotificationRuleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: NotificationRuleType,
    val enabled: Boolean = true,
    val scheduleTime: Long? = null,
    val targetLoanId: Long? = null,
    val targetRecurringRuleId: Long? = null,
    val thresholdValue: Double? = null,
    val title: String? = null,
    val message: String? = null,
    val isFired: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL,
        ),
        ForeignKey(
            entity = SubcategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["subcategoryId"],
            onDelete = ForeignKey.SET_NULL,
        ),
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["relatedAccountId"],
            onDelete = ForeignKey.SET_NULL,
        ),
        ForeignKey(
            entity = LoanEntity::class,
            parentColumns = ["id"],
            childColumns = ["loanId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index(value = ["dateTime"]),
        Index(value = ["categoryId"]),
        Index(value = ["subcategoryId"]),
        Index(value = ["accountId"]),
        Index(value = ["relatedAccountId"]),
        Index(value = ["loanId"]),
        Index(value = ["type"]),
        Index(value = ["sourceType"]),
        Index(value = ["status"]),
        Index(value = ["syncId"], unique = true),
    ],
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val syncId: String =
        com.mmr.core.common.sync.MmrSyncId
            .newId(),
    val dateTime: Long,
    val amount: Double,
    val type: TransactionType,
    val categoryId: Long? = null,
    val subcategoryId: Long? = null,
    val note: String? = null,
    val accountId: Long,
    val relatedAccountId: Long? = null,
    val loanId: Long? = null,
    val sourceType: TransactionSourceType = TransactionSourceType.MANUAL,
    val sourceReferenceId: String? = null,
    val status: RecordStatus = RecordStatus.APPROVED,
    val attachmentCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isEdited: Boolean = false,
    val paymentChannel: String? = null,
    val paymentInstrument: String? = null,
    val cardLastFour: String? = null,
    val cardNetwork: CardNetwork? = null,
)
