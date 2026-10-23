package com.mmr.core.backup

import com.mmr.core.backup.model.MmrBackupPayload
import com.mmr.core.database.entity.LoanPaymentEntity
import com.mmr.core.database.entity.LoanScheduleEntryEntity

/**
 * Prepares backup payloads for replace/merge import: link sanitization and merge-only row selection.
 */
object BackupImportPlanner {
    data class ExistingIds(
        val categories: Set<Long>,
        val subcategories: Set<Long>,
        val accounts: Set<Long>,
        val loans: Set<Long>,
        val loanPayments: Set<Long>,
        val transactions: Set<Long>,
        val smsRecords: Set<Long>,
        val recurringRules: Set<Long>,
        val notificationRules: Set<Long>,
        val loanRevisions: Set<Long>,
        val loanScheduleEntries: Set<Long>,
        val transactionApprovals: Set<Long>,
    )

    fun sanitizeLoanLinks(
        payments: List<LoanPaymentEntity>,
        schedule: List<LoanScheduleEntryEntity>,
        validTransactionIds: Set<Long>,
        validPaymentIds: Set<Long>,
    ): Pair<List<LoanPaymentEntity>, List<LoanScheduleEntryEntity>> {
        val sanitizedPayments =
            payments.map { payment ->
                val txnId = payment.transactionId
                if (txnId != null && txnId > 0L && txnId !in validTransactionIds) {
                    payment.copy(transactionId = null)
                } else {
                    payment
                }
            }
        val sanitizedSchedule =
            schedule.map { entry ->
                val linkId = entry.linkedPaymentId
                if (linkId != null && linkId > 0L && linkId !in validPaymentIds) {
                    entry.copy(linkedPaymentId = null)
                } else {
                    entry
                }
            }
        return sanitizedPayments to sanitizedSchedule
    }

    fun prepareReplace(payload: MmrBackupPayload): MmrBackupPayload {
        val txnIds = payload.transactions.map { it.id }.toSet()
        val paymentIds = payload.loanPayments.map { it.id }.toSet()
        val (payments, schedule) =
            sanitizeLoanLinks(
                payload.loanPayments,
                payload.loanScheduleEntries,
                txnIds,
                paymentIds,
            )
        return payload.copy(loanPayments = payments, loanScheduleEntries = schedule)
    }

    fun prepareMerge(
        payload: MmrBackupPayload,
        existing: ExistingIds,
    ): MmrBackupPayload {
        val categoryIds = existing.categories.toMutableSet()
        val newCategories = payload.categories.filter { it.id > 0L && it.id !in categoryIds }
        categoryIds.addAll(newCategories.map { it.id })

        val subcategoryIds = existing.subcategories.toMutableSet()
        val newSubcategories =
            payload.subcategories.filter {
                it.id > 0L && it.id !in subcategoryIds && it.categoryId in categoryIds
            }
        subcategoryIds.addAll(newSubcategories.map { it.id })

        val accountIds = existing.accounts.toMutableSet()
        val newAccounts = payload.accounts.filter { it.id > 0L && it.id !in accountIds }
        accountIds.addAll(newAccounts.map { it.id })

        val loanIds = existing.loans.toMutableSet()
        val newLoans =
            payload.loans.filter { loan ->
                loan.id > 0L &&
                    loan.id !in loanIds &&
                    (loan.linkedLoanAccountId == null || loan.linkedLoanAccountId in accountIds) &&
                    (loan.linkedDebitAccountId == null || loan.linkedDebitAccountId in accountIds)
            }
        loanIds.addAll(newLoans.map { it.id })

        val revisionIds = existing.loanRevisions.toMutableSet()
        val newRevisions =
            payload.loanInterestRevisions.filter {
                it.id > 0L && it.id !in revisionIds && it.loanId in loanIds
            }
        revisionIds.addAll(newRevisions.map { it.id })

        val recurringIds = existing.recurringRules.toMutableSet()
        val newRecurring =
            payload.recurringRules.filter { rule ->
                rule.id > 0L &&
                    rule.id !in recurringIds &&
                    rule.accountId in accountIds &&
                    (rule.categoryId == null || rule.categoryId in categoryIds) &&
                    (rule.subcategoryId == null || rule.subcategoryId in subcategoryIds)
            }
        recurringIds.addAll(newRecurring.map { it.id })

        val transactionIds = existing.transactions.toMutableSet()
        val newTransactions =
            payload.transactions.filter { txn ->
                txn.id > 0L &&
                    txn.id !in transactionIds &&
                    txn.accountId in accountIds &&
                    (txn.categoryId == null || txn.categoryId in categoryIds) &&
                    (txn.subcategoryId == null || txn.subcategoryId in subcategoryIds) &&
                    (txn.relatedAccountId == null || txn.relatedAccountId in accountIds) &&
                    (txn.loanId == null || txn.loanId in loanIds)
            }
        transactionIds.addAll(newTransactions.map { it.id })

        val paymentIds = existing.loanPayments.toMutableSet()
        val newPaymentsRaw =
            payload.loanPayments.filter { payment ->
                payment.id > 0L &&
                    payment.id !in paymentIds &&
                    payment.loanId in loanIds &&
                    (payment.transactionId == null || payment.transactionId in transactionIds)
            }
        val allPaymentIds = paymentIds + newPaymentsRaw.map { it.id }
        val (newPayments, _) =
            sanitizeLoanLinks(
                newPaymentsRaw,
                emptyList(),
                transactionIds,
                allPaymentIds,
            )
        paymentIds.addAll(newPayments.map { it.id })

        val scheduleIds = existing.loanScheduleEntries.toMutableSet()
        val newScheduleRaw =
            payload.loanScheduleEntries.filter { entry ->
                entry.id > 0L && entry.id !in scheduleIds && entry.loanId in loanIds
            }
        val (_, newSchedule) =
            sanitizeLoanLinks(
                emptyList(),
                newScheduleRaw,
                transactionIds,
                paymentIds,
            )
        scheduleIds.addAll(newSchedule.map { it.id })

        val notificationIds = existing.notificationRules.toMutableSet()
        val newNotifications =
            payload.notificationRules.filter { rule ->
                rule.id > 0L &&
                    rule.id !in notificationIds &&
                    (rule.targetLoanId == null || rule.targetLoanId in loanIds) &&
                    (rule.targetRecurringRuleId == null || rule.targetRecurringRuleId in recurringIds)
            }
        notificationIds.addAll(newNotifications.map { it.id })

        val smsIds = existing.smsRecords.toMutableSet()
        val newSms = payload.smsMessageRecords.filter { it.id > 0L && it.id !in smsIds }
        smsIds.addAll(newSms.map { it.id })

        val approvalIds = existing.transactionApprovals.toMutableSet()
        val newApprovals =
            payload.transactionApprovals.filter { approval ->
                approval.id > 0L &&
                    approval.id !in approvalIds &&
                    approval.smsMessageRecordId in smsIds
            }

        return payload.copy(
            categories = newCategories,
            subcategories = newSubcategories,
            accounts = newAccounts,
            loans = newLoans,
            loanInterestRevisions = newRevisions,
            transactions = newTransactions,
            loanPayments = newPayments,
            loanScheduleEntries = newSchedule,
            recurringRules = newRecurring,
            notificationRules = newNotifications,
            smsMessageRecords = newSms,
            transactionApprovals = newApprovals,
        )
    }

    fun countRecords(payload: MmrBackupPayload): Int =
        payload.categories.size + payload.subcategories.size + payload.accounts.size +
            payload.loans.size + payload.loanInterestRevisions.size + payload.loanScheduleEntries.size +
            payload.transactions.size + payload.loanPayments.size + payload.recurringRules.size +
            payload.notificationRules.size + payload.smsMessageRecords.size + payload.transactionApprovals.size
}
