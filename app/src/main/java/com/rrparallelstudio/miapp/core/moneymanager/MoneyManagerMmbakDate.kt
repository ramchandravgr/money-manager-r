package com.rrparallelstudio.miapp.core.moneymanager

import android.database.Cursor
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar

/**
 * Normalizes [WDATE] (and similar) values from Realbyte Money Manager exports into epoch millis.
 */
internal object MoneyManagerMmbakDate {
    private const val MIN_UNIX_SECONDS = 1_000_000_000L
    private const val MIN_UNIX_MILLIS = 1_000_000_000_000L

    private val DATE_TIME_FORMATTERS =
        listOf(
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd"),
        )

    fun readDateMillis(
        cursor: Cursor,
        dateIdx: Int,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): Long =
        when (cursor.getType(dateIdx)) {
            Cursor.FIELD_TYPE_STRING ->
                fromStorageValue(
                    text = cursor.getString(dateIdx),
                    zoneId = zoneId,
                )
            Cursor.FIELD_TYPE_NULL -> 0L
            else -> toEpochMillis(cursor.getLong(dateIdx), zoneId)
        }

    fun fromStorageValue(
        text: String?,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): Long {
        val trimmed = text?.trim().orEmpty()
        if (trimmed.isEmpty()) return 0L
        val numeric = trimmed.toLongOrNull()
        if (numeric != null) return toEpochMillis(numeric, zoneId)
        val parsed =
            DATE_TIME_FORMATTERS.firstNotNullOfOrNull { formatter ->
                parseWithFormatter(trimmed, formatter, zoneId)
            }
        return parsed ?: 0L
    }

    fun toEpochMillis(
        raw: Long,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): Long =
        when {
            raw >= MIN_UNIX_MILLIS -> raw
            raw >= MIN_UNIX_SECONDS -> raw * 1_000L
            else -> yyyymmddToMillis(raw, zoneId) ?: raw
        }

    private fun parseWithFormatter(
        trimmed: String,
        formatter: DateTimeFormatter,
        zoneId: ZoneId,
    ): Long? =
        runCatching {
            if (trimmed.length <= 10 && !trimmed.contains(' ')) {
                LocalDate
                    .parse(trimmed, formatter)
                    .atStartOfDay(zoneId)
                    .toInstant()
                    .toEpochMilli()
            } else {
                LocalDateTime
                    .parse(trimmed, formatter)
                    .atZone(zoneId)
                    .toInstant()
                    .toEpochMilli()
            }
        }.getOrNull()

    private fun yyyymmddToMillis(
        raw: Long,
        zoneId: ZoneId,
    ): Long? {
        if (raw < 19_000_101L || raw > 99_991_231L) return null
        val digits = raw.toString().padStart(8, '0')
        val year = digits.substring(0, 4).toIntOrNull() ?: return null
        val month = digits.substring(4, 6).toIntOrNull() ?: return null
        val day = digits.substring(6, 8).toIntOrNull() ?: return null
        if (year !in 1970..2100) return null
        val fromLocalDate =
            runCatching {
                LocalDate
                    .of(year, month, day)
                    .atStartOfDay(zoneId)
                    .toInstant()
                    .toEpochMilli()
            }.getOrNull()
        if (fromLocalDate != null) return fromLocalDate
        return runCatching {
            Calendar
                .getInstance()
                .apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month - 1)
                    set(Calendar.DAY_OF_MONTH, day)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
        }.getOrNull()
    }
}
