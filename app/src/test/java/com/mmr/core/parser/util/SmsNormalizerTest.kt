package com.mmr.core.parser.util

import org.junit.Assert.assertEquals
import org.junit.Test

class SmsNormalizerTest {
    @Test
    fun normalizeForMatching_rsColonBecomesInr() {
        val normalized = SmsNormalizer.normalizeForMatching("Debited for Rs:60.00 on 29-03-2026")
        assertEquals("debited for inr 60.00 on 29-03-2026", normalized)
    }
}
