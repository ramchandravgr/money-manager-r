package com.mmr.core.parser.template

import com.mmr.core.parser.model.RawSmsMessage
import com.mmr.core.parser.util.SmsNormalizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
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
        val match = registry.match(
            RawSmsMessage("1", "AD-BOBCRD-S", body, 0L),
            normalized
        )
        assertNotNull(match)
        assertEquals("bobcard_spend", match!!.template.id)
        assertEquals(320.0, match.amount, 0.01)
    }

    @Test
    fun matches_pluxeeMealWallet() {
        val body = "Rs. 250 spent from Pluxee meal card wallet, card no.xx9999 on 01-01-2026 12:00:00 at zepto . Avl bal Rs.1000."
        val normalized = SmsNormalizer.normalizeForMatching(body)
        val match = registry.match(
            RawSmsMessage("2", "VM-PLUXEE", body, 0L),
            normalized
        )
        assertNotNull(match)
        assertEquals("pluxee_meal_spend", match!!.template.id)
    }
}
