package com.mmr.core.lansync

import com.mmr.core.backup.model.MmrAppSettingsBackup
import com.mmr.core.common.sync.MmrSyncId
import com.mmr.core.database.dao.AccountDao
import com.mmr.core.database.dao.CategoryDao
import com.mmr.core.database.dao.LoanDao
import com.mmr.core.database.dao.TransactionDao
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
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LanSyncExporter
    @Inject
    constructor(
        private val transactionDao: TransactionDao,
        private val categoryDao: CategoryDao,
        private val accountDao: AccountDao,
        private val loanDao: LoanDao,
    ) {
        suspend fun buildPayload(
            sourceDeviceId: String,
            appSettings: MmrAppSettingsBackup?,
            displaySettings: com.mmr.core.data.preferences.AppDisplaySettingsSnapshot?,
        ): MmrLanSyncPayload {
            val approved = transactionDao.getAllApprovedTransactions()
            val accountIds = mutableSetOf<Long>()
            val categoryIds = mutableSetOf<Long>()
            val subcategoryIds = mutableSetOf<Long>()
            val loanIds = mutableSetOf<Long>()
            approved.forEach { txn ->
                accountIds += txn.accountId
                txn.relatedAccountId?.let { accountIds += it }
                txn.categoryId?.let { categoryIds += it }
                txn.subcategoryId?.let { subcategoryIds += it }
                txn.loanId?.let { loanIds += it }
            }

            val accounts = accountIds.mapNotNull { accountDao.getById(it) }.map { it.withSyncId() }
            val accountSyncById = accounts.associate { it.id to it.syncId }

            val loans =
                loanIds.mapNotNull { loanDao.getById(it) }.map { loan ->
                    loan.withSyncId().also { resolved ->
                        resolved.linkedDebitAccountId?.let { accountIds += it }
                        resolved.linkedLoanAccountId?.let { accountIds += it }
                    }
                }
            val allAccounts =
                accountIds
                    .mapNotNull {
                        accountDao.getById(
                            it,
                        )
                    }.map { it.withSyncId() }
                    .distinctBy { it.syncId }
            val accountSyncMap = allAccounts.associate { it.id to it.syncId }

            val categories = categoryIds.mapNotNull { categoryDao.getCategoryById(it) }.map { it.withSyncId() }
            val categorySyncMap = categories.associate { it.id to it.syncId }

            val subcategories =
                subcategoryIds.mapNotNull { categoryDao.getSubcategoryById(it) }.map { it.withSyncId() }
            subcategories.forEach { sub ->
                categoryIds += sub.categoryId
            }
            val allCategories = categoryIds.mapNotNull { categoryDao.getCategoryById(it) }.map { it.withSyncId() }
            val finalCategorySyncMap = allCategories.associate { it.id to it.syncId }
            val subcategorySyncMap = subcategories.associate { it.id to it.syncId }

            return MmrLanSyncPayload(
                formatVersion = LanSyncJsonCodec.CURRENT_FORMAT_VERSION,
                exportedAtEpochMillis = System.currentTimeMillis(),
                sourceDeviceId = sourceDeviceId,
                categories = allCategories.map { it.toLanRecord() },
                subcategories =
                    subcategoryIds.mapNotNull { categoryDao.getSubcategoryById(it) }.map { sub ->
                        sub.withSyncId().toLanRecord(finalCategorySyncMap)
                    },
                accounts = allAccounts.map { it.toLanRecord() },
                loans = loans.map { it.toLanRecord(accountSyncMap) },
                transactions =
                    approved.map {
                        it.withSyncId().toLanRecord(finalCategorySyncMap, subcategorySyncMap, accountSyncMap, loans)
                    },
                appSettings = appSettings,
                displaySettings = displaySettings,
            )
        }

        private fun CategoryEntity.withSyncId() = copy(syncId = MmrSyncId.normalize(syncId))

        private fun SubcategoryEntity.withSyncId() = copy(syncId = MmrSyncId.normalize(syncId))

        private fun AccountEntity.withSyncId() = copy(syncId = MmrSyncId.normalize(syncId))

        private fun LoanEntity.withSyncId() = copy(syncId = MmrSyncId.normalize(syncId))

        private fun TransactionEntity.withSyncId() = copy(syncId = MmrSyncId.normalize(syncId))

        private fun CategoryEntity.toLanRecord() =
            LanCategoryRecord(
                syncId = syncId,
                name = name,
                iconName = iconName,
                colorHex = colorHex,
                type = type.name,
                isDefault = isDefault,
                isActive = isActive,
                orderIndex = orderIndex,
            )

        private fun SubcategoryEntity.toLanRecord(categorySyncMap: Map<Long, String>) =
            LanSubcategoryRecord(
                syncId = syncId,
                categorySyncId = categorySyncMap[categoryId] ?: error("Missing category sync id"),
                name = name,
                isActive = isActive,
                orderIndex = orderIndex,
            )

        private fun AccountEntity.toLanRecord() =
            LanAccountRecord(
                syncId = syncId,
                name = name,
                type = type.name,
                openingBalance = openingBalance,
                currentBalance = currentBalance,
                currencyCode = currencyCode,
                isActive = isActive,
                isDefault = isDefault,
                cardLastFour = cardLastFour,
                createdAt = createdAt,
                updatedAt = updatedAt,
            )

        private fun LoanEntity.toLanRecord(accountSyncMap: Map<Long, String>) =
            LanLoanRecord(
                syncId = syncId,
                name = name,
                lenderName = lenderName,
                principalAmount = principalAmount,
                interestRate = interestRate,
                outstandingAmount = outstandingAmount,
                openingDate = openingDate,
                dueDate = dueDate,
                autoTransferDate = autoTransferDate,
                linkedDebitAccountSyncId = linkedDebitAccountId?.let { accountSyncMap[it] },
                linkedLoanAccountSyncId = linkedLoanAccountId?.let { accountSyncMap[it] },
                loanType = loanType.name,
                status = status.name,
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

        private fun TransactionEntity.toLanRecord(
            categorySyncMap: Map<Long, String>,
            subcategorySyncMap: Map<Long, String>,
            accountSyncMap: Map<Long, String>,
            loans: List<LoanEntity>,
        ): LanTransactionRecord {
            val loanSyncMap = loans.associate { it.id to it.syncId }
            return LanTransactionRecord(
                syncId = syncId,
                dateTime = dateTime,
                amount = amount,
                type = type.name,
                categorySyncId = categoryId?.let { categorySyncMap[it] },
                subcategorySyncId = subcategoryId?.let { subcategorySyncMap[it] },
                note = note,
                accountSyncId = accountSyncMap[accountId] ?: error("Missing account sync id"),
                relatedAccountSyncId = relatedAccountId?.let { accountSyncMap[it] },
                loanSyncId = loanId?.let { loanSyncMap[it] },
                sourceType = sourceType.name,
                sourceReferenceId = sourceReferenceId,
                attachmentCount = attachmentCount,
                createdAt = createdAt,
                updatedAt = updatedAt,
                isEdited = isEdited,
                paymentChannel = paymentChannel,
                paymentInstrument = paymentInstrument,
                cardLastFour = cardLastFour,
                cardNetwork = cardNetwork?.name,
            )
        }
    }
