package com.mmr.core.parser.catalog

import com.mmr.core.model.enums.CardNetwork

object CardMetadataExtractor {
    fun detectNetwork(smsBody: String): CardNetwork? {
        val lower = smsBody.lowercase()
        return when {
            lower.contains("rupay") -> CardNetwork.RUPAY
            lower.contains("visa") -> CardNetwork.VISA
            lower.contains("mastercard") || lower.contains("master card") -> CardNetwork.MASTERCARD
            lower.contains("amex") || lower.contains("american express") -> CardNetwork.AMEX
            lower.contains("diners") -> CardNetwork.DINERS
            lower.contains("maestro") -> CardNetwork.MAESTRO
            else -> null
        }
    }
}
