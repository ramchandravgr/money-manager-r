package com.mmr.app.ui.components

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import java.text.NumberFormat
import java.util.Locale

/**
 * Shows locale grouping commas on amount fields while the underlying value stays a plain decimal string.
 */
class MmrLocaleAmountVisualTransformation(
    private val locale: Locale = Locale.getDefault(),
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val sanitized = text.text
        val formatted = formatSanitizedAmountForDisplay(sanitized, locale)
        return TransformedText(
            AnnotatedString(formatted),
            SanitizedAmountOffsetMapping(sanitized, locale),
        )
    }
}

internal fun formatSanitizedAmountForDisplay(
    sanitized: String,
    locale: Locale = Locale.getDefault(),
): String {
    if (sanitized.isEmpty()) return ""
    val dotIndex = sanitized.indexOf('.')
    val integerFormatter = NumberFormat.getIntegerInstance(locale)
    return if (dotIndex < 0) {
        val digits = sanitized.filter { it.isDigit() }
        if (digits.isEmpty()) return ""
        integerFormatter.format(digits.toLongOrNull() ?: 0L)
    } else {
        val intDigits = sanitized.substring(0, dotIndex).filter { it.isDigit() }
        val decDigits = sanitized.substring(dotIndex + 1).filter { it.isDigit() }
        val intFormatted =
            if (intDigits.isEmpty()) {
                "0"
            } else {
                integerFormatter.format(intDigits.toLongOrNull() ?: 0L)
            }
        when {
            sanitized.endsWith('.') && decDigits.isEmpty() -> "$intFormatted."
            decDigits.isNotEmpty() -> "$intFormatted.$decDigits"
            else -> intFormatted
        }
    }
}

private class SanitizedAmountOffsetMapping(
    private val sanitized: String,
    private val locale: Locale,
) : OffsetMapping {
    override fun originalToTransformed(offset: Int): Int {
        val clamped = offset.coerceIn(0, sanitized.length)
        return formatSanitizedAmountForDisplay(sanitized.substring(0, clamped), locale).length
    }

    override fun transformedToOriginal(offset: Int): Int {
        val formatted = formatSanitizedAmountForDisplay(sanitized, locale)
        val target = offset.coerceIn(0, formatted.length)
        var orig = 0
        while (orig < sanitized.length) {
            val next = orig + 1
            if (formatSanitizedAmountForDisplay(sanitized.substring(0, next), locale).length >= target) {
                return next.coerceAtMost(sanitized.length)
            }
            orig = next
        }
        return sanitized.length
    }
}
