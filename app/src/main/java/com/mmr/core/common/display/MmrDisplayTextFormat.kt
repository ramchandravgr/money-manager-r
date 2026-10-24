package com.mmr.core.common.display

/**
 * Capitalizes the next letter after start, space, comma, or period (transaction titles / notes while typing).
 */
object MmrDisplayTextFormat {
    fun capitalizeAfterWordBoundaries(input: String): String {
        if (input.isEmpty()) return input
        val out = StringBuilder(input.length)
        var capitalizeNext = true
        for (ch in input) {
            if (capitalizeNext && ch.isLetter()) {
                out.append(ch.uppercaseChar())
                capitalizeNext = false
            } else {
                out.append(ch)
                capitalizeNext = ch == ' ' || ch == ',' || ch == '.'
            }
        }
        return out.toString()
    }

    fun formatStoredTitle(text: String?): String? =
        text
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?.let { capitalizeAfterWordBoundaries(it) }
}
