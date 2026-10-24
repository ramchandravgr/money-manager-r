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

    @Query("SELECT * FROM transactions WHERE syncId = :syncId LIMIT 1")
    suspend fun getBySyncId(syncId: String): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE status = 'APPROVED'")
    suspend fun getAllApprovedTransactions(): List<TransactionEntity>

    @Transaction
    @Query("SELECT * FROM transactions WHERE status = 'APPROVED' ORDER BY dateTime DESC")
    fun getApprovedTransactionsDetailed(): Flow<List<TransactionWithDetails>>

    @Query(
        """
        SELECT note FROM transactions
        WHERE status = 'APPROVED'
        AND note IS NOT NULL
        AND TRIM(note) != ''
        ORDER BY dateTime DESC
        LIMIT :limit
        """,
    )
    suspend fun recentTransactionNotes(limit: Int = 250): List<String>

    @Transaction
    @Query(
        """
        SELECT * FROM transactions 
        WHERE status = 'APPROVED' 
        AND (:accountId IS NULL OR accountId = :accountId) 
        AND (:categoryId IS NULL OR categoryId = :categoryId) 
        AND (:subcategoryId IS NULL OR subcategoryId = :subcategoryId) 
        AND (:type IS NULL OR type = :type) 
        AND dateTime BETWEEN :startDate AND :endDate 
        ORDER BY dateTime DESC
    """,
    )
    fun getFilteredTransactions(
        startDate: Long,
        endDate: Long,
        accountId: Long?,
        categoryId: Long?,
        subcategoryId: Long?,
        type: TransactionType?,
    ): Flow<List<TransactionWithDetails>>

    @Transaction
    @Query(
        """
        SELECT * FROM transactions 
        WHERE status = 'APPROVED' 
        AND (note LIKE '%' || :query || '%' OR sourceReferenceId LIKE '%' || :query || '%') 
        ORDER BY dateTime DESC
    """,
    )
    fun searchTransactions(query: String): Flow<List<TransactionWithDetails>>

    @Transaction
    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getTransactionDetailedById(id: Long): TransactionWithDetails?

    @Query("SELECT * FROM transactions WHERE loanId = :loanId ORDER BY dateTime DESC")
    fun getTransactionsForLoan(loanId: Long): Flow<List<TransactionEntity>>

    @Query(
        """
        SELECT COUNT(*) FROM transactions
        WHERE accountId = :accountId OR relatedAccountId = :accountId
        """,
    )
    suspend fun countInvolvingAccount(accountId: Long): Int

    @Query(
        """
        SELECT COUNT(*) FROM transactions
        WHERE categoryId = :categoryId
        OR subcategoryId IN (SELECT id FROM subcategories WHERE categoryId = :categoryId)
        """,
    )
    suspend fun countByCategoryTree(categoryId: Long): Int

    @Query(
        """
        UPDATE transactions
        SET accountId = :toAccountId, updatedAt = :now, isEdited = 1
        WHERE accountId = :fromAccountId
        """,
    )
    suspend fun reassignTransactionAccount(
        fromAccountId: Long,
        toAccountId: Long,
        now: Long = System.currentTimeMillis(),
    )

    @Query(
        """
        UPDATE transactions
        SET relatedAccountId = :toAccountId, updatedAt = :now, isEdited = 1
        WHERE relatedAccountId = :fromAccountId
        """,
    )
    suspend fun reassignTransactionRelatedAccount(
        fromAccountId: Long,
        toAccountId: Long,
        now: Long = System.currentTimeMillis(),
    )

    @Query(
        """
        UPDATE transactions
        SET categoryId = :toCategoryId, subcategoryId = NULL, updatedAt = :now, isEdited = 1
        WHERE categoryId = :fromCategoryId
        """,
    )
    suspend fun migrateTransactionsToCategory(
        fromCategoryId: Long,
        toCategoryId: Long,
        now: Long = System.currentTimeMillis(),
    )

    @Query(
        """
        UPDATE transactions
        SET categoryId = NULL, subcategoryId = NULL, updatedAt = :now, isEdited = 1
        WHERE categoryId = :categoryId
        OR subcategoryId IN (SELECT id FROM subcategories WHERE categoryId = :categoryId)
        """,
    )
    suspend fun uncategorizeTransactionsForCategory(
        categoryId: Long,
        now: Long = System.currentTimeMillis(),
    )

    @Query("SELECT id FROM transactions WHERE sourceReferenceId = :ref LIMIT 1")
    suspend fun findIdBySourceReference(ref: String): Long?

    @Query(
        """
        SELECT * FROM transactions
        WHERE status = 'APPROVED'
        AND type = :type
        AND accountId = :accountId
        AND dateTime BETWEEN :startMillis AND :endMillis
        AND (loanId IS NULL OR loanId = 0)
        ORDER BY ABS(amount - :amount) ASC, dateTime DESC
        LIMIT 5
        """,
    )
    suspend fun findSmsMergeCandidates(
        type: TransactionType,
        accountId: Long,
        amount: Double,
        startMillis: Long,
        endMillis: Long,
    ): List<TransactionEntity>

    @Query(
        """
        SELECT * FROM transactions
        WHERE status = 'APPROVED'
        AND type = 'EXPENSE'
        AND dateTime BETWEEN :startMillis AND :endMillis
        AND amount >= :refundAmount
        AND (loanId IS NULL OR loanId = 0)
        AND (
            (:cardLastFour IS NOT NULL AND cardLastFour = :cardLastFour)
            OR (:cardLastFour IS NULL AND accountId = :accountId)
        )
        ORDER BY ABS(amount - :refundAmount) ASC, dateTime DESC
        LIMIT 30
        """,
    )
    suspend fun findCardRefundPurchaseCandidates(
        accountId: Long,
        cardLastFour: String?,
        refundAmount: Double,
        startMillis: Long,
        endMillis: Long,
    ): List<TransactionEntity>

    @Query(
        """
        SELECT SUM(t.amount) FROM transactions t
        LEFT JOIN loans l ON l.id = t.loanId
        WHERE t.status = 'APPROVED'
        AND (
            t.type = 'EXPENSE'
            OR (
                t.type = 'TRANSFER'
                AND t.loanId IS NOT NULL AND t.loanId > 0
                AND l.loanType = 'BORROWED'
                AND (t.sourceReferenceId IS NULL OR (
                    t.sourceReferenceId NOT LIKE 'loan_topup:%'
                    AND t.sourceReferenceId NOT LIKE 'loan_disbursal:%'
                ))
            )
        )
        """,
    )
    fun getTotalExpense(): Flow<Double?>

    @Query(
        """
        SELECT SUM(t.amount) FROM transactions t
        LEFT JOIN loans l ON l.id = t.loanId
        WHERE t.status = 'APPROVED'
        AND (
            t.type = 'INCOME'
            OR (
                t.type = 'TRANSFER'
                AND t.loanId IS NOT NULL AND t.loanId > 0
                AND l.loanType = 'LENT'
                AND (t.sourceReferenceId IS NULL OR (
                    t.sourceReferenceId NOT LIKE 'loan_topup:%'
                    AND t.sourceReferenceId NOT LIKE 'loan_disbursal:%'
                ))
            )
        )
        """,
    )
    fun getTotalIncome(): Flow<Double?>

    @Query(
        """
        SELECT SUM(t.amount) FROM transactions t
        LEFT JOIN loans l ON l.id = t.loanId
        WHERE t.status = 'APPROVED'
        AND t.dateTime BETWEEN :startDate AND :endDate
        AND (
            t.type = 'EXPENSE'
            OR (
                t.type = 'TRANSFER'
                AND t.loanId IS NOT NULL AND t.loanId > 0
                AND l.loanType = 'BORROWED'
                AND (t.sourceReferenceId IS NULL OR (
                    t.sourceReferenceId NOT LIKE 'loan_topup:%'
                    AND t.sourceReferenceId NOT LIKE 'loan_disbursal:%'
                ))
            )
        )
        """,
    )
    fun getTotalExpenseBetween(
        startDate: Long,
        endDate: Long,
    ): Flow<Double?>

    @Query(
        """
        SELECT SUM(t.amount) FROM transactions t
        LEFT JOIN loans l ON l.id = t.loanId
        WHERE t.status = 'APPROVED'
        AND t.dateTime BETWEEN :startDate AND :endDate
        AND (
            t.type = 'INCOME'
            OR (
                t.type = 'TRANSFER'
                AND t.loanId IS NOT NULL AND t.loanId > 0
                AND l.loanType = 'LENT'
                AND (t.sourceReferenceId IS NULL OR (
                    t.sourceReferenceId NOT LIKE 'loan_topup:%'
                    AND t.sourceReferenceId NOT LIKE 'loan_disbursal:%'
                ))
            )
        )
        """,
    )
    fun getTotalIncomeBetween(
        startDate: Long,
        endDate: Long,
    ): Flow<Double?>

    @Query(
        """
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
    """,
    )
    fun getCategorySpendingBetween(
        startDate: Long,
        endDate: Long,
    ): Flow<List<com.mmr.core.database.model.CategorySpending>>

    @Query(
        """
        SELECT 
            t.categoryId AS categoryId,
            c.name AS categoryName,
            c.colorHex AS colorHex,
            SUM(t.amount) AS totalAmount,
            COUNT(t.id) AS transactionCount
        FROM transactions t
        LEFT JOIN categories c ON t.categoryId = c.id
        WHERE t.type = 'INCOME' 
          AND t.status = 'APPROVED' 
          AND t.dateTime BETWEEN :startDate AND :endDate
        GROUP BY t.categoryId
        ORDER BY totalAmount DESC
    """,
    )
    fun getCategoryIncomeBetween(
        startDate: Long,
        endDate: Long,
    ): Flow<List<com.mmr.core.database.model.CategorySpending>>

    @Query(
        """
        SELECT 
            t.subcategoryId AS subcategoryId,
            s.name AS subcategoryName,
            SUM(t.amount) AS totalAmount,
            COUNT(t.id) AS transactionCount
        FROM transactions t
        LEFT JOIN subcategories s ON t.subcategoryId = s.id
        WHERE t.categoryId = :categoryId
          AND t.type = :type
          AND t.status = 'APPROVED'
          AND t.dateTime BETWEEN :startDate AND :endDate
        GROUP BY t.subcategoryId
        ORDER BY totalAmount DESC
    """,
    )
    fun getSubcategorySpendingForCategory(
        categoryId: Long,
        type: TransactionType,
        startDate: Long,
        endDate: Long,
    ): Flow<List<com.mmr.core.database.model.SubcategorySpending>>

    @Query(
        """
        SELECT 
            strftime('%Y-%m', datetime(dateTime / 1000, 'unixepoch', 'localtime')) AS yearMonth,
            SUM(amount) AS totalAmount
        FROM transactions
        WHERE categoryId = :categoryId
          AND type = :type
          AND status = 'APPROVED'
          AND dateTime BETWEEN :startDate AND :endDate
        GROUP BY yearMonth
        ORDER BY yearMonth ASC
    """,
    )
    fun getCategoryMonthlyTotals(
        categoryId: Long,
        type: TransactionType,
        startDate: Long,
        endDate: Long,
    ): Flow<List<com.mmr.core.database.model.MonthTotalRow>>
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

    @Query("SELECT * FROM categories WHERE syncId = :syncId LIMIT 1")
    suspend fun getCategoryBySyncId(syncId: String): CategoryEntity?

    @Query(
        """
        SELECT * FROM categories
        WHERE isActive = 1 AND lower(name) = lower(:name)
        LIMIT 1
        """,
    )
    suspend fun findActiveByName(name: String): CategoryEntity?

    @Query("SELECT * FROM subcategories WHERE id = :id LIMIT 1")
    suspend fun getSubcategoryById(id: Long): SubcategoryEntity?

    @Query("SELECT * FROM subcategories WHERE syncId = :syncId LIMIT 1")
    suspend fun getSubcategoryBySyncId(syncId: String): SubcategoryEntity?

    @Query("SELECT * FROM categories WHERE isActive = 1 ORDER BY orderIndex, name")
    fun getActiveCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE isActive = 1 ORDER BY orderIndex, name")
    suspend fun getActiveCategoriesOnce(): List<CategoryEntity>

    @Query(
        """
        SELECT * FROM categories
        WHERE isActive = 1 AND type = :type
        ORDER BY orderIndex, name
        """,
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
        """,
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
        """,
    )
    fun getSubcategoriesForCategory(categoryId: Long): Flow<List<SubcategoryEntity>>

    @Query(
        """
        SELECT * FROM subcategories
        WHERE categoryId = :categoryId AND isActive = 1
        ORDER BY orderIndex, name
        """,
    )
    suspend fun getSubcategoriesForCategoryOnce(categoryId: Long): List<SubcategoryEntity>

    @Query(
        """
        SELECT * FROM subcategories
        WHERE categoryId = :categoryId
        ORDER BY orderIndex, name
        """,
    )
    fun getAllSubcategoriesForCategory(categoryId: Long): Flow<List<SubcategoryEntity>>

    @Query("UPDATE subcategories SET isActive = 0 WHERE categoryId = :categoryId AND isActive = 1")
    suspend fun deactivateSubcategoriesForCategory(categoryId: Long)
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

    @Query("SELECT * FROM accounts WHERE syncId = :syncId LIMIT 1")
    suspend fun getBySyncId(syncId: String): AccountEntity?

    @Query(
        """
        SELECT * FROM accounts
        WHERE isActive = 1 AND lower(name) = lower(:name)
        LIMIT 1
        """,
    )
    suspend fun findActiveByName(name: String): AccountEntity?

    @Query("SELECT * FROM accounts WHERE isActive = 1 ORDER BY name")
    suspend fun getActiveAccountsOnce(): List<AccountEntity>

    @Query("SELECT * FROM accounts WHERE isDefault = 1 AND isActive = 1 LIMIT 1")
    suspend fun getDefaultAccount(): AccountEntity?

    @Query("UPDATE accounts SET isDefault = 0")
    suspend fun clearDefaultFlags()

    @Query("SELECT * FROM accounts WHERE isActive = 1 ORDER BY name")
    suspend fun getAccountsOnce(): List<AccountEntity>
}
