package com.mmr.core.parser.scanner

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class SmsScanPeriodTest {
    @Test
    fun clampMonths_respectsBounds() {
        assertEquals(1, SmsScanPeriod.clampMonths(0))
        assertEquals(36, SmsScanPeriod.clampMonths(100))
        assertEquals(6, SmsScanPeriod.clampMonths(6))
    }

    @Test
    fun sinceTimestampMillis_longerPeriodIsFurtherBack() {
        val zone = ZoneId.of("UTC")
        val oneMonth = SmsScanPeriod.sinceTimestampMillis(1, zone)
        val threeMonths = SmsScanPeriod.sinceTimestampMillis(3, zone)
        assertTrue(threeMonths < oneMonth)
    }

    @Test
    fun sinceTimestampMillis_isNotInFuture() {
        val since = SmsScanPeriod.sinceTimestampMillis(3)
        assertTrue(since <= Instant.now().toEpochMilli())
    }
}
