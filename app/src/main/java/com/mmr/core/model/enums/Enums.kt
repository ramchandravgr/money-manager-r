package com.mmr.core.model.enums

enum class TransactionType {
    EXPENSE,
    INCOME,
    TRANSFER,
    LOAN_PAYMENT,
    ADJUSTMENT,
}

enum class TransactionSourceType {
    MANUAL,
    SMS,
    LOAN,
    RECURRING,
    SYSTEM,
}

enum class RecordStatus {
    PENDING,
    APPROVED,
    REJECTED,
    DELETED,
}

enum class SmsApprovalState {
    PENDING,
    APPROVED,
    REJECTED,
    DUPLICATE,
    IGNORED,
}

enum class SmsParsedStatus {
    UNPROCESSED,
    MATCHED,
    FAILED,
    NON_FINANCIAL,
}

/** Pending approval queue item: normal spend/income vs card refund to apply to a purchase. */
enum class SmsApprovalKind {
    TRANSACTION,
    CARD_REFUND,
}

/** Role of an SMS linked to a ledger transaction. */
enum class SmsLinkRole {
    PURCHASE,
    REFUND,
}

enum class CategoryType {
    EXPENSE,
    INCOME,
    TRANSFER,
    ALL,
}

enum class AccountType {
    CASH,
    BANK,
    DEBIT,
    CREDIT,
    WALLET,
    LOAN,
    OTHER,
}

enum class LoanStatus {
    ACTIVE,
    CLOSED,
    OVERDUE,
    PAUSED,
}

enum class LoanType {
    BORROWED,
    LENT,
}

enum class LoanPaymentType {
    MANUAL,
    AUTO_TRANSFER,
    PART_PAYMENT,
    CLOSING_PAYMENT,
    TOP_UP,
}

/** Fixed monthly EMI vs pay-as-you-go (multiple payments per month allowed). */
enum class LoanRepaymentMode {
    FIXED_EMI,
    FLEXIBLE,
}

enum class LoanScheduleStatus {
    PLANNED,
    PAID,
    CANCELLED,
}

/** After a part payment on a fixed-EMI loan: shorten tenure or lower future EMI. */
enum class LoanPartPaymentAdjustment {
    REDUCE_TENURE,
    REDUCE_EMI,
}

/** After a top-up on fixed-EMI: keep EMI and extend tenure, or keep tenure and raise EMI. */
enum class LoanTopUpAdjustment {
    EXTEND_TENURE,
    INCREASE_EMI,
}

enum class RecurrenceInterval {
    DAILY,
    WEEKLY,
    MONTHLY,
    YEARLY,
    CUSTOM,
}

enum class NotificationRuleType {
    LOAN_DUE,
    RECURRING_REMINDER,
    APPROVAL_PENDING,
    LARGE_SPEND_ALERT,
    BUDGET_THRESHOLD,
}

enum class CategorizationMatchType {
    MERCHANT_OR_NOTE_CONTAINS,
    SMS_BODY_CONTAINS,
    SENDER_CONTAINS,
}

enum class CardNetwork {
    VISA,
    MASTERCARD,
    RUPAY,
    AMEX,
    DINERS,
    MAESTRO,
}
