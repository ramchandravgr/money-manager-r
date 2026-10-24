package com.mmr.core.database.dao

import androidx.room.*
import com.mmr.core.database.entity.LoanEntity
import com.mmr.core.database.entity.LoanPaymentEntity
import com.mmr.core.database.relation.LoanWithPayments
import com.mmr.core.model.enums.LoanStatus
import com.mmr.core.model.enums.LoanType
import kotlinx.coroutines.flow.Flow

@Dao
interface LoanDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(loan: LoanEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLoans(loans: List<LoanEntity>)

    @Update
    suspend fun update(loan: LoanEntity)

    @Delete
    suspend fun delete(loan: LoanEntity)

    @Query("SELECT * FROM loans WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): LoanEntity?

    @Query("SELECT * FROM loans WHERE syncId = :syncId LIMIT 1")
    suspend fun getBySyncId(syncId: String): LoanEntity?

    @Query("SELECT * FROM loans WHERE id = :id LIMIT 1")
    fun getByIdFlow(id: Long): Flow<LoanEntity?>

    @Transaction
    @Query("SELECT * FROM loans WHERE id = :id LIMIT 1")
    fun getLoanWithPaymentsById(id: Long): Flow<LoanWithPayments?>

    @Transaction
    @Query("SELECT * FROM loans ORDER BY updatedAt DESC")
    fun getLoansWithPayments(): Flow<List<LoanWithPayments>>

    @Query("SELECT * FROM loans ORDER BY updatedAt DESC")
    fun getAllLoans(): Flow<List<LoanEntity>>

    @Query("SELECT * FROM loans WHERE status = :status ORDER BY dueDate ASC")
    fun getLoansByStatus(status: LoanStatus): Flow<List<LoanEntity>>

    @Query("SELECT * FROM loans WHERE loanType = :type ORDER BY updatedAt DESC")
    fun getLoansByType(type: LoanType): Flow<List<LoanEntity>>

    @Query("SELECT * FROM loans WHERE loanType = :type AND status = :status ORDER BY dueDate ASC")
    fun getLoansByTypeAndStatus(
        type: LoanType,
        status: LoanStatus,
    ): Flow<List<LoanEntity>>

    @Query("SELECT * FROM loans WHERE status = 'ACTIVE' ORDER BY dueDate ASC")
    fun getActiveLoans(): Flow<List<LoanEntity>>

    @Query("SELECT SUM(outstandingAmount) FROM loans WHERE status = 'ACTIVE' AND loanType = 'BORROWED'")
    fun getTotalBorrowedOutstanding(): Flow<Double?>

    @Query("SELECT SUM(outstandingAmount) FROM loans WHERE status = 'ACTIVE' AND loanType = 'LENT'")
    fun getTotalLentOutstanding(): Flow<Double?>

    @Query("SELECT SUM(principalAmount) FROM loans WHERE loanType = 'BORROWED'")
    fun getTotalBorrowedPrincipal(): Flow<Double?>

    @Query("SELECT SUM(principalAmount) FROM loans WHERE loanType = 'LENT'")
    fun getTotalLentPrincipal(): Flow<Double?>

    @Query("SELECT COUNT(*) FROM loans WHERE status = 'ACTIVE'")
    fun getActiveLoansCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM loans WHERE linkedLoanAccountId = :accountId")
    suspend fun countByLinkedLoanAccountId(accountId: Long): Int

    @Query("SELECT COUNT(*) FROM loans WHERE linkedDebitAccountId = :accountId")
    suspend fun countByLinkedDebitAccountId(accountId: Long): Int

    @Query(
        """
        UPDATE loans
        SET linkedDebitAccountId = :toAccountId, updatedAt = :now
        WHERE linkedDebitAccountId = :fromAccountId
        """,
    )
    suspend fun reassignLinkedDebitAccount(
        fromAccountId: Long,
        toAccountId: Long,
        now: Long = System.currentTimeMillis(),
    )

    @Query(
        "UPDATE loans SET outstandingAmount = :newOutstanding, status = :newStatus, updatedAt = :now WHERE id = :loanId",
    )
    suspend fun updateOutstandingAndStatus(
        loanId: Long,
        newOutstanding: Double,
        newStatus: LoanStatus,
        now: Long = System.currentTimeMillis(),
    )
}

@Dao
interface LoanPaymentDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(payment: LoanPaymentEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayments(payments: List<LoanPaymentEntity>)

    @Update
    suspend fun update(payment: LoanPaymentEntity)

    @Delete
    suspend fun delete(payment: LoanPaymentEntity)

    @Query("SELECT * FROM loan_payments WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): LoanPaymentEntity?

    @Query("SELECT * FROM loan_payments WHERE syncId = :syncId LIMIT 1")
    suspend fun getBySyncId(syncId: String): LoanPaymentEntity?

    @Query("SELECT * FROM loan_payments WHERE transactionId = :transactionId LIMIT 1")
    suspend fun getByTransactionId(transactionId: Long): LoanPaymentEntity?

    @Query("UPDATE loan_payments SET transactionId = NULL WHERE transactionId = :transactionId")
    suspend fun clearTransactionLink(transactionId: Long)

    @Query("SELECT * FROM loan_payments WHERE loanId = :loanId ORDER BY paymentDate DESC")
    fun getPaymentsForLoan(loanId: Long): Flow<List<LoanPaymentEntity>>

    @Query("SELECT SUM(paymentAmount) FROM loan_payments WHERE loanId = :loanId")
    fun getTotalRepaidForLoan(loanId: Long): Flow<Double?>

    @Query("DELETE FROM loan_payments WHERE loanId = :loanId")
    suspend fun deletePaymentsForLoan(loanId: Long)
}
