package com.mmr.core.parser.account

import com.mmr.core.parser.model.ParsedSmsCandidate
import com.mmr.core.parser.model.SmsInstrument
import com.mmr.core.parser.template.SmsTemplateRegistry

internal object SmsSpendChannel {

    fun isCreditCardSpend(candidate: ParsedSmsCandidate): Boolean {
        if (candidate.instrument == SmsInstrument.CREDIT_CARD) return true
        val body = candidate.rawBody.lowercase()
        return body.contains("bobcard") ||
            body.contains("credit card") ||
            body.contains("is spent on your") && body.contains("card ending")
    }

    fun isHpPayWalletSpend(candidate: ParsedSmsCandidate): Boolean {
        if (candidate.instrument == SmsInstrument.FUEL_WALLET &&
            !isCreditCardSpend(candidate) &&
            !looksLikeCardAtHpPayMerchant(candidate)
        ) {
            return true
        }
        val senderKey = SmsTemplateRegistry.normalizeSenderId(candidate.sender)
        return senderKey.contains("HPPAY") &&
            !isCreditCardSpend(candidate) &&
            !looksLikeCardAtHpPayMerchant(candidate)
    }

    fun isPluxeeMealSpend(candidate: ParsedSmsCandidate): Boolean =
        candidate.instrument == SmsInstrument.MEAL_WALLET ||
            candidate.rawBody.lowercase().contains("pluxee meal")

    private fun looksLikeCardAtHpPayMerchant(candidate: ParsedSmsCandidate): Boolean {
        val body = candidate.rawBody.lowercase()
        return body.contains("bobcard") ||
            body.contains("credit card") ||
            body.contains("card ending") ||
            (body.contains("hp pay") && candidate.accountLastDigits != null && !body.contains("hppays"))
    }
}
