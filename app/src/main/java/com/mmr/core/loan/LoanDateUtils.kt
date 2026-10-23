package com.mmr.core.loan

import java.time.Instant
import java.time.ZoneId

object LoanDateUtils {
    /** Aligns a calendar day to noon local time (same convention as EMI due dates). */
    fun normalizeLoanCalendarDay(epochMillis: Long): Long {
        val zone = ZoneId.systemDefault()
        val date = Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalDate()
        return date
            .atTime(12, 0)
            .atZone(zone)
            .toInstant()
            .toEpochMilli()
    }
}
