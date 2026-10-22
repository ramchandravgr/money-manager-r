package com.mmr.core.parser.scanner

import java.time.ZoneId
import java.time.ZonedDateTime

object SmsScanPeriod {
    const val DEFAULT_MONTHS = 3
    const val MIN_MONTHS = 1
    const val MAX_MONTHS = 36

    fun clampMonths(months: Int): Int = months.coerceIn(MIN_MONTHS, MAX_MONTHS)

    fun sinceTimestampMillis(
        monthsBack: Int,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): Long {
        val clamped = clampMonths(monthsBack)
        return ZonedDateTime.now(zoneId)
            .minusMonths(clamped.toLong())
            .toInstant()
            .toEpochMilli()
    }
}
