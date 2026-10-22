package com.mmr.core.parser.rule

import java.util.regex.Pattern

object MerchantRule {

    private val MERCHANT_PATTERNS = listOf(
        Pattern.compile("(?i)to\\s+vpa\\s+([^\\s(]+)"),
        Pattern.compile("(?i)vpa\\s+([^\\s(]+@[^\\s(]+)"),
        Pattern.compile(
            "(?i)(?:is\\s+)?spent on your bobcard ending \\d+ at (.+?) on \\d",
            Pattern.CASE_INSENSITIVE
        ),
        Pattern.compile(
            "(?i)spent (?:inr\\s+)?[0-9.,]+ on (?:hdfc bank )?credit card \\w+ at (.+?) on ",
            Pattern.CASE_INSENSITIVE
        ),
        Pattern.compile("(?i)at ([a-z0-9][a-z0-9 &.'-]{1,40}?) on \\d{1,2}[-/]", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?:at|to|info)\\s+([a-zA-Z0-9&@._-]+(?:\\s+[a-zA-Z0-9&@._-]+)?)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?:paid to|transferred to)\\s+([a-zA-Z0-9&@._-]+)", Pattern.CASE_INSENSITIVE)
    )

    private val ACCOUNT_PATTERNS = listOf(
        Pattern.compile("(?i)bobcard ending ([0-9]{4})"),
        Pattern.compile("(?i)credit card (?:xx|x)?([0-9]{4})"),
        Pattern.compile("(?:a/c|acct|account|card|ending)\\s*[:.]?\\s*[*xX]*([0-9]{4})", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?:ending with|ending in)\\s*([0-9]{4})", Pattern.CASE_INSENSITIVE)
    )

    private val REFERENCE_PATTERNS = listOf(
        Pattern.compile("(?:ref|rrn|utr|txn\\s*id|transaction\\s*id|ref\\s*no)[:.]?\\s*([a-zA-Z0-9]+)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?i)upi ref(?:erence)?\\s*no\\.?\\s*([a-zA-Z0-9]+)")
    )

    fun extractMerchant(text: String): String? {
        for (pattern in MERCHANT_PATTERNS) {
            val matcher = pattern.matcher(text)
            if (matcher.find()) {
                val candidate = matcher.group(1)?.trim()
                if (!candidate.isNullOrBlank() && !isInvalidMerchantToken(candidate)) {
                    return prettifyMerchant(candidate).take(40)
                }
            }
        }
        return null
    }

    private fun isInvalidMerchantToken(candidate: String): Boolean =
        candidate.equals("a/c", ignoreCase = true) ||
            candidate.equals("vpa", ignoreCase = true)

    private fun prettifyMerchant(raw: String): String {
        val trimmed = raw.trim().trimEnd('.', ',')
        if (trimmed.contains("@")) {
            val handle = trimmed.substringBefore("@")
            return handle.substringBefore('.').replace('-', ' ').trim()
        }
        return trimmed
    }

    fun extractAccountLastDigits(text: String): String? {
        for (pattern in ACCOUNT_PATTERNS) {
            val matcher = pattern.matcher(text)
            if (matcher.find()) {
                val digits = matcher.group(1)?.trim()
                if (!digits.isNullOrBlank() && digits.length == 4) {
                    return digits
                }
            }
        }
        return null
    }

    fun extractReference(text: String): String? {
        for (pattern in REFERENCE_PATTERNS) {
            val matcher = pattern.matcher(text)
            if (matcher.find()) {
                val ref = matcher.group(1)?.trim()
                if (!ref.isNullOrBlank() && ref.length >= 4) {
                    return ref
                }
            }
        }
        return null
    }

    fun inferCategoryHint(text: String, merchant: String?): String? {
        val target = ((merchant ?: "") + " " + text).lowercase()
        return when {
            target.contains("swiggy") || target.contains("zomato") || target.contains("mcdonald") ||
                target.contains("restaurant") || target.contains("cafe") || target.contains("starbucks") -> "Food & Dining"

            target.contains("blinkit") || target.contains("zepto") || target.contains("instamart") ||
                target.contains("supermarket") || target.contains("grocery") -> "Groceries"

            target.contains("uber") || target.contains("ola") || target.contains("petrol") ||
                target.contains("fuel") || target.contains("metro") || target.contains("irctc") ||
                target.contains("hp pay") -> "Transport & Fuel"

            target.contains("amazon") || target.contains("flipkart") || target.contains("myntra") ||
                target.contains("mall") || target.contains("shopping") -> "Shopping"

            target.contains("bescom") || target.contains("electricity") || target.contains("airtel") ||
                target.contains("jio") || target.contains("broadband") || target.contains("bill") -> "Bills & Utilities"

            target.contains("salary") || target.contains("payroll") -> "Salary"
            target.contains("zerodha") || target.contains("groww") || target.contains("mutual fund") -> "Investment / Return"

            else -> null
        }
    }
}
