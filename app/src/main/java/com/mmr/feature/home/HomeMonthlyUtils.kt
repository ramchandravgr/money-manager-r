package com.mmr.feature.home

import com.mmr.core.common.validation.MmrValidation
import com.mmr.core.database.relation.TransactionWithDetails
import com.mmr.core.transaction.TransactionCashFlow
import com.mmr.feature.home.model.MonthlyPeriodUi
import com.mmr.feature.home.model.WeeklyPeriodUi
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale

object HomeMonthlyUtils {
    private val dayMonthFormat: DateTimeFormatter =
        DateTimeFormatter.ofPattern("dd.MM", Locale.getDefault())

    fun buildMonthlyPeriods(
        transactions: List<TransactionWithDetails>,
        anchorMonth: YearMonth,
        monthCount: Int,
        zoneId: ZoneId,
        monthStartDay: Int,
        weekStart: DayOfWeek,
    ): List<MonthlyPeriodUi> {
        val months = (0 until monthCount).map { anchorMonth.minusMonths(it.toLong()) }
        return months.map { yearMonth ->
            val (periodStart, periodEnd) = HomePeriodUtils.monthDateRange(yearMonth, monthStartDay)
            val monthTx =
                transactions.filter { item ->
                    MmrValidation.isValidEpochMillis(item.transaction.dateTime) &&
                        item.transaction.dateTime in
                        HomePeriodUtils.dayStartMillis(
                            periodStart,
                            zoneId,
                        )..HomePeriodUtils.dayEndMillis(periodEnd, zoneId)
                }
            val totals = sumTotals(monthTx)
            val weeks =
                buildWeeksInPeriod(
                    periodStart = periodStart,
                    periodEnd = periodEnd,
                    transactions = monthTx,
                    weekStart = weekStart,
                    zoneId = zoneId,
                )
            val monthLabel = yearMonth.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())
            MonthlyPeriodUi(
                yearMonth = yearMonth,
                monthLabel = monthLabel,
                rangeLabel = "${dayMonthFormat.format(periodStart)} ~ ${dayMonthFormat.format(periodEnd)}",
                income = totals.income,
                expense = totals.expense,
                weeks = weeks,
            )
        }
    }

    private data class Totals(
        val income: Double,
        val expense: Double,
    )

    private fun sumTotals(items: List<TransactionWithDetails>): Totals {
        var income = 0.0
        var expense = 0.0
        for (item in items) {
            val loanType = item.loan?.loanType
            income += TransactionCashFlow.incomeAmount(item.transaction, loanType)
            expense += TransactionCashFlow.expenseAmount(item.transaction, loanType)
        }
        return Totals(income = income, expense = expense)
    }

    private fun buildWeeksInPeriod(
        periodStart: LocalDate,
        periodEnd: LocalDate,
        transactions: List<TransactionWithDetails>,
        weekStart: DayOfWeek,
        zoneId: ZoneId,
    ): List<WeeklyPeriodUi> {
        val weeks = mutableListOf<WeeklyPeriodUi>()
        var cursor = periodStart.with(TemporalAdjusters.previousOrSame(weekStart))
        while (cursor <= periodEnd) {
            val weekEnd = cursor.plusDays(6)
            val clipStart = maxOf(cursor, periodStart)
            val clipEnd = minOf(weekEnd, periodEnd)
            if (clipStart <= clipEnd) {
                val startMillis = HomePeriodUtils.dayStartMillis(clipStart, zoneId)
                val endMillis = HomePeriodUtils.dayEndMillis(clipEnd, zoneId)
                val weekTx = transactions.filter { it.transaction.dateTime in startMillis..endMillis }
                val totals = sumTotals(weekTx)
                weeks +=
                    WeeklyPeriodUi(
                        startDate = clipStart,
                        endDate = clipEnd,
                        rangeLabel = "${dayMonthFormat.format(clipStart)} ~ ${dayMonthFormat.format(clipEnd)}",
                        income = totals.income,
                        expense = totals.expense,
                    )
            }
            cursor = cursor.plusDays(7)
        }
        return weeks
    }
}
