package com.rrparallelstudio.miapp.core.lansync

import com.rrparallelstudio.miapp.core.database.MmrDatabase
import com.rrparallelstudio.miapp.core.database.dao.SyncTombstoneDao
import com.rrparallelstudio.miapp.core.database.entity.SyncTombstoneEntity
import com.rrparallelstudio.miapp.core.domain.repository.AssetAttachmentRepository
import com.rrparallelstudio.miapp.core.domain.repository.TransactionAttachmentRepository
import com.rrparallelstudio.miapp.core.lansync.model.LanSyncTombstoneRecord
import javax.inject.Inject
import javax.inject.Singleton

data class LanSyncTombstoneMergeStats(
    val applied: Int = 0,
    val keptAfterLocalEdit: Int = 0,
)

@Singleton
class LanSyncTombstoneMerger
    @Inject
    constructor(
        private val database: MmrDatabase,
        private val tombstoneDao: SyncTombstoneDao,
        private val attachmentRepository: TransactionAttachmentRepository,
        private val assetAttachmentRepository: AssetAttachmentRepository,
    ) {
        suspend fun ingestRemoteTombstones(
            remote: List<LanSyncTombstoneRecord>,
            journal: LanSyncInboundJournal = LanSyncInboundJournal(),
        ): LanSyncTombstoneMergeStats {
            if (remote.isEmpty()) return LanSyncTombstoneMergeStats()
            var applied = 0
            var keptAfterLocalEdit = 0
            for (record in remote) {
                val merged = mergeTombstoneRow(record)
                val outcome = applyTombstone(merged, journal)
                applied += outcome.applied
                keptAfterLocalEdit += outcome.keptAfterLocalEdit
            }
            return LanSyncTombstoneMergeStats(applied = applied, keptAfterLocalEdit = keptAfterLocalEdit)
        }

        suspend fun deletedAtFor(syncId: String): Long? = tombstoneDao.getBySyncId(syncId)?.deletedAt

        suspend fun clearIfSuperseded(
            syncId: String,
            updatedAt: Long,
        ) {
            val tomb = tombstoneDao.getBySyncId(syncId) ?: return
            if (updatedAt > tomb.deletedAt) {
                tombstoneDao.deleteBySyncId(syncId)
            }
        }

        suspend fun clearTombstone(syncId: String) {
            tombstoneDao.deleteBySyncId(syncId)
        }

        suspend fun isDeletedForUpsert(
            syncId: String,
            updatedAt: Long,
        ): Boolean {
            val tomb = tombstoneDao.getBySyncId(syncId) ?: return false
            return tomb.deletedAt >= updatedAt
        }

        private suspend fun mergeTombstoneRow(record: LanSyncTombstoneRecord): SyncTombstoneEntity {
            val existing = tombstoneDao.getBySyncId(record.syncId)
            val merged =
                if (existing == null || record.deletedAt >= existing.deletedAt) {
                    SyncTombstoneEntity(
                        syncId = record.syncId,
                        entityType = record.entityType,
                        deletedAt = record.deletedAt,
                        sourceDeviceId = record.sourceDeviceId,
                    )
                } else {
                    existing
                }
            if (existing == null || merged.deletedAt > existing.deletedAt) {
                tombstoneDao.upsert(merged)
            }
            return merged
        }

        private suspend fun applyTombstone(
            tombstone: SyncTombstoneEntity,
            journal: LanSyncInboundJournal,
        ): LanSyncTombstoneMergeStats =
            when (tombstone.entityType) {
                SyncEntityType.TRANSACTION -> applyTransactionTombstone(tombstone, journal)
                SyncEntityType.RECURRING_RULE -> applyRecurringTombstone(tombstone, journal)
                SyncEntityType.LOAN -> applyLoanTombstone(tombstone, journal)
                SyncEntityType.LOAN_PAYMENT -> applyLoanPaymentTombstone(tombstone, journal)
                SyncEntityType.ACCOUNT -> applyAccountTombstone(tombstone, journal)
                SyncEntityType.CATEGORY -> applyCategoryTombstone(tombstone, journal)
                SyncEntityType.SUBCATEGORY -> applySubcategoryTombstone(tombstone, journal)
                SyncEntityType.ATTACHMENT -> applyAttachmentTombstone(tombstone)
                SyncEntityType.CATEGORIZATION_RULE -> applyCategorizationRuleTombstone(tombstone)
                SyncEntityType.NOTIFICATION_RULE -> applyNotificationRuleTombstone(tombstone)
                SyncEntityType.BUDGET -> applyBudgetTombstone(tombstone, journal)
                SyncEntityType.ASSET -> applyAssetTombstone(tombstone, journal)
                SyncEntityType.ASSET_VALUATION -> applyAssetValuationTombstone(tombstone, journal)
                SyncEntityType.ASSET_ATTACHMENT -> applyAssetAttachmentTombstone(tombstone)
                else -> LanSyncTombstoneMergeStats()
            }

        private suspend fun applyTransactionTombstone(
            tombstone: SyncTombstoneEntity,
            journal: LanSyncInboundJournal,
        ): LanSyncTombstoneMergeStats {
            val txn = database.transactionDao().getBySyncId(tombstone.syncId) ?: return LanSyncTombstoneMergeStats()
            if (txn.updatedAt > tombstone.deletedAt) {
                return LanSyncTombstoneMergeStats(keptAfterLocalEdit = 1)
            }
            journal.replaced(
                kind = "Transaction",
                title = txn.note?.takeIf { it.isNotBlank() } ?: txn.type.name,
                yours = LanSyncOverrideCopy.transaction(txn.amount, txn.type.name, txn.note),
                theirs = LanSyncOverrideCopy.DELETED_ON_OTHER_PHONE,
            )
            val payment = database.loanPaymentDao().getByTransactionId(txn.id)
            if (payment != null) {
                database.loanPaymentDao().clearTransactionLink(txn.id)
            }
            attachmentRepository.deleteAllForTransaction(txn.id)
            database.transactionDao().delete(txn)
            return LanSyncTombstoneMergeStats(applied = 1)
        }

        private suspend fun applyRecurringTombstone(
            tombstone: SyncTombstoneEntity,
            journal: LanSyncInboundJournal,
        ): LanSyncTombstoneMergeStats {
            val rule = database.recurringRuleDao().getBySyncId(tombstone.syncId) ?: return LanSyncTombstoneMergeStats()
            if (rule.updatedAt > tombstone.deletedAt) {
                return LanSyncTombstoneMergeStats(keptAfterLocalEdit = 1)
            }
            journal.replaced(
                kind = "Recurring",
                title = rule.name,
                yours = LanSyncOverrideCopy.recurring(rule.amount, rule.isEnabled, rule.noteTemplate),
                theirs = LanSyncOverrideCopy.DELETED_ON_OTHER_PHONE,
            )
            database.notificationRuleDao().deleteForRecurringRule(rule.id)
            database.recurringRuleDao().delete(rule)
            return LanSyncTombstoneMergeStats(applied = 1)
        }

        private suspend fun applyLoanTombstone(
            tombstone: SyncTombstoneEntity,
            journal: LanSyncInboundJournal,
        ): LanSyncTombstoneMergeStats {
            val loan = database.loanDao().getBySyncId(tombstone.syncId) ?: return LanSyncTombstoneMergeStats()
            if (loan.updatedAt > tombstone.deletedAt) {
                return LanSyncTombstoneMergeStats(keptAfterLocalEdit = 1)
            }
            journal.replaced(
                kind = "Loan",
                title = loan.name,
                yours = LanSyncOverrideCopy.loan(loan.outstandingAmount, loan.interestRate, loan.status.name),
                theirs = LanSyncOverrideCopy.DELETED_ON_OTHER_PHONE,
            )
            database.loanScheduleDao().deleteAllForLoan(loan.id)
            database.loanPaymentDao().deletePaymentsForLoan(loan.id)
            database.loanDao().delete(loan)
            return LanSyncTombstoneMergeStats(applied = 1)
        }

        private suspend fun applyLoanPaymentTombstone(
            tombstone: SyncTombstoneEntity,
            journal: LanSyncInboundJournal,
        ): LanSyncTombstoneMergeStats {
            val payment = database.loanPaymentDao().getBySyncId(tombstone.syncId) ?: return LanSyncTombstoneMergeStats()
            if (payment.paymentDate > tombstone.deletedAt) {
                return LanSyncTombstoneMergeStats(keptAfterLocalEdit = 1)
            }
            journal.replaced(
                kind = "Loan payment",
                title = LanSyncOverrideCopy.money(payment.paymentAmount),
                yours = LanSyncOverrideCopy.money(payment.paymentAmount),
                theirs = LanSyncOverrideCopy.DELETED_ON_OTHER_PHONE,
            )
            database.loanPaymentDao().delete(payment)
            return LanSyncTombstoneMergeStats(applied = 1)
        }

        private suspend fun applyAccountTombstone(
            tombstone: SyncTombstoneEntity,
            journal: LanSyncInboundJournal,
        ): LanSyncTombstoneMergeStats {
            val account = database.accountDao().getBySyncId(tombstone.syncId) ?: return LanSyncTombstoneMergeStats()
            if (account.updatedAt > tombstone.deletedAt) {
                return LanSyncTombstoneMergeStats(keptAfterLocalEdit = 1)
            }
            if (!account.isActive) return LanSyncTombstoneMergeStats()
            journal.replaced(
                kind = "Account",
                title = account.name,
                yours = LanSyncOverrideCopy.account(account.currentBalance, active = true),
                theirs = "Marked inactive on the other phone",
            )
            database.accountDao().update(
                account.copy(
                    isActive = false,
                    updatedAt = System.currentTimeMillis(),
                ),
            )
            return LanSyncTombstoneMergeStats(applied = 1)
        }

        private suspend fun applyCategoryTombstone(
            tombstone: SyncTombstoneEntity,
            journal: LanSyncInboundJournal,
        ): LanSyncTombstoneMergeStats {
            val category =
                database.categoryDao().getCategoryBySyncId(tombstone.syncId) ?: return LanSyncTombstoneMergeStats()
            if (!category.isActive) return LanSyncTombstoneMergeStats()
            journal.replaced(
                kind = "Category",
                title = category.name,
                yours = category.name,
                theirs = "Hidden on the other phone",
            )
            database.categoryDao().update(category.copy(isActive = false))
            return LanSyncTombstoneMergeStats(applied = 1)
        }

        private suspend fun applySubcategoryTombstone(
            tombstone: SyncTombstoneEntity,
            journal: LanSyncInboundJournal,
        ): LanSyncTombstoneMergeStats {
            val sub =
                database.categoryDao().getSubcategoryBySyncId(tombstone.syncId) ?: return LanSyncTombstoneMergeStats()
            if (!sub.isActive) return LanSyncTombstoneMergeStats()
            journal.replaced(
                kind = "Subcategory",
                title = sub.name,
                yours = sub.name,
                theirs = "Hidden on the other phone",
            )
            database.transactionDao().detachSubcategory(sub.id)
            database.recurringRuleDao().detachSubcategory(sub.id)
            database.categoryDao().updateSubcategory(sub.copy(isActive = false))
            return LanSyncTombstoneMergeStats(applied = 1)
        }

        private suspend fun applyCategorizationRuleTombstone(
            tombstone: SyncTombstoneEntity,
        ): LanSyncTombstoneMergeStats {
            val rule =
                database.categorizationRuleDao().getBySyncId(tombstone.syncId) ?: return LanSyncTombstoneMergeStats()
            if (rule.updatedAt > tombstone.deletedAt) {
                return LanSyncTombstoneMergeStats(keptAfterLocalEdit = 1)
            }
            database.categorizationRuleDao().deleteBySyncId(tombstone.syncId)
            return LanSyncTombstoneMergeStats(applied = 1)
        }

        private suspend fun applyBudgetTombstone(
            tombstone: SyncTombstoneEntity,
            journal: LanSyncInboundJournal,
        ): LanSyncTombstoneMergeStats {
            val budget =
                database.budgetDao().getBySyncId(tombstone.syncId) ?: return LanSyncTombstoneMergeStats()
            if (budget.updatedAt > tombstone.deletedAt) {
                return LanSyncTombstoneMergeStats(keptAfterLocalEdit = 1)
            }
            journal.replaced(
                kind = "Budget",
                title = "Budget",
                yours = LanSyncOverrideCopy.budget(budget.amountLimit, budget.isActive),
                theirs = LanSyncOverrideCopy.DELETED_ON_OTHER_PHONE,
            )
            database.budgetDao().deleteBySyncId(tombstone.syncId)
            return LanSyncTombstoneMergeStats(applied = 1)
        }

        private suspend fun applyNotificationRuleTombstone(tombstone: SyncTombstoneEntity): LanSyncTombstoneMergeStats {
            val rule =
                database.notificationRuleDao().getBySyncId(tombstone.syncId) ?: return LanSyncTombstoneMergeStats()
            if (rule.createdAt > tombstone.deletedAt) {
                return LanSyncTombstoneMergeStats(keptAfterLocalEdit = 1)
            }
            database.notificationRuleDao().delete(rule)
            return LanSyncTombstoneMergeStats(applied = 1)
        }

        private suspend fun applyAttachmentTombstone(tombstone: SyncTombstoneEntity): LanSyncTombstoneMergeStats {
            val attachment =
                database.transactionAttachmentDao().getBySyncId(tombstone.syncId) ?: return LanSyncTombstoneMergeStats()
            if (attachment.updatedAt > tombstone.deletedAt) {
                return LanSyncTombstoneMergeStats(keptAfterLocalEdit = 1)
            }
            attachmentRepository.delete(attachment.id)
            return LanSyncTombstoneMergeStats(applied = 1)
        }

        private suspend fun applyAssetTombstone(
            tombstone: SyncTombstoneEntity,
            journal: LanSyncInboundJournal,
        ): LanSyncTombstoneMergeStats {
            val asset = database.assetDao().getBySyncId(tombstone.syncId) ?: return LanSyncTombstoneMergeStats()
            if (asset.updatedAt > tombstone.deletedAt) {
                return LanSyncTombstoneMergeStats(keptAfterLocalEdit = 1)
            }
            journal.replaced(
                kind = "Asset",
                title = asset.name,
                yours = LanSyncOverrideCopy.asset(asset.currentValue, asset.status.name),
                theirs = LanSyncOverrideCopy.DELETED_ON_OTHER_PHONE,
            )
            database.notificationRuleDao().deleteForAsset(asset.id)
            database.assetAttachmentDao().getByAssetId(asset.id).forEach { attachment ->
                assetAttachmentRepository.delete(attachment.id)
            }
            database.assetDao().delete(asset)
            return LanSyncTombstoneMergeStats(applied = 1)
        }

        private suspend fun applyAssetValuationTombstone(
            tombstone: SyncTombstoneEntity,
            journal: LanSyncInboundJournal,
        ): LanSyncTombstoneMergeStats {
            val valuation =
                database.assetValuationDao().getBySyncId(tombstone.syncId) ?: return LanSyncTombstoneMergeStats()
            if (valuation.recordedAt > tombstone.deletedAt) {
                return LanSyncTombstoneMergeStats(keptAfterLocalEdit = 1)
            }
            journal.replaced(
                kind = "Asset value",
                title = LanSyncOverrideCopy.money(valuation.amount),
                yours = LanSyncOverrideCopy.money(valuation.amount),
                theirs = LanSyncOverrideCopy.DELETED_ON_OTHER_PHONE,
            )
            database.assetValuationDao().delete(valuation)
            return LanSyncTombstoneMergeStats(applied = 1)
        }

        private suspend fun applyAssetAttachmentTombstone(tombstone: SyncTombstoneEntity): LanSyncTombstoneMergeStats {
            val attachment =
                database.assetAttachmentDao().getBySyncId(tombstone.syncId) ?: return LanSyncTombstoneMergeStats()
            if (attachment.updatedAt > tombstone.deletedAt) {
                return LanSyncTombstoneMergeStats(keptAfterLocalEdit = 1)
            }
            assetAttachmentRepository.delete(attachment.id)
            return LanSyncTombstoneMergeStats(applied = 1)
        }
    }
