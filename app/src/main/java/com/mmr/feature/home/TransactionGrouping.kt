package com.mmr.feature.home

import com.mmr.core.common.validation.MmrValidation
import com.mmr.core.database.relation.TransactionWithDetails
import com.mmr.core.model.enums.TransactionType
import com.mmr.feature.home.model.TransactionGroupUi
import com.mmr.feature.home.model.TransactionRowUi
import java.text.SimpleDateFormat
import java.time.ZoneId
import java.util.Date
import java.util.Locale

object TransactionGrouping {

    fun groupByDate(
        items: List<TransactionWithDetails>,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): List<TransactionGroupUi> {
        val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
        val groups = linkedMapOf<java.time.LocalDate, MutableList<TransactionRowUi>>()
        val daySums = linkedMapOf<java.time.LocalDate, Double>()

        for (item in items) {
            if (!MmrValidation.isValidEpochMillis(item.transaction.dateTime)) continue
            val localDate = HomePeriodUtils.toLocalDate(item.transaction.dateTime, zoneId)
            val isIncome = item.transaction.type == TransactionType.INCOME
            val sign = if (isIncome) "+" else "-"
            val categoryName = item.category?.name ?: "Uncategorized"
            val accountName = item.account?.name ?: "Account"
            val title = item.transaction.note?.trim()?.takeIf { it.isNotEmpty() } ?: categoryName
            val row = TransactionRowUi(
                id = item.transaction.id,
                title = title,
                subtitle = "$categoryName • $accountName",
                amountFormatted = "$sign₹${"%.2f".format(item.transaction.amount)}",
                isIncome = isIncome,
                timeLabel = timeFormat.format(Date(item.transaction.dateTime))
            )
            groups.getOrPut(localDate) { mutableListOf() }.add(row)
            val netDelta = if (isIncome) item.transaction.amount else -item.transaction.amount
            daySums[localDate] = daySums.getOrDefault(localDate, 0.0) + netDelta
        }

        return groups.entries
            .sortedByDescending { it.key }
            .map { (localDate, rows) ->
                TransactionGroupUi(
                    dateLabel = HomePeriodUtils.transactionGroupDateLabel(localDate),
                    dayTotal = daySums.getOrDefault(localDate, 0.0),
                    items = rows
                )
            }
    }
}
