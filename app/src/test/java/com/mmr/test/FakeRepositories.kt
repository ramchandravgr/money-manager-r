package com.mmr.test

import com.mmr.core.data.preferences.LanSyncPeerMode
import com.mmr.core.database.entity.CategoryEntity
import com.mmr.core.database.entity.LoanEntity
import com.mmr.core.database.entity.LoanPaymentEntity
import com.mmr.core.database.entity.SubcategoryEntity
import com.mmr.core.database.entity.TransactionApprovalEntity
import com.mmr.core.database.entity.TransactionEntity
import com.mmr.core.database.model.LoanSummaryAggregate
import com.mmr.core.database.relation.LoanWithPayments
import com.mmr.core.database.relation.TransactionWithDetails
import com.mmr.core.domain.repository.ApprovalRepository
import com.mmr.core.domain.repository.CategoryRepository
import com.mmr.core.domain.repository.LanSyncNowResult
import com.mmr.core.domain.repository.LanSyncRepository
import com.mmr.core.domain.repository.LanSyncSessionState
import com.mmr.core.domain.repository.LoanRepository
import com.mmr.core.domain.repository.TransactionRepository
import com.mmr.core.lansync.LanSyncMergeOptions
import com.mmr.core.lansync.LanSyncMergeSummary
import com.mmr.core.lansync.LanSyncPairingOffer
import com.mmr.core.model.category.CategoryWithChildren
import com.mmr.core.model.enums.CategoryType
import com.mmr.core.model.enums.LoanPaymentType
import com.mmr.core.model.enums.LoanStatus
import com.mmr.core.model.enums.LoanType
import com.mmr.core.model.enums.SmsApprovalState
import com.mmr.core.model.enums.TransactionType
import com.mmr.core.parser.model.ParsedSmsCandidate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class FakeTransactionRepository : TransactionRepository {
    var approvedTransactions: List<TransactionWithDetails> = emptyList()
    var approvedTransactionsError: Throwable? = null

    override fun getApprovedTransactionsDetailed(): Flow<List<TransactionWithDetails>> =
        flow {
            approvedTransactionsError?.let { throw it }
            emit(approvedTransactions)
        }

    override fun getFilteredTransactions(
        startDate: Long,
        endDate: Long,
        accountId: Long?,
        categoryId: Long?,
        subcategoryId: Long?,
        type: TransactionType?,
    ): Flow<List<TransactionWithDetails>> = flowOf(emptyList())

    override fun searchTransactions(query: String): Flow<List<TransactionWithDetails>> = flowOf(emptyList())

    override suspend fun getTransactionDetailedById(id: Long): TransactionWithDetails? = null

    override fun getTransactionsForLoan(loanId: Long): Flow<List<TransactionEntity>> = flowOf(emptyList())

    override suspend fun insertTransaction(transaction: TransactionEntity): Long = 0L

    override suspend fun updateTransaction(transaction: TransactionEntity) = Unit

    override suspend fun deleteTransaction(transaction: TransactionEntity) = Unit

    override suspend fun hasLinkedLoanPayment(transactionId: Long): Boolean = false

    override suspend fun deleteTransactionWithLoanPolicy(
        transaction: TransactionEntity,
        mode: com.mmr.core.loan.LoanLinkedTransactionDeleteMode,
    ) = Unit

    override suspend fun mergeTransactions(
        transactionIds: List<Long>,
        mergedName: String,
        mergedDateTime: Long?,
    ): Long = 0L

    override fun getLinkedSmsForTransaction(
        transactionId: Long,
    ): Flow<List<com.mmr.core.database.entity.SmsMessageRecordEntity>> = flowOf(emptyList())

    override fun getTotalExpense(): Flow<Double?> = flowOf(0.0)

    override fun getTotalIncome(): Flow<Double?> = flowOf(0.0)

    override fun getTotalExpenseBetween(
        startDate: Long,
        endDate: Long,
    ): Flow<Double?> = flowOf(0.0)

    override fun getTotalIncomeBetween(
        startDate: Long,
        endDate: Long,
    ): Flow<Double?> = flowOf(0.0)

    override fun getCategorySpendingBetween(
        startDate: Long,
        endDate: Long,
    ): Flow<List<com.mmr.core.database.model.CategorySpending>> = flowOf(emptyList())

    override fun getCategoryIncomeBetween(
        startDate: Long,
        endDate: Long,
    ): Flow<List<com.mmr.core.database.model.CategorySpending>> = flowOf(emptyList())

    override fun getSubcategorySpendingForCategory(
        categoryId: Long,
        type: com.mmr.core.model.enums.TransactionType,
        startDate: Long,
        endDate: Long,
    ): Flow<List<com.mmr.core.database.model.SubcategorySpending>> = flowOf(emptyList())

    override fun getCategoryMonthlyTotals(
        categoryId: Long,
        type: com.mmr.core.model.enums.TransactionType,
        startDate: Long,
        endDate: Long,
    ): Flow<List<com.mmr.core.database.model.MonthTotalRow>> = flowOf(emptyList())

    override suspend fun suggestTransactionNotes(
        prefix: String,
        limit: Int,
    ): List<String> = emptyList()
}

class FakeApprovalRepository(
    pendingCount: Int = 0,
) : ApprovalRepository {
    private val pendingCountFlow = MutableStateFlow(pendingCount)

    fun setPendingCount(count: Int) {
        pendingCountFlow.value = count
    }

    override fun getApprovalsByState(state: SmsApprovalState): Flow<List<TransactionApprovalEntity>> =
        flowOf(emptyList())

    override fun getApprovalsByStateWithSms(
        state: SmsApprovalState,
    ): Flow<List<com.mmr.core.domain.model.PendingApprovalWithSms>> = flowOf(emptyList())

    override fun getPendingCount(): Flow<Int> = pendingCountFlow

    override suspend fun getPendingCountOnce(): Int = pendingCountFlow.value

    override suspend fun getApprovalById(id: Long): TransactionApprovalEntity? = null

    override suspend fun insertCandidates(candidates: List<ParsedSmsCandidate>): Int = 0

    override suspend fun reconcilePendingWithCurrentParser(): Int = 0

    override suspend fun updateApproval(
        id: Long,
        amount: Double,
        type: com.mmr.core.model.enums.TransactionType,
        categoryId: Long?,
        accountId: Long?,
        note: String?,
        dateTimeMillis: Long?,
    ) = Unit

    override suspend fun findMergeCandidatesForApproval(
        approvalId: Long,
    ): List<com.mmr.core.domain.model.SmsMergeCandidate> = emptyList()

    override suspend fun approveCandidate(
        id: Long,
        fallbackAccountId: Long,
        fallbackCategoryId: Long?,
        subcategoryId: Long?,
        relatedAccountId: Long?,
    ): Long = 0L

    override suspend fun mergeCandidateWithTransaction(
        approvalId: Long,
        transactionId: Long,
        categoryId: Long?,
        subcategoryId: Long?,
        useLedgerCategory: Boolean,
    ): Long = transactionId

    override suspend fun applyCardRefund(
        approvalId: Long,
        purchaseTransactionId: Long,
        refundAmount: Double,
    ): Long = purchaseTransactionId

    override suspend fun mergeApprovalsWithTransaction(
        approvalIds: List<Long>,
        transactionId: Long,
        categoryId: Long?,
        subcategoryId: Long?,
        useLedgerCategory: Boolean,
    ): Long = transactionId

    override suspend fun rejectCandidate(id: Long) = Unit

    override suspend fun markDuplicate(id: Long) = Unit

    override suspend fun submitManualSms(
        sender: String,
        body: String,
        timestampMillis: Long,
        resolution: com.mmr.core.domain.model.ManualSmsDuplicateResolution?,
        existingApprovalIdForRevert: Long?,
    ): com.mmr.core.domain.model.ManualSmsSubmitResult =
        com.mmr.core.domain.model.ManualSmsSubmitResult
            .Added(0L)
}

class FakeCategoryRepository(
    categories: List<CategoryEntity> = emptyList(),
    private val subcategoriesByCategory: Map<Long, List<SubcategoryEntity>> = emptyMap(),
    categoryTree: List<CategoryWithChildren> = emptyList(),
) : CategoryRepository {
    private val categoriesFlow = MutableStateFlow(categories)
    private val categoryTreeFlow = MutableStateFlow(categoryTree)

    override fun getActiveCategories(): Flow<List<CategoryEntity>> = categoriesFlow

    override fun getSubcategoriesForCategory(categoryId: Long): Flow<List<SubcategoryEntity>> =
        flowOf(subcategoriesByCategory[categoryId] ?: emptyList())

    override fun observeActiveCategoryTree(): Flow<List<CategoryWithChildren>> = categoryTreeFlow

    override fun observeActiveCategoryTreeByType(type: CategoryType): Flow<List<CategoryWithChildren>> =
        categoryTreeFlow.map { tree -> tree.filter { it.category.type == type } }

    override fun observeCategoryTree(categoryId: Long): Flow<CategoryWithChildren?> =
        categoryTreeFlow.map { tree -> tree.firstOrNull { it.category.id.value == categoryId } }

    override suspend fun resolveCategory(categoryId: Long): CategoryEntity? =
        categoriesFlow.value.firstOrNull { it.id == categoryId && it.isActive }

    override suspend fun resolveSubcategory(subcategoryId: Long): SubcategoryEntity? =
        subcategoriesByCategory.values.flatten().firstOrNull { it.id == subcategoryId && it.isActive }

    override suspend fun resolveCategoryWithChildren(categoryId: Long): CategoryWithChildren? =
        categoryTreeFlow.value.firstOrNull { it.category.id.value == categoryId }

    override suspend fun createCategory(category: CategoryEntity): Long = category.id

    override suspend fun updateCategory(category: CategoryEntity) = Unit

    override suspend fun countTransactionsForCategory(categoryId: Long): Int = 0

    override suspend fun deleteCategory(
        categoryId: Long,
        transactionMode: com.mmr.core.domain.model.CategoryDeleteTransactionMode,
        migrateToCategoryId: Long?,
    ) = Unit

    override suspend fun createSubcategory(subcategory: SubcategoryEntity): Long = subcategory.id

    override suspend fun updateSubcategory(subcategory: SubcategoryEntity) = Unit

    override suspend fun deleteSubcategory(subcategoryId: Long) = Unit
}

class FakeLoanRepository : LoanRepository {
    private val loansFlow = MutableStateFlow<List<LoanEntity>>(emptyList())
    var allLoansError: Throwable? = null
    var loans: List<LoanEntity>
        get() = loansFlow.value
        set(value) {
            loansFlow.value = value
        }

    override fun getAllLoans(): Flow<List<LoanEntity>> =
        loansFlow.map { list ->
            allLoansError?.let { throw it }
            list
        }

    override fun getLoansWithPayments(): Flow<List<LoanWithPayments>> = flowOf(emptyList())

    override fun getLoanWithPaymentsById(id: Long): Flow<LoanWithPayments?> = flowOf(null)

    override fun getLoansByType(type: LoanType): Flow<List<LoanEntity>> = flowOf(emptyList())

    override fun getLoansByStatus(status: LoanStatus): Flow<List<LoanEntity>> = flowOf(emptyList())

    override fun getActiveLoans(): Flow<List<LoanEntity>> = flowOf(emptyList())

    override suspend fun getLoanById(id: Long): LoanEntity? = loans.find { it.id == id }

    override fun getLoanByIdFlow(id: Long): Flow<LoanEntity?> = flowOf(null)

    override suspend fun createLoan(loan: LoanEntity): Long = 1L

    override suspend fun updateLoan(loan: LoanEntity) = Unit

    override suspend fun updateLoanStatus(
        loanId: Long,
        status: LoanStatus,
    ) = Unit

    override suspend fun deleteLoan(loan: LoanEntity) = Unit

    override fun getPaymentsForLoan(loanId: Long): Flow<List<LoanPaymentEntity>> = flowOf(emptyList())

    override suspend fun recordLoanPayment(
        loanId: Long,
        paymentAmount: Double,
        paymentType: LoanPaymentType,
        paymentDate: Long,
        partPaymentAdjustment: com.mmr.core.model.enums.LoanPartPaymentAdjustment?,
        principalComponent: Double?,
        interestComponent: Double?,
        transactionId: Long?,
        notes: String?,
    ): Long = 0L

    override suspend fun changeInterestRate(
        loanId: Long,
        newAnnualRatePercent: Double,
        effectiveFromMillis: Long,
        note: String?,
    ) = Unit

    override suspend fun updateFutureEmi(
        loanId: Long,
        newEmiAmount: Double,
    ) = Unit

    override suspend fun refreshPlannedEmiSchedule(loanId: Long) = Unit

    override suspend fun recordLoanTopUp(
        loanId: Long,
        topUpAmount: Double,
        topUpDate: Long,
        topUpAdjustment: com.mmr.core.model.enums.LoanTopUpAdjustment?,
        transactionId: Long?,
        notes: String?,
        postLedgerTransfer: Boolean,
    ): Long = 0L

    override suspend fun changeRepaymentMode(
        loanId: Long,
        newMode: com.mmr.core.model.enums.LoanRepaymentMode,
        fixedEmiOverride: Double?,
        tenureMonths: Int?,
    ) = Unit

    override fun getInterestRevisions(
        loanId: Long,
    ): Flow<List<com.mmr.core.database.entity.LoanInterestRevisionEntity>> = flowOf(emptyList())

    override fun getScheduleEntries(
        loanId: Long,
        status: com.mmr.core.model.enums.LoanScheduleStatus?,
    ): Flow<List<com.mmr.core.database.entity.LoanScheduleEntryEntity>> = flowOf(emptyList())

    override fun getLoanSummary(): Flow<LoanSummaryAggregate> =
        flowOf(
            LoanSummaryAggregate(0.0, 0.0, 0.0, 0.0, 0),
        )
}

class FakeLanSyncRepository : LanSyncRepository {
    private val stateFlow = MutableStateFlow(LanSyncSessionState())

    override val sessionState: MutableStateFlow<LanSyncSessionState> = stateFlow

    override suspend fun refreshSessionState() = Unit

    override fun setLanSyncEnabled(enabled: Boolean) = Unit

    override fun setDisplayName(name: String) = Unit

    override fun pairingOffer(): LanSyncPairingOffer =
        LanSyncPairingOffer(deviceId = "test", displayName = "Test", inboundAuthToken = "token")

    override suspend fun resolvePairingInput(raw: String): LanSyncPairingOffer {
        val trimmed = raw.trim()
        if (trimmed.startsWith(com.mmr.core.lansync.LanSyncPairingCodec.LEGACY_PREFIX)) {
            return com.mmr.core.lansync.LanSyncPairingCodec
                .decode(trimmed)
        }
        return pairingOffer()
    }

    override suspend fun trustDeviceFromPairingOffer(offer: LanSyncPairingOffer) = Unit

    override suspend fun removeTrustedDevice(deviceId: String) = Unit

    override suspend fun markCurrentWifiTrusted() = Unit

    override suspend fun clearSyncLog() = Unit

    override fun setPeerMode(mode: LanSyncPeerMode) = Unit

    override fun setSelectedPeerDeviceId(deviceId: String?) = Unit

    override fun setIncludeAttachments(enabled: Boolean) = Unit

    override suspend fun syncNow(mergeOptions: LanSyncMergeOptions): LanSyncNowResult =
        LanSyncNowResult(summary = emptyMergeSummary(), peerFailures = emptyList())

    override suspend fun restoreFromPeer(
        remoteDeviceId: String,
        mergeOptions: LanSyncMergeOptions,
    ): LanSyncMergeSummary = emptyMergeSummary()

    private fun emptyMergeSummary(): LanSyncMergeSummary =
        LanSyncMergeSummary(
            categoriesInserted = 0,
            categoriesUpdated = 0,
            subcategoriesInserted = 0,
            subcategoriesUpdated = 0,
            accountsInserted = 0,
            accountsUpdated = 0,
            loansInserted = 0,
            loansUpdated = 0,
            transactionsInserted = 0,
            transactionsUpdated = 0,
        )
}
