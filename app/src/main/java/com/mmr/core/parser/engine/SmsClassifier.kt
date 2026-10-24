package com.mmr.core.parser.engine

import com.mmr.core.parser.model.SmsParseIntent
import java.util.Locale

object SmsClassifier {
    private val IGNORED_EXCLUSIONS =
        listOf(
            "verification code",
            "one time password",
            "secret otp",
            " is otp ",
            " otp is ",
            "your otp",
            "otp valid",
            "otp for txn",
            "otp for transaction",
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
            "reward points credited",
            "has requested money on the paytm app",
        )

    private val COIN_OR_POINTS_EXCLUSIONS =
        listOf(
            "happy coins",
            "coins worth",
            "reward points",
            "ride points",
            "wallet points",
            "supercoins",
            "neucoins",
        )

    fun detectInformational(textLower: String): SmsParseIntent? {
        if (isLikelyCardMerchantRefund(textLower)) return null
        return when {
            isUpcomingDebitOrNachNotice(textLower) -> SmsParseIntent.MANDATE

            textLower.contains("e-statement of") ||
                textLower.contains("total amt due") ||
                textLower.contains("minimum due") ||
                (textLower.contains("minimum amount is") && textLower.contains("payable by")) ||
                (textLower.contains("credit balance of inr") && textLower.contains("credit card")) ||
                (textLower.contains("total due:") && textLower.contains("minimum due:")) -> SmsParseIntent.STATEMENT

            isIpoBlockOrRelease(textLower) -> SmsParseIntent.INVESTMENT

            isFdSweepLiquidationNotice(textLower) -> SmsParseIntent.INVESTMENT

            else -> null
        }
    }

    fun isLikelyCardMerchantRefund(textLower: String): Boolean {
        if (textLower.contains("transaction reversed") && textLower.contains("credit card")) return true
        if (
            textLower.contains("online refund") &&
            textLower.contains("credit card") &&
            textLower.contains("credited")
        ) {
            return true
        }
        return false
    }

    fun isFdSweepLiquidationNotice(textLower: String): Boolean {
        if (!textLower.contains("will be liquidated")) return false
        return textLower.contains("sweep in debit") || textLower.contains("for sweep in debit")
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

    fun informationalReason(intent: SmsParseIntent): String =
        when (intent) {
            SmsParseIntent.MANDATE -> "Upcoming debit mandate (not imported as a transaction)"
            SmsParseIntent.STATEMENT -> "Card/account statement notice (not a new transaction)"
            SmsParseIntent.INVESTMENT -> "IPO block/release or holding notice (not imported)"
            SmsParseIntent.CARD_PAYMENT -> "Credit card bill payment (not imported; spend already tracked)"
            SmsParseIntent.CARD_REFUND -> "Card merchant refund (apply to original purchase)"
            SmsParseIntent.SETTLED -> "Financial SMS"
        }

    fun isUpcomingDebitOrNachNotice(textLower: String): Boolean {
        if (textLower.contains("will be presented")) return true
        if (textLower.contains("nach for") && textLower.contains("will be")) return true
        return textLower.contains("upcoming mandate") ||
            textLower.contains("for the upcoming mandate") ||
            textLower.contains("will be debited from your") ||
            textLower.contains("will be debited on")
    }

    fun isDeclinedOrFailedTransaction(textLower: String): Boolean {
        if (textLower.contains("declined")) return true
        if (textLower.contains("unsuccessful")) return true
        if (textLower.contains("could not be processed")) return true
        if (textLower.contains("transaction failed") || textLower.contains("has failed")) return true
        if (textLower.contains("payment for") && textLower.contains("not completed")) return true
        if (textLower.contains("if debited will get refunded")) return true
        return false
    }

    /**
     * Axis Bank auto-sweep / flexi-FD breakage: internal BRN-Flexi credits and Flexi/… debits.
     * Not mutual-fund "Flexi Cap Fund" SIP messages (see [isMutualFundFlexiCapSms]).
     */
    fun isAxisAutoSweepFlexi(text: String): Boolean {
        val textLower = text.lowercase(Locale.getDefault())
        if (isMutualFundFlexiCapSms(textLower)) return false
        if (textLower.contains("brn-flexi") || textLower.contains("brn flexi")) return true
        if (textLower.contains("info - brn-flexi")) return true
        val axisContext = textLower.contains("axis bank") || textLower.contains("axbk.in")
        if (axisContext && AXIS_FLEXI_SWEEP_REF.containsMatchIn(textLower)) return true
        return false
    }

    private val AXIS_FLEXI_SWEEP_REF =
        Regex("""flexi/\d{10,}""", RegexOption.IGNORE_CASE)

    private fun isMutualFundFlexiCapSms(textLower: String): Boolean {
        if (textLower.contains("flexi cap fund")) return true
        if (textLower.contains("sip purchase") || textLower.contains("sip pause")) return true
        if (textLower.contains("nav of") || textLower.contains(" at the nav of ")) return true
        if (textLower.contains("mutual fund") || textLower.contains("hdfcmf") || textLower.contains("ppfasmf")) {
            return true
        }
        if (textLower.contains("folio") && textLower.contains("fund")) return true
        return false
    }

    fun isIgnored(textLower: String): Boolean {
        if (isDeclinedOrFailedTransaction(textLower)) return true
        if (isAxisAutoSweepFlexi(textLower)) return true
        return IGNORED_EXCLUSIONS.any { textLower.contains(it) }
    }

    fun isNonFinancial(textLower: String): Boolean = isIgnored(textLower)
}
