package com.mmr.core.export

import com.mmr.core.database.relation.TransactionWithDetails
import com.mmr.core.model.enums.TransactionType
import com.mmr.core.transaction.TransactionCashFlow
import org.dhatim.fastexcel.Workbook
import java.io.OutputStream
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Date

/**
 * Period Excel export (fastexcel), column layout:
 * Date, Account, Category, Subcategory, Note, Amount, Income/Expense, Description, Payment, Currency, Amount (account).
 */
object PeriodExcelWriter {
    private val headers =
        listOf(
            "Date",
            "Account",
            "Category",
            "Subcategory",
            "Note",
            "Amount",
            "Income/Expense",
            "Description",
            "Payment",
            "Currency",
            "Amount",
        )

    fun write(
        items: List<TransactionWithDetails>,
        zoneId: ZoneId,
        currencySymbol: String,
        currencyCode: String,
        applicationName: String,
        sheetName: String,
        output: OutputStream,
    ) {
        val sorted = items.sortedByDescending { it.transaction.dateTime }
        Workbook(output, applicationName, "1.0").use { workbook ->
            val sheet = workbook.newWorksheet(sanitizeSheetName(sheetName))
            headers.forEachIndexed { col, title ->
                val header = if (col == 5) currencySymbol else title
                sheet.value(0, col, header)
            }
            columnWidths.forEachIndexed { col, width -> sheet.width(col, width) }

            var row = 1
            for (item in sorted) {
                writeRow(sheet, row, item, zoneId, currencyCode)
                row++
            }
        }
    }

    private fun writeRow(
        sheet: org.dhatim.fastexcel.Worksheet,
        row: Int,
        item: TransactionWithDetails,
        zoneId: ZoneId,
        currencyCode: String,
    ) {
        val tx = item.transaction
        val zoned = ZonedDateTime.ofInstant(Instant.ofEpochMilli(tx.dateTime), zoneId)
        val excelDate = Date.from(zoned.toInstant())
        sheet.value(row, 0, excelDate)

        val accountName = item.account?.name ?: ""
        sheet.value(row, 1, accountName)

        val categoryName = item.category?.name ?: ""
        val subcategoryName = item.subcategory?.name ?: ""
        sheet.value(row, 2, categoryName)
        sheet.value(row, 3, subcategoryName)
        sheet.value(row, 4, noteWithTransferSuffix(item))
        sheet.value(row, 5, tx.amount)
        sheet.value(row, 6, incomeExpenseLabel(item))
        sheet.value(row, 7, descriptionLabel(item))
        sheet.value(row, 8, paymentLabel(tx.paymentInstrument, tx.paymentChannel))
        sheet.value(row, 9, currencyCode)
        sheet.value(row, 10, tx.amount)
    }

    private fun noteWithTransferSuffix(item: TransactionWithDetails): String {
        val base = item.transaction.note?.trim() ?: ""
        if (item.transaction.type != TransactionType.TRANSFER) return base
        val dest = item.relatedAccount?.name ?: item.loan?.name ?: return base
        val arrow = " → $dest"
        return if (base.isEmpty()) arrow.trimStart() else base + arrow
    }

    private fun descriptionLabel(item: TransactionWithDetails): String {
        val note = item.transaction.note?.trim()
        if (!note.isNullOrEmpty()) return note
        return item.category?.name ?: ""
    }

    private fun paymentLabel(
        instrument: String?,
        channel: String?,
    ): String {
        instrument?.trim()?.takeIf { it.isNotEmpty() }?.let { return it }
        channel?.trim()?.takeIf { it.isNotEmpty() }?.let { return it }
        return ""
    }

    private fun incomeExpenseLabel(item: TransactionWithDetails): String {
        val tx = item.transaction
        val loanType = item.loan?.loanType
        return when {
            TransactionCashFlow.countsAsIncome(tx, loanType) -> "Income"
            TransactionCashFlow.countsAsExpense(tx, loanType) -> "Expense"
            tx.type == TransactionType.INCOME -> "Income"
            tx.type == TransactionType.EXPENSE -> "Expense"
            tx.type == TransactionType.TRANSFER -> "Transfer-Out"
            tx.type == TransactionType.LOAN_PAYMENT -> "Expense"
            tx.type == TransactionType.ADJUSTMENT -> "Expense Balance"
            else -> ""
        }
    }

    private fun sanitizeSheetName(raw: String): String {
        val cleaned = raw.replace(Regex("[/\\\\?*\\[\\]:]"), "-")
        return if (cleaned.length <= 31) cleaned else cleaned.substring(0, 31)
    }

    private val columnWidths =
        doubleArrayOf(
            20.0,
            15.0,
            15.0,
            15.0,
            30.0,
            15.0,
            12.0,
            30.0,
            10.0,
            10.0,
            10.0,
        )
}
