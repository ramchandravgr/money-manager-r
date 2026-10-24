package com.mmr.core.data.repository

import androidx.room.withTransaction
import com.mmr.core.common.sync.MmrSyncId
import com.mmr.core.common.widget.HomeScreenWidgetRefreshTrigger
import com.mmr.core.database.MmrDatabase
import com.mmr.core.database.dao.AccountDao
import com.mmr.core.database.dao.CategoryDao
import com.mmr.core.database.dao.LoanDao
import com.mmr.core.database.dao.RecurringRuleDao
import com.mmr.core.database.dao.SmsMessageRecordDao
import com.mmr.core.database.dao.TransactionDao
import com.mmr.core.database.entity.AccountEntity
import com.mmr.core.database.entity.CategoryEntity
import com.mmr.core.database.entity.SubcategoryEntity
import com.mmr.core.database.entity.TransactionEntity
import com.mmr.core.database.mapper.CategoryPersistenceMapper
import com.mmr.core.database.relation.TransactionWithDetails
import com.mmr.core.domain.model.AccountDeletionPreview
import com.mmr.core.domain.model.CategoryDeleteTransactionMode
import com.mmr.core.domain.repository.AccountRepository
import com.mmr.core.domain.repository.CategoryRepository
import com.mmr.core.domain.repository.TransactionRepository
import com.mmr.core.loan.LoanLinkedTransactionDeleteMode
import com.mmr.core.loan.LoanTransactionCoordinator
import com.mmr.core.model.category.CategoryWithChildren
import com.mmr.core.model.enums.CategoryType
import com.mmr.core.model.enums.TransactionType
import com.mmr.core.transaction.TransactionCashFlow
import com.mmr.core.transaction.TransactionMergeCoordinator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

@Singleton
class TransactionRepositoryImpl
    @Inject
    constructor(
        private val transactionDao: TransactionDao,
        private val accountDao: AccountDao,
        private val loanTransactionCoordinator: LoanTransactionCoordinator,
        private val transactionMergeCoordinator: TransactionMergeCoordinator,
        private val smsMessageRecordDao: SmsMessageRecordDao,
        private val widgetRefreshTrigger: HomeScreenWidgetRefreshTrigger,
    ) : TransactionRepository {
        private fun refreshWidgets() {
            widgetRefreshTrigger.requestRefresh()
        }

        override fun getApprovedTransactionsDetailed(): Flow<List<TransactionWithDetails>> =
            transactionDao.getApprovedTransactionsDetailed()

        override fun getFilteredTransactions(
            startDate: Long,
            endDate: Long,
            accountId: Long?,
            categoryId: Long?,
            subcategoryId: Long?,
            type: TransactionType?,
        ): Flow<List<TransactionWithDetails>> {
            val (safeStart, safeEnd) = RepositoryGuards.normalizedQueryRange(startDate, endDate)
            return transactionDao.getFilteredTransactions(
                safeStart,
                safeEnd,
                accountId?.takeIf { it > 0L },
                categoryId?.takeIf { it > 0L },
                subcategoryId?.takeIf { it > 0L },
                type,
            )
        }

        override fun searchTransactions(query: String): Flow<List<TransactionWithDetails>> {
            val trimmed = query.trim()
            if (trimmed.isEmpty()) return flowOf(emptyList())
            return transactionDao.searchTransactions(trimmed)
        }

        override suspend fun getTransactionDetailedById(id: Long): TransactionWithDetails? =
            if (id <= 0L) null else transactionDao.getTransactionDetailedById(id)

        override fun getTransactionsForLoan(loanId: Long): Flow<List<TransactionEntity>> =
            if (loanId <= 0L) flowOf(emptyList()) else transactionDao.getTransactionsForLoan(loanId)

        override suspend fun insertTransaction(transaction: TransactionEntity): Long {
            val validated =
                transaction.copy(
                    syncId = MmrSyncId.normalize(transaction.syncId),
                    accountId = transaction.accountId,
                    dateTime = transaction.dateTime,
                )
            RepositoryGuards.requireValidTransactionEntity(validated)
            accountDao.getById(validated.accountId)
                ?: throw IllegalArgumentException("Account not found.")
            val id = transactionDao.insert(validated)
            refreshWidgets()
            return id
        }

        override suspend fun updateTransaction(transaction: TransactionEntity) {
            require(transaction.id > 0L) { "Transaction id is required." }
            RepositoryGuards.requireValidTransactionEntity(transaction)
            accountDao.getById(transaction.accountId)
                ?: throw IllegalArgumentException("Account not found.")
            transactionDao.update(transaction)
            loanTransactionCoordinator.syncPaymentFromTransaction(transaction)
            refreshWidgets()
        }

        override suspend fun hasLinkedLoanPayment(transactionId: Long): Boolean =
            loanTransactionCoordinator.findPaymentForTransaction(transactionId) != null

        override suspend fun deleteTransaction(transaction: TransactionEntity) {
            require(transaction.id > 0L) { "Transaction id is required." }
            loanTransactionCoordinator.deleteTransaction(
                transaction,
                LoanLinkedTransactionDeleteMode.UNLINK,
            )
            refreshWidgets()
        }

        override suspend fun deleteTransactionWithLoanPolicy(
            transaction: TransactionEntity,
            mode: LoanLinkedTransactionDeleteMode,
        ) {
            require(transaction.id > 0L) { "Transaction id is required." }
            loanTransactionCoordinator.deleteTransaction(transaction, mode)
            refreshWidgets()
        }

        override suspend fun suggestTransactionNotes(
            prefix: String,
            limit: Int,
        ): List<String> {
            val needle = prefix.trim().lowercase()
            if (needle.isEmpty()) return emptyList()
            val results = ArrayList<String>()
            val seen = HashSet<String>()
            for (raw in transactionDao.recentTransactionNotes(250)) {
                val note = raw.trim()
                if (note.isEmpty()) continue
                val key = note.lowercase()
                if (!key.startsWith(needle)) continue
                if (seen.add(key)) {
                    results.add(note)
                    if (results.size >= limit) break
                }
            }
            return results
        }

        override suspend fun mergeTransactions(
            transactionIds: List<Long>,
            mergedName: String,
            mergedDateTime: Long?,
        ): Long = transactionMergeCoordinator.mergeTransactions(transactionIds, mergedName, mergedDateTime)

        override fun getLinkedSmsForTransaction(transactionId: Long) =
            if (transactionId <= 0L) {
                flowOf(emptyList())
            } else {
                smsMessageRecordDao.getByMatchedTransactionId(transactionId)
            }

        override fun getTotalExpense(): Flow<Double?> = transactionDao.getTotalExpense()

        override fun getTotalIncome(): Flow<Double?> = transactionDao.getTotalIncome()

        override fun getTotalExpenseBetween(
            startDate: Long,
            endDate: Long,
        ): Flow<Double?> {
            val (safeStart, safeEnd) = RepositoryGuards.normalizedQueryRange(startDate, endDate)
            return transactionDao.getTotalExpenseBetween(safeStart, safeEnd)
        }

        override fun getTotalIncomeBetween(
            startDate: Long,
            endDate: Long,
        ): Flow<Double?> {
            val (safeStart, safeEnd) = RepositoryGuards.normalizedQueryRange(startDate, endDate)
            return transactionDao.getTotalIncomeBetween(safeStart, safeEnd)
        }

        override fun getCategorySpendingBetween(
            startDate: Long,
            endDate: Long,
        ): Flow<List<com.mmr.core.database.model.CategorySpending>> {
            val (safeStart, safeEnd) = RepositoryGuards.normalizedQueryRange(startDate, endDate)
            return transactionDao.getCategorySpendingBetween(safeStart, safeEnd)
        }

        override fun getCategoryIncomeBetween(
            startDate: Long,
            endDate: Long,
        ): Flow<List<com.mmr.core.database.model.CategorySpending>> {
            val (safeStart, safeEnd) = RepositoryGuards.normalizedQueryRange(startDate, endDate)
            return transactionDao.getCategoryIncomeBetween(safeStart, safeEnd)
        }

        override fun getSubcategorySpendingForCategory(
            categoryId: Long,
            type: TransactionType,
            startDate: Long,
            endDate: Long,
        ): Flow<List<com.mmr.core.database.model.SubcategorySpending>> {
            val (safeStart, safeEnd) = RepositoryGuards.normalizedQueryRange(startDate, endDate)
            return transactionDao.getSubcategorySpendingForCategory(categoryId, type, safeStart, safeEnd)
        }

        override fun getCategoryMonthlyTotals(
            categoryId: Long,
            type: TransactionType,
            startDate: Long,
            endDate: Long,
        ): Flow<List<com.mmr.core.database.model.MonthTotalRow>> {
            val (safeStart, safeEnd) = RepositoryGuards.normalizedQueryRange(startDate, endDate)
            return transactionDao.getCategoryMonthlyTotals(categoryId, type, safeStart, safeEnd)
        }
    }

@Singleton
class CategoryRepositoryImpl
    @Inject
    constructor(
        private val categoryDao: CategoryDao,
        private val transactionDao: TransactionDao,
        private val recurringRuleDao: RecurringRuleDao,
        private val tombstoneRecorder: com.mmr.core.lansync.LanSyncTombstoneRecorder,
    ) : CategoryRepository {
        override fun getActiveCategories(): Flow<List<CategoryEntity>> = categoryDao.getActiveCategories()

        override fun getSubcategoriesForCategory(categoryId: Long): Flow<List<SubcategoryEntity>> =
            if (categoryId <= 0L) flowOf(emptyList()) else categoryDao.getSubcategoriesForCategory(categoryId)

        override fun observeActiveCategoryTree(): Flow<List<CategoryWithChildren>> =
            categoryDao.getActiveCategoriesWithSubcategories().map { rows ->
                rows.map { with(CategoryPersistenceMapper) { it.toModel() } }
            }

        override fun observeActiveCategoryTreeByType(type: CategoryType): Flow<List<CategoryWithChildren>> =
            categoryDao.getActiveCategoriesWithSubcategoriesByType(type).map { rows ->
                rows.map { with(CategoryPersistenceMapper) { it.toModel() } }
            }

        override fun observeCategoryTree(categoryId: Long): Flow<CategoryWithChildren?> {
            if (categoryId <= 0L) return flowOf(null)
            return categoryDao.observeCategoryWithSubcategories(categoryId).map { row ->
                row?.let { with(CategoryPersistenceMapper) { it.toModel() } }
            }
        }

        override suspend fun resolveCategory(categoryId: Long): CategoryEntity? {
            if (categoryId <= 0L) return null
            return categoryDao.getCategoryById(categoryId)?.takeIf { it.isActive }
        }

        override suspend fun resolveSubcategory(subcategoryId: Long): SubcategoryEntity? {
            if (subcategoryId <= 0L) return null
            return categoryDao.getSubcategoryById(subcategoryId)?.takeIf { it.isActive }
        }

        override suspend fun resolveCategoryWithChildren(categoryId: Long): CategoryWithChildren? {
            if (categoryId <= 0L) return null
            val row = categoryDao.observeCategoryWithSubcategories(categoryId).first() ?: return null
            if (!row.category.isActive) return null
            return with(CategoryPersistenceMapper) { row.toModel() }
        }

        override suspend fun createCategory(category: CategoryEntity): Long {
            RepositoryGuards.requireValidCategoryEntity(category)
            return categoryDao.insert(
                category.copy(
                    id = 0L,
                    syncId = MmrSyncId.normalize(category.syncId),
                ),
            )
        }

        override suspend fun updateCategory(category: CategoryEntity) {
            require(category.id > 0L) { "Category id is required for update." }
            RepositoryGuards.requireValidCategoryEntity(category)
            categoryDao.update(category)
        }

        override suspend fun countTransactionsForCategory(categoryId: Long): Int {
            if (categoryId <= 0L) return 0
            return transactionDao.countByCategoryTree(categoryId)
        }

        override suspend fun deleteCategory(
            categoryId: Long,
            transactionMode: CategoryDeleteTransactionMode,
            migrateToCategoryId: Long?,
        ) {
            val existing =
                categoryDao.getCategoryById(categoryId)
                    ?: throw IllegalArgumentException("Category not found.")
            val taggedCount = transactionDao.countByCategoryTree(categoryId)
            val now = System.currentTimeMillis()
            when {
                taggedCount == 0 -> Unit
                transactionMode == CategoryDeleteTransactionMode.MIGRATE -> {
                    val targetId =
                        migrateToCategoryId
                            ?: throw IllegalArgumentException("Choose a category to move transactions to.")
                    require(targetId != categoryId) { "Choose a different category." }
                    val target =
                        categoryDao.getCategoryById(targetId)
                            ?: throw IllegalArgumentException("Target category not found.")
                    require(target.isActive) { "Target category is not active." }
                    require(target.type == existing.type) {
                        "Move transactions to another ${existing.type.name.lowercase()} category."
                    }
                    transactionDao.migrateTransactionsToCategory(categoryId, targetId, now)
                    recurringRuleDao.migrateCategory(categoryId, targetId, now)
                }
                transactionMode == CategoryDeleteTransactionMode.UNCATEGORIZE -> {
                    transactionDao.uncategorizeTransactionsForCategory(categoryId, now)
                    recurringRuleDao.clearCategory(categoryId, now)
                }
                else ->
                    throw IllegalArgumentException(
                        "This category is used on $taggedCount transaction(s). Choose how to handle them.",
                    )
            }
            val subs = categoryDao.getSubcategoriesForCategoryOnce(categoryId)
            subs.filter { it.isActive }.forEach { sub ->
                tombstoneRecorder.recordDeletion(sub.syncId, com.mmr.core.lansync.SyncEntityType.SUBCATEGORY)
            }
            categoryDao.deactivateSubcategoriesForCategory(categoryId)
            tombstoneRecorder.recordDeletion(existing.syncId, com.mmr.core.lansync.SyncEntityType.CATEGORY)
            categoryDao.update(existing.copy(isActive = false))
        }

        override suspend fun createSubcategory(subcategory: SubcategoryEntity): Long {
            RepositoryGuards.requireValidSubcategoryEntity(subcategory)
            val parent =
                categoryDao.getCategoryById(subcategory.categoryId)
                    ?: throw IllegalArgumentException("Parent category not found.")
            require(parent.isActive) { "Parent category is not active." }
            return categoryDao.insertSubcategory(subcategory.copy(id = 0L))
        }

        override suspend fun updateSubcategory(subcategory: SubcategoryEntity) {
            require(subcategory.id > 0L) { "Subcategory id is required for update." }
            RepositoryGuards.requireValidSubcategoryEntity(subcategory)
            val parent =
                categoryDao.getCategoryById(subcategory.categoryId)
                    ?: throw IllegalArgumentException("Parent category not found.")
            require(parent.isActive) { "Parent category is not active." }
            categoryDao.updateSubcategory(subcategory)
        }

        override suspend fun deleteSubcategory(subcategoryId: Long) {
            val existing =
                categoryDao.getSubcategoryById(subcategoryId)
                    ?: throw IllegalArgumentException("Subcategory not found.")
            tombstoneRecorder.recordDeletion(existing.syncId, com.mmr.core.lansync.SyncEntityType.SUBCATEGORY)
            categoryDao.updateSubcategory(existing.copy(isActive = false))
        }
    }

@Singleton
class AccountRepositoryImpl
    @Inject
    constructor(
        private val database: MmrDatabase,
        private val accountDao: AccountDao,
        private val transactionDao: TransactionDao,
        private val recurringRuleDao: RecurringRuleDao,
        private val loanDao: LoanDao,
        private val tombstoneRecorder: com.mmr.core.lansync.LanSyncTombstoneRecorder,
    ) : AccountRepository {
        override fun getAccounts(): Flow<List<AccountEntity>> = accountDao.getAccounts()

        override fun getActiveAccounts(): Flow<List<AccountEntity>> =
            accountDao.getAccounts().map { list -> list.filter { it.isActive } }

        override suspend fun getAccountById(id: Long): AccountEntity? = if (id <= 0L) null else accountDao.getById(id)

        override suspend fun createAccount(
            name: String,
            type: com.mmr.core.model.enums.AccountType,
            openingBalance: Double,
            cardLastFour: String?,
            isDefault: Boolean,
        ): Long {
            val trimmed = name.trim()
            require(trimmed.isNotEmpty()) { "Account name is required." }
            val last4 = cardLastFour?.trim()?.takeIf { it.length == 4 && it.all(Char::isDigit) }
            val now = System.currentTimeMillis()
            return database.withTransaction {
                if (isDefault) {
                    accountDao.clearDefaultFlags()
                }
                accountDao.insert(
                    AccountEntity(
                        name = trimmed,
                        type = type,
                        openingBalance = openingBalance,
                        currentBalance = openingBalance,
                        cardLastFour = last4,
                        isDefault = isDefault,
                        isActive = true,
                        createdAt = now,
                        updatedAt = now,
                    ),
                )
            }
        }

        override suspend fun getDefaultAccount(): AccountEntity? = accountDao.getDefaultAccount()

        override suspend fun setPrimaryAccount(accountId: Long) {
            require(accountId > 0L) { "Account id is required." }
            val account = getAccountById(accountId) ?: throw IllegalArgumentException("Account not found.")
            require(account.isActive) { "Inactive accounts cannot be primary." }
            database.withTransaction {
                accountDao.clearDefaultFlags()
                accountDao.update(
                    account.copy(isDefault = true, updatedAt = System.currentTimeMillis()),
                )
            }
        }

        override suspend fun updateAccount(
            id: Long,
            name: String,
            type: com.mmr.core.model.enums.AccountType,
            openingBalance: Double,
            cardLastFour: String?,
            isActive: Boolean,
        ) {
            val existing = getAccountById(id) ?: throw IllegalArgumentException("Account not found.")
            val trimmed = name.trim()
            require(trimmed.isNotEmpty()) { "Account name is required." }
            val last4 = cardLastFour?.trim()?.takeIf { it.length == 4 && it.all(Char::isDigit) }
            if (!isActive && existing.isActive) {
                tombstoneRecorder.recordDeletion(existing.syncId, com.mmr.core.lansync.SyncEntityType.ACCOUNT)
            }
            accountDao.update(
                existing.copy(
                    name = trimmed,
                    type = type,
                    openingBalance = openingBalance,
                    cardLastFour = last4,
                    isActive = isActive,
                    updatedAt = System.currentTimeMillis(),
                ),
            )
        }

        override suspend fun previewAccountDeletion(accountId: Long): AccountDeletionPreview {
            require(accountId > 0L) { "Account id is required." }
            getAccountById(accountId) ?: throw IllegalArgumentException("Account not found.")
            if (loanDao.countByLinkedLoanAccountId(accountId) > 0) {
                return AccountDeletionPreview(
                    linkedTransactionCount = transactionDao.countInvolvingAccount(accountId),
                    linkedRecurringRuleCount = recurringRuleDao.countInvolvingAccount(accountId),
                    isBlocked = true,
                    blockReason = "This account is the loan ledger for an active loan. Close or reconfigure the loan first.",
                )
            }
            return AccountDeletionPreview(
                linkedTransactionCount = transactionDao.countInvolvingAccount(accountId),
                linkedRecurringRuleCount = recurringRuleDao.countInvolvingAccount(accountId),
                isBlocked = false,
            )
        }

        override suspend fun deleteAccount(
            accountId: Long,
            reassignTransactionsToAccountId: Long?,
        ) {
            require(accountId > 0L) { "Account id is required." }
            val existing = getAccountById(accountId) ?: throw IllegalArgumentException("Account not found.")
            require(existing.isActive) { "Account is already removed." }
            if (loanDao.countByLinkedLoanAccountId(accountId) > 0) {
                throw IllegalStateException(
                    "This account is the loan ledger for a loan. Close or reconfigure the loan first.",
                )
            }
            val activeOthers = accountDao.getActiveAccountsOnce().count { it.id != accountId }
            require(activeOthers > 0) { "Add another active account before removing this one." }

            val txnCount = transactionDao.countInvolvingAccount(accountId)
            val recurringCount = recurringRuleDao.countInvolvingAccount(accountId)
            val needsReassign = txnCount > 0 || recurringCount > 0 || loanDao.countByLinkedDebitAccountId(accountId) > 0
            val targetId =
                if (needsReassign) {
                    val chosen =
                        reassignTransactionsToAccountId
                            ?: throw IllegalArgumentException("Choose an active account to move transactions to.")
                    require(chosen != accountId) { "Choose a different account." }
                    val target = getAccountById(chosen) ?: throw IllegalArgumentException("Target account not found.")
                    require(target.isActive) { "Target account must be active." }
                    chosen
                } else {
                    null
                }

            val now = System.currentTimeMillis()
            database.withTransaction {
                if (targetId != null) {
                    transactionDao.reassignTransactionAccount(accountId, targetId, now)
                    transactionDao.reassignTransactionRelatedAccount(accountId, targetId, now)
                    recurringRuleDao.reassignPrimaryAccount(accountId, targetId, now)
                    recurringRuleDao.reassignRelatedAccount(accountId, targetId, now)
                    loanDao.reassignLinkedDebitAccount(accountId, targetId, now)
                    refreshStoredBalances(setOf(accountId, targetId))
                }
                if (existing.isDefault) {
                    accountDao.clearDefaultFlags()
                }
                tombstoneRecorder.recordDeletion(existing.syncId, com.mmr.core.lansync.SyncEntityType.ACCOUNT)
                accountDao.update(
                    existing.copy(
                        isActive = false,
                        isDefault = false,
                        updatedAt = now,
                    ),
                )
            }
        }

        private suspend fun refreshStoredBalances(accountIds: Set<Long>) {
            if (accountIds.isEmpty()) return
            val transactions = transactionDao.getAllApprovedTransactions()
            val now = System.currentTimeMillis()
            for (id in accountIds) {
                val account = accountDao.getById(id) ?: continue
                val expected = recomputeBalance(account.openingBalance, id, transactions)
                if (abs(account.currentBalance - expected) > BALANCE_EPSILON) {
                    accountDao.update(account.copy(currentBalance = expected, updatedAt = now))
                }
            }
        }

        private fun recomputeBalance(
            openingBalance: Double,
            accountId: Long,
            transactions: List<TransactionEntity>,
        ): Double {
            var balance = openingBalance
            for (txn in transactions) {
                when (txn.type) {
                    TransactionType.INCOME ->
                        if (txn.accountId == accountId) balance += txn.amount
                    TransactionType.EXPENSE ->
                        if (txn.accountId == accountId) balance -= txn.amount
                    TransactionType.TRANSFER -> {
                        if (TransactionCashFlow.skipsAccountBalanceRecompute(txn)) continue
                        if (txn.accountId == accountId) balance -= txn.amount
                        if (txn.relatedAccountId == accountId) balance += txn.amount
                    }
                    else -> Unit
                }
            }
            return balance
        }

        private companion object {
            const val BALANCE_EPSILON = 0.01
        }
    }
