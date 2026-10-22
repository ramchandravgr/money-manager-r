package com.mmr.core.domain.repository

import com.mmr.core.database.entity.LoanEntity
import com.mmr.core.database.entity.LoanPaymentEntity
import com.mmr.core.database.model.LoanSummaryAggregate
import com.mmr.core.database.relation.LoanWithPayments
import com.mmr.core.model.enums.LoanPaymentType
import com.mmr.core.model.enums.LoanStatus
import com.mmr.core.model.enums.LoanType
import kotlinx.coroutines.flow.Flow

interface LoanRepository {
    fun getAllLoans(): Flow<List<LoanEntity>>
    fun getLoansWithPayments(): Flow<List<LoanWithPayments>>
    fun getLoanWithPaymentsById(id: Long): Flow<LoanWithPayments?>
    fun getLoansByType(type: LoanType): Flow<List<LoanEntity>>
    fun getLoansByStatus(status: LoanStatus): Flow<List<LoanEntity>>
    fun getActiveLoans(): Flow<List<LoanEntity>>
    suspend fun getLoanById(id: Long): LoanEntity?
    fun getLoanByIdFlow(id: Long): Flow<LoanEntity?>

    suspend fun createLoan(loan: LoanEntity): Long
    suspend fun updateLoan(loan: LoanEntity)
    suspend fun updateLoanStatus(loanId: Long, status: LoanStatus)
    suspend fun deleteLoan(loan: LoanEntity)

    fun getPaymentsForLoan(loanId: Long): Flow<List<LoanPaymentEntity>>
    suspend fun recordLoanPayment(
        loanId: Long,
        paymentAmount: Double,
        paymentType: LoanPaymentType = LoanPaymentType.PART_PAYMENT,
        principalComponent: Double? = null,
        interestComponent: Double? = null,
        transactionId: Long? = null,
        notes: String? = null
    ): Long

    fun getLoanSummary(): Flow<LoanSummaryAggregate>
}
