package com.mmr.app.widget

import com.mmr.core.common.display.MmrMoneyFormat
import com.mmr.core.common.validation.MmrValidation
import com.mmr.core.domain.model.PendingApprovalWithSms
import com.mmr.core.model.enums.SmsApprovalKind
import com.mmr.core.model.enums.TransactionType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal object MmrWidgetApprovalMapper {
    fun toHeadline(
        row: PendingApprovalWithSms,
        currencySymbol: String,
    ): MmrApprovalTriageHeadline {
        val entity = row.approval
        val isCardRefund = entity.approvalKind == SmsApprovalKind.CARD_REFUND
        val isIncome = entity.extractedType == TransactionType.INCOME
        val sign = if (isIncome && !isCardRefund) "+" else ""
        val amountFormatted =
            if (isCardRefund) {
                MmrMoneyFormat.format(entity.extractedAmount, currencySymbol)
            } else {
                "$sign${MmrMoneyFormat.format(entity.extractedAmount, currencySymbol)}"
            }
        val note =
            entity.extractedNote?.takeIf { it.isNotBlank() }
                ?: entity.extractedMerchant?.takeIf { it.isNotBlank() }
                ?: entity.extractedSender
        val dateLabel = formatDate(entity.extractedDateTime)
        val typeLabel =
            if (isCardRefund) {
                "Card refund"
            } else {
                entity.extractedType.name
                    .lowercase(
                        Locale.getDefault(),
                    ).replaceFirstChar { it.titlecase(Locale.getDefault()) }
            }
        return MmrApprovalTriageHeadline(
            approvalId = entity.id,
            amountFormatted = amountFormatted,
            subtitle = note.take(80),
            metaLine = "$typeLabel · $dateLabel",
            isCardRefund = isCardRefund,
            canQuickApprove = !isCardRefund,
        )
    }

    fun pickHeadlineRow(
        pending: List<PendingApprovalWithSms>,
        deferStore: MmrWidgetDeferStore,
    ): PendingApprovalWithSms? {
        if (pending.isEmpty()) return null
        val notDeferred = pending.filter { !deferStore.isDeferred(it.approval.id) }
        val queue = if (notDeferred.isNotEmpty()) notDeferred else pending
        return queue.first()
    }

    private fun formatDate(epochMillis: Long): String {
        if (!MmrValidation.isValidEpochMillis(epochMillis)) return "Unknown date"
        val format = SimpleDateFormat("dd MMM", Locale.getDefault())
        return format.format(Date(epochMillis))
    }
}
