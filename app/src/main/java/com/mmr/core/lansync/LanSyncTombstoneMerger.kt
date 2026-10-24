package com.mmr.core.lansync

import com.mmr.core.database.MmrDatabase
import com.mmr.core.database.dao.SyncTombstoneDao
import com.mmr.core.database.entity.SyncTombstoneEntity
import com.mmr.core.domain.repository.TransactionAttachmentRepository
import com.mmr.core.lansync.model.LanSyncTombstoneRecord
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
    ) {
        suspend fun ingestRemoteTombstones(remote: List<LanSyncTombstoneRecord>): LanSyncTombstoneMergeStats {
            if (remote.isEmpty()) return LanSyncTombstoneMergeStats()
            var applied = 0
            var keptAfterLocalEdit = 0
            for (record in remote) {
                val merged = mergeTombstoneRow(record)
                val outcome = applyTombstone(merged)
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

        private suspend fun applyTombstone(tombstone: SyncTombstoneEntity): LanSyncTombstoneMergeStats =
            when (tombstone.entityType) {
                SyncEntityType.TRANSACTION -> applyTransactionTombstone(tombstone)
                SyncEntityType.RECURRING_RULE -> applyRecurringTombstone(tombstone)
                SyncEntityType.LOAN -> applyLoanTombstone(tombstone)
                SyncEntityType.LOAN_PAYMENT -> applyLoanPaymentTombstone(tombstone)
                SyncEntityType.ACCOUNT -> applyAccountTombstone(tombstone)
                SyncEntityType.CATEGORY -> applyCategoryTombstone(tombstone)
                SyncEntityType.SUBCATEGORY -> applySubcategoryTombstone(tombstone)
                SyncEntityType.ATTACHMENT -> applyAttachmentTombstone(tombstone)
                SyncEntityType.CATEGORIZATION_RULE -> applyCategorizationRuleTombstone(tombstone)
                SyncEntityType.NOTIFICATION_RULE -> applyNotificationRuleTombstone(tombstone)
                else -> LanSyncTombstoneMergeStats()
            }

        private suspend fun applyTransactionTombstone(tombstone: SyncTombstoneEntity): LanSyncTombstoneMergeStats {
            val txn = database.transactionDao().getBySyncId(tombstone.syncId) ?: return LanSyncTombstoneMergeStats()
            if (txn.updatedAt > tombstone.deletedAt) {
                return LanSyncTombstoneMergeStats(keptAfterLocalEdit = 1)
            }
            val payment = database.loanPaymentDao().getByTransactionId(txn.id)
            if (payment != null) {
                database.loanPaymentDao().clearTransactionLink(txn.id)
            }
            attachmentRepository.deleteAllForTransaction(txn.id)
            database.transactionDao().delete(txn)
            return LanSyncTombstoneMergeStats(applied = 1)
        }

        private suspend fun applyRecurringTombstone(tombstone: SyncTombstoneEntity): LanSyncTombstoneMergeStats {
            val rule = database.recurringRuleDao().getBySyncId(tombstone.syncId) ?: return LanSyncTombstoneMergeStats()
            if (rule.updatedAt > tombstone.deletedAt) {
                return LanSyncTombstoneMergeStats(keptAfterLocalEdit = 1)
            }
            database.notificationRuleDao().deleteForRecurringRule(rule.id)
            database.recurringRuleDao().delete(rule)
            return LanSyncTombstoneMergeStats(applied = 1)
        }

        private suspend fun applyLoanTombstone(tombstone: SyncTombstoneEntity): LanSyncTombstoneMergeStats {
            val loan = database.loanDao().getBySyncId(tombstone.syncId) ?: return LanSyncTombstoneMergeStats()
            if (loan.updatedAt > tombstone.deletedAt) {
                return LanSyncTombstoneMergeStats(keptAfterLocalEdit = 1)
            }
            database.loanScheduleDao().deleteAllForLoan(loan.id)
            database.loanPaymentDao().deletePaymentsForLoan(loan.id)
            database.loanDao().delete(loan)
            return LanSyncTombstoneMergeStats(applied = 1)
        }

        private suspend fun applyLoanPaymentTombstone(tombstone: SyncTombstoneEntity): LanSyncTombstoneMergeStats {
            val payment = database.loanPaymentDao().getBySyncId(tombstone.syncId) ?: return LanSyncTombstoneMergeStats()
            if (payment.paymentDate > tombstone.deletedAt) {
                return LanSyncTombstoneMergeStats(keptAfterLocalEdit = 1)
            }
            database.loanPaymentDao().delete(payment)
            return LanSyncTombstoneMergeStats(applied = 1)
        }

        private suspend fun applyAccountTombstone(tombstone: SyncTombstoneEntity): LanSyncTombstoneMergeStats {
            val account = database.accountDao().getBySyncId(tombstone.syncId) ?: return LanSyncTombstoneMergeStats()
            if (account.updatedAt > tombstone.deletedAt) {
                return LanSyncTombstoneMergeStats(keptAfterLocalEdit = 1)
            }
            if (!account.isActive) return LanSyncTombstoneMergeStats()
            database.accountDao().update(
                account.copy(
                    isActive = false,
                    updatedAt = System.currentTimeMillis(),
                ),
            )
            return LanSyncTombstoneMergeStats(applied = 1)
        }

        private suspend fun applyCategoryTombstone(tombstone: SyncTombstoneEntity): LanSyncTombstoneMergeStats {
            val category =
                database.categoryDao().getCategoryBySyncId(tombstone.syncId) ?: return LanSyncTombstoneMergeStats()
            if (!category.isActive) return LanSyncTombstoneMergeStats()
            database.categoryDao().update(category.copy(isActive = false))
            return LanSyncTombstoneMergeStats(applied = 1)
        }

        private suspend fun applySubcategoryTombstone(tombstone: SyncTombstoneEntity): LanSyncTombstoneMergeStats {
            val sub =
                database.categoryDao().getSubcategoryBySyncId(tombstone.syncId) ?: return LanSyncTombstoneMergeStats()
            if (!sub.isActive) return LanSyncTombstoneMergeStats()
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
    }
