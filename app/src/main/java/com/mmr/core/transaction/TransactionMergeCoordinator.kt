package com.mmr.core.transaction

import androidx.room.withTransaction
import com.mmr.core.data.repository.RepositoryGuards
import com.mmr.core.database.MmrDatabase
import com.mmr.core.database.dao.LoanPaymentDao
import com.mmr.core.database.dao.SmsMessageRecordDao
import com.mmr.core.database.dao.TransactionDao
import com.mmr.core.database.entity.TransactionEntity
import com.mmr.core.loan.LoanLinkedTransactionDeleteMode
import com.mmr.core.loan.LoanTransactionCoordinator
import com.mmr.core.model.enums.RecordStatus
import com.mmr.core.model.enums.TransactionSourceType
import com.mmr.core.model.enums.TransactionType
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
        ): Long {
            val ids = transactionIds.distinct().filter { it > 0L }
            require(ids.size >= 2) { "Select at least two transactions to merge." }
            val name = mergedName.trim()
            require(name.isNotEmpty()) { "Enter a name for the merged transaction." }

            return database.withTransaction {
                val transactions = ids.mapNotNull { transactionDao.getById(it) }
                require(transactions.size == ids.size) { "One or more transactions were not found." }

                val type = transactions.first().type
                require(transactions.all { it.type == type }) {
                    "Merged transactions must share the same type (expense, income, or transfer)."
                }
                require(transactions.all { it.status == RecordStatus.APPROVED }) {
                    "Only approved transactions can be merged."
                }
                require(transactions.none { it.sourceType == TransactionSourceType.LOAN }) {
                    "Loan ledger entries cannot be merged from the home screen."
                }

                val accountId = transactions.first().accountId
                require(transactions.all { it.accountId == accountId }) {
                    "Merged transactions must use the same account."
                }
                if (type == TransactionType.TRANSFER) {
                    val related = transactions.first().relatedAccountId
                    require(transactions.all { it.relatedAccountId == related }) {
                        "Transfers must share the same from/to accounts to merge."
                    }
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

                val totalAmount = transactions.sumOf { it.amount }
                require(totalAmount > 0.0) { "Merged total must be greater than zero." }

                val hasSmsLink =
                    transactions.any { txn ->
                        smsMessageRecordDao.countByMatchedTransactionId(txn.id) > 0
                    }
                val template = transactions.maxByOrNull { it.dateTime } ?: transactions.first()
                val now = System.currentTimeMillis()

                val merged =
                    TransactionEntity(
                        dateTime = template.dateTime,
                        amount = totalAmount,
                        type = type,
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
                RepositoryGuards.requireValidTransactionEntity(merged)
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
