package com.mmr.core.data.repository

import com.mmr.core.common.validation.MmrValidation
import com.mmr.core.database.entity.CategoryEntity
import com.mmr.core.database.entity.LoanEntity
import com.mmr.core.database.entity.RecurringRuleEntity
import com.mmr.core.database.entity.SubcategoryEntity
import com.mmr.core.database.entity.TransactionEntity

internal object RepositoryGuards {
    fun requireValidTransactionEntity(transaction: TransactionEntity) {
        MmrValidation.requirePositiveAmount(transaction.amount)
        require(transaction.accountId > 0L) { "Account is required." }
        require(MmrValidation.isValidEpochMillis(transaction.dateTime)) { "Invalid transaction date." }
    }

    fun requireValidTransactionEntityAllowZeroAmount(transaction: TransactionEntity) {
        require(transaction.amount.isFinite() && transaction.amount >= 0.0) {
            "Amount cannot be negative."
        }
        require(transaction.accountId > 0L) { "Account is required." }
        require(MmrValidation.isValidEpochMillis(transaction.dateTime)) { "Invalid transaction date." }
    }

    fun requireValidLoanEntity(loan: LoanEntity) {
        MmrValidation.requirePositiveAmount(loan.principalAmount, "Principal")
        require(loan.outstandingAmount.isFinite() && loan.outstandingAmount >= 0.0) {
            "Outstanding amount cannot be negative."
        }
        require(loan.outstandingAmount <= loan.principalAmount + 0.01) {
            "Outstanding amount cannot exceed principal."
        }
        require(loan.name.isNotBlank() && loan.lenderName.isNotBlank()) {
            "Loan name and lender are required."
        }
        loan.dueDate?.let { due ->
            require(MmrValidation.isValidEpochMillis(due)) { "Invalid due date." }
        }
    }

    fun requireValidRecurringRule(rule: RecurringRuleEntity) {
        MmrValidation.requirePositiveAmount(rule.amount)
        require(rule.accountId > 0L) { "Account is required for recurring rules." }
        require(rule.name.isNotBlank()) { "Recurring rule name is required." }
        require(MmrValidation.isValidEpochMillis(rule.nextRunDate)) { "Invalid next run date." }
        rule.endDate?.let { end ->
            require(MmrValidation.isValidEpochMillis(end)) { "Invalid end date." }
        }
    }

    fun requireValidPaymentAmount(paymentAmount: Double) {
        MmrValidation.requirePositiveAmount(paymentAmount, "Payment")
    }

    fun normalizedQueryRange(
        startDate: Long,
        endDate: Long,
    ): Pair<Long, Long> = MmrValidation.normalizeDateRange(startDate, endDate)

    fun requireValidCategoryEntity(category: CategoryEntity) {
        MmrValidation.requireNonBlank(category.name, "Category name")
        requireValidColorHex(category.colorHex)
        require(category.orderIndex >= 0) { "Category order cannot be negative." }
    }

    fun requireValidSubcategoryEntity(subcategory: SubcategoryEntity) {
        MmrValidation.requireNonBlank(subcategory.name, "Subcategory name")
        require(subcategory.categoryId > 0L) { "Parent category is required." }
        require(subcategory.orderIndex >= 0) { "Subcategory order cannot be negative." }
    }

    private fun requireValidColorHex(colorHex: String) {
        require(colorHex.matches(Regex("^#[0-9A-Fa-f]{6}$"))) { "Category color must be a #RRGGBB hex value." }
    }
}
