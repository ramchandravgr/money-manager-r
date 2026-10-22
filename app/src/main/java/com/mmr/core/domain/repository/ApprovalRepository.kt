package com.mmr.core.domain.repository

import com.mmr.core.database.entity.TransactionApprovalEntity
import com.mmr.core.domain.model.ManualSmsDuplicateResolution
import com.mmr.core.domain.model.ManualSmsSubmitResult
import com.mmr.core.model.enums.SmsApprovalState
import com.mmr.core.model.enums.TransactionType
import com.mmr.core.parser.model.ParsedSmsCandidate
import kotlinx.coroutines.flow.Flow

interface ApprovalRepository {
    fun getApprovalsByState(state: SmsApprovalState): Flow<List<TransactionApprovalEntity>>
    fun getPendingCount(): Flow<Int>
    suspend fun getApprovalById(id: Long): TransactionApprovalEntity?
    suspend fun insertCandidates(candidates: List<ParsedSmsCandidate>): Int
    suspend fun updateApproval(
        id: Long,
        amount: Double,
        type: TransactionType,
        categoryId: Long?,
        accountId: Long?,
        note: String?
    )
    suspend fun approveCandidate(id: Long, fallbackAccountId: Long, fallbackCategoryId: Long?): Long
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
        existingApprovalIdForRevert: Long? = null
    ): ManualSmsSubmitResult
}
