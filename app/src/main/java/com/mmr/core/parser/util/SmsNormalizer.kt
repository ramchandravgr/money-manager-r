package com.mmr.core.parser.util

import java.util.Locale

object SmsNormalizer {
    /**
     * Normalizes SMS body text for deterministic rule matching and hashing:
     * - Trims leading/trailing whitespace
     * - Collapses multiple spaces, tabs, and newlines into single spaces
     * - Replaces standard rupee variations with normalized 'inr '
     */
    fun normalizeForMatching(text: String): String =
        text
            .trim()
            .replace("₹", "inr ")
            // Require non-letter before "rs" so words like "reversed" are not corrupted.
            .replace("(?<![a-z])rs[:.]?\\s*".toRegex(RegexOption.IGNORE_CASE), "inr ")
            .replace("\\s+".toRegex(), " ")
            .lowercase(Locale.getDefault())

    /**
     * Preserves original casing but trims and collapses spaces for hashing.
     */
    fun normalizeForHashing(
        sender: String,
        body: String,
        timestamp: Long,
    ): String {
        val cleanSender = sender.trim().uppercase(Locale.getDefault())
        val cleanBody = body.trim().replace("\\s+".toRegex(), " ")
        return "${cleanSender}_${timestamp}_$cleanBody"
    }
}
