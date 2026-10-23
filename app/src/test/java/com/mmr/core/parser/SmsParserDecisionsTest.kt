package com.mmr.core.parser

import com.mmr.core.model.enums.TransactionType
import com.mmr.core.parser.engine.SmsClassifier
import com.mmr.core.parser.engine.SmsParserEngine
import com.mmr.core.parser.model.RawSmsMessage
import com.mmr.core.parser.model.SmsParseIntent
import com.mmr.core.parser.model.SmsParseResult
import com.mmr.core.parser.template.SmsTemplateRegistry
import com.mmr.core.parser.template.SmsTestAssets
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SmsParserDecisionsTest {
    private lateinit var engine: SmsParserEngine

    @Before
    fun setUp() {
        engine = SmsParserEngine(SmsTemplateRegistry.fromJson(SmsTestAssets.loadTemplatesJson()))
    }

    @Test
    fun ipoBlock_isInformational_notTransaction() {
        val raw =
            RawSmsMessage(
                id = "1",
                sender = "VM-HDFCBK",
                body = "IPO application: amount blocked Rs 15000 for ASBA. Funds will be released if not allotted.",
                timestamp = 1_700_000_000_000L,
            )
        val result = engine.parseResult(raw)
        assertTrue(result is SmsParseResult.Informational)
        assertEquals(SmsParseIntent.INVESTMENT, (result as SmsParseResult.Informational).intent)
    }

    @Test
    fun sipSuccess_isPendingTransaction() {
        val raw =
            RawSmsMessage(
                id = "2",
                sender = "VM-HDFCMF",
                body = "Dear investor, your SIP purchase of Rs.5000 in folio 123 in fund abc has been processed for nav of Rs.100",
                timestamp = 1_700_000_000_000L,
            )
        val result = engine.parseResult(raw)
        assertTrue(result is SmsParseResult.Transaction)
        assertEquals("mf_sip_success", (result as SmsParseResult.Transaction).candidate.templateId)
    }

    @Test
    fun cashbackReview_isLowConfidenceIncome() {
        val raw =
            RawSmsMessage(
                id = "3",
                sender = "VM-HDFCBK",
                body = "Rs 75 cashback has been credited to your account **1234",
                timestamp = 1_700_000_000_000L,
            )
        val result = engine.parseResult(raw)
        assertTrue(result is SmsParseResult.Transaction)
        val txn = (result as SmsParseResult.Transaction).candidate
        assertEquals(TransactionType.INCOME, txn.type)
        assertTrue(txn.confidenceScore <= 45)
        assertEquals("Cashback (confirm)", txn.categoryHint)
    }

    @Test
    fun happyCoins_ignored() {
        assertTrue(SmsClassifier.isIgnored("cashback of happy coins worth rs 50 credited"))
    }
}
