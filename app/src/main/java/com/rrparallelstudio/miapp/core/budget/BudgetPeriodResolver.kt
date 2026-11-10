package com.rrparallelstudio.miapp.core.budget

import com.rrparallelstudio.miapp.core.database.entity.BudgetEntity
import com.rrparallelstudio.miapp.core.model.enums.BudgetPeriodType
import com.rrparallelstudio.miapp.core.model.settings.MmrWeekStartDay
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.TextStyle
import java.time.temporal.IsoFields
import java.time.temporal.TemporalAdjusters
import java.util.Locale

data class BudgetPeriodBounds(
    val startMillis: Long,
    val endMillis: Long,
    val periodKey: String,
    val label: String,
)

object BudgetPeriodResolver {
    fun resolve(
        budget: BudgetEntity,
        anchorMillis: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault(),
        monthStartDay: Int = 1,
        weekStartDay: MmrWeekStartDay = MmrWeekStartDay.MONDAY,
    ): BudgetPeriodBounds {
        val anchorDate = Instant.ofEpochMilli(anchorMillis).atZone(zoneId).toLocalDate()
        return when (budget.periodType) {
            BudgetPeriodType.WEEKLY -> {
                val weekStart = DayOfWeek.of(weekStartDay.javaDayOfWeek)
                val start = anchorDate.with(TemporalAdjusters.previousOrSame(weekStart))
                val end = start.plusDays(6)
                val startMillis = dayStartMillis(start, zoneId)
                val endMillis = dayEndMillis(end, zoneId)
                val week = anchorDate.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)
                val year = anchorDate.get(IsoFields.WEEK_BASED_YEAR)
                BudgetPeriodBounds(
                    startMillis = startMillis,
                    endMillis = endMillis,
                    periodKey = "W-$year-$week",
                    label = "${formatShortDate(start)} – ${formatShortDate(end)}",
                )
            }
            BudgetPeriodType.MONTHLY -> {
                val ym = yearMonthForCustomStart(anchorDate, monthStartDay)
                val startDay = monthStartDay.coerceIn(1, 28)
                val start = ym.atDay(startDay)
                val end = ym.plusMonths(1).atDay(startDay).minusDays(1)
                BudgetPeriodBounds(
                    startMillis = dayStartMillis(start, zoneId),
                    endMillis = dayEndMillis(end, zoneId),
                    periodKey = "M-${ym.year}-${ym.monthValue}-d$startDay",
                    label = monthTitle(ym),
                )
            }
            BudgetPeriodType.ANNUAL -> {
                val start = LocalDate.of(anchorDate.year, 1, 1)
                val end = LocalDate.of(anchorDate.year, 12, 31)
                BudgetPeriodBounds(
                    startMillis = dayStartMillis(start, zoneId),
                    endMillis = dayEndMillis(end, zoneId),
                    periodKey = "Y-${anchorDate.year}",
                    label = anchorDate.year.toString(),
                )
            }
            BudgetPeriodType.CUSTOM -> {
                val start = budget.customPeriodStartMillis ?: anchorMillis
                val end = budget.customPeriodEndMillis ?: anchorMillis
                BudgetPeriodBounds(
                    startMillis = minOf(start, end),
                    endMillis = maxOf(start, end),
                    periodKey = "C-$start-$end",
                    label = "Custom period",
                )
            }
        }
    }

    private fun yearMonthForCustomStart(
        anchor: LocalDate,
        monthStartDay: Int,
    ): YearMonth {
        val day = monthStartDay.coerceIn(1, 28)
        return if (anchor.dayOfMonth >= day) {
            YearMonth.from(anchor)
        } else {
            YearMonth.from(anchor).minusMonths(1)
        }
    }

    private fun dayStartMillis(
        localDate: LocalDate,
        zoneId: ZoneId,
    ): Long = localDate.atStartOfDay(zoneId).toInstant().toEpochMilli()

    private fun dayEndMillis(
        localDate: LocalDate,
        zoneId: ZoneId,
    ): Long =
        localDate
            .plusDays(1)
            .atStartOfDay(zoneId)
            .toInstant()
            .toEpochMilli() - 1L

    private fun monthTitle(yearMonth: YearMonth): String {
        val month = yearMonth.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())
        return "$month ${yearMonth.year}"
    }

    private fun formatShortDate(localDate: LocalDate): String {
        val month = localDate.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())
        return "${localDate.dayOfMonth} $month"
    }
}
