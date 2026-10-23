package com.mmr.feature.home

import com.mmr.core.common.validation.MmrValidation
import com.mmr.core.database.relation.TransactionWithDetails
import com.mmr.core.transaction.TransactionCashFlow
import com.mmr.feature.home.model.CalendarDayUi
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

object HomeCalendarUtils {
    data class DayTotals(
        val expense: Double = 0.0,
        val income: Double = 0.0,
        val count: Int = 0,
    )

    fun aggregateByDate(
        items: List<TransactionWithDetails>,
        zoneId: ZoneId,
    ): Map<LocalDate, DayTotals> {
        val map = linkedMapOf<LocalDate, DayTotals>()
        for (item in items) {
            if (!MmrValidation.isValidEpochMillis(item.transaction.dateTime)) continue
            val date = HomePeriodUtils.toLocalDate(item.transaction.dateTime, zoneId)
            val loanType = item.loan?.loanType
            val income = TransactionCashFlow.incomeAmount(item.transaction, loanType)
            val expense = TransactionCashFlow.expenseAmount(item.transaction, loanType)
            val prev = map[date] ?: DayTotals()
            map[date] =
                DayTotals(
                    expense = prev.expense + expense,
                    income = prev.income + income,
                    count = prev.count + 1,
                )
        }
        return map
    }

    fun buildMonthGrid(
        yearMonth: YearMonth,
        weekStart: DayOfWeek,
        totalsByDate: Map<LocalDate, DayTotals>,
    ): List<CalendarDayUi> {
        val firstOfMonth = yearMonth.atDay(1)
        val gridStart = firstOfMonth.with(TemporalAdjusters.previousOrSame(weekStart))
        return (0 until 42).map { offset ->
            val date = gridStart.plusDays(offset.toLong())
            val totals = totalsByDate[date] ?: DayTotals()
            CalendarDayUi(
                date = date,
                isCurrentMonth = date.month == yearMonth.month,
                expenseTotal = totals.expense,
                incomeTotal = totals.income,
                transactionCount = totals.count,
            )
        }
    }

    fun prefillMillisForDate(
        date: LocalDate,
        zoneId: ZoneId,
    ): Long {
        val time = LocalTime.now(zoneId)
        return date
            .atTime(time)
            .atZone(zoneId)
            .toInstant()
            .toEpochMilli()
    }
}
