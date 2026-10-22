package com.mmr.core.domain.repository

import com.mmr.core.database.entity.AccountEntity
import com.mmr.core.database.entity.CategoryEntity
import com.mmr.core.database.entity.SubcategoryEntity
import com.mmr.core.database.entity.TransactionEntity
import com.mmr.core.database.relation.TransactionWithDetails
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
        type: TransactionType?
    ): Flow<List<TransactionWithDetails>>
    fun searchTransactions(query: String): Flow<List<TransactionWithDetails>>
    suspend fun getTransactionDetailedById(id: Long): TransactionWithDetails?
    fun getTransactionsForLoan(loanId: Long): Flow<List<TransactionEntity>>
    suspend fun insertTransaction(transaction: TransactionEntity): Long
    suspend fun updateTransaction(transaction: TransactionEntity)
    suspend fun deleteTransaction(transaction: TransactionEntity)
    fun getTotalExpense(): Flow<Double?>
    fun getTotalIncome(): Flow<Double?>
    fun getTotalExpenseBetween(startDate: Long, endDate: Long): Flow<Double?>
    fun getTotalIncomeBetween(startDate: Long, endDate: Long): Flow<Double?>
    fun getCategorySpendingBetween(startDate: Long, endDate: Long): Flow<List<com.mmr.core.database.model.CategorySpending>>
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

    suspend fun deleteCategory(categoryId: Long)

    suspend fun createSubcategory(subcategory: SubcategoryEntity): Long

    suspend fun updateSubcategory(subcategory: SubcategoryEntity)

    suspend fun deleteSubcategory(subcategoryId: Long)
}

interface AccountRepository {
    fun getAccounts(): Flow<List<AccountEntity>>
    suspend fun getAccountById(id: Long): AccountEntity?
}
