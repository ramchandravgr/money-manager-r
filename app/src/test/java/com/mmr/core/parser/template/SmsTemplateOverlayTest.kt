package com.mmr.core.parser.template

import com.mmr.core.model.enums.TransactionType
import com.mmr.core.parser.model.SmsParseIntent
import com.mmr.core.parser.model.SmsTemplate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.regex.Pattern

class SmsTemplateOverlayTest {
    @Test
    fun merge_overlayReplacesBuiltInByIdAndSortsByPriority() {
        val builtIn =
            listOf(
                template("builtin_a", priority = 10),
                template("shared", priority = 50),
            )
        val overlay =
            listOf(
                template("overlay_only", priority = 100),
                template("shared", priority = 80, bodyRegex = "overlay-regex"),
            )
        val merged = SmsTemplateJsonCodec.mergeBuiltInWithOverlay(builtIn, overlay)
        assertEquals(listOf("overlay_only", "shared", "builtin_a"), merged.map { it.id })
        assertEquals("overlay-regex", merged.first { it.id == "shared" }.bodyPattern.pattern())
    }

    @Test
    fun parseOverlayJson_rejectsInvalidRegex() {
        val json =
            """
            {
              "templates": [
                {
                  "id": "bad",
                  "senderSuffixes": [],
                  "priority": 50,
                  "bodyRegex": "(unclosed",
                  "transactionType": "EXPENSE"
                }
              ]
            }
            """.trimIndent()
        val result = SmsTemplateJsonCodec.parseOverlayJson(json)
        assertFalse(result.ok)
        assertTrue(result.errors.any { it.templateId == "bad" })
    }

    @Test
    fun parseOverlayJson_rejectsDuplicateIdsInFile() {
        val json =
            """
            {
              "templates": [
                {
                  "id": "dup",
                  "senderSuffixes": [],
                  "priority": 50,
                  "bodyRegex": "inr (?<amount>[0-9]+)",
                  "transactionType": "EXPENSE"
                },
                {
                  "id": "dup",
                  "senderSuffixes": [],
                  "priority": 40,
                  "bodyRegex": "inr (?<amount>[0-9]+)",
                  "transactionType": "EXPENSE"
                }
              ]
            }
            """.trimIndent()
        val result = SmsTemplateJsonCodec.parseOverlayJson(json)
        assertFalse(result.ok)
        assertTrue(result.errors.any { it.message.contains("Duplicate") })
    }

    @Test
    fun parseOverlayJson_allowsEmptyTemplatesArray() {
        val result = SmsTemplateJsonCodec.parseOverlayJson("""{"templates":[]}""")
        assertTrue(result.ok)
        assertTrue(result.templates.isEmpty())
    }

    private fun template(
        id: String,
        priority: Int,
        bodyRegex: String = "inr (?<amount>[0-9]+)",
    ): SmsTemplate =
        SmsTemplate(
            id = id,
            senderSuffixes = emptyList(),
            priority = priority,
            bodyPattern = Pattern.compile(bodyRegex, Pattern.CASE_INSENSITIVE),
            transactionType = TransactionType.EXPENSE,
            paymentChannel = null,
            instrument = null,
            intent = SmsParseIntent.SETTLED,
        )
}
