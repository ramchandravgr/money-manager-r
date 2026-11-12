package com.rrparallelstudio.miapp.core.export

import org.dhatim.fastexcel.reader.ReadableWorkbook
import org.dhatim.fastexcel.reader.Row
import java.io.ByteArrayInputStream
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

internal data class SpreadsheetLedgerRow(
    val rowIndex: Int,
    val dateTimeMillis: Long,
    val accountName: String,
    val categoryName: String,
    val subcategoryName: String,
    val note: String,
    val amount: Double,
    val flowLabel: String,
)

internal object PeriodExcelSheetReader {
    private val dateFormats =
        listOf(
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("dd.MM.yy", Locale.getDefault()),
            DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.getDefault()),
            DateTimeFormatter.ofPattern("M/d/yyyy", Locale.US),
        )

    fun readRows(
        data: ByteArray,
        zoneId: ZoneId,
    ): List<SpreadsheetLedgerRow> {
        ByteArrayInputStream(data).use { input ->
            ReadableWorkbook(input).use { workbook ->
                val rows = mutableListOf<SpreadsheetLedgerRow>()
                workbook.firstSheet.openStream().use { stream ->
                    stream.forEach { row ->
                        if (row.rowNum == 1) return@forEach
                        val parsed = parseFixedRow(row, zoneId) ?: return@forEach
                        rows += parsed
                    }
                }
                return rows
            }
        }
    }

    private fun parseFixedRow(
        row: Row,
        zoneId: ZoneId,
    ): SpreadsheetLedgerRow? {
        val account = cellText(row, 1)
        if (account.isEmpty()) return null
        val category = cellText(row, 2)
        val subcategory = cellText(row, 3)
        val note = cellText(row, 4)
        val amount =
            row.getCellAsNumber(5).map { it.toDouble() }.orElse(null)
                ?: cellText(row, 5).toDoubleOrNull()
                ?: return null
        if (amount == 0.0) return null
        val flow = cellText(row, 6)
        if (flow.isEmpty()) return null
        if (flow.equals("transfer-out", ignoreCase = true)) return null
        if (note.equals("example note", ignoreCase = true) && category.equals("food", ignoreCase = true)) {
            return null
        }
        val dateMillis = parseDateRow(row, zoneId) ?: return null
        return SpreadsheetLedgerRow(
            rowIndex = row.rowNum,
            dateTimeMillis = dateMillis,
            accountName = account,
            categoryName = category,
            subcategoryName = subcategory,
            note = note,
            amount = kotlin.math.abs(amount),
            flowLabel = flow,
        )
    }

    private fun cellText(
        row: Row,
        index: Int,
    ): String =
        row
            .getOptionalCell(index)
            .map { it.text.trim() }
            .orElse("")

    private fun parseDateRow(
        row: Row,
        zoneId: ZoneId,
    ): Long? {
        val excelDate = row.getCellAsDate(0).orElse(null)
        if (excelDate != null) {
            return excelDate.atZone(zoneId).toInstant().toEpochMilli()
        }
        val raw = cellText(row, 0)
        if (raw.isEmpty()) return null
        for (fmt in dateFormats) {
            runCatching { LocalDate.parse(raw, fmt) }.getOrNull()?.let { local ->
                return local.atStartOfDay(zoneId).toInstant().toEpochMilli()
            }
        }
        return null
    }
}
