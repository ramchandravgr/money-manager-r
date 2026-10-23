package com.mmr.core.parser.model

data class SmsInboxScanSummary(
    val transactions: List<ParsedSmsCandidate>,
    val mandateNotices: Int = 0,
    val statementNotices: Int = 0,
    val investmentNotices: Int = 0,
    val cardPaymentNotices: Int = 0,
    val duplicateSuppressed: Int = 0,
) {
    val informationalCount: Int =
        mandateNotices + statementNotices + investmentNotices + cardPaymentNotices
}

sealed class SmsParseResult {
    data class Transaction(
        val candidate: ParsedSmsCandidate,
    ) : SmsParseResult()

    data class Informational(
        val intent: SmsParseIntent,
        val reason: String,
        val sender: String,
        val timestamp: Long,
        val templateId: String? = null,
        val amount: Double? = null,
    ) : SmsParseResult()

    data object Ignored : SmsParseResult()
}

fun buildParseSourceLabel(
    templateId: String?,
    paymentChannel: SmsPaymentChannel?,
    instrument: SmsInstrument?,
): String? {
    val parts = mutableListOf<String>()
    paymentChannel?.let { parts.add(it.toDisplayLabel()) }
    instrument?.let { parts.add(it.toDisplayLabel()) }
    if (!templateId.isNullOrBlank()) {
        parts.add(templateId.replace('_', ' '))
    }
    return parts.takeIf { it.isNotEmpty() }?.joinToString(" · ")
}

fun SmsPaymentChannel.toDisplayLabel(): String =
    when (this) {
        SmsPaymentChannel.UPI -> "UPI"
        SmsPaymentChannel.NEFT -> "NEFT"
        SmsPaymentChannel.CARD -> "Card"
        SmsPaymentChannel.NACH -> "NACH"
        SmsPaymentChannel.WALLET -> "Wallet"
        SmsPaymentChannel.NET_BANKING -> "Net banking"
        SmsPaymentChannel.UNKNOWN -> "Unknown"
    }

fun SmsInstrument.toDisplayLabel(): String =
    when (this) {
        SmsInstrument.SAVINGS -> "Savings"
        SmsInstrument.CREDIT_CARD -> "Credit card"
        SmsInstrument.MEAL_WALLET -> "Meal wallet"
        SmsInstrument.FUEL_WALLET -> "Fuel wallet"
        SmsInstrument.UNKNOWN -> "Unknown"
    }
