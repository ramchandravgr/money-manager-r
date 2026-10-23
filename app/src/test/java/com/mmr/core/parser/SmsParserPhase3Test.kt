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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SmsParserPhase3Test {
    private lateinit var engine: SmsParserEngine

    @Before
    fun setUp() {
        engine = SmsParserEngine(SmsTemplateRegistry.fromJson(SmsTestAssets.loadTemplatesJson()))
    }

    @Test
    fun parseResult_mandateIsInformational_notTransaction() {
        val raw =
            RawSmsMessage(
                id = "1",
                sender = "VM-AXISBK",
                body = "For the upcoming mandate set for 01-02-2026, INR 500 will be debited from your a/c towards Paytm Money Limited",
                timestamp = 1_700_000_000_000L,
            )
        val result = engine.parseResult(raw)
        assertTrue(result is SmsParseResult.Informational)
        val info = result as SmsParseResult.Informational
        assertEquals(SmsParseIntent.MANDATE, info.intent)
        assertNull(engine.parse(raw))
        assertEquals(500.0, info.amount!!, 0.01)
    }

    @Test
    fun parseResult_settledTransactionStillWorks() {
        val raw =
            RawSmsMessage(
                id = "2",
                sender = "VM-HDFCBK",
                body = "HDFC Bank: Rs 499 debited from a/c **5678 on 01-01-2026 to VPA zepto@ybl (UPI Ref No 123456789012)",
                timestamp = 1_700_000_000_000L,
            )
        val result = engine.parseResult(raw)
        assertTrue(result is SmsParseResult.Transaction)
        val txn = (result as SmsParseResult.Transaction).candidate
        assertEquals(TransactionType.EXPENSE, txn.type)
        assertEquals("hdfc_upi_debit", txn.templateId)
    }

    @Test
    fun classifier_statementIntent() {
        val body = "e-statement of sbi credit card ending xx1234 dated 01/01/2026 has been mailed. total amt due rs 4500"
        assertEquals(SmsParseIntent.STATEMENT, SmsClassifier.detectInformational(body))
    }

    @Test
    fun parseResult_creditCardBillPayment_isInformational_notTransaction() {
        val raw =
            RawSmsMessage(
                id = "3",
                sender = "VM-HDFCBK-S",
                body = "DEAR HDFCBANK CARDMEMBER, PAYMENT OF Rs. 6219.00 RECEIVED TOWARDS YOUR CREDIT CARD ENDING WITH 4148 ON 15-2-2026.YOUR AVAILABLE LIMIT IS RS. 241999.95",
                timestamp = 1_700_000_000_000L,
            )
        val result = engine.parseResult(raw)
        assertTrue(result is SmsParseResult.Informational)
        val info = result as SmsParseResult.Informational
        assertEquals(SmsParseIntent.CARD_PAYMENT, info.intent)
        assertNull(engine.parse(raw))
        assertEquals(6219.0, info.amount!!, 0.01)
    }

    @Test
    fun parseResult_hdfcUpiP2pDebit_isSettledTransaction() {
        val raw =
            RawSmsMessage(
                id = "4",
                sender = "VM-HDFCBK-S",
                body = "HDFC Bank:Rs. 150.00 debited from a/c *0561 on 26/06/26 to a/c **8273 (UPI Ref No. 654301971766). Not you? Call on 18002586161 to report",
                timestamp = 1_700_000_000_000L,
            )
        val result = engine.parseResult(raw)
        assertTrue(result is SmsParseResult.Transaction)
        assertEquals("hdfc_upi_debit_p2p", (result as SmsParseResult.Transaction).candidate.templateId)
    }

    @Test
    fun parseResult_iciciCcUpiDebit_isSettledTransaction() {
        val raw =
            RawSmsMessage(
                id = "5",
                sender = "VM-ICICIT-S",
                body = "ICICI Bank Credit Card XX4007 debited for INR 50.00 on 25-Mar-26 for UPI-601551381212-BOYAPALL. To dispute call 18001080/SMS BLOCK 4007 to 9215676766",
                timestamp = 1_700_000_000_000L,
            )
        val result = engine.parseResult(raw)
        assertTrue(result is SmsParseResult.Transaction)
        val txn = (result as SmsParseResult.Transaction).candidate
        assertEquals("icici_cc_upi_debit", txn.templateId)
        assertEquals("boyapall", txn.merchant)
    }
}
