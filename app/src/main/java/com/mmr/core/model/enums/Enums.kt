package com.mmr.core.model.enums

enum class TransactionType {
    EXPENSE, INCOME, TRANSFER, LOAN_PAYMENT, ADJUSTMENT
}

enum class TransactionSourceType {
    MANUAL, SMS, LOAN, RECURRING, SYSTEM
}

enum class RecordStatus {
    PENDING, APPROVED, REJECTED, DELETED
}

enum class SmsApprovalState {
    PENDING, APPROVED, REJECTED, DUPLICATE, IGNORED
}

enum class SmsParsedStatus {
    UNPROCESSED, MATCHED, FAILED, NON_FINANCIAL
}

enum class CategoryType {
    EXPENSE, INCOME, TRANSFER, ALL
}

enum class AccountType {
    CASH, BANK, DEBIT, CREDIT, WALLET, LOAN, OTHER
}

enum class LoanStatus {
    ACTIVE, CLOSED, OVERDUE, PAUSED
}

enum class LoanType {
    BORROWED, LENT
}

enum class LoanPaymentType {
    MANUAL, AUTO_TRANSFER, PART_PAYMENT, CLOSING_PAYMENT
}

enum class RecurrenceInterval {
    DAILY, WEEKLY, MONTHLY, YEARLY, CUSTOM
}

enum class NotificationRuleType {
    LOAN_DUE, RECURRING_REMINDER, APPROVAL_PENDING, LARGE_SPEND_ALERT, BUDGET_THRESHOLD
}
