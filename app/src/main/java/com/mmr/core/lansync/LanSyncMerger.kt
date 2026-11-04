package com.mmr.core.lansync

import androidx.room.withTransaction
import com.mmr.core.common.sync.MmrSyncId
import com.mmr.core.common.widget.HomeScreenWidgetRefreshTrigger
import com.mmr.core.data.preferences.LanSyncDuplicateEntityPolicy
import com.mmr.core.database.MmrDatabase
import com.mmr.core.database.entity.AccountEntity
import com.mmr.core.database.entity.AssetAttachmentEntity
import com.mmr.core.database.entity.AssetEntity
import com.mmr.core.database.entity.AssetValuationEntity
import com.mmr.core.database.entity.BudgetEntity
import com.mmr.core.database.entity.CategorizationRuleEntity
import com.mmr.core.database.entity.CategoryEntity
import com.mmr.core.database.entity.LoanEntity
import com.mmr.core.database.entity.LoanInterestRevisionEntity
import com.mmr.core.database.entity.LoanPaymentEntity
import com.mmr.core.database.entity.LoanScheduleEntryEntity
import com.mmr.core.database.entity.NotificationRuleEntity
import com.mmr.core.database.entity.RecurringRuleEntity
import com.mmr.core.database.entity.SubcategoryEntity
import com.mmr.core.database.entity.TransactionAttachmentEntity
import com.mmr.core.database.entity.TransactionEntity
import com.mmr.core.lansync.model.LanAccountRecord
import com.mmr.core.lansync.model.LanAssetAttachmentRecord
import com.mmr.core.lansync.model.LanAssetRecord
import com.mmr.core.lansync.model.LanAssetValuationRecord
import com.mmr.core.lansync.model.LanAttachmentRecord
import com.mmr.core.lansync.model.LanBudgetRecord
import com.mmr.core.lansync.model.LanCategorizationRuleRecord
import com.mmr.core.lansync.model.LanCategoryRecord
import com.mmr.core.lansync.model.LanLoanInterestRevisionRecord
import com.mmr.core.lansync.model.LanLoanPaymentRecord
import com.mmr.core.lansync.model.LanLoanRecord
import com.mmr.core.lansync.model.LanLoanScheduleEntryRecord
import com.mmr.core.lansync.model.LanNotificationRuleRecord
import com.mmr.core.lansync.model.LanRecurringRuleRecord
import com.mmr.core.lansync.model.LanSubcategoryRecord
import com.mmr.core.lansync.model.LanTransactionRecord
import com.mmr.core.lansync.model.MmrLanSyncPayload
import com.mmr.core.model.enums.AccountType
import com.mmr.core.model.enums.BudgetPeriodType
import com.mmr.core.model.enums.CardNetwork
import com.mmr.core.model.enums.CategorizationMatchType
import com.mmr.core.model.enums.CategoryType
import com.mmr.core.model.enums.LoanPaymentType
import com.mmr.core.model.enums.LoanScheduleStatus
import com.mmr.core.model.enums.LoanStatus
import com.mmr.core.model.enums.LoanType
import com.mmr.core.model.enums.NotificationRuleType
import com.mmr.core.model.enums.RecordStatus
import com.mmr.core.model.enums.RecurrenceInterval
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
    val tombstonesApplied: Int = 0,
    val keptAfterRemoteDelete: Int = 0,
    val warnings: List<String> = emptyList(),
) {
    val totalChanges: Int
        get() =
            categoriesInserted + categoriesUpdated + subcategoriesInserted + subcategoriesUpdated +
                accountsInserted + accountsUpdated + loansInserted + loansUpdated +
                transactionsInserted + transactionsUpdated + tombstonesApplied
}

@Singleton
class LanSyncMerger
    @Inject
    constructor(
        private val database: MmrDatabase,
        private val balanceValidator: LanSyncAccountBalanceValidator,
        private val tombstoneMerger: LanSyncTombstoneMerger,
        private val widgetRefreshTrigger: HomeScreenWidgetRefreshTrigger,
    ) {
        suspend fun merge(
            payload: MmrLanSyncPayload,
            options: LanSyncMergeOptions = LanSyncMergeOptions(),
        ): LanSyncMergeSummary {
            val categoryDao = database.categoryDao()
            val accountDao = database.accountDao()
            val loanDao = database.loanDao()
            val loanPaymentDao = database.loanPaymentDao()
            val loanInterestRevisionDao = database.loanInterestRevisionDao()
            val loanScheduleDao = database.loanScheduleDao()
            val recurringRuleDao = database.recurringRuleDao()
            val categorizationRuleDao = database.categorizationRuleDao()
            val budgetDao = database.budgetDao()
            val notificationRuleDao = database.notificationRuleDao()
            val backupDao = database.backupDao()
            val transactionDao = database.transactionDao()
            val attachmentDao = database.transactionAttachmentDao()
            val assetDao = database.assetDao()
            val assetValuationDao = database.assetValuationDao()
            val assetAttachmentDao = database.assetAttachmentDao()

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

            var tombstoneStats = LanSyncTombstoneMergeStats()
            database.withTransaction {
                tombstoneStats = tombstoneMerger.ingestRemoteTombstones(payload.tombstones)
                val categorySyncToLocal = mutableMapOf<String, Long>()
                for (record in payload.categories) {
                    val existing = categoryDao.getCategoryBySyncId(record.syncId)
                    if (existing == null) {
                        val nameMatch = categoryDao.findActiveByName(record.name)
                        if (
                            nameMatch != null &&
                            options.duplicateCategoryPolicy == LanSyncDuplicateEntityPolicy.MERGE_BY_NAME
                        ) {
                            categoryDao.update(record.toEntity(nameMatch.id))
                            categorySyncToLocal[record.syncId] = nameMatch.id
                            categoriesUpdated++
                            continue
                        }
                        val id =
                            categoryDao.insert(
                                record.toEntity(),
                            )
                        categorySyncToLocal[record.syncId] = id
                        categoriesInserted++
                    } else {
                        categoryDao.update(record.toEntity(existing.id))
                        categorySyncToLocal[record.syncId] = existing.id
                        categoriesUpdated++
                        if (record.isActive) {
                            tombstoneMerger.clearTombstone(record.syncId)
                        }
                    }
                }

                val subcategorySyncToLocal = mutableMapOf<String, Long>()
                for (record in payload.subcategories) {
                    val categoryId =
                        categorySyncToLocal[record.categorySyncId]
                            ?: categoryDao.getCategoryBySyncId(record.categorySyncId)?.id
                            ?: continue
                    if (tombstoneMerger.isDeletedForUpsert(record.syncId, 0)) continue
                    val existing = categoryDao.getSubcategoryBySyncId(record.syncId)
                    if (existing == null) {
                        val id =
                            categoryDao.insertSubcategory(
                                record.toEntity(categoryId),
                            )
                        subcategorySyncToLocal[record.syncId] = id
                        subcategoriesInserted++
                        if (record.isActive) {
                            tombstoneMerger.clearTombstone(record.syncId)
                        }
                    } else {
                        categoryDao.updateSubcategory(
                            record.toEntity(categoryId).copy(id = existing.id),
                        )
                        subcategorySyncToLocal[record.syncId] = existing.id
                        subcategoriesUpdated++
                        if (record.isActive) {
                            tombstoneMerger.clearTombstone(record.syncId)
                        }
                    }
                }

                val accountSyncToLocal = mutableMapOf<String, Long>()
                for (record in payload.accounts) {
                    if (tombstoneMerger.isDeletedForUpsert(record.syncId, record.updatedAt)) continue
                    val existing = accountDao.getBySyncId(record.syncId)
                    if (existing == null) {
                        val nameMatch = accountDao.findActiveByName(record.name)
                        if (
                            nameMatch != null &&
                            options.duplicateAccountPolicy == LanSyncDuplicateEntityPolicy.MERGE_BY_NAME
                        ) {
                            accountSyncToLocal[record.syncId] = nameMatch.id
                            if (record.updatedAt > nameMatch.updatedAt) {
                                accountDao.update(record.toEntity(nameMatch.id))
                                accountsUpdated++
                            }
                            continue
                        }
                        val id = accountDao.insert(record.toEntity())
                        accountSyncToLocal[record.syncId] = id
                        accountsInserted++
                    } else if (record.updatedAt > existing.updatedAt) {
                        accountDao.update(record.toEntity(existing.id))
                        accountSyncToLocal[record.syncId] = existing.id
                        accountsUpdated++
                        tombstoneMerger.clearIfSuperseded(record.syncId, record.updatedAt)
                    } else {
                        accountSyncToLocal[record.syncId] = existing.id
                    }
                }

                val loanSyncToLocal = mutableMapOf<String, Long>()
                for (record in payload.loans) {
                    if (tombstoneMerger.isDeletedForUpsert(record.syncId, record.updatedAt)) continue
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
                        tombstoneMerger.clearIfSuperseded(record.syncId, record.updatedAt)
                    } else {
                        loanSyncToLocal[record.syncId] = existing.id
                    }
                }

                val transactionSyncToLocal = mutableMapOf<String, Long>()
                for (record in payload.transactions) {
                    if (tombstoneMerger.isDeletedForUpsert(record.syncId, record.updatedAt)) continue
                    val accountId = accountSyncToLocal[record.accountSyncId] ?: continue
                    val categoryId = record.categorySyncId?.let { categorySyncToLocal[it] }
                    val subcategoryId = record.subcategorySyncId?.let { subcategorySyncToLocal[it] }
                    val relatedAccountId = record.relatedAccountSyncId?.let { accountSyncToLocal[it] }
                    val loanId = record.loanSyncId?.let { loanSyncToLocal[it] }
                    val existing = transactionDao.getBySyncId(record.syncId)
                    if (existing == null) {
                        val id =
                            transactionDao.insert(
                                record.toEntity(
                                    accountId = accountId,
                                    categoryId = categoryId,
                                    subcategoryId = subcategoryId,
                                    relatedAccountId = relatedAccountId,
                                    loanId = loanId,
                                    payload = payload,
                                ),
                            )
                        transactionSyncToLocal[record.syncId] = id
                        transactionsInserted++
                        tombstoneMerger.clearIfSuperseded(record.syncId, record.updatedAt)
                    } else if (record.updatedAt > existing.updatedAt) {
                        transactionDao.update(
                            record.toEntity(
                                accountId = accountId,
                                categoryId = categoryId,
                                subcategoryId = subcategoryId,
                                relatedAccountId = relatedAccountId,
                                loanId = loanId,
                                existingId = existing.id,
                                payload = payload,
                            ),
                        )
                        transactionSyncToLocal[record.syncId] = existing.id
                        transactionsUpdated++
                        tombstoneMerger.clearIfSuperseded(record.syncId, record.updatedAt)
                    } else {
                        transactionSyncToLocal[record.syncId] = existing.id
                    }
                }

                val attachmentTxnIds = mutableSetOf<Long>()
                for (record in payload.attachments) {
                    val txnId = transactionSyncToLocal[record.transactionSyncId] ?: continue
                    mergeAttachmentMetadata(attachmentDao, record, txnId)
                    attachmentTxnIds.add(txnId)
                }
                for (txnId in attachmentTxnIds) {
                    val txn = transactionDao.getById(txnId) ?: continue
                    val count = attachmentDao.getByTransactionId(txnId).size
                    if (txn.attachmentCount != count) {
                        transactionDao.update(txn.copy(attachmentCount = count))
                    }
                }

                val paymentSyncToLocal = mutableMapOf<String, Long>()
                for (record in payload.loanPayments) {
                    val loanId = loanSyncToLocal[record.loanSyncId] ?: continue
                    val existing = loanPaymentDao.getBySyncId(record.syncId)
                    if (existing != null) {
                        paymentSyncToLocal[record.syncId] = existing.id
                        continue
                    }
                    val txnId = record.transactionSyncId?.let { transactionSyncToLocal[it] }
                    val id =
                        loanPaymentDao.insert(
                            record.toEntity(loanId = loanId, transactionId = txnId),
                        )
                    paymentSyncToLocal[record.syncId] = id
                }

                val revisions =
                    payload.loanInterestRevisions.mapNotNull { record ->
                        val loanId = loanSyncToLocal[record.loanSyncId] ?: return@mapNotNull null
                        if (loanInterestRevisionDao.getBySyncId(record.syncId) != null) return@mapNotNull null
                        record.toEntity(loanId)
                    }
                if (revisions.isNotEmpty()) {
                    backupDao.insertLoanInterestRevisions(revisions)
                }

                val scheduleEntries =
                    payload.loanScheduleEntries.mapNotNull { record ->
                        val loanId = loanSyncToLocal[record.loanSyncId] ?: return@mapNotNull null
                        if (loanScheduleDao.getBySyncId(record.syncId) != null) return@mapNotNull null
                        val paymentId = record.linkedPaymentSyncId?.let { paymentSyncToLocal[it] }
                        record.toEntity(loanId = loanId, linkedPaymentId = paymentId)
                    }
                if (scheduleEntries.isNotEmpty()) {
                    backupDao.insertLoanScheduleEntries(scheduleEntries)
                }

                val recurringSyncToLocal = mutableMapOf<String, Long>()
                for (record in payload.recurringRules) {
                    if (tombstoneMerger.isDeletedForUpsert(record.syncId, record.updatedAt)) continue
                    val accountId = accountSyncToLocal[record.accountSyncId] ?: continue
                    val categoryId = record.categorySyncId?.let { categorySyncToLocal[it] }
                    val subcategoryId = record.subcategorySyncId?.let { subcategorySyncToLocal[it] }
                    val relatedAccountId =
                        record.relatedAccountSyncId?.let { accountSyncToLocal[it] }
                    val loanId = record.loanSyncId?.let { loanSyncToLocal[it] }
                    val existing = recurringRuleDao.getBySyncId(record.syncId)
                    if (existing == null) {
                        val id =
                            recurringRuleDao.insert(
                                record.toEntity(
                                    accountId = accountId,
                                    categoryId = categoryId,
                                    subcategoryId = subcategoryId,
                                    relatedAccountId = relatedAccountId,
                                    loanId = loanId,
                                ),
                            )
                        recurringSyncToLocal[record.syncId] = id
                        tombstoneMerger.clearIfSuperseded(record.syncId, record.updatedAt)
                    } else {
                        val resolution = options.recurringBySyncId[record.syncId]
                        when (resolution) {
                            LanSyncRecurringResolution.USE_LOCAL -> {
                                recurringSyncToLocal[record.syncId] = existing.id
                            }
                            LanSyncRecurringResolution.USE_REMOTE,
                            null,
                            -> {
                                if (record.updatedAt > existing.updatedAt) {
                                    recurringRuleDao.update(
                                        record.toEntity(
                                            accountId = accountId,
                                            categoryId = categoryId,
                                            subcategoryId = subcategoryId,
                                            relatedAccountId = relatedAccountId,
                                            loanId = loanId,
                                            existingId = existing.id,
                                        ),
                                    )
                                    tombstoneMerger.clearIfSuperseded(record.syncId, record.updatedAt)
                                }
                                recurringSyncToLocal[record.syncId] = existing.id
                            }
                        }
                    }
                }

                for (record in payload.categorizationRules) {
                    if (tombstoneMerger.isDeletedForUpsert(record.syncId, record.updatedAt)) continue
                    val existing = categorizationRuleDao.getBySyncId(record.syncId)
                    if (existing == null) {
                        categorizationRuleDao.upsert(record.toEntity())
                        tombstoneMerger.clearIfSuperseded(record.syncId, record.updatedAt)
                    } else if (record.updatedAt > existing.updatedAt) {
                        categorizationRuleDao.upsert(record.toEntity(existing.id))
                        tombstoneMerger.clearIfSuperseded(record.syncId, record.updatedAt)
                    }
                }

                for (record in payload.budgets) {
                    if (tombstoneMerger.isDeletedForUpsert(record.syncId, record.updatedAt)) continue
                    val categoryId =
                        categorySyncToLocal[record.categorySyncId]
                            ?: categoryDao.getCategoryBySyncId(record.categorySyncId)?.id
                            ?: continue
                    val subcategoryId =
                        record.subcategorySyncId?.let { syncId ->
                            subcategorySyncToLocal[syncId] ?: categoryDao.getSubcategoryBySyncId(syncId)?.id
                        }
                    val existing = budgetDao.getBySyncId(record.syncId)
                    if (existing == null) {
                        budgetDao.insert(record.toEntity(categoryId, subcategoryId))
                        tombstoneMerger.clearIfSuperseded(record.syncId, record.updatedAt)
                    } else if (record.updatedAt > existing.updatedAt) {
                        budgetDao.update(record.toEntity(categoryId, subcategoryId, existing.id))
                        tombstoneMerger.clearIfSuperseded(record.syncId, record.updatedAt)
                    }
                }

                val assetSyncToLocal = mutableMapOf<String, Long>()
                for (record in payload.assets) {
                    if (tombstoneMerger.isDeletedForUpsert(record.syncId, record.updatedAt)) continue
                    val linkedLoanId = record.linkedLoanSyncId?.let { loanSyncToLocal[it] }
                    val existing = assetDao.getBySyncId(record.syncId)
                    if (existing == null) {
                        val id = assetDao.insert(record.toEntity(linkedLoanId))
                        assetSyncToLocal[record.syncId] = id
                        tombstoneMerger.clearIfSuperseded(record.syncId, record.updatedAt)
                    } else if (record.updatedAt > existing.updatedAt) {
                        assetDao.update(record.toEntity(linkedLoanId, existing.id))
                        assetSyncToLocal[record.syncId] = existing.id
                        tombstoneMerger.clearIfSuperseded(record.syncId, record.updatedAt)
                    } else {
                        assetSyncToLocal[record.syncId] = existing.id
                    }
                }

                for (record in payload.assetValuations) {
                    if (tombstoneMerger.isDeletedForUpsert(record.syncId, record.recordedAt)) continue
                    val assetId =
                        assetSyncToLocal[record.assetSyncId]
                            ?: assetDao.getBySyncId(record.assetSyncId)?.id
                            ?: continue
                    val existing = assetValuationDao.getBySyncId(record.syncId)
                    if (existing == null) {
                        assetValuationDao.insert(record.toEntity(assetId))
                        tombstoneMerger.clearIfSuperseded(record.syncId, record.recordedAt)
                    } else if (record.recordedAt > existing.recordedAt) {
                        assetValuationDao.delete(existing)
                        assetValuationDao.insert(record.toEntity(assetId))
                        tombstoneMerger.clearIfSuperseded(record.syncId, record.recordedAt)
                    }
                }

                for (record in payload.assetAttachments) {
                    val assetId = assetSyncToLocal[record.assetSyncId] ?: continue
                    mergeAssetAttachmentMetadata(assetAttachmentDao, record, assetId)
                }

                for (record in payload.notificationRules) {
                    if (tombstoneMerger.isDeletedForUpsert(record.syncId, record.createdAt)) continue
                    if (notificationRuleDao.getBySyncId(record.syncId) != null) continue
                    val loanId = record.targetLoanSyncId?.let { loanSyncToLocal[it] }
                    val recurringId =
                        record.targetRecurringRuleSyncId?.let { syncId ->
                            recurringSyncToLocal[syncId] ?: recurringRuleDao.getBySyncId(syncId)?.id
                        }
                    val assetId =
                        record.targetAssetSyncId?.let { syncId ->
                            assetSyncToLocal[syncId] ?: assetDao.getBySyncId(syncId)?.id
                        }
                    notificationRuleDao.insert(
                        record.toEntity(
                            loanId = loanId,
                            recurringRuleId = recurringId,
                            assetId = assetId,
                        ),
                    )
                }
            }

            val balanceWarnings = balanceValidator.warningsAfterMerge()
            val tombWarnings =
                buildList {
                    if (tombstoneStats.applied > 0) {
                        add("Applied ${tombstoneStats.applied} deletion(s) from peer.")
                    }
                    if (tombstoneStats.keptAfterLocalEdit > 0) {
                        add(
                            "${tombstoneStats.keptAfterLocalEdit} item(s) kept because you edited after peer deleted.",
                        )
                    }
                }
            val summary =
                LanSyncMergeSummary(
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
                    tombstonesApplied = tombstoneStats.applied,
                    keptAfterRemoteDelete = tombstoneStats.keptAfterLocalEdit,
                    warnings = (tombWarnings + balanceWarnings).distinct(),
                )
            widgetRefreshTrigger.requestRefresh()
            return summary
        }

        private fun LanCategoryRecord.toEntity(existingId: Long = 0L) =
            CategoryEntity(
                id = existingId,
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
            emiReminderDaysBefore = emiReminderDaysBefore,
            fixedEmiInputMode = fixedEmiInputMode,
            notes = notes,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )

        private fun LanLoanPaymentRecord.toEntity(
            loanId: Long,
            transactionId: Long?,
        ) = LoanPaymentEntity(
            id = 0L,
            syncId = MmrSyncId.normalize(syncId),
            loanId = loanId,
            transactionId = transactionId,
            paymentAmount = paymentAmount,
            paymentDate = paymentDate,
            paymentType = LoanPaymentType.valueOf(paymentType),
            principalComponent = principalComponent,
            interestComponent = interestComponent,
            outstandingAfterPayment = outstandingAfterPayment,
            notes = notes,
        )

        private fun LanLoanInterestRevisionRecord.toEntity(loanId: Long) =
            LoanInterestRevisionEntity(
                id = 0L,
                syncId = MmrSyncId.normalize(syncId),
                loanId = loanId,
                effectiveFromMillis = effectiveFromMillis,
                annualRatePercent = annualRatePercent,
                note = note,
                createdAt = createdAt,
            )

        private fun LanLoanScheduleEntryRecord.toEntity(
            loanId: Long,
            linkedPaymentId: Long?,
        ) = LoanScheduleEntryEntity(
            id = 0L,
            syncId = MmrSyncId.normalize(syncId),
            loanId = loanId,
            sequenceNumber = sequenceNumber,
            dueDateMillis = dueDateMillis,
            emiAmount = emiAmount,
            principalPlanned = principalPlanned,
            interestPlanned = interestPlanned,
            status = LoanScheduleStatus.valueOf(status),
            linkedPaymentId = linkedPaymentId,
            annualRatePercentAtPlan = annualRatePercentAtPlan,
        )

        private fun LanRecurringRuleRecord.toEntity(
            accountId: Long,
            categoryId: Long?,
            subcategoryId: Long?,
            relatedAccountId: Long? = null,
            loanId: Long? = null,
            existingId: Long = 0L,
        ) = RecurringRuleEntity(
            id = existingId,
            syncId = MmrSyncId.normalize(syncId),
            name = name,
            transactionTemplateType = TransactionType.valueOf(transactionTemplateType),
            amount = amount,
            categoryId = categoryId,
            subcategoryId = subcategoryId,
            accountId = accountId,
            relatedAccountId = relatedAccountId,
            loanId = loanId,
            dayOfMonth = dayOfMonth,
            recurrenceInterval = RecurrenceInterval.valueOf(recurrenceInterval),
            nextRunDate = nextRunDate,
            isEnabled = isEnabled,
            lastGeneratedDate = lastGeneratedDate,
            endDate = endDate,
            noteTemplate = noteTemplate,
            reminderMinutesBefore = reminderMinutesBefore,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )

        private fun LanBudgetRecord.toEntity(
            categoryId: Long,
            subcategoryId: Long?,
            existingId: Long = 0L,
        ) = BudgetEntity(
            id = existingId,
            syncId = MmrSyncId.normalize(syncId),
            categoryId = categoryId,
            subcategoryId = subcategoryId,
            amountLimit = amountLimit,
            periodType = BudgetPeriodType.valueOf(periodType),
            customPeriodStartMillis = customPeriodStartMillis,
            customPeriodEndMillis = customPeriodEndMillis,
            alertOnExceeded = alertOnExceeded,
            lastAlertPeriodKey = lastAlertPeriodKey,
            isActive = isActive,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )

        private fun LanCategorizationRuleRecord.toEntity(existingId: Long = 0L) =
            CategorizationRuleEntity(
                id = existingId,
                syncId = MmrSyncId.normalize(syncId),
                priority = priority,
                matchType = CategorizationMatchType.valueOf(matchType),
                pattern = pattern,
                categoryName = categoryName,
                subcategoryName = subcategoryName,
                isEnabled = isEnabled,
                createdAt = createdAt,
                updatedAt = updatedAt,
            )

        private fun LanAssetRecord.toEntity(
            linkedLoanId: Long?,
            existingId: Long = 0L,
        ) = AssetEntity(
            id = existingId,
            syncId = MmrSyncId.normalize(syncId),
            name = name,
            assetType = assetType,
            status =
                com.mmr.core.model.enums
                    .assetStatusFromStored(status),
            isInherited = isInherited,
            purchaseDateMillis = purchaseDateMillis,
            purchasePrice = purchasePrice,
            currentValue = currentValue,
            ownedByName = ownedByName,
            location = location,
            identifier = identifier,
            linkedLoanId = linkedLoanId,
            nextValuationDueMillis = nextValuationDueMillis,
            valuationReminderDaysBefore = valuationReminderDaysBefore,
            saleDateMillis = saleDateMillis,
            salePrice = salePrice,
            notes = notes,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )

        private fun LanAssetValuationRecord.toEntity(assetId: Long) =
            AssetValuationEntity(
                syncId = MmrSyncId.normalize(syncId),
                assetId = assetId,
                amount = amount,
                recordedAt = recordedAt,
                notes = notes,
                createdAt = createdAt,
            )

        private fun LanNotificationRuleRecord.toEntity(
            loanId: Long?,
            recurringRuleId: Long?,
            assetId: Long?,
        ) = NotificationRuleEntity(
            id = 0L,
            syncId = MmrSyncId.normalize(syncId),
            type = NotificationRuleType.valueOf(type),
            enabled = enabled,
            scheduleTime = scheduleTime,
            targetLoanId = loanId,
            targetRecurringRuleId = recurringRuleId,
            targetAssetId = assetId,
            thresholdValue = thresholdValue,
            title = title,
            message = message,
            isFired = isFired,
            createdAt = createdAt,
        )

        private suspend fun mergeAssetAttachmentMetadata(
            attachmentDao: com.mmr.core.database.dao.AssetAttachmentDao,
            record: LanAssetAttachmentRecord,
            assetId: Long,
        ) {
            val syncId = MmrSyncId.normalize(record.syncId)
            val existing = attachmentDao.getBySyncId(syncId)
            if (existing == null) {
                attachmentDao.insert(
                    AssetAttachmentEntity(
                        syncId = syncId,
                        assetId = assetId,
                        mimeType = record.mimeType,
                        fileSizeBytes = record.fileSizeBytes,
                        createdAt = record.createdAt,
                        updatedAt = record.updatedAt,
                    ),
                )
            } else if (record.updatedAt > existing.updatedAt) {
                attachmentDao.update(
                    existing.copy(
                        assetId = assetId,
                        mimeType = record.mimeType,
                        fileSizeBytes = record.fileSizeBytes,
                        updatedAt = record.updatedAt,
                    ),
                )
            }
        }

        private suspend fun mergeAttachmentMetadata(
            attachmentDao: com.mmr.core.database.dao.TransactionAttachmentDao,
            record: LanAttachmentRecord,
            transactionId: Long,
        ) {
            val syncId = MmrSyncId.normalize(record.syncId)
            val existing = attachmentDao.getBySyncId(syncId)
            if (existing == null) {
                attachmentDao.insert(
                    TransactionAttachmentEntity(
                        syncId = syncId,
                        transactionId = transactionId,
                        mimeType = record.mimeType,
                        fileSizeBytes = record.fileSizeBytes,
                        createdAt = record.createdAt,
                        updatedAt = record.updatedAt,
                    ),
                )
            } else if (record.updatedAt > existing.updatedAt) {
                attachmentDao.update(
                    existing.copy(
                        transactionId = transactionId,
                        mimeType = record.mimeType,
                        fileSizeBytes = record.fileSizeBytes,
                        updatedAt = record.updatedAt,
                    ),
                )
            }
        }

        private fun LanTransactionRecord.toEntity(
            accountId: Long,
            categoryId: Long?,
            subcategoryId: Long?,
            relatedAccountId: Long?,
            loanId: Long?,
            existingId: Long = 0L,
            payload: MmrLanSyncPayload,
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
            syncedFromDeviceId = syncedFromDeviceId ?: payload.sourceDeviceId,
            syncedFromDeviceName = syncedFromDeviceName ?: payload.sourceDeviceName,
        )
    }
