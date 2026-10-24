package com.mmr.core.database.dao

import androidx.room.*
import com.mmr.core.database.entity.TransactionApprovalEntity
import com.mmr.core.database.model.TransactionApprovalWithSmsBody
import com.mmr.core.model.enums.SmsApprovalState
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionApprovalDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(approval: TransactionApprovalEntity): Long

    @Update
    suspend fun update(approval: TransactionApprovalEntity)

    @Query("SELECT * FROM transaction_approvals WHERE approvalState = :state ORDER BY extractedDateTime DESC")
    fun getApprovalsByState(state: SmsApprovalState): Flow<List<TransactionApprovalEntity>>

    @Query(
        """
        SELECT ta.*,
            COALESCE(smr.messageBodyFull, smr.messageBodySnippet, ta.rawBodySnippet) AS smsFullBody
        FROM transaction_approvals ta
        LEFT JOIN sms_message_records smr ON ta.smsMessageRecordId = smr.id
        WHERE ta.approvalState = :state
        ORDER BY ta.extractedDateTime DESC
        """,
    )
    fun getApprovalsByStateWithSmsBody(state: SmsApprovalState): Flow<List<TransactionApprovalWithSmsBody>>

    @Query("SELECT * FROM transaction_approvals WHERE approvalState = 'PENDING' ORDER BY extractedDateTime DESC")
    fun getPendingApprovals(): Flow<List<TransactionApprovalEntity>>

    @Query("SELECT COUNT(*) FROM transaction_approvals WHERE approvalState = 'PENDING'")
    fun getPendingCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM transaction_approvals WHERE approvalState = 'PENDING'")
    suspend fun getPendingCountOnce(): Int

    @Query("SELECT * FROM transaction_approvals WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): TransactionApprovalEntity?

    @Query("UPDATE transaction_approvals SET approvalState = :state, updatedAt = :now WHERE id = :id")
    suspend fun updateApprovalState(
        id: Long,
        state: SmsApprovalState,
        now: Long = System.currentTimeMillis(),
    )

    @Query(
        """
        UPDATE transaction_approvals
        SET approvalState = :state, updatedAt = :now
        WHERE id = :id AND approvalState = 'PENDING'
        """,
    )
    suspend fun updateApprovalStateIfPending(
        id: Long,
        state: SmsApprovalState,
        now: Long = System.currentTimeMillis(),
    ): Int

    @Query("DELETE FROM transaction_approvals WHERE id = :id")
    suspend fun deleteById(id: Long)
}
