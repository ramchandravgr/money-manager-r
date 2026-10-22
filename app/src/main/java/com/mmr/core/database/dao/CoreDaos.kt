package com.mmr.core.database.dao

import androidx.room.*
import com.mmr.core.database.entity.AccountEntity
import com.mmr.core.database.entity.CategoryEntity
import com.mmr.core.database.entity.SubcategoryEntity
import com.mmr.core.database.entity.TransactionEntity
import com.mmr.core.database.relation.CategoryWithSubcategories
import com.mmr.core.database.relation.TransactionWithDetails
import com.mmr.core.model.enums.CategoryType
import com.mmr.core.model.enums.TransactionType
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(transaction: TransactionEntity): Long

    @Update
    suspend fun update(transaction: TransactionEntity)

    @Delete
    suspend fun delete(transaction: TransactionEntity)

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): TransactionEntity?

    @Transaction
    @Query("SELECT * FROM transactions WHERE status = 'APPROVED' ORDER BY dateTime DESC")
    fun getApprovedTransactionsDetailed(): Flow<List<TransactionWithDetails>>

    @Transaction
    @Query("""
        SELECT * FROM transactions 
        WHERE status = 'APPROVED' 
        AND (:accountId IS NULL OR accountId = :accountId) 
        AND (:categoryId IS NULL OR categoryId = :categoryId) 
        AND (:subcategoryId IS NULL OR subcategoryId = :subcategoryId) 
        AND (:type IS NULL OR type = :type) 
        AND dateTime BETWEEN :startDate AND :endDate 
        ORDER BY dateTime DESC
    """)
    fun getFilteredTransactions(
        startDate: Long,
        endDate: Long,
        accountId: Long?,
        categoryId: Long?,
        subcategoryId: Long?,
        type: TransactionType?
    ): Flow<List<TransactionWithDetails>>

    @Transaction
    @Query("""
        SELECT * FROM transactions 
        WHERE status = 'APPROVED' 
        AND (note LIKE '%' || :query || '%' OR sourceReferenceId LIKE '%' || :query || '%') 
        ORDER BY dateTime DESC
    """)
    fun searchTransactions(query: String): Flow<List<TransactionWithDetails>>

    @Transaction
    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getTransactionDetailedById(id: Long): TransactionWithDetails?

    @Query("SELECT * FROM transactions WHERE loanId = :loanId ORDER BY dateTime DESC")
    fun getTransactionsForLoan(loanId: Long): Flow<List<TransactionEntity>>

    @Query("SELECT SUM(amount) FROM transactions WHERE type = 'EXPENSE' AND status = 'APPROVED'")
    fun getTotalExpense(): Flow<Double?>

    @Query("SELECT SUM(amount) FROM transactions WHERE type = 'INCOME' AND status = 'APPROVED'")
    fun getTotalIncome(): Flow<Double?>

    @Query("SELECT SUM(amount) FROM transactions WHERE type = 'EXPENSE' AND status = 'APPROVED' AND dateTime BETWEEN :startDate AND :endDate")
    fun getTotalExpenseBetween(startDate: Long, endDate: Long): Flow<Double?>

    @Query("SELECT SUM(amount) FROM transactions WHERE type = 'INCOME' AND status = 'APPROVED' AND dateTime BETWEEN :startDate AND :endDate")
    fun getTotalIncomeBetween(startDate: Long, endDate: Long): Flow<Double?>

    @Query("""
        SELECT 
            t.categoryId AS categoryId,
            c.name AS categoryName,
            c.colorHex AS colorHex,
            SUM(t.amount) AS totalAmount,
            COUNT(t.id) AS transactionCount
        FROM transactions t
        LEFT JOIN categories c ON t.categoryId = c.id
        WHERE t.type = 'EXPENSE' 
          AND t.status = 'APPROVED' 
          AND t.dateTime BETWEEN :startDate AND :endDate
        GROUP BY t.categoryId
        ORDER BY totalAmount DESC
    """)
    fun getCategorySpendingBetween(startDate: Long, endDate: Long): Flow<List<com.mmr.core.database.model.CategorySpending>>
}

@Dao
interface CategoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(category: CategoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<CategoryEntity>)

    @Update
    suspend fun update(category: CategoryEntity)

    @Delete
    suspend fun delete(category: CategoryEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSubcategory(subcategory: SubcategoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubcategories(subcategories: List<SubcategoryEntity>)

    @Update
    suspend fun updateSubcategory(subcategory: SubcategoryEntity)

    @Delete
    suspend fun deleteSubcategory(subcategory: SubcategoryEntity)

    @Query("SELECT * FROM categories WHERE id = :id LIMIT 1")
    suspend fun getCategoryById(id: Long): CategoryEntity?

    @Query("SELECT * FROM subcategories WHERE id = :id LIMIT 1")
    suspend fun getSubcategoryById(id: Long): SubcategoryEntity?

    @Query("SELECT * FROM categories WHERE isActive = 1 ORDER BY orderIndex, name")
    fun getActiveCategories(): Flow<List<CategoryEntity>>

    @Query(
        """
        SELECT * FROM categories
        WHERE isActive = 1 AND type = :type
        ORDER BY orderIndex, name
        """
    )
    fun getActiveCategoriesByType(type: CategoryType): Flow<List<CategoryEntity>>

    @Transaction
    @Query("SELECT * FROM categories WHERE isActive = 1 ORDER BY orderIndex, name")
    fun getActiveCategoriesWithSubcategories(): Flow<List<CategoryWithSubcategories>>

    @Transaction
    @Query(
        """
        SELECT * FROM categories
        WHERE isActive = 1 AND type = :type
        ORDER BY orderIndex, name
        """
    )
    fun getActiveCategoriesWithSubcategoriesByType(type: CategoryType): Flow<List<CategoryWithSubcategories>>

    @Transaction
    @Query("SELECT * FROM categories WHERE id = :categoryId LIMIT 1")
    fun observeCategoryWithSubcategories(categoryId: Long): Flow<CategoryWithSubcategories?>

    @Query(
        """
        SELECT * FROM subcategories
        WHERE categoryId = :categoryId AND isActive = 1
        ORDER BY orderIndex, name
        """
    )
    fun getSubcategoriesForCategory(categoryId: Long): Flow<List<SubcategoryEntity>>

    @Query(
        """
        SELECT * FROM subcategories
        WHERE categoryId = :categoryId
        ORDER BY orderIndex, name
        """
    )
    fun getAllSubcategoriesForCategory(categoryId: Long): Flow<List<SubcategoryEntity>>
}

@Dao
interface AccountDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(account: AccountEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccounts(accounts: List<AccountEntity>)

    @Update
    suspend fun update(account: AccountEntity)

    @Query("SELECT * FROM accounts WHERE isActive = 1 ORDER BY name")
    fun getAccounts(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): AccountEntity?

    @Query("SELECT * FROM accounts WHERE isActive = 1 ORDER BY name")
    suspend fun getActiveAccountsOnce(): List<AccountEntity>
}
