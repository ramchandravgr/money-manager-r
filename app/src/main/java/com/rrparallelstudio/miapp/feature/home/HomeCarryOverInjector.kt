package com.rrparallelstudio.miapp.feature.home

import com.rrparallelstudio.miapp.feature.home.model.TransactionGroupUi
import com.rrparallelstudio.miapp.feature.home.model.TransactionRowUi
import com.rrparallelstudio.miapp.feature.home.ui.formatRupeeCarryOver
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/** Inserts a read-only carry-over row at the first day of the ledger period. */
object HomeCarryOverInjector {
    const val ROW_ID = -9001L

    fun isCarryOverRow(id: Long): Boolean = id == ROW_ID

    fun rowForAmount(
        amount: Double,
        currencySymbol: String = "₹",
    ): TransactionRowUi {
        val amountLabel = formatRupeeCarryOver(amount)
        return TransactionRowUi(
            id = ROW_ID,
            title = "Carry-over",
            categoryLabel = "Carry-over",
            detailLine = "Balance from before this period",
            subtitle = "",
            amountLabel = amountLabel,
            amountFormatted = amountLabel,
            isIncome = amount >= 0.0,
            timeLabel = "",
            isSynthetic = true,
        )
    }

    fun injectIntoGrouped(
        groups: List<TransactionGroupUi>,
        periodStart: LocalDate,
        carriedForward: Double,
        withinDayAscending: Boolean,
        currencySymbol: String = "₹",
    ): List<TransactionGroupUi> {
        val row = rowForAmount(carriedForward, currencySymbol)
        val dateKey = periodStart.toString()
        val withoutExistingSynthetic =
            groups.map { group ->
                group.copy(items = group.items.filter { !isCarryOverRow(it.id) })
            }
        val existing = withoutExistingSynthetic.find { it.dateKey == dateKey }
        return if (existing != null) {
            val mergedItems = mergeRowIntoDay(existing.items, row, withinDayAscending)
            withoutExistingSynthetic.map { group ->
                if (group.dateKey == dateKey) group.copy(items = mergedItems) else group
            }
        } else {
            val locale = Locale.getDefault()
            val newGroup =
                TransactionGroupUi(
                    dateKey = dateKey,
                    dayOfMonth = periodStart.dayOfMonth,
                    weekdayShort = periodStart.dayOfWeek.getDisplayName(TextStyle.SHORT, locale),
                    monthYearLabel = "${periodStart.monthValue}.${periodStart.year}",
                    dayExpenseTotal = 0.0,
                    dayIncomeTotal = 0.0,
                    items = listOf(row),
                )
            (withoutExistingSynthetic + newGroup).sortedByDescending { it.dateKey }
        }
    }

    fun injectIntoGroup(
        group: TransactionGroupUi,
        carriedForward: Double,
        withinDayAscending: Boolean,
        currencySymbol: String = "₹",
    ): TransactionGroupUi {
        val row = rowForAmount(carriedForward, currencySymbol)
        val itemsWithoutSynthetic = group.items.filter { !isCarryOverRow(it.id) }
        return group.copy(items = mergeRowIntoDay(itemsWithoutSynthetic, row, withinDayAscending))
    }

    private fun mergeRowIntoDay(
        items: List<TransactionRowUi>,
        row: TransactionRowUi,
        withinDayAscending: Boolean,
    ): List<TransactionRowUi> =
        if (withinDayAscending) {
            listOf(row) + items
        } else {
            items + row
        }
}
