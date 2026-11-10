package com.rrparallelstudio.miapp.core.transaction

import com.rrparallelstudio.miapp.core.common.math.MmrAmountMath
import com.rrparallelstudio.miapp.core.database.entity.TransactionEntity
import com.rrparallelstudio.miapp.core.model.enums.TransactionType

object TransactionMergeAmounts {
    data class Resolved(
        val amount: Double,
        val type: TransactionType,
    )

    fun resolve(transactions: List<TransactionEntity>): Resolved {
        require(transactions.isNotEmpty())
        val types = transactions.map { it.type }.toSet()
        if (types.size == 1) {
            val type = types.first()
            return Resolved(
                amount = MmrAmountMath.sum(transactions.map { it.amount }),
                type = type,
            )
        }
        require(types == setOf(TransactionType.EXPENSE, TransactionType.INCOME)) {
            "Merged transactions must share the same type, or combine only income with expense."
        }
        val expenseTotal =
            MmrAmountMath.sum(
                transactions.filter { it.type == TransactionType.EXPENSE }.map { it.amount },
            )
        val incomeTotal =
            MmrAmountMath.sum(
                transactions.filter { it.type == TransactionType.INCOME }.map { it.amount },
            )
        val net = MmrAmountMath.netIncomeAndExpense(expenseTotal, incomeTotal)
        return Resolved(amount = net.amount, type = net.type)
    }

    fun resolveLedgerWithSmsAmount(
        ledger: TransactionEntity,
        smsAmount: Double,
        smsType: TransactionType,
    ): Resolved {
        require(
            ledger.type == TransactionType.EXPENSE ||
                ledger.type == TransactionType.INCOME,
        ) {
            "Only income or expense ledger rows can be linked with SMS amounts."
        }
        require(
            smsType == TransactionType.EXPENSE ||
                smsType == TransactionType.INCOME,
        ) {
            "Only income or expense SMS can be linked to a ledger row."
        }
        if (ledger.type == smsType) {
            return Resolved(
                amount = MmrAmountMath.add(ledger.amount, smsAmount),
                type = ledger.type,
            )
        }
        val expenseTotal =
            if (ledger.type == TransactionType.EXPENSE) ledger.amount else smsAmount
        val incomeTotal =
            if (ledger.type == TransactionType.INCOME) ledger.amount else smsAmount
        val net = MmrAmountMath.netIncomeAndExpense(expenseTotal, incomeTotal)
        return Resolved(amount = net.amount, type = net.type)
    }
}
