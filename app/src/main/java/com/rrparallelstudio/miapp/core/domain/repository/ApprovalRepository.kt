package com.rrparallelstudio.miapp.core.domain.repository

import com.rrparallelstudio.miapp.core.database.entity.TransactionApprovalEntity
import com.rrparallelstudio.miapp.core.domain.model.ManualSmsDuplicateResolution
import com.rrparallelstudio.miapp.core.domain.model.ManualSmsSubmitResult
import com.rrparallelstudio.miapp.core.domain.model.PendingApprovalWithSms
import com.rrparallelstudio.miapp.core.domain.model.SmsMergeCandidate
import com.rrparallelstudio.miapp.core.model.enums.SmsApprovalState
import com.rrparallelstudio.miapp.core.model.enums.TransactionType
import com.rrparallelstudio.miapp.core.parser.model.ParsedSmsCandidate
import kotlinx.coroutines.flow.Flow

interface ApprovalRepository {
    fun getApprovalsByState(state: SmsApprovalState): Flow<List<TransactionApprovalEntity>>

    fun getApprovalsByStateWithSms(state: SmsApprovalState): Flow<List<PendingApprovalWithSms>>

    fun getPendingCount(): Flow<Int>

    suspend fun getPendingCountOnce(): Int

    suspend fun getLatestQuickApprovablePendingId(): Long?

    suspend fun getApprovalById(id: Long): TransactionApprovalEntity?

    suspend fun insertCandidates(candidates: List<ParsedSmsCandidate>): Int

    /** Re-parses pending approvals with the current rules; clears queue entries that are no longer transactions. */
    suspend fun reconcilePendingWithCurrentParser(): Int

    suspend fun updateApproval(
        id: Long,
        amount: Double,
        type: TransactionType,
        categoryId: Long?,
        accountId: Long?,
        note: String?,
        dateTimeMillis: Long? = null,
    )

    suspend fun findMergeCandidatesForApproval(approvalId: Long): List<SmsMergeCandidate>

    suspend fun approveCandidate(
        id: Long,
        fallbackAccountId: Long,
        fallbackCategoryId: Long?,
        subcategoryId: Long? = null,
        relatedAccountId: Long? = null,
        loanId: Long? = null,
        /** When true, may create credit-card/wallet accounts from SMS metadata (Approval Center direct approve only). */
        provisionAccountFromSms: Boolean = false,
    ): Long

    suspend fun mergeCandidateWithTransaction(
        approvalId: Long,
        transactionId: Long,
        categoryId: Long? = null,
        subcategoryId: Long? = null,
        useLedgerCategory: Boolean = false,
    ): Long

    suspend fun mergeApprovalsWithTransaction(
        approvalIds: List<Long>,
        transactionId: Long,
        categoryId: Long? = null,
        subcategoryId: Long? = null,
        useLedgerCategory: Boolean = false,
    ): Long

    /**
     * Applies a pending [SmsApprovalKind.CARD_REFUND] to an existing card purchase.
     * Reduces purchase net amount and links refund SMS to the same transaction.
     */
    suspend fun applyCardRefund(
        approvalId: Long,
        purchaseTransactionId: Long,
        refundAmount: Double,
    ): Long

    suspend fun rejectCandidate(id: Long)

    suspend fun markDuplicate(id: Long)

    /**
     * Parses [body] as SMS, then inserts a pending approval or returns a duplicate prompt.
     * When [resolution] is set, applies revert-and-replace or add-as-new for an exact body match.
     */
    suspend fun submitManualSms(
        sender: String,
        body: String,
        timestampMillis: Long,
        resolution: ManualSmsDuplicateResolution? = null,
        existingApprovalIdForRevert: Long? = null,
    ): ManualSmsSubmitResult
}
