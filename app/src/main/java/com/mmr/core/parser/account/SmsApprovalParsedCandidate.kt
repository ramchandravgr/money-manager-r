package com.mmr.core.parser.account

import com.mmr.core.database.entity.SmsMessageRecordEntity
import com.mmr.core.database.entity.TransactionApprovalEntity
import com.mmr.core.parser.model.ParsedSmsCandidate
import com.mmr.core.parser.model.ParserConfidence
import com.mmr.core.parser.model.SmsInstrument
import com.mmr.core.parser.model.SmsPaymentChannel

internal fun parsedSmsCandidateFromApproval(
    approval: TransactionApprovalEntity,
    sms: SmsMessageRecordEntity,
): ParsedSmsCandidate {
    val body =
        sms.messageBodyFull?.trim()?.takeIf { it.isNotEmpty() }
            ?: sms.messageBodySnippet.trim()
    val confidenceBand =
        when {
            approval.confidenceScore >= ParserConfidence.HIGH.minScore -> ParserConfidence.HIGH
            approval.confidenceScore >= ParserConfidence.MEDIUM.minScore -> ParserConfidence.MEDIUM
            else -> ParserConfidence.LOW
        }
    return ParsedSmsCandidate(
        sender = approval.extractedSender,
        amount = approval.extractedAmount,
        type = approval.extractedType,
        timestamp = approval.extractedDateTime,
        merchant = approval.extractedMerchant,
        accountLastDigits = approval.extractedCardLastFour,
        rawMessageId = sms.rawMessageId ?: approval.id.toString(),
        messageHash = sms.messageBodyHash,
        confidenceScore = approval.confidenceScore,
        confidenceBand = confidenceBand,
        rawBody = body,
        templateId = approval.extractedTemplateId,
        paymentChannel =
            approval.extractedPaymentChannel?.let { stored ->
                runCatching { SmsPaymentChannel.valueOf(stored) }.getOrNull()
            },
        instrument =
            approval.extractedInstrument?.let { stored ->
                runCatching { SmsInstrument.valueOf(stored) }.getOrNull()
            },
        approvalKind = approval.approvalKind,
    )
}
