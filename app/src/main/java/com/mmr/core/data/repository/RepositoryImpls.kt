package com.mmr.core.data.repository

import com.mmr.core.database.dao.AccountDao
import com.mmr.core.database.dao.CategoryDao
import com.mmr.core.database.dao.TransactionDao
import com.mmr.core.database.entity.AccountEntity
import com.mmr.core.database.entity.CategoryEntity
import com.mmr.core.database.entity.SubcategoryEntity
import com.mmr.core.database.entity.TransactionEntity
import com.mmr.core.database.mapper.CategoryPersistenceMapper
import com.mmr.core.database.relation.TransactionWithDetails
import com.mmr.core.domain.repository.AccountRepository
import com.mmr.core.domain.repository.CategoryRepository
import com.mmr.core.domain.repository.TransactionRepository
import com.mmr.core.model.category.CategoryWithChildren
import com.mmr.core.model.enums.CategoryType
import com.mmr.core.model.enums.TransactionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionRepositoryImpl @Inject constructor(
    private val transactionDao: TransactionDao,
    private val accountDao: AccountDao
) : TransactionRepository {

    override fun getApprovedTransactionsDetailed(): Flow<List<TransactionWithDetails>> =
        transactionDao.getApprovedTransactionsDetailed()

    override fun getFilteredTransactions(
        startDate: Long,
        endDate: Long,
        accountId: Long?,
        categoryId: Long?,
        subcategoryId: Long?,
        type: TransactionType?
    ): Flow<List<TransactionWithDetails>> {
        val (safeStart, safeEnd) = RepositoryGuards.normalizedQueryRange(startDate, endDate)
        return transactionDao.getFilteredTransactions(
            safeStart,
            safeEnd,
            accountId?.takeIf { it > 0L },
            categoryId?.takeIf { it > 0L },
            subcategoryId?.takeIf { it > 0L },
            type
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
        val validated = transaction.copy(
            accountId = transaction.accountId,
            dateTime = transaction.dateTime
        )
        RepositoryGuards.requireValidTransactionEntity(validated)
        accountDao.getById(validated.accountId)
            ?: throw IllegalArgumentException("Account not found.")
        return transactionDao.insert(validated)
    }

    override suspend fun updateTransaction(transaction: TransactionEntity) {
        require(transaction.id > 0L) { "Transaction id is required." }
        RepositoryGuards.requireValidTransactionEntity(transaction)
        accountDao.getById(transaction.accountId)
            ?: throw IllegalArgumentException("Account not found.")
        transactionDao.update(transaction)
    }

    override suspend fun deleteTransaction(transaction: TransactionEntity) {
        require(transaction.id > 0L) { "Transaction id is required." }
        transactionDao.delete(transaction)
    }

    override fun getTotalExpense(): Flow<Double?> =
        transactionDao.getTotalExpense()

    override fun getTotalIncome(): Flow<Double?> =
        transactionDao.getTotalIncome()

    override fun getTotalExpenseBetween(startDate: Long, endDate: Long): Flow<Double?> {
        val (safeStart, safeEnd) = RepositoryGuards.normalizedQueryRange(startDate, endDate)
        return transactionDao.getTotalExpenseBetween(safeStart, safeEnd)
    }

    override fun getTotalIncomeBetween(startDate: Long, endDate: Long): Flow<Double?> {
        val (safeStart, safeEnd) = RepositoryGuards.normalizedQueryRange(startDate, endDate)
        return transactionDao.getTotalIncomeBetween(safeStart, safeEnd)
    }

    override fun getCategorySpendingBetween(
        startDate: Long,
        endDate: Long
    ): Flow<List<com.mmr.core.database.model.CategorySpending>> {
        val (safeStart, safeEnd) = RepositoryGuards.normalizedQueryRange(startDate, endDate)
        return transactionDao.getCategorySpendingBetween(safeStart, safeEnd)
    }
}

@Singleton
class CategoryRepositoryImpl @Inject constructor(
    private val categoryDao: CategoryDao
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
        return categoryDao.insert(category.copy(id = 0L))
    }

    override suspend fun updateCategory(category: CategoryEntity) {
        require(category.id > 0L) { "Category id is required for update." }
        RepositoryGuards.requireValidCategoryEntity(category)
        categoryDao.update(category)
    }

    override suspend fun deleteCategory(categoryId: Long) {
        val existing = categoryDao.getCategoryById(categoryId)
            ?: throw IllegalArgumentException("Category not found.")
        require(!existing.isDefault) { "Default categories cannot be deleted." }
        categoryDao.update(existing.copy(isActive = false))
    }

    override suspend fun createSubcategory(subcategory: SubcategoryEntity): Long {
        RepositoryGuards.requireValidSubcategoryEntity(subcategory)
        val parent = categoryDao.getCategoryById(subcategory.categoryId)
            ?: throw IllegalArgumentException("Parent category not found.")
        require(parent.isActive) { "Parent category is not active." }
        return categoryDao.insertSubcategory(subcategory.copy(id = 0L))
    }

    override suspend fun updateSubcategory(subcategory: SubcategoryEntity) {
        require(subcategory.id > 0L) { "Subcategory id is required for update." }
        RepositoryGuards.requireValidSubcategoryEntity(subcategory)
        val parent = categoryDao.getCategoryById(subcategory.categoryId)
            ?: throw IllegalArgumentException("Parent category not found.")
        require(parent.isActive) { "Parent category is not active." }
        categoryDao.updateSubcategory(subcategory)
    }

    override suspend fun deleteSubcategory(subcategoryId: Long) {
        val existing = categoryDao.getSubcategoryById(subcategoryId)
            ?: throw IllegalArgumentException("Subcategory not found.")
        categoryDao.updateSubcategory(existing.copy(isActive = false))
    }
}

@Singleton
class AccountRepositoryImpl @Inject constructor(
    private val accountDao: AccountDao
) : AccountRepository {
    override fun getAccounts(): Flow<List<AccountEntity>> = accountDao.getAccounts()

    override suspend fun getAccountById(id: Long): AccountEntity? =
        if (id <= 0L) null else accountDao.getById(id)
}
