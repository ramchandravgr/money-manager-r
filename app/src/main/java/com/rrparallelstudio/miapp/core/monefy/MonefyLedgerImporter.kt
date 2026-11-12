package com.rrparallelstudio.miapp.core.monefy

import com.rrparallelstudio.miapp.core.database.dao.TransactionDao
import com.rrparallelstudio.miapp.core.database.entity.CategoryEntity
import com.rrparallelstudio.miapp.core.database.entity.TransactionEntity
import com.rrparallelstudio.miapp.core.domain.repository.AccountRepository
import com.rrparallelstudio.miapp.core.domain.repository.CategoryRepository
import com.rrparallelstudio.miapp.core.domain.repository.TransactionRepository
import com.rrparallelstudio.miapp.core.lansync.LanSyncAccountBalanceValidator
import com.rrparallelstudio.miapp.core.model.enums.CategoryType
import com.rrparallelstudio.miapp.core.model.enums.RecordStatus
import com.rrparallelstudio.miapp.core.model.enums.TransactionSourceType
import com.rrparallelstudio.miapp.core.model.enums.TransactionType
import kotlinx.coroutines.flow.first
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Merges a Monefy Android backup into the local ledger.
 * Accounts and categories are matched by name; transactions and transfers are skipped when the same
 * Monefy id was already imported (`sourceReferenceId`).
 */
@Singleton
class MonefyLedgerImporter
    @Inject
    constructor(
        private val transactionDao: TransactionDao,
        private val transactionRepository: TransactionRepository,
        private val accountRepository: AccountRepository,
        private val categoryRepository: CategoryRepository,
        private val balanceValidator: LanSyncAccountBalanceValidator,
    ) {
        data class Result(
            val accountsCreated: Int,
            val categoriesCreated: Int,
            val transactionsAdded: Int,
            val transfersAdded: Int,
            val skippedExisting: Int,
            val skippedUnsupported: Int,
        )

        suspend fun importFromBytes(
            data: ByteArray,
            cacheDir: File,
        ): Result =
            try {
                importUnchecked(data, cacheDir)
            } catch (e: IllegalArgumentException) {
                throw e
            } catch (e: Exception) {
                throw IllegalArgumentException(
                    e.message?.takeIf { it.isNotBlank() }
                        ?: "Could not read Monefy backup (file may be corrupt or unsupported).",
                    e,
                )
            }

        private suspend fun importUnchecked(
            data: ByteArray,
            cacheDir: File,
        ): Result {
            val snapshot = MonefyBackupReader.read(data, cacheDir)
            val accounts = AccountCache(accountRepository)
            var accountsCreated = 0
            for (account in snapshot.accounts) {
                val created = accounts.resolve(account)
                if (created) accountsCreated++
            }
            val categories = CategoryCache(categoryRepository)
            var categoriesCreated = 0
            val categoryByMonefyId = HashMap<String, MonefyBackupReader.CategoryRow>()
            for (category in snapshot.categories) {
                categoryByMonefyId[category.id] = category
                if (categories.ensure(category)) categoriesCreated++
            }
            var transactionsAdded = 0
            var transfersAdded = 0
            var skippedExisting = 0
            var skippedUnsupported = 0
            for (row in snapshot.transactions) {
                val ref = "monefy:txn:${row.id}"
                if (transactionDao.findIdBySourceReference(ref) != null) {
                    skippedExisting++
                    continue
                }
                val accountId =
                    accounts.idFor(row.accountId) ?: run {
                        skippedUnsupported++
                        continue
                    }
                val category = row.categoryId?.let { categoryByMonefyId[it] }
                val isIncome = category?.isIncome == true
                val categoryId =
                    category?.let {
                        categories.idFor(it)
                    }
                transactionRepository.insertTransaction(
                    TransactionEntity(
                        dateTime = row.createdOnMillis,
                        amount = row.amount,
                        type = if (isIncome) TransactionType.INCOME else TransactionType.EXPENSE,
                        categoryId = categoryId,
                        note = row.note,
                        accountId = accountId,
                        sourceType = TransactionSourceType.MONEYFY,
                        sourceReferenceId = ref,
                        status = RecordStatus.APPROVED,
                    ),
                )
                transactionsAdded++
            }
            for (row in snapshot.transfers) {
                val ref = "monefy:xfer:${row.id}"
                if (transactionDao.findIdBySourceReference(ref) != null) {
                    skippedExisting++
                    continue
                }
                val fromId = accounts.idFor(row.fromAccountId)
                val toId = accounts.idFor(row.toAccountId)
                if (fromId == null || toId == null || fromId == toId) {
                    skippedUnsupported++
                    continue
                }
                transactionRepository.insertTransaction(
                    TransactionEntity(
                        dateTime = row.createdOnMillis,
                        amount = row.amount,
                        type = TransactionType.TRANSFER,
                        note = row.note,
                        accountId = fromId,
                        relatedAccountId = toId,
                        sourceType = TransactionSourceType.MONEYFY,
                        sourceReferenceId = ref,
                        status = RecordStatus.APPROVED,
                    ),
                )
                transfersAdded++
            }
            balanceValidator.reconcileAfterMerge()
            return Result(
                accountsCreated = accountsCreated,
                categoriesCreated = categoriesCreated,
                transactionsAdded = transactionsAdded,
                transfersAdded = transfersAdded,
                skippedExisting = skippedExisting,
                skippedUnsupported = skippedUnsupported,
            )
        }

        private class AccountCache(
            private val accountRepository: AccountRepository,
        ) {
            private val byMonefyId = HashMap<String, Long>()
            private val byName = HashMap<String, Long>()

            suspend fun resolve(account: MonefyBackupReader.AccountRow): Boolean {
                byName[account.title.lowercase()]?.let {
                    byMonefyId[account.id] = it
                    return false
                }
                val existing =
                    accountRepository
                        .getActiveAccounts()
                        .first()
                        .firstOrNull { it.name.equals(account.title, ignoreCase = true) }
                if (existing != null) {
                    byName[account.title.lowercase()] = existing.id
                    byMonefyId[account.id] = existing.id
                    return false
                }
                val id =
                    accountRepository.createAccount(
                        name = account.title,
                        type = MonefyAccountTypeMapper.fromTitle(account.title),
                        openingBalance = account.openingBalance,
                        isDefault = false,
                    )
                byName[account.title.lowercase()] = id
                byMonefyId[account.id] = id
                return true
            }

            fun idFor(monefyId: String): Long? = byMonefyId[monefyId]
        }

        private class CategoryCache(
            private val categoryRepository: CategoryRepository,
        ) {
            private val byKey = HashMap<String, Long>()

            suspend fun ensure(category: MonefyBackupReader.CategoryRow): Boolean {
                val type = if (category.isIncome) CategoryType.INCOME else CategoryType.EXPENSE
                val key = key(category.title, type)
                if (byKey.containsKey(key)) return false
                val trees = categoryRepository.observeActiveCategoryTreeByType(type).first()
                val existing =
                    trees
                        .firstOrNull { it.category.name.equals(category.title, ignoreCase = true) }
                        ?.category
                        ?.id
                        ?.value
                if (existing != null) {
                    byKey[key] = existing
                    return false
                }
                val id =
                    categoryRepository.createCategory(
                        CategoryEntity(
                            name = category.title,
                            type = type,
                            iconName = "category_default",
                        ),
                    )
                byKey[key] = id
                return true
            }

            fun idFor(category: MonefyBackupReader.CategoryRow): Long? {
                val type = if (category.isIncome) CategoryType.INCOME else CategoryType.EXPENSE
                return byKey[key(category.title, type)]
            }

            private fun key(
                name: String,
                type: CategoryType,
            ): String = "${type.name}:${name.lowercase()}"
        }
    }
