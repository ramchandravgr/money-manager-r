package com.mmr.feature.home

import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

object HomePeriodUtils {

    fun monthTitle(yearMonth: YearMonth): String {
        val month = yearMonth.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())
        return "$month ${yearMonth.year}"
    }

    fun epochRangeMillis(yearMonth: YearMonth, zoneId: ZoneId = ZoneId.systemDefault()): Pair<Long, Long> {
        val start = yearMonth.atDay(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
        val end = yearMonth.plusMonths(1).atDay(1).atStartOfDay(zoneId).toInstant().toEpochMilli() - 1L
        return start to end
    }

    fun isInMonth(epochMillis: Long, yearMonth: YearMonth, zoneId: ZoneId = ZoneId.systemDefault()): Boolean {
        if (epochMillis <= 0L) return false
        val (start, end) = epochRangeMillis(yearMonth, zoneId)
        return epochMillis in start..end
    }

    fun toLocalDate(epochMillis: Long, zoneId: ZoneId = ZoneId.systemDefault()): LocalDate {
        return Instant.ofEpochMilli(epochMillis).atZone(zoneId).toLocalDate()
    }

    fun dayStartMillis(localDate: LocalDate, zoneId: ZoneId = ZoneId.systemDefault()): Long {
        return localDate.atStartOfDay(zoneId).toInstant().toEpochMilli()
    }

    fun dayEndMillis(localDate: LocalDate, zoneId: ZoneId = ZoneId.systemDefault()): Long {
        return localDate.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli() - 1L
    }

    fun formatShortDate(localDate: LocalDate, locale: Locale = Locale.getDefault()): String {
        val month = localDate.month.getDisplayName(TextStyle.SHORT, locale)
        return "${localDate.dayOfMonth} $month ${localDate.year}"
    }

    /** e.g. "22, Wednesday, October, 2026" */
    fun transactionGroupDateLabel(
        localDate: LocalDate,
        locale: Locale = Locale.getDefault()
    ): String {
        val dayName = localDate.dayOfWeek.getDisplayName(TextStyle.FULL, locale)
        val monthName = localDate.month.getDisplayName(TextStyle.FULL, locale)
        return "${localDate.dayOfMonth}, $dayName, $monthName, ${localDate.year}"
    }
}
