package com.mmr.core.loan

import java.util.Locale

object LoanDisplayFormat {
    /** Title-case loan names for display (e.g. "home loan" → "Home Loan"). */
    fun displayName(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return "Loan details"
        return trimmed
            .split(Regex("\\s+"))
            .joinToString(" ") { word ->
                word.lowercase(Locale.US).replaceFirstChar { char ->
                    char.titlecase(Locale.US)
                }
            }
    }

    /** Shows the stored rate without spurious rounding (e.g. 7.75 stays 7.75, not 7.8). */
    fun annualRatePercent(rate: Double): String {
        if (!rate.isFinite()) return "0%"
        var text = String.format(Locale.US, "%.4f", rate)
        text = text.trimEnd('0').trimEnd('.')
        if (text.isEmpty()) text = "0"
        return "$text%"
    }

    fun annualRatePerAnnumLabel(rate: Double): String = "${annualRatePercent(rate)} p.a."
}
