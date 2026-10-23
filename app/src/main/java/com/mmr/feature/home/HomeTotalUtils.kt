package com.mmr.feature.home

import com.mmr.core.common.validation.MmrValidation
import com.mmr.core.database.relation.TransactionWithDetails
import com.mmr.core.model.enums.AccountType
import com.mmr.core.model.enums.TransactionType
import com.mmr.core.transaction.TransactionCashFlow
import com.mmr.feature.home.model.HomeTotalAccountsSummaryUi
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

object HomeTotalUtils {
    private val shortRangeFormat: DateTimeFormatter =
        DateTimeFormatter.ofPattern("dd.MM.yy", Locale.getDefault())

    /** MM `AG_TYPE` 1 / 11 — cash-like spending accounts (not card). */
    private val cashExpenseAccountTypes =
        setOf(
            AccountType.CASH,
            AccountType.BANK,
            AccountType.DEBIT,
            AccountType.WALLET,
            AccountType.OTHER,
        )

    /** Destination types that are still “internal” transfers (not transfer-expense). */
    private val internalTransferDestinationTypes =
        setOf(
            AccountType.CASH,
            AccountType.BANK,
            AccountType.DEBIT,
            AccountType.CREDIT,
            AccountType.WALLET,
        )

    fun buildAccountsSummary(
        currentPeriodItems: List<TransactionWithDetails>,
        previousPeriodItems: List<TransactionWithDetails>,
        yearMonth: YearMonth,
        zoneId: ZoneId,
        monthStartDay: Int,
    ): HomeTotalAccountsSummaryUi {
        val (periodStart, periodEnd) = HomePeriodUtils.monthDateRange(yearMonth, monthStartDay)
        val rangeLabel = "${shortRangeFormat.format(periodStart)} ~ ${shortRangeFormat.format(periodEnd)}"

        var accountExpense = 0.0
        var cardExpense = 0.0
        var transferTotal = 0.0
        val cashAccountNames = linkedSetOf<String>()
        val cardAccountNames = linkedSetOf<String>()
        val transferSourceNames = linkedSetOf<String>()

        for (item in currentPeriodItems) {
            val loanType = item.loan?.loanType
            val expense = TransactionCashFlow.expenseAmount(item.transaction, loanType)
            if (expense > 0.0) {
                val account = item.account
                when (account?.type) {
                    AccountType.CREDIT -> {
                        cardExpense += expense
                        account?.name?.let { cardAccountNames.add(it) }
                    }
                    in cashExpenseAccountTypes -> {
                        accountExpense += expense
                        account?.name?.let { cashAccountNames.add(it) }
                    }
                    else -> Unit
                }
            }
            val transfer = transferExpenseAmount(item)
            if (transfer > 0.0) {
                transferTotal += transfer
                item.account?.name?.let { transferSourceNames.add(it) }
            }
        }

        val currentExpenseTotal =
            currentPeriodItems.sumOf {
                TransactionCashFlow.expenseAmount(it.transaction, it.loan?.loanType)
            }
        val previousExpenseTotal =
            previousPeriodItems.sumOf {
                TransactionCashFlow.expenseAmount(it.transaction, it.loan?.loanType)
            }
        val comparedPercent = mmComparedExpensePercent(currentExpenseTotal, previousExpenseTotal)

        return HomeTotalAccountsSummaryUi(
            periodRangeLabel = rangeLabel,
            comparedExpensePercentLabel = "$comparedPercent%",
            cashExpenseLabel = mmLabelWithAccounts("Cash Expenses", cashAccountNames),
            cardExpenseLabel = mmLabelWithAccounts("Card Expenses", cardAccountNames),
            transferExpenseLabel = mmTransferExpenseLabel(transferSourceNames),
            showCardExpenseRow = cardExpense > 0.0,
            accountExpense = accountExpense,
            cardExpense = cardExpense,
            transferAmount = transferTotal,
        )
    }

    /**
     * MM `BudgetService.m39078c`: `((currentOut - lastOut) * 100 / lastOut) + 100`, or `0` when lastOut is 0.
     */
    fun mmComparedExpensePercent(
        currentExpense: Double,
        previousExpense: Double,
    ): Long {
        val deltaPercent =
            if (previousExpense <= 0.0) {
                0L
            } else {
                ((currentExpense - previousExpense) * 100.0 / previousExpense).roundToInt().toLong()
            }
        return deltaPercent + 100L
    }

    private fun mmLabelWithAccounts(
        base: String,
        accountNames: Collection<String>,
    ): String {
        if (accountNames.isEmpty()) return base
        return "$base (${accountNames.joinToString(", ")})"
    }

    private fun mmTransferExpenseLabel(sourceAccountNames: Collection<String>): String {
        val base = "Transfer-Expense"
        if (sourceAccountNames.isEmpty()) return base
        return "$base (${sourceAccountNames.joinToString(", ")} → )"
    }

    fun filterToMonth(
        items: List<TransactionWithDetails>,
        yearMonth: YearMonth,
        zoneId: ZoneId,
        monthStartDay: Int,
    ): List<TransactionWithDetails> =
        items.filter { item ->
            MmrValidation.isValidEpochMillis(item.transaction.dateTime) &&
                HomePeriodUtils.isInMonth(item.transaction.dateTime, yearMonth, zoneId, monthStartDay)
        }

    /**
     * MM transfer-expense: outflow from cash/bank groups to non-internal destination types.
     */
    private fun transferExpenseAmount(item: TransactionWithDetails): Double {
        val transaction = item.transaction
        if (transaction.type != TransactionType.TRANSFER) return 0.0
        if (TransactionCashFlow.isLoanTopUp(transaction)) return 0.0

        val fromType = item.account?.type ?: return 0.0
        if (fromType !in cashExpenseAccountTypes) return 0.0

        val relatedId = transaction.relatedAccountId
        if (relatedId != null && relatedId > 0L) {
            val toType = item.relatedAccount?.type
            if (toType != null && toType in internalTransferDestinationTypes) {
                return 0.0
            }
            return transaction.amount
        }

        val loanId = transaction.loanId
        if (loanId != null && loanId > 0L) {
            return transaction.amount
        }
        return 0.0
    }
}
