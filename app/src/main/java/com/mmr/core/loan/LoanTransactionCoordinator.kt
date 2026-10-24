package com.mmr.core.loan

import androidx.room.withTransaction
import com.mmr.core.database.MmrDatabase
import com.mmr.core.database.dao.LoanDao
import com.mmr.core.database.dao.LoanPaymentDao
import com.mmr.core.database.dao.LoanScheduleDao
import com.mmr.core.database.dao.TransactionDao
import com.mmr.core.database.entity.LoanPaymentEntity
import com.mmr.core.database.entity.TransactionEntity
import com.mmr.core.domain.repository.TransactionAttachmentRepository
import com.mmr.core.lansync.LanSyncTombstoneRecorder
import com.mmr.core.lansync.SyncEntityType
import com.mmr.core.model.enums.LoanScheduleStatus
import com.mmr.core.model.enums.LoanStatus
import javax.inject.Inject
import javax.inject.Singleton

enum class LoanLinkedTransactionDeleteMode {
    UNLINK,
    REVERSE,
}

@Singleton
class LoanTransactionCoordinator
    @Inject
    constructor(
        private val database: MmrDatabase,
        private val loanPaymentDao: LoanPaymentDao,
        private val loanDao: LoanDao,
        private val loanScheduleDao: LoanScheduleDao,
        private val transactionDao: TransactionDao,
        private val attachmentRepository: TransactionAttachmentRepository,
        private val tombstoneRecorder: LanSyncTombstoneRecorder,
    ) {
        suspend fun findPaymentForTransaction(transactionId: Long): LoanPaymentEntity? {
            if (transactionId <= 0L) return null
            return loanPaymentDao.getByTransactionId(transactionId)
        }

        suspend fun syncPaymentFromTransaction(transaction: TransactionEntity) {
            if (transaction.id <= 0L) return
            val payment = loanPaymentDao.getByTransactionId(transaction.id) ?: return
            val updated =
                payment.copy(
                    paymentAmount = transaction.amount,
                    paymentDate = transaction.dateTime,
                    notes = transaction.note?.trim()?.takeIf { it.isNotEmpty() } ?: payment.notes,
                )
            loanPaymentDao.update(updated)
        }

        suspend fun deleteTransaction(
            transaction: TransactionEntity,
            loanDeleteMode: LoanLinkedTransactionDeleteMode?,
        ) {
            require(transaction.id > 0L)
            val payment = loanPaymentDao.getByTransactionId(transaction.id)
            tombstoneRecorder.recordDeletion(transaction.syncId, SyncEntityType.TRANSACTION)
            database.withTransaction {
                if (payment != null) {
                    when (loanDeleteMode) {
                        LoanLinkedTransactionDeleteMode.REVERSE -> reverseLoanPayment(payment)
                        LoanLinkedTransactionDeleteMode.UNLINK, null ->
                            loanPaymentDao.clearTransactionLink(
                                transaction.id,
                            )
                    }
                }
                attachmentRepository.deleteAllForTransaction(transaction.id)
                transactionDao.delete(transaction)
            }
        }

        private suspend fun reverseLoanPayment(payment: LoanPaymentEntity) {
            val loan =
                loanDao.getById(payment.loanId) ?: run {
                    loanPaymentDao.delete(payment)
                    return
                }
            val principalRestored = payment.principalComponent ?: payment.paymentAmount
            val newOutstanding = (loan.outstandingAmount + principalRestored).coerceAtLeast(0.0)
            val newStatus =
                if (newOutstanding > 0.0 && loan.status == LoanStatus.CLOSED) {
                    LoanStatus.ACTIVE
                } else {
                    loan.status
                }
            loanDao.update(
                loan.copy(
                    outstandingAmount = newOutstanding,
                    status = newStatus,
                    updatedAt = System.currentTimeMillis(),
                ),
            )
            loanScheduleDao.findByLinkedPaymentId(payment.id).forEach { entry ->
                loanScheduleDao.markEntry(entry.id, LoanScheduleStatus.PLANNED, null)
            }
            tombstoneRecorder.recordDeletion(payment.syncId, SyncEntityType.LOAN_PAYMENT)
            loanPaymentDao.delete(payment)
        }
    }
