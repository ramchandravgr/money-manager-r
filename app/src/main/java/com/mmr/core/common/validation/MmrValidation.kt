package com.mmr.core.common.validation

import com.mmr.core.common.display.MmrMoneyFormat

object MmrValidation {
    private const val MAX_FUTURE_MS = 86_400_000L * 2

    fun sanitizeDecimalInput(input: String): String =
        MmrMoneyFormat.stripGroupingSeparators(input).filter { it.isDigit() || it == '.' }

    fun parsePositiveAmount(input: String): Double? {
        val parsed = sanitizeDecimalInput(input).toDoubleOrNull()
        return parsed?.takeIf { it.isFinite() && it > 0.0 }
    }

    fun requirePositiveAmount(
        amount: Double,
        fieldName: String = "Amount",
    ) {
        require(amount.isFinite() && amount > 0.0) { "$fieldName must be greater than zero." }
    }

    fun requireNonBlank(
        value: String?,
        fieldName: String,
    ): String {
        val trimmed = value?.trim() ?: ""
        require(trimmed.isNotEmpty()) { "$fieldName cannot be empty." }
        return trimmed
    }

    fun isValidEpochMillis(value: Long): Boolean = value > 0L && value <= System.currentTimeMillis() + MAX_FUTURE_MS

    fun normalizeEpochMillis(
        value: Long,
        fallback: Long = System.currentTimeMillis(),
    ): Long = if (isValidEpochMillis(value)) value else fallback

    fun normalizeDateRange(
        start: Long,
        end: Long,
    ): Pair<Long, Long> {
        val safeStart = start.coerceAtLeast(0L)
        val safeEnd = end.coerceAtLeast(0L)
        return if (safeStart <= safeEnd) {
            safeStart to safeEnd
        } else {
            safeEnd to safeStart
        }
    }

    fun clampDayOfMonth(day: Int?): Int? {
        day ?: return null
        return day.coerceIn(1, 31)
    }
}
