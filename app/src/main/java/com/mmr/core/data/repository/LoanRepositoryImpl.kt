package com.mmr.core.data.repository

import androidx.room.withTransaction
import com.mmr.core.database.MmrDatabase
import com.mmr.core.database.dao.LoanDao
import com.mmr.core.database.dao.LoanPaymentDao
import com.mmr.core.database.entity.LoanEntity
import com.mmr.core.database.entity.LoanPaymentEntity
import com.mmr.core.database.model.LoanSummaryAggregate
import com.mmr.core.database.relation.LoanWithPayments
import com.mmr.core.domain.repository.LoanRepository
import com.mmr.core.model.enums.LoanPaymentType
import com.mmr.core.model.enums.LoanStatus
import com.mmr.core.model.enums.LoanType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LoanRepositoryImpl @Inject constructor(
    private val database: MmrDatabase,
    private val loanDao: LoanDao,
    private val loanPaymentDao: LoanPaymentDao
) : LoanRepository {

    override fun getAllLoans(): Flow<List<LoanEntity>> =
        loanDao.getAllLoans()

    override fun getLoansWithPayments(): Flow<List<LoanWithPayments>> =
        loanDao.getLoansWithPayments()

    override fun getLoanWithPaymentsById(id: Long): Flow<LoanWithPayments?> =
        if (id <= 0L) {
            kotlinx.coroutines.flow.flowOf(null)
        } else {
            loanDao.getLoanWithPaymentsById(id)
        }

    override fun getLoansByType(type: LoanType): Flow<List<LoanEntity>> =
        loanDao.getLoansByType(type)

    override fun getLoansByStatus(status: LoanStatus): Flow<List<LoanEntity>> =
        loanDao.getLoansByStatus(status)

    override fun getActiveLoans(): Flow<List<LoanEntity>> =
        loanDao.getActiveLoans()

    override suspend fun getLoanById(id: Long): LoanEntity? =
        if (id <= 0L) null else loanDao.getById(id)

    override fun getLoanByIdFlow(id: Long): Flow<LoanEntity?> =
        if (id <= 0L) {
            kotlinx.coroutines.flow.flowOf(null)
        } else {
            loanDao.getByIdFlow(id)
        }

    override suspend fun createLoan(loan: LoanEntity): Long {
        RepositoryGuards.requireValidLoanEntity(loan)
        return loanDao.insert(loan)
    }

    override suspend fun updateLoan(loan: LoanEntity) {
        require(loan.id > 0L) { "Loan id is required." }
        RepositoryGuards.requireValidLoanEntity(loan)
        loanDao.update(loan)
    }

    override suspend fun updateLoanStatus(loanId: Long, status: LoanStatus) {
        if (loanId <= 0L) {
            throw IllegalArgumentException("Loan id is required.")
        }
        val loan = loanDao.getById(loanId)
            ?: throw IllegalArgumentException("Loan with id $loanId not found")
        loanDao.updateOutstandingAndStatus(loanId, loan.outstandingAmount, status)
    }

    override suspend fun deleteLoan(loan: LoanEntity) {
        require(loan.id > 0L) { "Loan id is required." }
        database.withTransaction {
            loanPaymentDao.deletePaymentsForLoan(loan.id)
            loanDao.delete(loan)
        }
    }

    override fun getPaymentsForLoan(loanId: Long): Flow<List<LoanPaymentEntity>> =
        if (loanId <= 0L) {
            kotlinx.coroutines.flow.flowOf(emptyList())
        } else {
            loanPaymentDao.getPaymentsForLoan(loanId)
        }

    override suspend fun recordLoanPayment(
        loanId: Long,
        paymentAmount: Double,
        paymentType: LoanPaymentType,
        principalComponent: Double?,
        interestComponent: Double?,
        transactionId: Long?,
        notes: String?
    ): Long {
        if (loanId <= 0L) {
            throw IllegalArgumentException("Loan id is required.")
        }
        RepositoryGuards.requireValidPaymentAmount(paymentAmount)
        principalComponent?.let {
            require(it.isFinite() && it >= 0.0) { "Principal component cannot be negative." }
        }
        interestComponent?.let {
            require(it.isFinite() && it >= 0.0) { "Interest component cannot be negative." }
        }

        return database.withTransaction {
            val loan = loanDao.getById(loanId)
                ?: throw IllegalArgumentException("Loan with id $loanId not found")
            if (loan.outstandingAmount <= 0.0 && loan.status == LoanStatus.CLOSED) {
                throw IllegalStateException("Loan is already closed.")
            }

            val rawReduction = principalComponent ?: paymentAmount
            val reduction = rawReduction.coerceIn(0.0, loan.outstandingAmount)
            val newOutstanding = (loan.outstandingAmount - reduction).coerceAtLeast(0.0)
            val newStatus = if (newOutstanding <= 0.0) LoanStatus.CLOSED else loan.status

            val payment = LoanPaymentEntity(
                loanId = loanId,
                transactionId = transactionId?.takeIf { it > 0L },
                paymentAmount = paymentAmount,
                paymentDate = System.currentTimeMillis(),
                paymentType = paymentType,
                principalComponent = principalComponent,
                interestComponent = interestComponent,
                outstandingAfterPayment = newOutstanding,
                notes = notes?.trim()?.takeIf { it.isNotEmpty() }
            )

            val paymentId = loanPaymentDao.insert(payment)
            loanDao.updateOutstandingAndStatus(loanId, newOutstanding, newStatus)
            paymentId
        }
    }

    override fun getLoanSummary(): Flow<LoanSummaryAggregate> = combine(
        loanDao.getTotalBorrowedPrincipal(),
        loanDao.getTotalBorrowedOutstanding(),
        loanDao.getTotalLentPrincipal(),
        loanDao.getTotalLentOutstanding(),
        loanDao.getActiveLoansCount()
    ) { borrowedPrincipal, borrowedOutstanding, lentPrincipal, lentOutstanding, activeCount ->
        LoanSummaryAggregate(
            totalBorrowedPrincipal = borrowedPrincipal ?: 0.0,
            totalBorrowedOutstanding = borrowedOutstanding ?: 0.0,
            totalLentPrincipal = lentPrincipal ?: 0.0,
            totalLentOutstanding = lentOutstanding ?: 0.0,
            activeLoansCount = activeCount
        )
    }
}
