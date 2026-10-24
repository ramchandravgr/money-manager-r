package com.mmr.feature.home

import com.mmr.core.common.display.MmrMoneyFormat
import com.mmr.core.common.display.TransactionCategoryLabels
import com.mmr.core.database.relation.TransactionWithDetails
import com.mmr.core.parser.model.SmsInstrument
import com.mmr.core.transaction.TransactionCashFlow
import com.mmr.feature.home.model.TransactionRowUi
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object TransactionRowMapping {
    fun fromDetails(
        item: TransactionWithDetails,
        timeFormat: SimpleDateFormat,
        showDescription: Boolean = true,
        showTime: Boolean = false,
        currencySymbol: String = "₹",
    ): TransactionRowUi {
        val loanType = item.loan?.loanType
        val isIncome = TransactionCashFlow.countsAsIncome(item.transaction, loanType)
        val categoryName = item.category?.name ?: "Uncategorized"
        val subcategoryName = item.subcategory?.name
        val categoryDisplay = TransactionCategoryLabels.format(categoryName, subcategoryName)
        val accountName = item.account?.name ?: "Account"
        val note =
            item.transaction.note
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
        val title = if (showDescription) note ?: categoryDisplay else categoryDisplay
        val paymentLabel = formatPaymentInstrument(item.transaction.paymentInstrument)
        val detailLine =
            buildList {
                item.loan?.name?.let { add(it) }
                add(accountName)
                paymentLabel?.let { add(it) }
            }.joinToString(" • ")
        val metaParts =
            buildList {
                add(categoryDisplay)
                if (paymentLabel != null) add(paymentLabel)
            }
        val amountLabel = MmrMoneyFormat.formatWithSpace(item.transaction.amount, currencySymbol)
        return TransactionRowUi(
            id = item.transaction.id,
            title = title,
            categoryLabel = categoryDisplay,
            detailLine = detailLine,
            subtitle = metaParts.joinToString(" • "),
            amountLabel = amountLabel,
            amountFormatted = amountLabel,
            isIncome = isIncome,
            timeLabel = if (showTime) timeFormat.format(Date(item.transaction.dateTime)) else "",
        )
    }

    private fun formatPaymentInstrument(raw: String?): String? {
        raw ?: return null
        return runCatching { SmsInstrument.valueOf(raw) }
            .getOrNull()
            ?.name
            ?.replace('_', ' ')
            ?.lowercase(Locale.getDefault())
            ?.replaceFirstChar { it.titlecase(Locale.getDefault()) }
            ?: raw
    }
}
