package com.mmr.core.lansync

import androidx.room.withTransaction
import com.mmr.core.common.sync.MmrSyncId
import com.mmr.core.database.MmrDatabase
import com.mmr.core.database.entity.AccountEntity
import com.mmr.core.database.entity.CategoryEntity
import com.mmr.core.database.entity.LoanEntity
import com.mmr.core.database.entity.SubcategoryEntity
import com.mmr.core.database.entity.TransactionEntity
import com.mmr.core.lansync.model.LanAccountRecord
import com.mmr.core.lansync.model.LanCategoryRecord
import com.mmr.core.lansync.model.LanLoanRecord
import com.mmr.core.lansync.model.LanSubcategoryRecord
import com.mmr.core.lansync.model.LanTransactionRecord
import com.mmr.core.lansync.model.MmrLanSyncPayload
import com.mmr.core.model.enums.AccountType
import com.mmr.core.model.enums.CardNetwork
import com.mmr.core.model.enums.CategoryType
import com.mmr.core.model.enums.LoanStatus
import com.mmr.core.model.enums.LoanType
import com.mmr.core.model.enums.RecordStatus
import com.mmr.core.model.enums.TransactionSourceType
import com.mmr.core.model.enums.TransactionType
import javax.inject.Inject
import javax.inject.Singleton

data class LanSyncMergeSummary(
    val categoriesInserted: Int,
    val categoriesUpdated: Int,
    val subcategoriesInserted: Int,
    val subcategoriesUpdated: Int,
    val accountsInserted: Int,
    val accountsUpdated: Int,
    val loansInserted: Int,
    val loansUpdated: Int,
    val transactionsInserted: Int,
    val transactionsUpdated: Int,
) {
    val totalChanges: Int
        get() =
            categoriesInserted + categoriesUpdated + subcategoriesInserted + subcategoriesUpdated +
                accountsInserted + accountsUpdated + loansInserted + loansUpdated +
                transactionsInserted + transactionsUpdated
}

@Singleton
class LanSyncMerger
    @Inject
    constructor(
        private val database: MmrDatabase,
    ) {
        suspend fun merge(payload: MmrLanSyncPayload): LanSyncMergeSummary {
            val categoryDao = database.categoryDao()
            val accountDao = database.accountDao()
            val loanDao = database.loanDao()
            val transactionDao = database.transactionDao()

            var categoriesInserted = 0
            var categoriesUpdated = 0
            var subcategoriesInserted = 0
            var subcategoriesUpdated = 0
            var accountsInserted = 0
            var accountsUpdated = 0
            var loansInserted = 0
            var loansUpdated = 0
            var transactionsInserted = 0
            var transactionsUpdated = 0

            database.withTransaction {
                val categorySyncToLocal = mutableMapOf<String, Long>()
                for (record in payload.categories) {
                    val existing = categoryDao.getCategoryBySyncId(record.syncId)
                    if (existing == null) {
                        val id =
                            categoryDao.insert(
                                record.toEntity(),
                            )
                        categorySyncToLocal[record.syncId] = id
                        categoriesInserted++
                    } else {
                        categorySyncToLocal[record.syncId] = existing.id
                    }
                }

                val subcategorySyncToLocal = mutableMapOf<String, Long>()
                for (record in payload.subcategories) {
                    val categoryId =
                        categorySyncToLocal[record.categorySyncId]
                            ?: categoryDao.getCategoryBySyncId(record.categorySyncId)?.id
                            ?: continue
                    val existing = categoryDao.getSubcategoryBySyncId(record.syncId)
                    if (existing == null) {
                        val id =
                            categoryDao.insertSubcategory(
                                record.toEntity(categoryId),
                            )
                        subcategorySyncToLocal[record.syncId] = id
                        subcategoriesInserted++
                    } else {
                        subcategorySyncToLocal[record.syncId] = existing.id
                    }
                }

                val accountSyncToLocal = mutableMapOf<String, Long>()
                for (record in payload.accounts) {
                    val existing = accountDao.getBySyncId(record.syncId)
                    if (existing == null) {
                        val id = accountDao.insert(record.toEntity())
                        accountSyncToLocal[record.syncId] = id
                        accountsInserted++
                    } else if (record.updatedAt > existing.updatedAt) {
                        accountDao.update(record.toEntity(existing.id))
                        accountSyncToLocal[record.syncId] = existing.id
                        accountsUpdated++
                    } else {
                        accountSyncToLocal[record.syncId] = existing.id
                    }
                }

                val loanSyncToLocal = mutableMapOf<String, Long>()
                for (record in payload.loans) {
                    val debitId = record.linkedDebitAccountSyncId?.let { accountSyncToLocal[it] }
                    val loanAccountId = record.linkedLoanAccountSyncId?.let { accountSyncToLocal[it] }
                    val existing = loanDao.getBySyncId(record.syncId)
                    if (existing == null) {
                        val id =
                            loanDao.insert(
                                record.toEntity(debitId, loanAccountId),
                            )
                        loanSyncToLocal[record.syncId] = id
                        loansInserted++
                    } else if (record.updatedAt > existing.updatedAt) {
                        loanDao.update(record.toEntity(debitId, loanAccountId, existing.id))
                        loanSyncToLocal[record.syncId] = existing.id
                        loansUpdated++
                    } else {
                        loanSyncToLocal[record.syncId] = existing.id
                    }
                }

                for (record in payload.transactions) {
                    val accountId = accountSyncToLocal[record.accountSyncId] ?: continue
                    val categoryId = record.categorySyncId?.let { categorySyncToLocal[it] }
                    val subcategoryId = record.subcategorySyncId?.let { subcategorySyncToLocal[it] }
                    val relatedAccountId = record.relatedAccountSyncId?.let { accountSyncToLocal[it] }
                    val loanId = record.loanSyncId?.let { loanSyncToLocal[it] }
                    val existing = transactionDao.getBySyncId(record.syncId)
                    if (existing == null) {
                        transactionDao.insert(
                            record.toEntity(
                                accountId = accountId,
                                categoryId = categoryId,
                                subcategoryId = subcategoryId,
                                relatedAccountId = relatedAccountId,
                                loanId = loanId,
                            ),
                        )
                        transactionsInserted++
                    } else if (record.updatedAt > existing.updatedAt) {
                        transactionDao.update(
                            record.toEntity(
                                accountId = accountId,
                                categoryId = categoryId,
                                subcategoryId = subcategoryId,
                                relatedAccountId = relatedAccountId,
                                loanId = loanId,
                                existingId = existing.id,
                            ),
                        )
                        transactionsUpdated++
                    }
                }
            }

            return LanSyncMergeSummary(
                categoriesInserted = categoriesInserted,
                categoriesUpdated = categoriesUpdated,
                subcategoriesInserted = subcategoriesInserted,
                subcategoriesUpdated = subcategoriesUpdated,
                accountsInserted = accountsInserted,
                accountsUpdated = accountsUpdated,
                loansInserted = loansInserted,
                loansUpdated = loansUpdated,
                transactionsInserted = transactionsInserted,
                transactionsUpdated = transactionsUpdated,
            )
        }

        private fun LanCategoryRecord.toEntity() =
            CategoryEntity(
                id = 0L,
                syncId = syncId,
                name = name,
                iconName = iconName,
                colorHex = colorHex,
                type = CategoryType.valueOf(type),
                isDefault = isDefault,
                isActive = isActive,
                orderIndex = orderIndex,
            )

        private fun LanSubcategoryRecord.toEntity(categoryId: Long) =
            SubcategoryEntity(
                id = 0L,
                syncId = syncId,
                categoryId = categoryId,
                name = name,
                isActive = isActive,
                orderIndex = orderIndex,
            )

        private fun LanAccountRecord.toEntity(existingId: Long = 0L) =
            AccountEntity(
                id = existingId,
                syncId = syncId,
                name = name,
                type = AccountType.valueOf(type),
                openingBalance = openingBalance,
                currentBalance = currentBalance,
                currencyCode = currencyCode,
                isActive = isActive,
                isDefault = isDefault,
                cardLastFour = cardLastFour,
                createdAt = createdAt,
                updatedAt = updatedAt,
            )

        private fun LanLoanRecord.toEntity(
            linkedDebitAccountId: Long?,
            linkedLoanAccountId: Long?,
            existingId: Long = 0L,
        ) = LoanEntity(
            id = existingId,
            syncId = syncId,
            name = name,
            lenderName = lenderName,
            principalAmount = principalAmount,
            interestRate = interestRate,
            outstandingAmount = outstandingAmount,
            openingDate = openingDate,
            dueDate = dueDate,
            autoTransferDate = autoTransferDate,
            linkedDebitAccountId = linkedDebitAccountId,
            linkedLoanAccountId = linkedLoanAccountId,
            loanType = LoanType.valueOf(loanType),
            status = LoanStatus.valueOf(status),
            repaymentMode = repaymentMode,
            tenureMonths = tenureMonths,
            remainingTenureMonths = remainingTenureMonths,
            fixedEmiAmount = fixedEmiAmount,
            emiDayOfMonth = emiDayOfMonth,
            fixedEmiInputMode = fixedEmiInputMode,
            notes = notes,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )

        private fun LanTransactionRecord.toEntity(
            accountId: Long,
            categoryId: Long?,
            subcategoryId: Long?,
            relatedAccountId: Long?,
            loanId: Long?,
            existingId: Long = 0L,
        ) = TransactionEntity(
            id = existingId,
            syncId = MmrSyncId.normalize(syncId),
            dateTime = dateTime,
            amount = amount,
            type = TransactionType.valueOf(type),
            categoryId = categoryId,
            subcategoryId = subcategoryId,
            note = note,
            accountId = accountId,
            relatedAccountId = relatedAccountId,
            loanId = loanId,
            sourceType = TransactionSourceType.valueOf(sourceType),
            sourceReferenceId = sourceReferenceId,
            status = RecordStatus.APPROVED,
            attachmentCount = attachmentCount,
            createdAt = createdAt,
            updatedAt = updatedAt,
            isEdited = isEdited,
            paymentChannel = paymentChannel,
            paymentInstrument = paymentInstrument,
            cardLastFour = cardLastFour,
            cardNetwork = cardNetwork?.let { runCatching { CardNetwork.valueOf(it) }.getOrNull() },
        )
    }
