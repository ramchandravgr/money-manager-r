package com.rrparallelstudio.miapp.core.export

import com.rrparallelstudio.miapp.core.database.relation.TransactionWithDetails
import com.rrparallelstudio.miapp.core.model.enums.TransactionType
import com.rrparallelstudio.miapp.core.transaction.TransactionCashFlow
import org.xmlpull.v1.XmlSerializer
import java.io.OutputStream
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Ledger period export as XML (same fields as [PeriodExcelWriter]). */
object PeriodXmlWriter {
    private val isoDateTime: DateTimeFormatter =
        DateTimeFormatter.ISO_OFFSET_DATE_TIME

    fun write(
        items: List<TransactionWithDetails>,
        zoneId: ZoneId,
        currencyCode: String,
        applicationName: String,
        output: OutputStream,
    ) {
        val sorted = items.sortedByDescending { it.transaction.dateTime }
        val serializer: XmlSerializer = android.util.Xml.newSerializer()
        serializer.setOutput(output, "UTF-8")
        serializer.startDocument("UTF-8", true)
        serializer.startTag(null, "MoneyInboxLedgerExport")
        serializer.attribute(null, "app", applicationName)
        serializer.attribute(null, "currency", currencyCode)
        serializer.attribute(null, "count", sorted.size.toString())
        for (item in sorted) {
            writeTransaction(serializer, item, zoneId)
        }
        serializer.endTag(null, "MoneyInboxLedgerExport")
        serializer.endDocument()
        serializer.flush()
    }

    private fun writeTransaction(
        serializer: XmlSerializer,
        item: TransactionWithDetails,
        zoneId: ZoneId,
    ) {
        val tx = item.transaction
        val zoned = Instant.ofEpochMilli(tx.dateTime).atZone(zoneId)
        serializer.startTag(null, "Transaction")
        serializer.attribute(null, "dateTime", isoDateTime.format(zoned))
        serializer.attribute(null, "account", item.account?.name ?: "")
        serializer.attribute(null, "category", item.category?.name ?: "")
        serializer.attribute(null, "subcategory", item.subcategory?.name ?: "")
        serializer.attribute(null, "note", tx.note ?: "")
        serializer.attribute(null, "amount", tx.amount.toString())
        serializer.attribute(null, "flow", incomeExpenseLabel(item))
        serializer.attribute(null, "payment", paymentLabel(tx.paymentInstrument, tx.paymentChannel))
        if (item.transaction.type == TransactionType.TRANSFER) {
            item.relatedAccount?.name?.let { serializer.attribute(null, "transferTo", it) }
        }
        serializer.endTag(null, "Transaction")
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
}
