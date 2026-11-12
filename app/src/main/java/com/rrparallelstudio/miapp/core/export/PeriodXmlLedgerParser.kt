package com.rrparallelstudio.miapp.core.export

import org.xmlpull.v1.XmlPullParser
import java.io.ByteArrayInputStream
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

internal object PeriodXmlLedgerParser {
    private val isoParser: DateTimeFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME

    fun readRows(data: ByteArray): List<SpreadsheetLedgerRow> {
        val parser: XmlPullParser = android.util.Xml.newPullParser()
        parser.setInput(ByteArrayInputStream(data), "UTF-8")
        val rows = mutableListOf<SpreadsheetLedgerRow>()
        var event = parser.eventType
        var rowIndex = 0
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG && parser.name == "Transaction") {
                rowIndex++
                val flow = parser.getAttributeValue(null, "flow").orEmpty()
                if (flow.equals("Transfer-Out", ignoreCase = true)) {
                    event = parser.next()
                    continue
                }
                val amount =
                    parser.getAttributeValue(null, "amount")?.toDoubleOrNull()
                        ?: continue
                if (amount == 0.0) continue
                val dateRaw = parser.getAttributeValue(null, "dateTime") ?: continue
                val millis =
                    runCatching { ZonedDateTime.parse(dateRaw, isoParser).toInstant().toEpochMilli() }
                        .getOrNull() ?: continue
                val account = parser.getAttributeValue(null, "account").orEmpty()
                if (account.isEmpty()) continue
                rows +=
                    SpreadsheetLedgerRow(
                        rowIndex = rowIndex,
                        dateTimeMillis = millis,
                        accountName = account,
                        categoryName = parser.getAttributeValue(null, "category").orEmpty(),
                        subcategoryName = parser.getAttributeValue(null, "subcategory").orEmpty(),
                        note = parser.getAttributeValue(null, "note").orEmpty(),
                        amount = kotlin.math.abs(amount),
                        flowLabel = flow,
                    )
            }
            event = parser.next()
        }
        return rows
    }
}
