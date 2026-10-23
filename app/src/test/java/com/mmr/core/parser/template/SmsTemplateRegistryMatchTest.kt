package com.mmr.core.parser.template

import com.mmr.core.parser.model.RawSmsMessage
import com.mmr.core.parser.util.SmsNormalizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class SmsTemplateRegistryMatchTest {
    private lateinit var registry: SmsTemplateRegistry

    @Before
    fun load() {
        registry = SmsTemplateRegistry.fromJson(SmsTestAssets.loadTemplatesJson())
    }

    @Test
    fun matches_bobCardSpend() {
        val body = "Alert: INR 320 is spent on your bobcard ending 4321 at Zepto on 12-10-2025."
        val normalized = SmsNormalizer.normalizeForMatching(body)
        val match =
            registry.match(
                RawSmsMessage("1", "AD-BOBCRD-S", body, 0L),
                normalized,
            )
        assertNotNull(match)
        assertEquals("bobcard_spend", match!!.template.id)
        assertEquals(320.0, match.amount, 0.01)
    }

    @Test
    fun matches_pluxeeMealWallet() {
        val body = "Rs. 250 spent from Pluxee meal card wallet, card no.xx9999 on 01-01-2026 12:00:00 at zepto . Avl bal Rs.1000."
        val normalized = SmsNormalizer.normalizeForMatching(body)
        val match =
            registry.match(
                RawSmsMessage("2", "VM-PLUXEE", body, 0L),
                normalized,
            )
        assertNotNull(match)
        assertEquals("pluxee_meal_spend", match!!.template.id)
    }

    @Test
    fun matches_hdfcUpiSent_fromS25Export() {
        val body =
            "Sent Rs.1512.00 From HDFC Bank A/C *0561 To Meesho On 23/01/26 Ref 638965291412 Not You? Call 18002586161/SMS BLOCK UPI to 7308080808 "
        val normalized = SmsNormalizer.normalizeForMatching(body)
        val match =
            registry.match(
                RawSmsMessage("3", "VM-HDFCBK-S", body, 0L),
                normalized,
            )
        assertNotNull(match)
        assertEquals("hdfc_upi_sent", match!!.template.id)
        assertEquals(1512.0, match.amount, 0.01)
        assertEquals("0561", match.accountLastDigits)
        assertEquals("meesho", match.merchant)
    }

    @Test
    fun matches_hdfcCardSpendRs_fromS25Export() {
        val body =
            "Spent Rs.584 On HDFC Bank Card 4148 At AMAZON On 2026-01-25:19:52:17.Not You? To Block+Reissue Call 18002586161/SMS BLOCK CC 4148 to 7308080808"
        val normalized = SmsNormalizer.normalizeForMatching(body)
        val match =
            registry.match(
                RawSmsMessage("4", "VM-HDFCBK-S", body, 0L),
                normalized,
            )
        assertNotNull(match)
        assertEquals("hdfc_cc_spend", match!!.template.id)
        assertEquals(584.0, match.amount, 0.01)
        assertEquals("4148", match.accountLastDigits)
    }

    @Test
    fun matches_hdfcCreditAlertVpa_fromS25Export() {
        val body =
            "Credit Alert! Rs.108.00 credited to HDFC Bank A/c XX0561 on 13-06-26 from VPA paytm-17731298@ptybl (UPI 616400743998) "
        val normalized = SmsNormalizer.normalizeForMatching(body)
        val match =
            registry.match(
                RawSmsMessage("5", "VM-HDFCBK-S", body, 0L),
                normalized,
            )
        assertNotNull(match)
        assertEquals("hdfc_credit_alert_vpa", match!!.template.id)
        assertEquals(108.0, match.amount, 0.01)
        assertEquals("paytm 17731298", match.merchant)
    }

    @Test
    fun matches_axisCardSpend_fromS25Export() {
        val body =
            "Spent INR 4905 Axis Bank Card no. XX0099 18-01-26 15:30:48 IST MEESHO TECH Avl Limit: INR 87095 Not you? SMS BLOCK 0099 to 919951860002"
        val normalized = SmsNormalizer.normalizeForMatching(body)
        val match =
            registry.match(
                RawSmsMessage("6", "VM-AXISBK-S", body, 0L),
                normalized,
            )
        assertNotNull(match)
        assertEquals("axis_cc_spent", match!!.template.id)
        assertEquals(4905.0, match.amount, 0.01)
    }

    @Test
    fun matches_unionMobBkDebit_fromS25Export() {
        val body =
            "A/c *9007 Debited for Rs:60.00 on 29-03-2026 19:37:39 by Mob Bk ref no 645473518484 Avl Bal Rs:9426.55.If not you, Call 1800222243 -Union Bank of India"
        val normalized = SmsNormalizer.normalizeForMatching(body)
        val match =
            registry.match(
                RawSmsMessage("7", "VM-UNIONB-S", body, 0L),
                normalized,
            )
        assertNotNull(match)
        assertEquals("union_mob_bk_debit", match!!.template.id)
        assertEquals(60.0, match.amount, 0.01)
    }

    @Test
    fun matches_axisCcBillPayment_informationalIntent() {
        val body =
            "Payment of INR 11252 has been received towards your Axis Bank Credit Card XX0099 on 18-01-26 - Axis Bank"
        val normalized = SmsNormalizer.normalizeForMatching(body)
        val sms = RawSmsMessage("8", "VM-AXISBK-S", body, 0L)
        assertNull(registry.match(sms, normalized))
        val info = registry.matchInformational(sms, normalized)
        assertNotNull(info)
        assertEquals("axis_cc_bill_payment", info!!.template.id)
        assertEquals(11252.0, info.amount, 0.01)
    }
}
