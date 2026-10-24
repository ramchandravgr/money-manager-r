package com.mmr.core.parser

import com.mmr.core.model.enums.SmsApprovalKind
import com.mmr.core.parser.engine.SmsParserEngine
import com.mmr.core.parser.model.RawSmsMessage
import com.mmr.core.parser.model.SmsParseResult
import com.mmr.core.parser.template.SmsTemplateRegistry
import com.mmr.core.parser.template.SmsTestAssets
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import kotlin.math.abs

/**
 * Offline benchmark: Mezo AAAA1 ledger rows (tools/mezo_ledger_export.csv) vs [SmsParserEngine].
 * Regenerate CSV from emulator bugle_db after Mezo summarization; run this test locally (not CI).
 */
class MezoLedgerParserComparisonTest {
    private lateinit var engine: SmsParserEngine

    @Before
    fun setUp() {
        engine = SmsParserEngine(SmsTemplateRegistry.fromJson(SmsTestAssets.loadTemplatesJson()))
    }

    @Test
    fun mezoLedgerRows_mostParseAsMmrTransaction() {
        val rows = loadLedgerRows()
        if (rows.isEmpty()) {
            println("Skip Mezo compare: add app/src/test/resources/mezo_ledger_export.csv")
            return
        }
        var txn = 0
        var cardRefund = 0
        var informational = 0
        var ignored = 0
        var amountMismatch = 0
        val ignoredSamples = mutableListOf<String>()
        val infoSamples = mutableListOf<String>()
        for (row in rows) {
            val amount = row["amount"]?.toDoubleOrNull() ?: continue
            val body = row["body"]?.trim() ?: continue
            if (body.isEmpty()) continue
            val sender = row["address"]?.trim().orEmpty().ifEmpty { row["sender_id"]?.trim() ?: "" }
            val ts = row["date_ms"]?.toLongOrNull() ?: 0L
            val mezoId = row["mezo_id"] ?: "0"
            val result =
                engine.parseResult(
                    RawSmsMessage(
                        id = mezoId,
                        sender = sender,
                        body = body,
                        timestamp = ts,
                    ),
                )
            when (result) {
                is SmsParseResult.Transaction -> {
                    txn++
                    if (abs(result.candidate.amount - amount) > 0.05) {
                        amountMismatch++
                    }
                }
                is SmsParseResult.CardRefund -> {
                    cardRefund++
                    if (result.candidate.approvalKind != SmsApprovalKind.CARD_REFUND) {
                        amountMismatch++
                    }
                    if (abs(result.candidate.amount - amount) > 0.05) {
                        amountMismatch++
                    }
                }
                is SmsParseResult.Informational -> {
                    informational++
                    if (infoSamples.size < 25) {
                        infoSamples.add("info mezo=$amount ($sender) ${body.take(140)}")
                    }
                }
                is SmsParseResult.Ignored -> {
                    ignored++
                    if (ignoredSamples.size < 40) {
                        ignoredSamples.add("ignored mezo=$amount ($sender) ${body.take(140)}")
                    }
                }
            }
        }
        val total = rows.size
        val report =
            buildString {
                appendLine("Mezo AAAA1 rows: $total")
                appendLine("MMR Transaction: $txn (${pct(txn, total)})")
                appendLine("MMR Card refund: $cardRefund (${pct(cardRefund, total)})")
                appendLine("MMR Informational: $informational (${pct(informational, total)})")
                appendLine("MMR Ignored: $ignored (${pct(ignored, total)})")
                appendLine("Amount mismatch (txn but wrong amt): $amountMismatch")
                appendLine("--- Informational samples ---")
                infoSamples.forEach { appendLine(it) }
                appendLine("--- Ignored samples ---")
                ignoredSamples.forEach { appendLine(it) }
            }
        writeReport(report)
        println(report)
        assertTrue(
            "MMR should parse most Mezo ledger rows as txn or card refund (got ${txn + cardRefund}/$total)",
            (txn + cardRefund).toDouble() / total.coerceAtLeast(1) >= 0.88,
        )
    }

    private fun pct(
        n: Int,
        total: Int,
    ): String = if (total == 0) "n/a" else "${(100.0 * n / total).toInt()}%"

    private fun loadLedgerRows(): List<Map<String, String>> {
        val override = System.getenv("MMR_MEZO_LEDGER_CSV")
        if (!override.isNullOrBlank()) {
            val f = File(override)
            if (f.exists()) return readCsv(f)
        }
        val fromTools = File("tools/mezo_ledger_export.csv")
        if (fromTools.exists()) return readCsv(fromTools)
        val stream =
            javaClass.classLoader.getResourceAsStream("mezo_ledger_export.csv")
                ?: return emptyList()
        return readCsvText(stream.bufferedReader().readText())
    }

    private fun writeReport(report: String) {
        File("../tools/mezo_mmr_gap_report.txt").writeText(report)
    }

    private fun readCsv(file: File): List<Map<String, String>> = readCsvText(file.readText())

    private fun readCsvText(text: String): List<Map<String, String>> {
        val lines = text.lines()
        if (lines.isEmpty()) return emptyList()
        val header = parseCsvLine(lines.first())
        return lines.drop(1).mapNotNull { line ->
            if (line.isBlank()) return@mapNotNull null
            val cols = parseCsvLine(line)
            if (cols.size < header.size) return@mapNotNull null
            header.indices.associate { i -> header[i] to cols.getOrElse(i) { "" } }
        }
    }

    private fun parseCsvLine(line: String): List<String> {
        val out = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            when {
                inQuotes && c == '"' && i + 1 < line.length && line[i + 1] == '"' -> {
                    sb.append('"')
                    i += 2
                    continue
                }
                c == '"' -> {
                    inQuotes = !inQuotes
                }
                c == ',' && !inQuotes -> {
                    out.add(sb.toString())
                    sb.clear()
                }
                else -> sb.append(c)
            }
            i++
        }
        out.add(sb.toString())
        return out
    }
}
