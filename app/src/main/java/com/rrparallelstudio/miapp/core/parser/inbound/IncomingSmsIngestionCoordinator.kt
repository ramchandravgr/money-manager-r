package com.rrparallelstudio.miapp.core.parser.inbound

import com.rrparallelstudio.miapp.core.common.display.MmrMoneyFormat
import com.rrparallelstudio.miapp.core.domain.repository.ApprovalRepository
import com.rrparallelstudio.miapp.core.model.enums.SmsApprovalKind
import com.rrparallelstudio.miapp.core.model.enums.TransactionType
import com.rrparallelstudio.miapp.core.parser.engine.SmsParserEngine
import com.rrparallelstudio.miapp.core.parser.model.RawSmsMessage
import com.rrparallelstudio.miapp.core.parser.model.SmsParseResult
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

data class IncomingSmsIngestionResult(
    val addedCount: Int,
    val summaryLine: String?,
)

@Singleton
class IncomingSmsIngestionCoordinator
    @Inject
    constructor(
        private val smsParserEngine: SmsParserEngine,
        private val approvalRepository: ApprovalRepository,
    ) {
        suspend fun ingest(raw: RawSmsMessage): IncomingSmsIngestionResult {
            val parseResult = smsParserEngine.parseResult(raw)
            val candidate =
                when (parseResult) {
                    is SmsParseResult.Transaction -> parseResult.candidate
                    is SmsParseResult.CardRefund -> parseResult.candidate
                    else -> return IncomingSmsIngestionResult(addedCount = 0, summaryLine = null)
                }
            val added = approvalRepository.insertCandidates(listOf(candidate))
            val line =
                if (added > 0) {
                    formatSummaryLine(
                        amount = candidate.amount,
                        type = candidate.type,
                        sender = candidate.sender,
                        isCardRefund = candidate.approvalKind == SmsApprovalKind.CARD_REFUND,
                    )
                } else {
                    null
                }
            return IncomingSmsIngestionResult(addedCount = added, summaryLine = line)
        }

        private fun formatSummaryLine(
            amount: Double,
            type: TransactionType,
            sender: String,
            isCardRefund: Boolean,
        ): String {
            val typeLabel =
                if (isCardRefund) {
                    "Card refund"
                } else {
                    type.name
                        .lowercase(Locale.getDefault())
                        .replaceFirstChar { ch ->
                            if (ch.isLowerCase()) ch.titlecase(Locale.getDefault()) else ch.toString()
                        }
                }
            val senderLabel = sender.trim().ifEmpty { "Unknown sender" }
            return "${MmrMoneyFormat.formatInrWithSpace(amount)} · $typeLabel · $senderLabel"
        }
    }
