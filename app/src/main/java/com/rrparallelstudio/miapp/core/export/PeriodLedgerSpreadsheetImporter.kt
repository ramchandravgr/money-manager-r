package com.rrparallelstudio.miapp.core.export

import com.rrparallelstudio.miapp.core.database.dao.TransactionDao
import com.rrparallelstudio.miapp.core.database.entity.CategoryEntity
import com.rrparallelstudio.miapp.core.database.entity.TransactionEntity
import com.rrparallelstudio.miapp.core.domain.repository.AccountRepository
import com.rrparallelstudio.miapp.core.domain.repository.CategoryRepository
import com.rrparallelstudio.miapp.core.domain.repository.TransactionRepository
import com.rrparallelstudio.miapp.core.lansync.LanSyncAccountBalanceValidator
import com.rrparallelstudio.miapp.core.model.enums.AccountType
import com.rrparallelstudio.miapp.core.model.enums.CategoryType
import com.rrparallelstudio.miapp.core.model.enums.RecordStatus
import com.rrparallelstudio.miapp.core.model.enums.TransactionSourceType
import com.rrparallelstudio.miapp.core.model.enums.TransactionType
import kotlinx.coroutines.flow.first
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PeriodLedgerSpreadsheetImporter
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
            val skippedExisting: Int,
            val skippedUnsupported: Int,
        )

        suspend fun importFromBytes(
            data: ByteArray,
            displayName: String?,
            zoneId: ZoneId = ZoneId.systemDefault(),
        ): Result {
            val rows =
                if (displayName?.lowercase()?.endsWith(".xml") == true) {
                    PeriodXmlLedgerParser.readRows(data)
                } else {
                    PeriodExcelSheetReader.readRows(data, zoneId)
                }
            if (rows.isEmpty()) {
                throw IllegalArgumentException("No transactions found in spreadsheet.")
            }
            val accounts = AccountCache(accountRepository)
            val categories = CategoryCache(categoryRepository)
            var accountsCreated = 0
            var categoriesCreated = 0
            var transactionsAdded = 0
            var skippedExisting = 0
            var skippedUnsupported = 0
            for (row in rows) {
                val ref = "spreadsheet:row:${row.rowIndex}"
                if (transactionDao.findIdBySourceReference(ref) != null) {
                    skippedExisting++
                    continue
                }
                val type =
                    flowToType(row.flowLabel) ?: run {
                        skippedUnsupported++
                        continue
                    }
                var accountId = accounts.resolve(row.accountName)
                if (accountId == null) {
                    accounts.create(row.accountName)
                    accountsCreated++
                    accountId = accounts.resolve(row.accountName)
                }
                if (accountId == null) {
                    skippedUnsupported++
                    continue
                }
                val categoryId =
                    row.categoryName.takeIf { it.isNotBlank() }?.let { name ->
                        val created = categories.ensure(name, type)
                        if (created) categoriesCreated++
                        categories.idFor(name, type)
                    }
                transactionRepository.insertTransaction(
                    TransactionEntity(
                        dateTime = row.dateTimeMillis,
                        amount = row.amount,
                        type = type,
                        categoryId = categoryId,
                        note = row.note.takeIf { it.isNotBlank() },
                        accountId = accountId,
                        sourceType = TransactionSourceType.MANUAL,
                        sourceReferenceId = ref,
                        status = RecordStatus.APPROVED,
                    ),
                )
                transactionsAdded++
            }
            balanceValidator.reconcileAfterMerge()
            return Result(
                accountsCreated = accountsCreated,
                categoriesCreated = categoriesCreated,
                transactionsAdded = transactionsAdded,
                skippedExisting = skippedExisting,
                skippedUnsupported = skippedUnsupported,
            )
        }

        private fun flowToType(flow: String): TransactionType? =
            when (flow.trim().lowercase()) {
                "income" -> TransactionType.INCOME
                "expense", "expense balance" -> TransactionType.EXPENSE
                else -> null
            }

        private class AccountCache(
            private val accountRepository: AccountRepository,
        ) {
            private val byLowerName = HashMap<String, Long>()

            suspend fun resolve(name: String): Long? {
                val key = name.trim().lowercase()
                if (key.isEmpty()) return null
                byLowerName[key]?.let { return it }
                val existing =
                    accountRepository
                        .getActiveAccounts()
                        .first()
                        .firstOrNull { it.name.equals(name, ignoreCase = true) }
                if (existing != null) {
                    byLowerName[key] = existing.id
                    return existing.id
                }
                return null
            }

            suspend fun create(name: String): Boolean {
                val id =
                    accountRepository.createAccount(
                        name = name.trim(),
                        type = AccountType.CASH,
                        openingBalance = 0.0,
                        isDefault = false,
                    )
                byLowerName[name.trim().lowercase()] = id
                return true
            }
        }

        private class CategoryCache(
            private val categoryRepository: CategoryRepository,
        ) {
            private val byKey = HashMap<String, Long>()

            private fun key(
                name: String,
                type: CategoryType,
            ): String = "${type.name}:${name.trim().lowercase()}"

            suspend fun ensure(
                name: String,
                txnType: TransactionType,
            ): Boolean {
                val type =
                    when (txnType) {
                        TransactionType.INCOME -> CategoryType.INCOME
                        TransactionType.EXPENSE -> CategoryType.EXPENSE
                        else -> return false
                    }
                val existingId = idFor(name, txnType)
                if (existingId != null) return false
                val id =
                    categoryRepository.createCategory(
                        CategoryEntity(
                            name = name.trim(),
                            type = type,
                            isDefault = false,
                        ),
                    )
                byKey[key(name, type)] = id
                return true
            }

            suspend fun idFor(
                name: String,
                txnType: TransactionType,
            ): Long? {
                val catType =
                    when (txnType) {
                        TransactionType.INCOME -> CategoryType.INCOME
                        TransactionType.EXPENSE -> CategoryType.EXPENSE
                        else -> return null
                    }
                val cacheKey = key(name, catType)
                byKey[cacheKey]?.let { return it }
                val existing =
                    categoryRepository
                        .getActiveCategories()
                        .first()
                        .firstOrNull {
                            it.type == catType && it.name.equals(name, ignoreCase = true)
                        }
                if (existing != null) {
                    byKey[cacheKey] = existing.id
                    return existing.id
                }
                return byKey[cacheKey]
            }
        }
    }
