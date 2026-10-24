package com.mmr.core.lansync

import com.mmr.core.database.MmrDatabase
import com.mmr.core.database.entity.TransactionEntity
import com.mmr.core.model.enums.TransactionType
import com.mmr.core.transaction.TransactionCashFlow
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

@Singleton
class LanSyncAccountBalanceValidator
    @Inject
    constructor(
        private val database: MmrDatabase,
    ) {
        suspend fun warningsAfterMerge(): List<String> {
            val accountDao = database.accountDao()
            val transactions = database.transactionDao().getAllApprovedTransactions()
            val warnings = mutableListOf<String>()
            for (account in accountDao.getAccountsOnce()) {
                val expected = recomputeBalance(account.openingBalance, account.id, transactions)
                if (abs(account.currentBalance - expected) > BALANCE_EPSILON) {
                    warnings +=
                        "${account.name}: stored balance differs from transactions on this device."
                }
            }
            return warnings
        }

        private fun recomputeBalance(
            openingBalance: Double,
            accountId: Long,
            transactions: List<TransactionEntity>,
        ): Double {
            var balance = openingBalance
            for (txn in transactions) {
                when (txn.type) {
                    TransactionType.INCOME ->
                        if (txn.accountId == accountId) balance += txn.amount
                    TransactionType.EXPENSE ->
                        if (txn.accountId == accountId) balance -= txn.amount
                    TransactionType.TRANSFER -> {
                        if (TransactionCashFlow.skipsAccountBalanceRecompute(txn)) continue
                        if (txn.accountId == accountId) balance -= txn.amount
                        if (txn.relatedAccountId == accountId) balance += txn.amount
                    }
                    else -> Unit
                }
            }
            return balance
        }

        private companion object {
            const val BALANCE_EPSILON = 0.01
        }
    }
