package com.mmr.core.parser.duplicate

import com.mmr.core.parser.account.SmsSpendChannel
import com.mmr.core.parser.model.ParsedSmsCandidate
import kotlin.math.abs

object SmsCrossChannelDedup {

    private const val TIME_WINDOW_MS = 2 * 60 * 1000L

    data class Result(
        val kept: List<ParsedSmsCandidate>,
        val suppressedCount: Int
    )

    fun deduplicate(candidates: List<ParsedSmsCandidate>): Result {
        val kept = mutableListOf<ParsedSmsCandidate>()
        var suppressed = 0

        for (candidate in candidates) {
            val duplicateIndex = kept.indexOfFirst { isHpPayCardDuplicate(it, candidate) }
            if (duplicateIndex < 0) {
                kept.add(candidate)
                continue
            }
            val existing = kept[duplicateIndex]
            when {
                SmsSpendChannel.isCreditCardSpend(candidate) &&
                    SmsSpendChannel.isHpPayWalletSpend(existing) -> {
                    kept[duplicateIndex] = mergePreferCard(candidate, existing)
                    suppressed++
                }
                SmsSpendChannel.isCreditCardSpend(existing) &&
                    SmsSpendChannel.isHpPayWalletSpend(candidate) -> {
                    kept[duplicateIndex] = mergePreferCard(existing, candidate)
                    suppressed++
                }
                else -> suppressed++
            }
        }

        return Result(kept = kept, suppressedCount = suppressed)
    }

    private fun isHpPayCardDuplicate(a: ParsedSmsCandidate, b: ParsedSmsCandidate): Boolean {
        if (abs(a.amount - b.amount) > 0.01) return false
        if (abs(a.timestamp - b.timestamp) > TIME_WINDOW_MS) return false
        return (SmsSpendChannel.isCreditCardSpend(a) && SmsSpendChannel.isHpPayWalletSpend(b)) ||
            (SmsSpendChannel.isCreditCardSpend(b) && SmsSpendChannel.isHpPayWalletSpend(a))
    }

    private fun mergePreferCard(
        card: ParsedSmsCandidate,
        hpPay: ParsedSmsCandidate
    ): ParsedSmsCandidate {
        val merchant = card.merchant?.takeIf { it.isNotBlank() } ?: hpPay.merchant
        return card.copy(merchant = merchant)
    }
}
