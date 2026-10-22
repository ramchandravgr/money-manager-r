package com.mmr.core.parser.template

import java.util.regex.Matcher

internal object SmsTemplateFieldMapper {

    fun parseAmount(raw: String?): Double? {
        val cleaned = raw?.replace(",", "")?.trim() ?: return null
        val value = cleaned.toDoubleOrNull() ?: return null
        return if (value > 0) value else null
    }

    fun groupValue(matcher: Matcher, name: String?): String? {
        if (name.isNullOrBlank()) return null
        return try {
            matcher.group(name)?.trim()?.takeIf { it.isNotEmpty() }
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    fun prettifyMerchant(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        val trimmed = raw.trim().trimEnd('.', ',')
        if (trimmed.contains("@")) {
            val handle = trimmed.substringBefore("@")
            return handle.substringBefore('.').replace('-', ' ').trim().take(40)
        }
        return trimmed.take(40)
    }
}
