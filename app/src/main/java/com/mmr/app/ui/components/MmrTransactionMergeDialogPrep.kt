package com.mmr.app.ui.components

import com.mmr.core.common.display.MmrMoneyFormat

data class MmrTransactionMergePrepResult(
    val dateOptions: List<MmrMergeDateOption>,
    val initialSelectedDateTimeMillis: Long?,
    val combinedAmountLabel: String,
)

/** Shared prep for [MmrTransactionMergeDialog] (home ledger merge and future multi-select merges). */
object MmrTransactionMergeDialogPrep {
    fun fromTransactionDates(
        dateTimes: List<Long>,
        totalAmount: Double,
        currencySymbol: String,
        formatDateLabel: (Long) -> String,
    ): MmrTransactionMergePrepResult {
        val distinctDates = dateTimes.distinct().sortedDescending()
        val options =
            distinctDates.map { millis ->
                MmrMergeDateOption(
                    dateTimeMillis = millis,
                    label = formatDateLabel(millis),
                )
            }
        return MmrTransactionMergePrepResult(
            dateOptions = options,
            initialSelectedDateTimeMillis = distinctDates.firstOrNull(),
            combinedAmountLabel = MmrMoneyFormat.format(totalAmount, currencySymbol),
        )
    }

    fun mergedDateTimeForConfirm(
        dateOptionCount: Int,
        selectedMillis: Long?,
    ): Long? = if (dateOptionCount > 1) selectedMillis else null
}
