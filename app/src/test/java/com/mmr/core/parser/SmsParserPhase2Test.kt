package com.mmr.core.parser

import com.mmr.core.model.enums.TransactionType
import com.mmr.core.parser.engine.SmsParserEngine
import com.mmr.core.parser.model.RawSmsMessage
import com.mmr.core.parser.model.SmsInstrument
import com.mmr.core.parser.model.SmsPaymentChannel
import com.mmr.core.parser.template.SmsTemplateRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import com.mmr.core.parser.template.SmsTestAssets

class SmsParserPhase2Test {

    private lateinit var engine: SmsParserEngine

    @Before
    fun setUp() {
        engine = SmsParserEngine(SmsTemplateRegistry.fromJson(SmsTestAssets.loadTemplatesJson()))
    }

    @Test
    fun template_hdfcUpiDebit_setsMetadata() {
        val parsed = engine.parse(
            RawSmsMessage(
                id = "1",
                sender = "VM-HDFCBK",
                body = "HDFC Bank: Rs 499 debited from a/c **5678 on 01-01-2026 to VPA zepto@ybl (UPI Ref No 123456789012)",
                timestamp = 1_700_000_000_000L
            )
        )
        assertNotNull(parsed)
        assertEquals("hdfc_upi_debit", parsed!!.templateId)
        assertEquals(499.0, parsed.amount, 0.01)
        assertEquals(SmsPaymentChannel.UPI, parsed.paymentChannel)
        assertEquals(SmsInstrument.SAVINGS, parsed.instrument)
        assertEquals("zepto", parsed.merchant?.lowercase())
        assertEquals("5678", parsed.accountLastDigits)
        assertEquals(TransactionType.EXPENSE, parsed.type)
        assertEquals(true, parsed.confidenceScore >= 80)
    }

    @Test
    fun template_bobCardSpend() {
        val parsed = engine.parse(
            RawSmsMessage(
                id = "2",
                sender = "AD-BOBCRD-S",
                body = "Alert: INR 320 is spent on your bobcard ending 4321 at Zepto on 12-10-2025. Available credit limit is Rs 50000",
                timestamp = 1_700_000_000_000L
            )
        )
        assertNotNull(parsed)
        assertEquals("bobcard_spend", parsed!!.templateId)
        assertEquals(320.0, parsed.amount, 0.01)
        assertEquals(SmsInstrument.CREDIT_CARD, parsed.instrument)
    }

    @Test
    fun template_pluxeeMealWallet() {
        val parsed = engine.parse(
            RawSmsMessage(
                id = "3",
                sender = "VM-PLUXEE",
                body = "Rs. 250 spent from Pluxee meal card wallet, card no.xx9999 on 01-01-2026 12:00:00 at zepto . Avl bal Rs.1000.",
                timestamp = 1_700_000_000_000L
            )
        )
        assertNotNull(parsed)
        assertEquals("pluxee_meal_spend", parsed!!.templateId)
        assertEquals(SmsInstrument.MEAL_WALLET, parsed.instrument)
    }

    @Test
    fun template_mandateStillExcluded() {
        val parsed = engine.parse(
            RawSmsMessage(
                id = "4",
                sender = "VM-AXISBK",
                body = "For the upcoming mandate set for 01-02-2026, INR 500 will be debited from your a/c towards Paytm Money Limited",
                timestamp = 1_700_000_000_000L
            )
        )
        assertNull(parsed)
    }

}
