package com.mmr.feature.home

import com.mmr.feature.home.model.HomeExcelExportOptionUi
import com.mmr.feature.home.model.HomeExcelExportPreset
import com.mmr.feature.home.model.HomeExcelExportRange
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * MM `ExcelExportDialog` period choices (month / last month / year / last year / total).
 */
object HomeExcelExportPeriods {
    private val shortRangeFormat: DateTimeFormatter =
        DateTimeFormatter.ofPattern("dd.MM.yy", Locale.getDefault())

    fun buildOptions(
        anchorMonth: YearMonth,
        monthStartDay: Int,
    ): List<HomeExcelExportOptionUi> {
        val day = monthStartDay.coerceIn(1, 28)
        val (thisMonthStart, thisMonthEnd) = HomePeriodUtils.monthDateRange(anchorMonth, day)
        val (lastMonthStart, lastMonthEnd) =
            HomePeriodUtils.monthDateRange(anchorMonth.minusMonths(1), day)
        val (thisYearStart, thisYearEnd) = calendarYearDateRange(anchorMonth.year, day)
        val (lastYearStart, lastYearEnd) = calendarYearDateRange(anchorMonth.year - 1, day)

        return listOf(
            option(
                preset = HomeExcelExportPreset.CURRENT_MONTH,
                title = "Monthly",
                start = thisMonthStart,
                end = thisMonthEnd,
            ),
            option(
                preset = HomeExcelExportPreset.LAST_MONTH,
                title = "Last month",
                start = lastMonthStart,
                end = lastMonthEnd,
            ),
            option(
                preset = HomeExcelExportPreset.CURRENT_YEAR,
                title = "Annually",
                start = thisYearStart,
                end = thisYearEnd,
            ),
            option(
                preset = HomeExcelExportPreset.LAST_YEAR,
                title = "Last year",
                start = lastYearStart,
                end = lastYearEnd,
            ),
            HomeExcelExportOptionUi(
                preset = HomeExcelExportPreset.ALL,
                label = "Total",
                range = HomeExcelExportRange.AllTime,
            ),
        )
    }

    fun calendarYearDateRange(
        calendarYear: Int,
        monthStartDay: Int,
    ): Pair<LocalDate, LocalDate> {
        val day = monthStartDay.coerceIn(1, 28)
        if (day == 1) {
            return LocalDate.of(calendarYear, 1, 1) to LocalDate.of(calendarYear, 12, 31)
        }
        val start = LocalDate.of(calendarYear, 1, day)
        val end = LocalDate.of(calendarYear + 1, 1, day).minusDays(1)
        return start to end
    }

    private val fileDateFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    fun buildFileName(
        appName: String,
        range: HomeExcelExportRange,
    ): String =
        when (range) {
            is HomeExcelExportRange.Bounded -> {
                val start = fileDateFormat.format(range.start)
                val end = fileDateFormat.format(range.end)
                "${start}_$end.xlsx"
            }
            HomeExcelExportRange.AllTime -> {
                val stamp = fileDateFormat.format(LocalDate.now())
                "${appName}_$stamp.xlsx"
            }
        }

    fun formatShareSubject(
        appName: String,
        range: HomeExcelExportRange,
    ): String =
        when (range) {
            is HomeExcelExportRange.Bounded -> {
                val start = shortRangeFormat.format(range.start)
                val end = shortRangeFormat.format(range.end)
                "$appName - Excel $start ~ $end"
            }
            HomeExcelExportRange.AllTime -> "$appName - Excel"
        }

    private fun option(
        preset: HomeExcelExportPreset,
        title: String,
        start: LocalDate,
        end: LocalDate,
    ): HomeExcelExportOptionUi {
        val rangeLabel = formatRange(start, end)
        return HomeExcelExportOptionUi(
            preset = preset,
            label = "$title ($rangeLabel)",
            range = HomeExcelExportRange.Bounded(start, end),
        )
    }

    fun formatRange(
        start: LocalDate,
        end: LocalDate,
    ): String = "${shortRangeFormat.format(start)} ~ ${shortRangeFormat.format(end)}"
}
