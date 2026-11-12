package com.rrparallelstudio.miapp.core.drive

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** When a scheduled Google Drive backup should run, from [lastSuccessMillis] and [schedule]. */
object GoogleDriveAutoBackupPolicy {
    fun isBackupDue(
        schedule: GoogleDriveAutoSchedule,
        lastSuccessMillis: Long,
        nowMillis: Long = System.currentTimeMillis(),
    ): Boolean {
        if (schedule == GoogleDriveAutoSchedule.OFF) return false
        return when (schedule) {
            GoogleDriveAutoSchedule.DAILY ->
                lastSuccessMillis <= 0L ||
                    !isSameCalendarDay(lastSuccessMillis, nowMillis)
            GoogleDriveAutoSchedule.WEEKLY ->
                lastSuccessMillis <= 0L ||
                    daysBetween(lastSuccessMillis, nowMillis) >= 7
            GoogleDriveAutoSchedule.MONTHLY ->
                lastSuccessMillis <= 0L ||
                    daysBetween(lastSuccessMillis, nowMillis) >= 30
            GoogleDriveAutoSchedule.OFF -> false
        }
    }

    private fun isSameCalendarDay(
        firstMillis: Long,
        secondMillis: Long,
    ): Boolean {
        if (firstMillis <= 0L) return false
        val fmt = SimpleDateFormat("yyyyMMdd", Locale.US)
        return fmt.format(Date(firstMillis)) == fmt.format(Date(secondMillis))
    }

    private fun daysBetween(
        startMillis: Long,
        endMillis: Long,
    ): Long = ((endMillis - startMillis) / (24L * 60L * 60L * 1000L)).coerceAtLeast(0L)
}
