package com.mmr.feature.stats

import com.mmr.feature.home.HomeExcelExportPeriods
import com.mmr.feature.home.HomePeriodUtils
import com.mmr.feature.stats.model.StatsPeriodMode
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object StatsPeriodUtils {
    data class PeriodBounds(
        val startMillis: Long,
        val endMillis: Long,
        val startDate: LocalDate,
        val endDate: LocalDate,
        val navigationLabel: String,
    )

    private val shortDate: DateTimeFormatter =
        DateTimeFormatter.ofPattern("dd.MM.yy", Locale.getDefault())
    private val monthYear: DateTimeFormatter =
        DateTimeFormatter.ofPattern("MMM yyyy", Locale.getDefault())
    private val slashDate: DateTimeFormatter =
        DateTimeFormatter.ofPattern("dd/MM/yy", Locale.getDefault())

    fun resolveBounds(
        mode: StatsPeriodMode,
        anchorDate: LocalDate,
        customStart: LocalDate?,
        customEnd: LocalDate?,
        zoneId: ZoneId = ZoneId.systemDefault(),
        monthStartDay: Int = 1,
        weekStartDay: DayOfWeek = DayOfWeek.MONDAY,
    ): PeriodBounds =
        when (mode) {
            StatsPeriodMode.WEEKLY -> {
                val (start, end) = HomePeriodUtils.weekRangeContaining(anchorDate, weekStartDay)
                PeriodBounds(
                    startMillis = HomePeriodUtils.dayStartMillis(start, zoneId),
                    endMillis = HomePeriodUtils.dayEndMillis(end, zoneId),
                    startDate = start,
                    endDate = end,
                    navigationLabel = "${shortDate.format(start)} ~ ${shortDate.format(end)}",
                )
            }
            StatsPeriodMode.MONTHLY -> {
                val ym = YearMonth.from(anchorDate)
                val (start, end) = HomePeriodUtils.monthDateRange(ym, monthStartDay)
                PeriodBounds(
                    startMillis = HomePeriodUtils.dayStartMillis(start, zoneId),
                    endMillis = HomePeriodUtils.dayEndMillis(end, zoneId),
                    startDate = start,
                    endDate = end,
                    navigationLabel = monthYear.format(start),
                )
            }
            StatsPeriodMode.ANNUALLY -> {
                val (start, end) =
                    HomeExcelExportPeriods.calendarYearDateRange(
                        anchorDate.year,
                        monthStartDay,
                    )
                PeriodBounds(
                    startMillis = HomePeriodUtils.dayStartMillis(start, zoneId),
                    endMillis = HomePeriodUtils.dayEndMillis(end, zoneId),
                    startDate = start,
                    endDate = end,
                    navigationLabel =
                        if (monthStartDay == 1) {
                            anchorDate.year.toString()
                        } else {
                            "${shortDate.format(start)} ~ ${shortDate.format(end)}"
                        },
                )
            }
            StatsPeriodMode.PERIOD -> {
                val start = customStart ?: anchorDate.minusDays(6)
                val end = customEnd ?: anchorDate
                val orderedStart = minOf(start, end)
                val orderedEnd = maxOf(start, end)
                PeriodBounds(
                    startMillis = HomePeriodUtils.dayStartMillis(orderedStart, zoneId),
                    endMillis = HomePeriodUtils.dayEndMillis(orderedEnd, zoneId),
                    startDate = orderedStart,
                    endDate = orderedEnd,
                    navigationLabel = "${slashDate.format(orderedStart)} ~ ${slashDate.format(orderedEnd)}",
                )
            }
        }

    fun shiftAnchor(
        mode: StatsPeriodMode,
        anchorDate: LocalDate,
        forward: Boolean,
    ): LocalDate {
        val delta = if (forward) 1 else -1
        return when (mode) {
            StatsPeriodMode.WEEKLY -> anchorDate.plusWeeks(delta.toLong())
            StatsPeriodMode.MONTHLY -> anchorDate.plusMonths(delta.toLong())
            StatsPeriodMode.ANNUALLY -> anchorDate.plusYears(delta.toLong())
            StatsPeriodMode.PERIOD -> anchorDate
        }
    }

    fun modeDropdownLabel(mode: StatsPeriodMode): String =
        when (mode) {
            StatsPeriodMode.WEEKLY -> "Weekly"
            StatsPeriodMode.MONTHLY -> "Monthly"
            StatsPeriodMode.ANNUALLY -> "Annually"
            StatsPeriodMode.PERIOD -> "Period"
        }
}
