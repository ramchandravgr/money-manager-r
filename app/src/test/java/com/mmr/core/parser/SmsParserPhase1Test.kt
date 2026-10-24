package com.mmr.core.parser

import com.mmr.core.model.enums.TransactionType
import com.mmr.core.parser.engine.SmsClassifier
import com.mmr.core.parser.engine.SmsParserEngine
import com.mmr.core.parser.model.RawSmsMessage
import com.mmr.core.parser.rule.AmountRule
import com.mmr.core.parser.rule.MerchantRule
import com.mmr.core.parser.rule.SenderRule
import com.mmr.core.parser.rule.TransactionTypeRule
import com.mmr.core.parser.template.SmsTemplateRegistry
import com.mmr.core.parser.util.SmsNormalizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SmsParserPhase1Test {
    private lateinit var engine: SmsParserEngine

    @Before
    fun setUp() {
        engine = SmsParserEngine(SmsTemplateRegistry.empty())
    }

    @Test
    fun amountRule_picksSpendAmount_notAvailableBalance() {
        val body =
            """
            You've spent Rs.1,250.00 on HDFC Bank Credit Card xx1234 at Amazon on 12-10-2025.
            Avl bal: Rs.50,000.00 Curr O/S: Rs.12,000.00
            """.trimIndent()
        val normalized = SmsNormalizer.normalizeForMatching(body)
        assertEquals(1250.0, AmountRule.extractAmount(normalized)!!, 0.01)
    }

    @Test
    fun amountRule_hdfcUpiDebit() {
        val body = "HDFC Bank: Rs 499 debited from a/c **5678 on 01-01-2026 to VPA zepto@ybl (UPI Ref No 123456789012)"
        val normalized = SmsNormalizer.normalizeForMatching(body)
        assertEquals(499.0, AmountRule.extractAmount(normalized)!!, 0.01)
    }

    @Test
    fun amountRule_hpPayPetrol() {
        val body = "Transaction of Rs.850.00 for purchase of petrol is successful . Regards, HP Pay team"
        val normalized = SmsNormalizer.normalizeForMatching(body)
        assertEquals(850.0, AmountRule.extractAmount(normalized)!!, 0.01)
    }

    @Test
    fun classifier_mandateIsInformational_telecomIsIgnored() {
        val mandate = "for the upcoming mandate set for 01-02-2026, inr 500 will be debited from your a/c"
        assertEquals(
            com.mmr.core.parser.model.SmsParseIntent.MANDATE,
            SmsClassifier.detectInformational(mandate),
        )

        val data = "we have credited your daily 1gb 4g/3g pack. it gets renewed every midnight."
        assertTrue(SmsClassifier.isIgnored(data))
    }

    @Test
    fun classifier_excludesFailedZeptoPayment() {
        val body = "your payment for zepto order abc was not completed. any amount if debited will get refunded within 2-3 days."
        assertTrue(SmsClassifier.isNonFinancial(body))
    }

    @Test
    fun classifier_nachWillBePresented_isMandate_notTransaction() {
        val body =
            "Dear Sir/Madam, The NACH for Rs. 112500 will be presented on 05/10/2026 towards the housing loan installment."
        assertEquals(com.mmr.core.parser.model.SmsParseIntent.MANDATE, SmsClassifier.detectInformational(body))
        assertNull(
            parse(
                sender = "JK-LICHFL-S",
                body = body,
            ),
        )
    }

    @Test
    fun classifier_axisAutoSweepFlexiCreditAndDebit_areIgnored_notFlexiCapFund() {
        val credit =
            "INR 7120.00 credited to A/c no. XX2043 on 30-09-24 at 20:42:16 IST. " +
                "Info - BRN-Flexi/924049290755257/. Chk Bal https://ccm.axbk.in/AXISBK/ltt3Dvko - Axis Bank"
        assertTrue(SmsClassifier.isAxisAutoSweepFlexi(credit))
        assertNull(parse(sender = "AX-AxisBk-S", body = credit))

        val debit =
            "Debit INR 32500.00\nAxis Bank A/c XX2043\n25-09-26 23:03:27\nFlexi/926049387489759/VANJ\n" +
                "WhatsApp BAL to 917036165000"
        assertTrue(SmsClassifier.isAxisAutoSweepFlexi(debit))
        assertNull(parse(sender = "AX-AxisBk-S", body = debit))

        val mfSip =
            "Your SIP Purchase in Folio 15686597/50 under HDFC Flexi Cap Fund-DP-Growth for Rs. 2,999.85 " +
                "has been processed at the NAV of 2301.233 for 1.304 units"
        assertFalse(SmsClassifier.isAxisAutoSweepFlexi(mfSip))
        assertFalse(SmsClassifier.isIgnored(mfSip))
    }

    @Test
    fun classifier_declinedCardTransaction_isIgnored() {
        val body =
            "A transaction of INR 1,700.00 on your YES BANK Credit Card ending 7663 on 21-10-2026 at GROK XAI is declined because International Ecom/ online transactions are disabled on your card."
        assertTrue(SmsClassifier.isDeclinedOrFailedTransaction(body))
        assertNull(parse(sender = "VA-YESBNK-S", body = body))
    }

    @Test
    fun merchantRule_extractsVpaAndBobCardMerchant() {
        val upi = "HDFC Bank: Rs 100 debited to VPA cred.club@axisb (UPI Ref No 999)"
        assertEquals("cred", MerchantRule.extractMerchant(upi)?.lowercase())

        val bob = "Alert: INR 320 is spent on your bobcard ending 4321 at Zepto on 12-10-2025."
        assertTrue(MerchantRule.extractMerchant(bob)?.contains("Zepto", ignoreCase = true) == true)
    }

    @Test
    fun engine_parsesRealWorldTemplates() {
        val hdfc =
            parse(
                sender = "VM-HDFCBK",
                body = "HDFC Bank: Rs 499 debited from a/c **5678 on 01-01-2026 to VPA zepto@ybl (UPI Ref No 123456789012)",
            )
        assertNotNull(hdfc)
        assertEquals(499.0, hdfc!!.amount, 0.01)
        assertEquals(TransactionType.EXPENSE, hdfc.type)
        assertTrue(SenderRule.isKnownFinancialSender(hdfc.sender))

        val mandate =
            parse(
                sender = "VM-AXISBK",
                body = "For the upcoming mandate set for 01-02-2026, INR 500 will be debited from your a/c towards Paytm Money Limited",
            )
        assertNull(mandate)
    }

    @Test
    fun typeRule_treatsNachDebitAsLoanPayment() {
        val body = "inr 15000 is debited to your account xxxxxx1234 on 01/01/2026 towards nach-123-lichousingfinancelt kotak bank"
        assertEquals(TransactionType.LOAN_PAYMENT, TransactionTypeRule.inferType(body))
    }

    private fun parse(
        sender: String,
        body: String,
    ) = engine.parse(
        RawSmsMessage(
            id = "1",
            sender = sender,
            body = body,
            timestamp = 1_700_000_000_000L,
        ),
    )
}
