package com.mmr.core.parser

import com.mmr.core.model.enums.SmsApprovalKind
import com.mmr.core.parser.engine.SmsParserEngine
import com.mmr.core.parser.model.RawSmsMessage
import com.mmr.core.parser.model.SmsParseResult
import com.mmr.core.parser.template.SmsTemplateRegistry
import com.mmr.core.parser.template.SmsTestAssets
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CardRefundParserTest {
    private lateinit var engine: SmsParserEngine

    @Before
    fun setUp() {
        engine = SmsParserEngine(SmsTemplateRegistry.fromJson(SmsTestAssets.loadTemplatesJson()))
    }

    @Test
    fun hdfcMerchantReversal_parsesAsCardRefund() {
        val result =
            engine.parseResult(
                RawSmsMessage(
                    id = "1",
                    sender = "AD-HDFCBK-S",
                    body =
                        "Transaction Reversed!On HDFC Bank CREDIT Card xx4148 Amt: Rs.1470 " +
                            "By AMAZON                000 On 2026-05-24:03:18:24",
                    timestamp = 1L,
                ),
            )
        assertTrue(result is SmsParseResult.CardRefund)
        val candidate = (result as SmsParseResult.CardRefund).candidate
        assertEquals(1470.0, candidate.amount, 0.01)
        assertEquals(SmsApprovalKind.CARD_REFUND, candidate.approvalKind)
    }

    @Test
    fun iciciOnlineRefund_parsesAsCardRefund_notStatement() {
        val result =
            engine.parseResult(
                RawSmsMessage(
                    id = "2",
                    sender = "AD-ICICIT-S",
                    body =
                        "Online Refund For UPI Ecom Trxn refund of Rs 77.00 credited to " +
                            "ICICI Bank Credit Card XX4007 on 26-MAY-26. Revised total due Rs 0, minimum due Rs .00",
                    timestamp = 1L,
                ),
            )
        assertTrue(result is SmsParseResult.CardRefund)
    }
}
