package com.mmr.feature.home

import com.mmr.core.common.validation.MmrValidation
import com.mmr.core.database.relation.TransactionWithDetails
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale

object HomePeriodUtils {
    fun monthTitle(yearMonth: YearMonth): String {
        val month = yearMonth.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())
        return "$month ${yearMonth.year}"
    }

    /** Compact label for narrow home-screen widgets (e.g. 1×2). */
    fun widgetMonthLabel(yearMonth: YearMonth): String {
        val month = yearMonth.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())
        val yearSuffix = (yearMonth.year % 100).toString().padStart(2, '0')
        return "$month '$yearSuffix"
    }

    /**
     * Custom month period: from [monthStartDay] of [yearMonth] through the day before
     * [monthStartDay] of the following month (custom month-start / “Every N” setting).
     */
    fun monthDateRange(
        yearMonth: YearMonth,
        monthStartDay: Int = 1,
    ): Pair<LocalDate, LocalDate> {
        val startDay = monthStartDay.coerceIn(1, 28)
        val start = yearMonth.atDay(startDay)
        val end = yearMonth.plusMonths(1).atDay(startDay).minusDays(1)
        return start to end
    }

    fun epochRangeMillis(
        yearMonth: YearMonth,
        zoneId: ZoneId = ZoneId.systemDefault(),
        monthStartDay: Int = 1,
    ): Pair<Long, Long> {
        val (start, end) = monthDateRange(yearMonth, monthStartDay)
        return dayStartMillis(start, zoneId) to dayEndMillis(end, zoneId)
    }

    fun isInMonth(
        epochMillis: Long,
        yearMonth: YearMonth,
        zoneId: ZoneId = ZoneId.systemDefault(),
        monthStartDay: Int = 1,
    ): Boolean {
        if (epochMillis <= 0L) return false
        val (start, end) = epochRangeMillis(yearMonth, zoneId, monthStartDay)
        return epochMillis in start..end
    }

    fun filterToDateRange(
        items: List<TransactionWithDetails>,
        periodStart: LocalDate,
        periodEnd: LocalDate,
        zoneId: ZoneId,
    ): List<TransactionWithDetails> {
        val startMillis = dayStartMillis(periodStart, zoneId)
        val endMillis = dayEndMillis(periodEnd, zoneId)
        return items.filter { item ->
            MmrValidation.isValidEpochMillis(item.transaction.dateTime) &&
                item.transaction.dateTime in startMillis..endMillis
        }
    }

    fun weekRangeContaining(
        anchor: LocalDate,
        weekStart: DayOfWeek = DayOfWeek.MONDAY,
    ): Pair<LocalDate, LocalDate> {
        val start = anchor.with(TemporalAdjusters.previousOrSame(weekStart))
        val end = start.plusDays(6)
        return start to end
    }

    fun toDayOfWeek(weekStartDay: com.mmr.core.model.settings.MmrWeekStartDay): DayOfWeek =
        DayOfWeek.of(weekStartDay.javaDayOfWeek)

    fun toLocalDate(
        epochMillis: Long,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): LocalDate = Instant.ofEpochMilli(epochMillis).atZone(zoneId).toLocalDate()

    fun dayStartMillis(
        localDate: LocalDate,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): Long = localDate.atStartOfDay(zoneId).toInstant().toEpochMilli()

    fun dayEndMillis(
        localDate: LocalDate,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): Long =
        localDate
            .plusDays(1)
            .atStartOfDay(zoneId)
            .toInstant()
            .toEpochMilli() - 1L

    fun formatShortDate(
        localDate: LocalDate,
        locale: Locale = Locale.getDefault(),
    ): String {
        val month = localDate.month.getDisplayName(TextStyle.SHORT, locale)
        return "${localDate.dayOfMonth} $month ${localDate.year}"
    }

    /** e.g. "22, Wednesday, October, 2026" */
    fun transactionGroupDateLabel(
        localDate: LocalDate,
        locale: Locale = Locale.getDefault(),
    ): String {
        val dayName = localDate.dayOfWeek.getDisplayName(TextStyle.FULL, locale)
        val monthName = localDate.month.getDisplayName(TextStyle.FULL, locale)
        return "${localDate.dayOfMonth}, $dayName, $monthName, ${localDate.year}"
    }
}
