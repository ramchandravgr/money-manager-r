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
 * Merges income/expense rows from a Realbyte Money Manager `.mmbak` into the local ledger.
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
                val accountCache = AccountNameResolver(accountRepository)
                val categoryCache = CategoryNameResolver(categoryRepository)
                var added = 0
                var skippedExisting = 0
                var skippedUnsupported = 0
                for (row in rows) {
                    if (row.dateTimeMillis < sinceTimestampMillis) continue
                    val ref = sourceRef(row.uid)
                    if (transactionDao.findIdBySourceReference(ref) != null) {
                        skippedExisting++
                        continue
                    }
                    val txnType = mapTransactionType(row.doType)
                    if (txnType == null) {
                        skippedUnsupported++
                        continue
                    }
                    val accountId =
                        accountCache.resolve(
                            assetNames[row.assetUid ?: -1L] ?: "Imported account",
                        )
                    val (categoryId, subcategoryId) =
                        resolveCategoryIds(
                            categories,
                            row.categoryUid,
                            txnType,
                            categoryCache,
                        )
                    val entity =
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
                        )
                    transactionRepository.insertTransaction(entity)
                    added++
                }
                Result(added = added, skippedExisting = skippedExisting, skippedUnsupported = skippedUnsupported)
            } finally {
                MoneyManagerMmbakDatabaseOpener.closeQuietly(opened)
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

        private fun mapTransactionType(doType: Int): TransactionType? =
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
            fun sourceRef(uid: Long): String = "mmbak:$uid"
        }
    }
