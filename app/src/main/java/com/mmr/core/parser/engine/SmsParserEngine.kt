package com.mmr.core.parser.engine

import com.mmr.core.model.enums.TransactionType
import com.mmr.core.parser.duplicate.SmsMessageHasher
import com.mmr.core.parser.model.ParsedSmsCandidate
import com.mmr.core.parser.model.ParserConfidence
import com.mmr.core.parser.model.RawSmsMessage
import com.mmr.core.parser.model.SmsParseIntent
import com.mmr.core.parser.model.SmsParseResult
import com.mmr.core.parser.rule.AmountRule
import com.mmr.core.parser.rule.BalanceRule
import com.mmr.core.parser.rule.MerchantRule
import com.mmr.core.parser.rule.SenderRule
import com.mmr.core.parser.rule.TransactionTypeRule
import com.mmr.core.parser.template.SmsTemplateRegistry
import com.mmr.core.parser.util.SmsNormalizer
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmsParserEngine
    @Inject
    constructor(
        private val templateRegistry: SmsTemplateRegistry,
    ) {
        fun parse(sms: RawSmsMessage): ParsedSmsCandidate? =
            (parseResult(sms) as? SmsParseResult.Transaction)?.candidate

        fun parseResult(sms: RawSmsMessage): SmsParseResult {
            val normalized = SmsNormalizer.normalizeForMatching(sms.body)

            if (SmsClassifier.isCashbackForReview(normalized)) {
                val amount = AmountRule.extractAmount(normalized) ?: return SmsParseResult.Ignored
                return SmsParseResult.Transaction(
                    buildCandidate(
                        sms = sms,
                        normalized = normalized,
                        amount = amount,
                        type = TransactionType.INCOME,
                        merchant = MerchantRule.extractMerchant(sms.body),
                        accountDigits = MerchantRule.extractAccountLastDigits(sms.body),
                        referenceNumber = MerchantRule.extractReference(sms.body),
                        balance = BalanceRule.extractBalance(normalized),
                        templateId = "cashback_review",
                        paymentChannel = null,
                        instrument = null,
                        templateMatched = false,
                        categoryHintOverride = "Cashback (confirm)",
                        confidenceCap = 45,
                    ),
                )
            }

            val classifierIntent = SmsClassifier.detectInformational(normalized)
            if (classifierIntent != null) {
                return informationalFromClassifier(sms, normalized, classifierIntent, templateId = null)
            }

            val infoTemplate = templateRegistry.matchInformational(sms, normalized)
            if (infoTemplate != null) {
                return SmsParseResult.Informational(
                    intent = infoTemplate.template.intent,
                    reason = SmsClassifier.informationalReason(infoTemplate.template.intent),
                    sender = sms.sender,
                    timestamp = sms.timestamp,
                    templateId = infoTemplate.template.id,
                    amount = infoTemplate.amount,
                )
            }

            if (SmsClassifier.isIgnored(normalized)) {
                return SmsParseResult.Ignored
            }

            val templateMatch = templateRegistry.matchSettled(sms, normalized)
            if (templateMatch != null) {
                return SmsParseResult.Transaction(
                    buildCandidate(
                        sms = sms,
                        normalized = normalized,
                        amount = templateMatch.amount,
                        type = templateMatch.template.transactionType,
                        merchant = templateMatch.merchant ?: MerchantRule.extractMerchant(sms.body),
                        accountDigits =
                            templateMatch.accountLastDigits
                                ?: MerchantRule.extractAccountLastDigits(sms.body),
                        referenceNumber =
                            templateMatch.referenceNumber
                                ?: MerchantRule.extractReference(sms.body),
                        balance = templateMatch.balanceAfter ?: BalanceRule.extractBalance(normalized),
                        templateId = templateMatch.template.id,
                        paymentChannel = templateMatch.template.paymentChannel,
                        instrument = templateMatch.template.instrument,
                        templateMatched = true,
                    ),
                )
            }

            val amount =
                AmountRule.extractAmount(normalized)
                    ?: return SmsParseResult.Ignored
            val type =
                TransactionTypeRule.inferType(normalized)
                    ?: return SmsParseResult.Ignored

            return SmsParseResult.Transaction(
                buildCandidate(
                    sms = sms,
                    normalized = normalized,
                    amount = amount,
                    type = type,
                    merchant = MerchantRule.extractMerchant(sms.body),
                    accountDigits = MerchantRule.extractAccountLastDigits(sms.body),
                    referenceNumber = MerchantRule.extractReference(sms.body),
                    balance = BalanceRule.extractBalance(normalized),
                    templateId = null,
                    paymentChannel = null,
                    instrument = null,
                    templateMatched = false,
                ),
            )
        }

        private fun informationalFromClassifier(
            sms: RawSmsMessage,
            normalized: String,
            intent: SmsParseIntent,
            templateId: String?,
        ): SmsParseResult.Informational {
            val templateMatch = templateRegistry.matchWithIntent(sms, normalized, intent)
            val amount = templateMatch?.amount ?: AmountRule.extractAmount(normalized)
            return SmsParseResult.Informational(
                intent = intent,
                reason = SmsClassifier.informationalReason(intent),
                sender = sms.sender,
                timestamp = sms.timestamp,
                templateId = templateId ?: templateMatch?.template?.id,
                amount = amount,
            )
        }

        private fun buildCandidate(
            sms: RawSmsMessage,
            normalized: String,
            amount: Double,
            type: com.mmr.core.model.enums.TransactionType,
            merchant: String?,
            accountDigits: String?,
            referenceNumber: String?,
            balance: Double?,
            templateId: String?,
            paymentChannel: com.mmr.core.parser.model.SmsPaymentChannel?,
            instrument: com.mmr.core.parser.model.SmsInstrument?,
            templateMatched: Boolean,
            categoryHintOverride: String? = null,
            confidenceCap: Int? = null,
        ): ParsedSmsCandidate {
            val categoryHint = categoryHintOverride ?: MerchantRule.inferCategoryHint(normalized, merchant)

            var score = 35
            score += 25
            if (templateMatched) score += 20
            if (SenderRule.isKnownFinancialSender(sms.sender)) score += 10
            if (!merchant.isNullOrBlank()) score += 10
            if (!referenceNumber.isNullOrBlank()) score += 10
            if (!accountDigits.isNullOrBlank()) score += 5
            if (balance != null) score += 5
            var finalScore = score.coerceIn(0, 100)
            if (confidenceCap != null) {
                finalScore = minOf(finalScore, confidenceCap)
            }

            val confidenceBand =
                when {
                    finalScore >= ParserConfidence.HIGH.minScore -> ParserConfidence.HIGH
                    finalScore >= ParserConfidence.MEDIUM.minScore -> ParserConfidence.MEDIUM
                    else -> ParserConfidence.LOW
                }

            val messageHash =
                SmsMessageHasher.generateHash(
                    sender = sms.sender,
                    body = sms.body,
                    timestamp = sms.timestamp,
                )

            return ParsedSmsCandidate(
                sender = sms.sender,
                amount = amount,
                type = type,
                timestamp = sms.timestamp,
                merchant = merchant,
                referenceNumber = referenceNumber,
                accountLastDigits = accountDigits,
                balanceAfter = balance,
                categoryHint = categoryHint,
                rawMessageId = sms.id,
                messageHash = messageHash,
                confidenceScore = finalScore,
                confidenceBand = confidenceBand,
                rawBody = sms.body,
                templateId = templateId,
                paymentChannel = paymentChannel,
                instrument = instrument,
            )
        }
    }
