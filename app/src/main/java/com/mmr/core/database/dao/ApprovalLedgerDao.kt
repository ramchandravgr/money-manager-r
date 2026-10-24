package com.mmr.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.mmr.core.database.entity.SmsMessageRecordEntity
import com.mmr.core.database.entity.TransactionApprovalEntity
import com.mmr.core.database.entity.TransactionEntity
import com.mmr.core.database.model.ExactBodyDuplicateRow
import com.mmr.core.model.enums.SmsApprovalState
import com.mmr.core.model.enums.SmsLinkRole

/**
 * Multi-table approval flows executed atomically.
 */
@Dao
interface ApprovalLedgerDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSmsRecord(record: SmsMessageRecordEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertApproval(approval: TransactionApprovalEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Update
    suspend fun updateSmsRecord(record: SmsMessageRecordEntity)

    @Update
    suspend fun updateApproval(approval: TransactionApprovalEntity)

    @Query(
        """
        SELECT ta.id AS approvalId, ta.approvalState AS approvalState
        FROM transaction_approvals ta
        INNER JOIN sms_message_records smr ON smr.id = ta.smsMessageRecordId
        WHERE smr.messageBodyFull = :body
        AND ta.approvalState IN ('PENDING', 'REJECTED', 'APPROVED', 'DUPLICATE')
        ORDER BY ta.updatedAt DESC
        LIMIT 1
        """,
    )
    suspend fun findLatestApprovalByExactBody(body: String): ExactBodyDuplicateRow?

    @Query("SELECT EXISTS(SELECT 1 FROM sms_message_records WHERE messageBodyHash = :hash)")
    suspend fun isDuplicateHash(hash: String): Boolean

    @Query("SELECT * FROM sms_message_records WHERE id = :id LIMIT 1")
    suspend fun getSmsRecordById(id: Long): SmsMessageRecordEntity?

    @Query("SELECT * FROM transaction_approvals WHERE id = :id LIMIT 1")
    suspend fun getApprovalById(id: Long): TransactionApprovalEntity?

    @Query(
        """
        UPDATE transaction_approvals
        SET approvalState = :state, updatedAt = :now
        WHERE id = :id AND approvalState = 'PENDING'
        """,
    )
    suspend fun markApprovalStateIfPending(
        id: Long,
        state: SmsApprovalState,
        now: Long = System.currentTimeMillis(),
    ): Int

    @Transaction
    suspend fun insertPendingCandidate(
        record: SmsMessageRecordEntity,
        approval: TransactionApprovalEntity,
    ): Long? {
        if (isDuplicateHash(record.messageBodyHash)) return null
        val recordId = insertSmsRecord(record)
        if (recordId <= 0L) return null
        return insertApproval(approval.copy(smsMessageRecordId = recordId))
    }

    @Transaction
    suspend fun approvePendingAndCreateTransaction(
        approvalId: Long,
        transaction: TransactionEntity,
        smsRecordId: Long,
        approvedSms: SmsMessageRecordEntity,
    ): Long {
        val updated = markApprovalStateIfPending(approvalId, SmsApprovalState.APPROVED)
        if (updated == 0) {
            throw IllegalStateException("This entry is no longer pending.")
        }
        val transactionId = insertTransaction(transaction)
        updateSmsRecord(
            approvedSms.copy(
                approvalState = SmsApprovalState.APPROVED,
                matchedTransactionId = transactionId,
                linkRole = approvedSms.linkRole ?: SmsLinkRole.PURCHASE,
            ),
        )
        return transactionId
    }

    @Transaction
    suspend fun mergeApprovalWithExistingTransaction(
        approvalId: Long,
        mergedTransaction: TransactionEntity,
        smsRecordId: Long,
        approvedSms: SmsMessageRecordEntity,
    ): Long {
        val updated = markApprovalStateIfPending(approvalId, SmsApprovalState.APPROVED)
        if (updated == 0) {
            throw IllegalStateException("This entry is no longer pending.")
        }
        updateTransaction(mergedTransaction)
        updateSmsRecord(
            approvedSms.copy(
                approvalState = SmsApprovalState.APPROVED,
                matchedTransactionId = mergedTransaction.id,
                linkRole = approvedSms.linkRole ?: SmsLinkRole.PURCHASE,
            ),
        )
        return mergedTransaction.id
    }

    @Transaction
    suspend fun applyCardRefundToPurchase(
        approvalId: Long,
        updatedPurchase: TransactionEntity,
        refundSms: SmsMessageRecordEntity,
    ): Long {
        val updated = markApprovalStateIfPending(approvalId, SmsApprovalState.APPROVED)
        if (updated == 0) {
            throw IllegalStateException("This entry is no longer pending.")
        }
        updateTransaction(updatedPurchase)
        updateSmsRecord(
            refundSms.copy(
                approvalState = SmsApprovalState.APPROVED,
                matchedTransactionId = updatedPurchase.id,
                linkRole = SmsLinkRole.REFUND,
            ),
        )
        return updatedPurchase.id
    }

    @Transaction
    suspend fun replaceApprovalWithPendingParse(
        approvalId: Long,
        smsRecordId: Long,
        updatedSms: SmsMessageRecordEntity,
        updatedApproval: TransactionApprovalEntity,
    ) {
        updateSmsRecord(updatedSms.copy(id = smsRecordId))
        updateApproval(updatedApproval.copy(id = approvalId, smsMessageRecordId = smsRecordId))
    }

    @Transaction
    suspend fun updatePendingApprovalState(
        approvalId: Long,
        smsRecordId: Long,
        newState: SmsApprovalState,
    ): Boolean {
        val updated = markApprovalStateIfPending(approvalId, newState)
        if (updated == 0) return false
        val sms = getSmsRecordById(smsRecordId)
        if (sms != null) {
            updateSmsRecord(sms.copy(approvalState = newState))
        }
        return true
    }
}
