package com.mmr.core.parser.engine

import com.mmr.core.parser.model.SmsParseIntent

object SmsClassifier {

    private val IGNORED_EXCLUSIONS = listOf(
        "otp",
        "verification code",
        "one time password",
        "secret code",
        "do not share",
        "security code",
        "win cash",
        "lottery",
        "congratulations! you won",
        "pre-approved loan offer",
        "click to apply",
        "kyc pending",
        "recharge successful",
        "talktime recharge",
        "special bonus recharge",
        "data pack is now active",
        "data alert!",
        "credited your daily",
        "1gb",
        " gb ",
        "happy coins",
        "e-gift voucher",
        "welcome offer is added",
        "credited to your limeroad",
        "credited to your account. shop",
        "ride points credited",
    )

    private val COIN_OR_POINTS_EXCLUSIONS = listOf(
        "happy coins",
        "coins worth",
        "reward points",
        "ride points",
        "wallet points",
        "supercoins",
        "neucoins"
    )

    fun detectInformational(textLower: String): SmsParseIntent? {
        return when {
            textLower.contains("upcoming mandate") ||
                textLower.contains("for the upcoming mandate") ||
                textLower.contains("will be debited from your") -> SmsParseIntent.MANDATE

            textLower.contains("e-statement of") ||
                textLower.contains("total amt due") ||
                textLower.contains("minimum due") -> SmsParseIntent.STATEMENT

            isIpoBlockOrRelease(textLower) -> SmsParseIntent.INVESTMENT

            else -> null
        }
    }

    fun isIpoBlockOrRelease(textLower: String): Boolean {
        if (!textLower.contains("ipo") && !textLower.contains("asba")) return false
        if (textLower.contains("debited")) return false
        if (textLower.contains("allotted") && !textLower.contains("not allotted")) return false
        return textLower.contains("blocked") ||
            textLower.contains("block of") ||
            textLower.contains("amount blocked") ||
            textLower.contains("funds released") ||
            textLower.contains("release of") ||
            textLower.contains("application received") ||
            textLower.contains("bid placed")
    }

    fun isCashbackForReview(textLower: String): Boolean {
        if (COIN_OR_POINTS_EXCLUSIONS.any { textLower.contains(it) }) return false
        val mentionsCashback = textLower.contains("cashback") || textLower.contains("cash back")
        val credited = textLower.contains("credited") || textLower.contains("has been credited")
        return mentionsCashback && credited
    }

    fun informationalReason(intent: SmsParseIntent): String = when (intent) {
        SmsParseIntent.MANDATE -> "Upcoming debit mandate (not imported as a transaction)"
        SmsParseIntent.STATEMENT -> "Card/account statement notice (not a new transaction)"
        SmsParseIntent.INVESTMENT -> "IPO block/release or holding notice (not imported)"
        SmsParseIntent.SETTLED -> "Financial SMS"
    }

    fun isIgnored(textLower: String): Boolean {
        if (textLower.contains("payment for") && textLower.contains("not completed")) return true
        if (textLower.contains("if debited will get refunded")) return true
        return IGNORED_EXCLUSIONS.any { textLower.contains(it) }
    }

    fun isNonFinancial(textLower: String): Boolean = isIgnored(textLower)
}
