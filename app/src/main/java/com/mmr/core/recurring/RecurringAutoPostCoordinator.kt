package com.mmr.core.recurring

import com.mmr.core.database.dao.LoanDao
import com.mmr.core.database.dao.TransactionDao
import com.mmr.core.database.entity.RecurringRuleEntity
import com.mmr.core.database.entity.TransactionEntity
import com.mmr.core.domain.repository.RecurringRuleRepository
import com.mmr.core.domain.repository.TransactionRepository
import com.mmr.core.domain.util.RecurrenceCalculator
import com.mmr.core.model.enums.RecordStatus
import com.mmr.core.model.enums.TransactionSourceType
import com.mmr.core.model.enums.TransactionType
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecurringAutoPostCoordinator
    @Inject
    constructor(
        private val recurringRuleRepository: RecurringRuleRepository,
        private val transactionRepository: TransactionRepository,
        private val transactionDao: TransactionDao,
        private val loanDao: LoanDao,
    ) {
        /**
         * Creates approved ledger rows for enabled rules whose [RecurringRuleEntity.nextRunDate]
         * is on or before [upToMillis]. Catches up missed periods (e.g. app not opened for months).
         */
        suspend fun postDueRecurringTransactions(upToMillis: Long = System.currentTimeMillis()): Int {
            val dueRules = recurringRuleRepository.getDueRules(upToMillis)
            var created = 0
            for (rule in dueRules) {
                created += postAllDueOccurrencesForRule(rule.id, upToMillis)
            }
            return created
        }

        private suspend fun postAllDueOccurrencesForRule(
            ruleId: Long,
            upToMillis: Long,
        ): Int {
            var created = 0
            var iterations = 0
            while (iterations < MAX_CATCH_UP_PER_RULE) {
                iterations++
                val rule =
                    recurringRuleRepository.getRuleById(ruleId)
                        ?: break
                if (!rule.isEnabled) break
                if (rule.nextRunDate > upToMillis) break
                if (RecurrenceCalculator.isExpired(rule.nextRunDate, rule.endDate)) {
                    recurringRuleRepository.setRuleEnabled(ruleId, false)
                    break
                }

                val runDate = rule.nextRunDate
                val ref = RecurringSourceRefs.occurrence(ruleId, runDate)
                if (transactionDao.findIdBySourceReference(ref) == null) {
                    if (createTransactionForOccurrence(rule, runDate, ref)) {
                        created++
                    }
                }
                recurringRuleRepository.advanceNextRunDate(ruleId)
            }
            return created
        }

        private suspend fun createTransactionForOccurrence(
            rule: RecurringRuleEntity,
            runDateMillis: Long,
            sourceRef: String,
        ): Boolean {
            if (rule.id <= 0L) return false
            return try {
                val note =
                    rule.noteTemplate?.trim()?.takeIf { it.isNotEmpty() }
                        ?: "Recurring: ${rule.name}"
                val (relatedAccountId, loanId) = resolveTransferTargets(rule)
                transactionRepository.insertTransaction(
                    TransactionEntity(
                        dateTime = runDateMillis,
                        amount = rule.amount,
                        type = rule.transactionTemplateType,
                        categoryId = rule.categoryId,
                        subcategoryId = rule.subcategoryId,
                        note = note,
                        accountId = rule.accountId,
                        relatedAccountId = relatedAccountId,
                        loanId = loanId,
                        sourceType = TransactionSourceType.RECURRING,
                        sourceReferenceId = sourceRef,
                        status = RecordStatus.APPROVED,
                    ),
                )
                true
            } catch (_: Exception) {
                false
            }
        }

        private suspend fun resolveTransferTargets(rule: RecurringRuleEntity): Pair<Long?, Long?> {
            if (rule.transactionTemplateType != TransactionType.TRANSFER) {
                return null to null
            }
            if (rule.loanId != null && rule.loanId > 0L) {
                val loan = loanDao.getById(rule.loanId) ?: return null to rule.loanId
                val linked = loan.linkedLoanAccountId
                return linked to rule.loanId
            }
            return rule.relatedAccountId to null
        }

        companion object {
            /** Avoid unbounded catch-up if the device was offline for years. */
            private const val MAX_CATCH_UP_PER_RULE = 366
        }
    }
