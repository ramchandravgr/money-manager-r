package com.mmr.feature.stats

import com.mmr.core.database.model.MonthTotalRow
import com.mmr.feature.home.HomePeriodUtils
import com.mmr.feature.stats.model.MonthlyChartPointUi
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

object StatsCategoryChartUtils {
    private const val CHART_MONTH_COUNT = 8

    fun chartRange(endDate: LocalDate): Pair<Long, Long> {
        val zoneId = ZoneId.systemDefault()
        val endMonth = YearMonth.from(endDate)
        val startMonth = endMonth.minusMonths((CHART_MONTH_COUNT - 1).toLong())
        val start = startMonth.atDay(1)
        val end = endMonth.atEndOfMonth()
        return HomePeriodUtils.dayStartMillis(start, zoneId) to
            HomePeriodUtils.dayEndMillis(end, zoneId)
    }

    fun buildChartPoints(
        endDate: LocalDate,
        rows: List<MonthTotalRow>,
    ): List<MonthlyChartPointUi> {
        val locale = Locale.getDefault()
        val endMonth = YearMonth.from(endDate)
        val amountsByKey = rows.associate { it.yearMonth to it.totalAmount }
        return (0 until CHART_MONTH_COUNT).map { offset ->
            val month = endMonth.minusMonths((CHART_MONTH_COUNT - 1 - offset).toLong())
            val key = "%04d-%02d".format(month.year, month.monthValue)
            val label = month.month.getDisplayName(TextStyle.SHORT, locale).take(3)
            MonthlyChartPointUi(
                label = label,
                amount = amountsByKey[key] ?: 0.0,
            )
        }
    }
}
