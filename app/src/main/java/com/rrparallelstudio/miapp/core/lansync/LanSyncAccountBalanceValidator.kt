package com.rrparallelstudio.miapp.core.lansync

import com.rrparallelstudio.miapp.core.database.MmrDatabase
import com.rrparallelstudio.miapp.core.database.entity.TransactionEntity
import com.rrparallelstudio.miapp.core.model.enums.TransactionType
import com.rrparallelstudio.miapp.core.transaction.TransactionCashFlow
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

@Singleton
class LanSyncAccountBalanceValidator
    @Inject
    constructor(
        private val database: MmrDatabase,
    ) {
        /**
         * After merge, set each account's stored balance to opening + approved transactions
         * on this phone (same math as Accounts). Does not add the two phones' stored
         * balances together.
         */
        suspend fun reconcileAfterMerge(): List<String> {
            val accountDao = database.accountDao()
            val transactions = database.transactionDao().getAllApprovedTransactions()
            val now = System.currentTimeMillis()
            val leftover = mutableListOf<String>()
            for (account in accountDao.getAccountsOnce()) {
                val expected = recomputeBalance(account.openingBalance, account.id, transactions)
                if (abs(account.currentBalance - expected) <= BALANCE_EPSILON) continue
                accountDao.update(
                    account.copy(
                        currentBalance = expected,
                        updatedAt = now,
                    ),
                )
                val after = accountDao.getById(account.id)
                if (after == null || abs(after.currentBalance - expected) > BALANCE_EPSILON) {
                    leftover +=
                        "${account.name} — saved balance does not match your transaction list."
                }
            }
            return leftover
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
