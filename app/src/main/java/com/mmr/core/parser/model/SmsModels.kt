package com.mmr.core.parser.model

import com.mmr.core.model.enums.TransactionType

data class RawSmsMessage(
    val id: String,
    val sender: String,
    val body: String,
    val timestamp: Long
)

enum class ParserConfidence(val minScore: Int) {
    HIGH(80),
    MEDIUM(50),
    LOW(0)
}

data class ParsedSmsCandidate(
    val sender: String,
    val amount: Double,
    val type: TransactionType,
    val timestamp: Long,
    val merchant: String? = null,
    val referenceNumber: String? = null,
    val accountLastDigits: String? = null,
    val balanceAfter: Double? = null,
    val categoryHint: String? = null,
    val rawMessageId: String,
    val messageHash: String,
    val confidenceScore: Int,
    val confidenceBand: ParserConfidence,
    val rawBody: String,
    val templateId: String? = null,
    val paymentChannel: SmsPaymentChannel? = null,
    val instrument: SmsInstrument? = null
)
