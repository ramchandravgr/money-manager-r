package com.mmr.core.parser.rule

import java.util.regex.Pattern

object AmountRule {
    private const val AMT = "([0-9]+(?:,[0-9]+)*(?:\\.[0-9]{1,2})?)"

    private val TRANSACTION_AMOUNT_PATTERNS =
        listOf(
            Pattern.compile("(?:spent|debited)\\s+(?:inr\\s+)?$AMT", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?:inr\\s+)?$AMT\\s+(?:is\\s+)?(?:debited|spent)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("transaction of\\s+(?:inr\\s+)?$AMT", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?:inr\\s+)?$AMT\\s+is spent on\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile(
                "(?:inr\\s+)?$AMT\\s+credited to(?: your)?\\s+(?:a/?c|acct|account)\\b",
                Pattern.CASE_INSENSITIVE,
            ),
            Pattern.compile(
                "credited to(?: your)?\\s+(?:a/?c|acct|account)\\b[^.]{0,100}?(?:inr\\s+)?$AMT",
                Pattern.CASE_INSENSITIVE,
            ),
            Pattern.compile("(?:paid|withdrawn|charged)\\s+(?:inr\\s+)?$AMT", Pattern.CASE_INSENSITIVE),
        )

    private val PREFIX_PATTERN =
        Pattern.compile(
            "(?:(?:inr|rs\\.?|₹)\\s*)$AMT",
            Pattern.CASE_INSENSITIVE,
        )

    private val SUFFIX_PATTERN =
        Pattern.compile(
            "$AMT\\s*(?:inr|rs\\.?|₹)",
            Pattern.CASE_INSENSITIVE,
        )

    private val BALANCE_OR_LIMIT_MARKERS =
        listOf(
            "avl bal",
            "available bal",
            "available credit limit",
            "credit limit is",
            "curr o/s",
            "current o/s",
            "curr os",
            "amt due",
            "amount due",
            "total amt due",
            "min due",
            "minimum due",
            "outstanding",
        )

    fun extractAmount(text: String): Double? {
        for (pattern in TRANSACTION_AMOUNT_PATTERNS) {
            val parsed = firstAmountFromPattern(pattern, text)
            if (parsed != null) return parsed
        }

        var matcher = PREFIX_PATTERN.matcher(text)
        while (matcher.find()) {
            if (!isBalanceOrLimitContext(text, matcher.start())) {
                val parsed = parseAmountGroup(matcher.group(1))
                if (parsed != null) return parsed
            }
        }

        matcher = SUFFIX_PATTERN.matcher(text)
        while (matcher.find()) {
            if (!isBalanceOrLimitContext(text, matcher.start())) {
                val parsed = parseAmountGroup(matcher.group(1))
                if (parsed != null) return parsed
            }
        }

        return null
    }

    private fun firstAmountFromPattern(
        pattern: Pattern,
        text: String,
    ): Double? {
        val matcher = pattern.matcher(text)
        if (!matcher.find()) return null
        return parseAmountGroup(matcher.group(1))
    }

    private fun parseAmountGroup(raw: String?): Double? {
        val cleaned = raw?.replace(",", "") ?: return null
        val value = cleaned.toDoubleOrNull() ?: return null
        return if (value > 0) value else null
    }

    private fun isBalanceOrLimitContext(
        text: String,
        amountStart: Int,
    ): Boolean {
        val windowStart = (amountStart - 48).coerceAtLeast(0)
        val before = text.substring(windowStart, amountStart)
        return BALANCE_OR_LIMIT_MARKERS.any { before.contains(it) }
    }
}

object BalanceRule {
    private val BALANCE_PATTERN =
        Pattern.compile(
            "(?:avl(?:\\.?)?\\s*bal(?:ance)?|available\\s*bal(?:ance)?|bal(?:ance)?\\s*(?:is|:)?)\\s*(?:inr|rs\\.?|₹)?\\s*([0-9]+(?:,[0-9]+)*(?:\\.[0-9]{1,2})?)",
            Pattern.CASE_INSENSITIVE,
        )

    fun extractBalance(text: String): Double? {
        val matcher = BALANCE_PATTERN.matcher(text)
        if (matcher.find()) {
            val raw = matcher.group(1)?.replace(",", "")
            return raw?.toDoubleOrNull()
        }
        return null
    }
}
