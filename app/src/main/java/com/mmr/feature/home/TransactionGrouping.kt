package com.mmr.feature.home

import com.mmr.core.common.validation.MmrValidation
import com.mmr.core.database.relation.TransactionWithDetails
import com.mmr.core.transaction.TransactionCashFlow
import com.mmr.feature.home.model.TransactionGroupUi
import com.mmr.feature.home.model.TransactionRowUi
import java.text.SimpleDateFormat
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

object TransactionGrouping {
    fun groupByDate(
        items: List<TransactionWithDetails>,
        zoneId: ZoneId = ZoneId.systemDefault(),
        withinDayAscending: Boolean = true,
        rowMapper: (TransactionWithDetails) -> TransactionRowUi = { item ->
            TransactionRowMapping.fromDetails(
                item,
                SimpleDateFormat("h:mm a", Locale.getDefault()),
            )
        },
    ): List<TransactionGroupUi> {
        val groups = linkedMapOf<java.time.LocalDate, MutableList<TransactionWithDetails>>()
        val dayExpense = linkedMapOf<java.time.LocalDate, Double>()
        val dayIncome = linkedMapOf<java.time.LocalDate, Double>()

        for (item in items) {
            if (!MmrValidation.isValidEpochMillis(item.transaction.dateTime)) continue
            val localDate = HomePeriodUtils.toLocalDate(item.transaction.dateTime, zoneId)
            groups.getOrPut(localDate) { mutableListOf() }.add(item)
            val loanType = item.loan?.loanType
            val income = TransactionCashFlow.incomeAmount(item.transaction, loanType)
            val expense = TransactionCashFlow.expenseAmount(item.transaction, loanType)
            if (income > 0.0) {
                dayIncome[localDate] = dayIncome.getOrDefault(localDate, 0.0) + income
            }
            if (expense > 0.0) {
                dayExpense[localDate] = dayExpense.getOrDefault(localDate, 0.0) + expense
            }
        }

        return groups.entries
            .sortedByDescending { it.key }
            .map { (localDate, dayItems) ->
                val sortedDay =
                    dayItems
                        .sortedBy { it.transaction.dateTime }
                        .let { if (withinDayAscending) it else it.asReversed() }
                val rows = sortedDay.map(rowMapper)
                val locale = Locale.getDefault()
                TransactionGroupUi(
                    dateKey = localDate.toString(),
                    dayOfMonth = localDate.dayOfMonth,
                    weekdayShort = localDate.dayOfWeek.getDisplayName(TextStyle.SHORT, locale),
                    monthYearLabel = "${localDate.monthValue}.${localDate.year}",
                    dayExpenseTotal = dayExpense.getOrDefault(localDate, 0.0),
                    dayIncomeTotal = dayIncome.getOrDefault(localDate, 0.0),
                    items = rows,
                )
            }
    }

    fun sortByDateTime(
        items: List<TransactionWithDetails>,
        ascending: Boolean,
    ): List<TransactionWithDetails> {
        val sorted = items.sortedBy { it.transaction.dateTime }
        return if (ascending) sorted else sorted.asReversed()
    }
}
