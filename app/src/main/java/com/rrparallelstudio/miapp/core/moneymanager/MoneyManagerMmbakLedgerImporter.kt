package com.rrparallelstudio.miapp.core.moneymanager

import com.rrparallelstudio.miapp.core.database.dao.TransactionDao
import com.rrparallelstudio.miapp.core.database.entity.CategoryEntity
import com.rrparallelstudio.miapp.core.database.entity.SubcategoryEntity
import com.rrparallelstudio.miapp.core.database.entity.TransactionEntity
import com.rrparallelstudio.miapp.core.domain.repository.AccountRepository
import com.rrparallelstudio.miapp.core.domain.repository.CategoryRepository
import com.rrparallelstudio.miapp.core.domain.repository.TransactionRepository
import com.rrparallelstudio.miapp.core.model.enums.AccountType
import com.rrparallelstudio.miapp.core.model.enums.CategoryType
import com.rrparallelstudio.miapp.core.model.enums.RecordStatus
import com.rrparallelstudio.miapp.core.model.enums.TransactionSourceType
import com.rrparallelstudio.miapp.core.model.enums.TransactionType
import kotlinx.coroutines.flow.first
import java.io.ByteArrayInputStream
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Merges income, expense, and transfer rows from a Realbyte Money Manager `.mmbak` into the local ledger.
 */
@Singleton
class MoneyManagerMmbakLedgerImporter
    @Inject
    constructor(
        private val transactionDao: TransactionDao,
        private val transactionRepository: TransactionRepository,
        private val accountRepository: AccountRepository,
        private val categoryRepository: CategoryRepository,
    ) {
        data class Result(
            val added: Int,
            val skippedExisting: Int,
            val skippedUnsupported: Int,
            val rowsInBackup: Int = 0,
        )

        suspend fun importFromBytes(
            data: ByteArray,
            cacheDir: File,
            sinceTimestampMillis: Long = 0L,
        ): Result =
            try {
                importFromBytesUnchecked(data, cacheDir, sinceTimestampMillis)
            } catch (e: Exception) {
                throw IllegalArgumentException(
                    e.message?.takeIf { it.isNotBlank() }
                        ?: "Could not read Money Manager backup (file may be corrupt or unsupported).",
                    e,
                )
            }

        private suspend fun importFromBytesUnchecked(
            data: ByteArray,
            cacheDir: File,
            sinceTimestampMillis: Long,
        ): Result {
            val opened =
                ByteArrayInputStream(data).use { input ->
                    MoneyManagerMmbakDatabaseOpener.openFromStream(input, cacheDir)
                } ?: error("Could not read Money Manager backup (not a supported .mmbak file).")

            return try {
                val db = opened.database
                val assetNames = MoneyManagerMmbakDatabaseOpener.readAssetNames(db)
                val categories = MoneyManagerMmbakDatabaseOpener.readCategoryNames(db)
                val rows = MoneyManagerMmbakDatabaseOpener.readTransactions(db)
                val importPlan = MoneyManagerMmbakTransferPlanner.plan(rows)
                val accountCache = AccountNameResolver(accountRepository)
                val categoryCache = CategoryNameResolver(categoryRepository)
                val counters =
                    ImportCounters(
                        skippedUnsupported = importPlan.unsupportedRowKeys.size,
                    )
                mergeIncomeExpenseRows(
                    rows = importPlan.incomeExpense,
                    sinceTimestampMillis = sinceTimestampMillis,
                    assetNames = assetNames,
                    categories = categories,
                    accountCache = accountCache,
                    categoryCache = categoryCache,
                    counters = counters,
                )
                mergeTransferRows(
                    transfers = importPlan.transfers,
                    sinceTimestampMillis = sinceTimestampMillis,
                    assetNames = assetNames,
                    accountCache = accountCache,
                    counters = counters,
                )
                Result(
                    added = counters.added,
                    skippedExisting = counters.skippedExisting,
                    skippedUnsupported = counters.skippedUnsupported,
                    rowsInBackup = rows.size,
                )
            } finally {
                MoneyManagerMmbakDatabaseOpener.closeQuietly(opened)
            }
        }

        private data class ImportCounters(
            var added: Int = 0,
            var skippedExisting: Int = 0,
            var skippedUnsupported: Int = 0,
        )

        private suspend fun mergeIncomeExpenseRows(
            rows: List<MoneyManagerMmbakDatabaseOpener.MoneyManagerTransactionRow>,
            sinceTimestampMillis: Long,
            assetNames: Map<String, String>,
            categories: Map<Long, MoneyManagerMmbakDatabaseOpener.CategoryRef>,
            accountCache: AccountNameResolver,
            categoryCache: CategoryNameResolver,
            counters: ImportCounters,
        ) {
            for (row in rows) {
                if (row.dateTimeMillis < sinceTimestampMillis) continue
                val ref = rowSourceRef(row.rowKey)
                if (transactionDao.findIdBySourceReference(ref) != null) {
                    counters.skippedExisting++
                    continue
                }
                val txnType =
                    mapIncomeExpenseType(row.doType) ?: run {
                        counters.skippedUnsupported++
                        continue
                    }
                val accountId =
                    accountCache.resolve(
                        assetNames[row.assetUid.orEmpty()] ?: "Imported account",
                    )
                val (categoryId, subcategoryId) =
                    resolveCategoryIds(categories, row.categoryUid, txnType, categoryCache)
                transactionRepository.insertTransaction(
                    TransactionEntity(
                        dateTime = row.dateTimeMillis,
                        amount = row.amount,
                        type = txnType,
                        categoryId = categoryId,
                        subcategoryId = subcategoryId,
                        note = row.note,
                        accountId = accountId,
                        sourceType = TransactionSourceType.MONEY_MANAGER,
                        sourceReferenceId = ref,
                        status = RecordStatus.APPROVED,
                    ),
                )
                counters.added++
            }
        }

        private suspend fun mergeTransferRows(
            transfers: List<MoneyManagerMmbakTransferPlanner.PlannedTransfer>,
            sinceTimestampMillis: Long,
            assetNames: Map<String, String>,
            accountCache: AccountNameResolver,
            counters: ImportCounters,
        ) {
            for (transfer in transfers) {
                if (transfer.dateTimeMillis < sinceTimestampMillis) continue
                if (transactionDao.findIdBySourceReference(transfer.sourceReferenceId) != null) {
                    counters.skippedExisting += transfer.legRowKeys.size
                    continue
                }
                val fromAccountId =
                    accountCache.resolve(
                        assetNames[transfer.fromAssetUid] ?: transfer.fromAssetUid,
                    )
                val toAccountId =
                    accountCache.resolve(
                        assetNames[transfer.toAssetUid] ?: transfer.toAssetUid,
                    )
                if (fromAccountId == toAccountId) {
                    counters.skippedUnsupported += transfer.legRowKeys.size
                    continue
                }
                transactionRepository.insertTransaction(
                    TransactionEntity(
                        dateTime = transfer.dateTimeMillis,
                        amount = transfer.amount,
                        type = TransactionType.TRANSFER,
                        note = transfer.note,
                        accountId = fromAccountId,
                        relatedAccountId = toAccountId,
                        sourceType = TransactionSourceType.MONEY_MANAGER,
                        sourceReferenceId = transfer.sourceReferenceId,
                        status = RecordStatus.APPROVED,
                    ),
                )
                counters.added++
            }
        }

        private suspend fun resolveCategoryIds(
            categories: Map<Long, MoneyManagerMmbakDatabaseOpener.CategoryRef>,
            categoryUid: Long?,
            txnType: TransactionType,
            cache: CategoryNameResolver,
        ): Pair<Long?, Long?> {
            if (categoryUid == null) return null to null
            val cat = categories[categoryUid] ?: return null to null
            val categoryType =
                when (txnType) {
                    TransactionType.INCOME -> CategoryType.INCOME
                    else -> CategoryType.EXPENSE
                }
            return if (cat.parentUid != null) {
                val parent = categories[cat.parentUid]
                val parentId = parent?.let { cache.resolveCategory(it.name, categoryType) }
                val subId = cache.resolveSubcategory(parentId, cat.name)
                parentId to subId
            } else {
                val parentId = cache.resolveCategory(cat.name, categoryType)
                parentId to null
            }
        }

        private fun mapIncomeExpenseType(doType: Int): TransactionType? =
            when (doType) {
                0 -> TransactionType.INCOME
                1 -> TransactionType.EXPENSE
                else -> null
            }

        private class AccountNameResolver(
            private val accountRepository: AccountRepository,
        ) {
            private val byLowerName = LinkedHashMap<String, Long>()

            suspend fun resolve(name: String): Long {
                val trimmed = name.trim().ifEmpty { "Imported account" }
                val key = trimmed.lowercase()
                byLowerName[key]?.let { return it }
                val accounts = accountRepository.getActiveAccounts().first()
                accounts.firstOrNull { it.name.equals(trimmed, ignoreCase = true) }?.id?.let {
                    byLowerName[key] = it
                    return it
                }
                val id =
                    accountRepository.createAccount(
                        name = trimmed,
                        type = AccountType.OTHER,
                        isDefault = false,
                    )
                byLowerName[key] = id
                return id
            }
        }

        private class CategoryNameResolver(
            private val categoryRepository: CategoryRepository,
        ) {
            private val categoriesByKey = LinkedHashMap<String, Long>()
            private val subcategoriesByKey = LinkedHashMap<String, Long>()

            suspend fun resolveCategory(
                name: String,
                type: CategoryType,
            ): Long {
                val trimmed = name.trim()
                val key = "${type.name}:${trimmed.lowercase()}"
                categoriesByKey[key]?.let { return it }
                val trees = categoryRepository.observeActiveCategoryTreeByType(type).first()
                trees
                    .firstOrNull { it.category.name.equals(trimmed, ignoreCase = true) }
                    ?.category
                    ?.id
                    ?.value
                    ?.let {
                        categoriesByKey[key] = it
                        return it
                    }
                val id =
                    categoryRepository.createCategory(
                        CategoryEntity(
                            name = trimmed,
                            type = type,
                            iconName = "category_default",
                        ),
                    )
                categoriesByKey[key] = id
                return id
            }

            suspend fun resolveSubcategory(
                parentCategoryId: Long?,
                name: String,
            ): Long? {
                if (parentCategoryId == null) return null
                val trimmed = name.trim()
                val key = "$parentCategoryId:${trimmed.lowercase()}"
                subcategoriesByKey[key]?.let { return it }
                val subs = categoryRepository.getSubcategoriesForCategory(parentCategoryId).first()
                subs
                    .firstOrNull { it.name.equals(trimmed, ignoreCase = true) }
                    ?.id
                    ?.let {
                        subcategoriesByKey[key] = it
                        return it
                    }
                val id =
                    categoryRepository.createSubcategory(
                        SubcategoryEntity(
                            categoryId = parentCategoryId,
                            name = trimmed,
                        ),
                    )
                subcategoriesByKey[key] = id
                return id
            }
        }

        private companion object {
            fun rowSourceRef(rowKey: String): String = "mmbak:$rowKey"
        }
    }
