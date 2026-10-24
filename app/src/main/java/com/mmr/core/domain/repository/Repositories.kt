package com.mmr.core.domain.repository

import com.mmr.core.database.entity.AccountEntity
import com.mmr.core.database.entity.CategoryEntity
import com.mmr.core.database.entity.SmsMessageRecordEntity
import com.mmr.core.database.entity.SubcategoryEntity
import com.mmr.core.database.entity.TransactionEntity
import com.mmr.core.database.relation.TransactionWithDetails
import com.mmr.core.loan.LoanLinkedTransactionDeleteMode
import com.mmr.core.model.category.CategoryWithChildren
import com.mmr.core.model.enums.CategoryType
import com.mmr.core.model.enums.TransactionType
import kotlinx.coroutines.flow.Flow

interface TransactionRepository {
    fun getApprovedTransactionsDetailed(): Flow<List<TransactionWithDetails>>

    fun getFilteredTransactions(
        startDate: Long,
        endDate: Long,
        accountId: Long?,
        categoryId: Long?,
        subcategoryId: Long?,
        type: TransactionType?,
    ): Flow<List<TransactionWithDetails>>

    fun searchTransactions(query: String): Flow<List<TransactionWithDetails>>

    suspend fun getTransactionDetailedById(id: Long): TransactionWithDetails?

    fun getTransactionsForLoan(loanId: Long): Flow<List<TransactionEntity>>

    suspend fun insertTransaction(transaction: TransactionEntity): Long

    suspend fun updateTransaction(transaction: TransactionEntity)

    suspend fun deleteTransaction(transaction: TransactionEntity)

    suspend fun hasLinkedLoanPayment(transactionId: Long): Boolean

    suspend fun deleteTransactionWithLoanPolicy(
        transaction: TransactionEntity,
        mode: LoanLinkedTransactionDeleteMode,
    )

    suspend fun suggestTransactionNotes(
        prefix: String,
        limit: Int = 8,
    ): List<String>

    suspend fun mergeTransactions(
        transactionIds: List<Long>,
        mergedName: String,
        mergedDateTime: Long? = null,
    ): Long

    fun getLinkedSmsForTransaction(transactionId: Long): Flow<List<SmsMessageRecordEntity>>

    fun getTotalExpense(): Flow<Double?>

    fun getTotalIncome(): Flow<Double?>

    fun getTotalExpenseBetween(
        startDate: Long,
        endDate: Long,
    ): Flow<Double?>

    fun getTotalIncomeBetween(
        startDate: Long,
        endDate: Long,
    ): Flow<Double?>

    fun getCategorySpendingBetween(
        startDate: Long,
        endDate: Long,
    ): Flow<List<com.mmr.core.database.model.CategorySpending>>

    fun getCategoryIncomeBetween(
        startDate: Long,
        endDate: Long,
    ): Flow<List<com.mmr.core.database.model.CategorySpending>>

    fun getSubcategorySpendingForCategory(
        categoryId: Long,
        type: TransactionType,
        startDate: Long,
        endDate: Long,
    ): Flow<List<com.mmr.core.database.model.SubcategorySpending>>

    fun getCategoryMonthlyTotals(
        categoryId: Long,
        type: TransactionType,
        startDate: Long,
        endDate: Long,
    ): Flow<List<com.mmr.core.database.model.MonthTotalRow>>
}

interface CategoryRepository {
    fun getActiveCategories(): Flow<List<CategoryEntity>>

    fun getSubcategoriesForCategory(categoryId: Long): Flow<List<SubcategoryEntity>>

    fun observeActiveCategoryTree(): Flow<List<CategoryWithChildren>>

    fun observeActiveCategoryTreeByType(type: CategoryType): Flow<List<CategoryWithChildren>>

    fun observeCategoryTree(categoryId: Long): Flow<CategoryWithChildren?>

    suspend fun resolveCategory(categoryId: Long): CategoryEntity?

    suspend fun resolveSubcategory(subcategoryId: Long): SubcategoryEntity?

    suspend fun resolveCategoryWithChildren(categoryId: Long): CategoryWithChildren?

    suspend fun createCategory(category: CategoryEntity): Long

    suspend fun updateCategory(category: CategoryEntity)

    suspend fun countTransactionsForCategory(categoryId: Long): Int

    suspend fun deleteCategory(
        categoryId: Long,
        transactionMode: com.mmr.core.domain.model.CategoryDeleteTransactionMode,
        migrateToCategoryId: Long? = null,
    )

    suspend fun createSubcategory(subcategory: SubcategoryEntity): Long

    suspend fun updateSubcategory(subcategory: SubcategoryEntity)

    suspend fun deleteSubcategory(subcategoryId: Long)
}

interface AccountRepository {
    fun getAccounts(): Flow<List<AccountEntity>>

    fun getActiveAccounts(): Flow<List<AccountEntity>>

    suspend fun getAccountById(id: Long): AccountEntity?

    suspend fun createAccount(
        name: String,
        type: com.mmr.core.model.enums.AccountType,
        openingBalance: Double = 0.0,
        cardLastFour: String? = null,
        isDefault: Boolean = false,
    ): Long

    suspend fun updateAccount(
        id: Long,
        name: String,
        type: com.mmr.core.model.enums.AccountType,
        openingBalance: Double,
        cardLastFour: String?,
        isActive: Boolean,
    )

    suspend fun getDefaultAccount(): AccountEntity?

    suspend fun setPrimaryAccount(accountId: Long)

    suspend fun previewAccountDeletion(accountId: Long): com.mmr.core.domain.model.AccountDeletionPreview

    suspend fun deleteAccount(
        accountId: Long,
        reassignTransactionsToAccountId: Long? = null,
    )
}
