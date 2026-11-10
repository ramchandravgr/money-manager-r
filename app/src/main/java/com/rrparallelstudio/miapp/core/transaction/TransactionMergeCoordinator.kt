package com.rrparallelstudio.miapp.core.transaction

import androidx.room.withTransaction
import com.rrparallelstudio.miapp.core.data.repository.RepositoryGuards
import com.rrparallelstudio.miapp.core.database.MmrDatabase
import com.rrparallelstudio.miapp.core.database.dao.LoanPaymentDao
import com.rrparallelstudio.miapp.core.database.dao.SmsMessageRecordDao
import com.rrparallelstudio.miapp.core.database.dao.TransactionDao
import com.rrparallelstudio.miapp.core.database.entity.TransactionEntity
import com.rrparallelstudio.miapp.core.loan.LoanLinkedTransactionDeleteMode
import com.rrparallelstudio.miapp.core.loan.LoanTransactionCoordinator
import com.rrparallelstudio.miapp.core.model.enums.RecordStatus
import com.rrparallelstudio.miapp.core.model.enums.TransactionSourceType
import com.rrparallelstudio.miapp.core.model.enums.TransactionType
import com.rrparallelstudio.miapp.core.transaction.TransactionMergeAmounts
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionMergeCoordinator
    @Inject
    constructor(
        private val database: MmrDatabase,
        private val transactionDao: TransactionDao,
        private val smsMessageRecordDao: SmsMessageRecordDao,
        private val loanPaymentDao: LoanPaymentDao,
        private val loanTransactionCoordinator: LoanTransactionCoordinator,
    ) {
        suspend fun mergeTransactions(
            transactionIds: List<Long>,
            mergedName: String,
            mergedDateTime: Long? = null,
        ): Long {
            val ids = transactionIds.distinct().filter { it > 0L }
            require(ids.size >= 2) { "Select at least two transactions to merge." }
            val name = mergedName.trim()
            require(name.isNotEmpty()) { "Enter a name for the merged transaction." }

            return database.withTransaction {
                val transactions = ids.mapNotNull { transactionDao.getById(it) }
                require(transactions.size == ids.size) { "One or more transactions were not found." }

                require(transactions.all { it.status == RecordStatus.APPROVED }) {
                    "Only approved transactions can be merged."
                }
                require(transactions.none { it.sourceType == TransactionSourceType.LOAN }) {
                    "Loan ledger entries cannot be merged from the home screen."
                }
                require(
                    transactions.none {
                        it.type == TransactionType.TRANSFER ||
                            it.type == TransactionType.LOAN_PAYMENT
                    },
                ) {
                    "Transfers and loan payments cannot be merged with other types from the home screen."
                }

                val resolved = TransactionMergeAmounts.resolve(transactions)
                require(resolved.amount >= 0.0) { "Merged total cannot be negative." }

                val accountId = transactions.first().accountId
                require(transactions.all { it.accountId == accountId }) {
                    "Merged transactions must use the same account."
                }

                val loanPaymentCount =
                    transactions.count { txn ->
                        loanPaymentDao.getByTransactionId(txn.id) != null
                    }
                require(loanPaymentCount <= 1) {
                    "Cannot merge when more than one transaction is linked to a loan payment."
                }
                val lonePayment =
                    transactions.firstNotNullOfOrNull { txn ->
                        loanPaymentDao.getByTransactionId(txn.id)
                    }

                val totalAmount = resolved.amount
                val mergedType = resolved.type

                val hasSmsLink =
                    transactions.any { txn ->
                        smsMessageRecordDao.countByMatchedTransactionId(txn.id) > 0
                    }
                val template = transactions.maxByOrNull { it.dateTime } ?: transactions.first()
                val resolvedDateTime =
                    mergedDateTime?.takeIf { millis ->
                        transactions.any { it.dateTime == millis }
                    } ?: template.dateTime
                val now = System.currentTimeMillis()

                val merged =
                    TransactionEntity(
                        dateTime = resolvedDateTime,
                        amount = totalAmount,
                        type = mergedType,
                        categoryId = template.categoryId,
                        subcategoryId = template.subcategoryId,
                        note = name,
                        accountId = accountId,
                        relatedAccountId = template.relatedAccountId,
                        loanId = template.loanId,
                        sourceType = if (hasSmsLink) TransactionSourceType.SMS else template.sourceType,
                        sourceReferenceId = if (hasSmsLink) "merge:$now" else template.sourceReferenceId,
                        status = RecordStatus.APPROVED,
                        createdAt = now,
                        updatedAt = now,
                        isEdited = true,
                    )
                if (totalAmount > 0.0) {
                    RepositoryGuards.requireValidTransactionEntity(merged)
                } else {
                    RepositoryGuards.requireValidTransactionEntityAllowZeroAmount(merged)
                }
                val mergedId = transactionDao.insert(merged)

                smsMessageRecordDao.reassignMatchedTransactions(oldIds = ids, newId = mergedId)

                lonePayment?.let { payment ->
                    loanPaymentDao.update(
                        payment.copy(
                            transactionId = mergedId,
                            paymentAmount = totalAmount,
                            notes = name,
                        ),
                    )
                }

                for (old in transactions) {
                    loanTransactionCoordinator.deleteTransaction(
                        old,
                        LoanLinkedTransactionDeleteMode.UNLINK,
                    )
                }

                mergedId
            }
        }
    }
