package com.mmr.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mmr.core.database.entity.LoanInterestRevisionEntity
import com.mmr.core.database.entity.LoanScheduleEntryEntity
import com.mmr.core.model.enums.LoanScheduleStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface LoanInterestRevisionDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(revision: LoanInterestRevisionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(revisions: List<LoanInterestRevisionEntity>)

    @Query(
        """
        SELECT * FROM loan_interest_revisions
        WHERE loanId = :loanId
        ORDER BY effectiveFromMillis DESC, id DESC
        """,
    )
    fun getForLoan(loanId: Long): Flow<List<LoanInterestRevisionEntity>>

    @Query(
        """
        SELECT * FROM loan_interest_revisions
        WHERE loanId = :loanId AND effectiveFromMillis <= :atMillis
        ORDER BY effectiveFromMillis DESC, id DESC
        LIMIT 1
        """,
    )
    suspend fun getRateEffectiveAt(
        loanId: Long,
        atMillis: Long,
    ): LoanInterestRevisionEntity?

    @Query("SELECT * FROM loan_interest_revisions WHERE syncId = :syncId LIMIT 1")
    suspend fun getBySyncId(syncId: String): LoanInterestRevisionEntity?
}

@Dao
interface LoanScheduleDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(entries: List<LoanScheduleEntryEntity>)

    @Query(
        """
        SELECT * FROM loan_schedule_entries
        WHERE loanId = :loanId AND status = :status
        ORDER BY dueDateMillis ASC, sequenceNumber ASC
        """,
    )
    fun getByStatus(
        loanId: Long,
        status: LoanScheduleStatus,
    ): Flow<List<LoanScheduleEntryEntity>>

    @Query(
        """
        SELECT * FROM loan_schedule_entries
        WHERE loanId = :loanId
        ORDER BY dueDateMillis ASC, sequenceNumber ASC
        """,
    )
    fun getAllForLoan(loanId: Long): Flow<List<LoanScheduleEntryEntity>>

    @Query(
        """
        DELETE FROM loan_schedule_entries
        WHERE loanId = :loanId AND status = 'PLANNED' AND dueDateMillis >= :fromMillis
        """,
    )
    suspend fun deletePlannedFrom(
        loanId: Long,
        fromMillis: Long,
    )

    @Query("DELETE FROM loan_schedule_entries WHERE loanId = :loanId")
    suspend fun deleteAllForLoan(loanId: Long)

    @Query("DELETE FROM loan_schedule_entries WHERE loanId = :loanId AND status = 'PLANNED'")
    suspend fun deletePlannedForLoan(loanId: Long)

    @Query(
        """
        SELECT * FROM loan_schedule_entries
        WHERE linkedPaymentId = :paymentId
        """,
    )
    suspend fun findByLinkedPaymentId(paymentId: Long): List<LoanScheduleEntryEntity>

    @Query("SELECT COALESCE(MAX(sequenceNumber), 0) FROM loan_schedule_entries WHERE loanId = :loanId")
    suspend fun maxSequenceNumber(loanId: Long): Int

    @Query(
        """
        UPDATE loan_schedule_entries
        SET status = :status, linkedPaymentId = :paymentId
        WHERE id = :entryId
        """,
    )
    suspend fun markEntry(
        entryId: Long,
        status: LoanScheduleStatus,
        paymentId: Long?,
    )

    @Query(
        """
        SELECT * FROM loan_schedule_entries
        WHERE status = 'PLANNED' AND dueDateMillis <= :upToMillis
        ORDER BY dueDateMillis ASC, sequenceNumber ASC
        """,
    )
    suspend fun getPlannedDueUntil(upToMillis: Long): List<LoanScheduleEntryEntity>

    @Query("SELECT * FROM loan_schedule_entries WHERE syncId = :syncId LIMIT 1")
    suspend fun getBySyncId(syncId: String): LoanScheduleEntryEntity?
}
