package com.mmr.core.parser.util

import org.junit.Assert.assertEquals
import org.junit.Test

class SmsNormalizerTest {
    @Test
    fun normalizeForMatching_rsColonBecomesInr() {
        val normalized = SmsNormalizer.normalizeForMatching("Debited for Rs:60.00 on 29-03-2026")
        assertEquals("debited for inr 60.00 on 29-03-2026", normalized)
    }

    @Test
    fun normalizeForMatching_doesNotBreakReversedWord() {
        val normalized =
            SmsNormalizer.normalizeForMatching(
                "Transaction Reversed!On HDFC Bank CREDIT Card xx4148 Amt: Rs.1470",
            )
        assertEquals(
            "transaction reversed!on hdfc bank credit card xx4148 amt: inr 1470",
            normalized,
        )
    }
}
