package com.rrparallelstudio.miapp.core.moneymanager

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.ZoneOffset

class MoneyManagerMmbakDateTest {
    @Test
    fun toEpochMillis_unixSeconds() {
        val seconds = 1_731_300_000L
        val expected = seconds * 1_000L
        assertEquals(expected, MoneyManagerMmbakDate.toEpochMillis(seconds, ZoneOffset.UTC))
    }

    @Test
    fun toEpochMillis_alreadyMillis() {
        val millis = 1_731_300_000_000L
        assertEquals(millis, MoneyManagerMmbakDate.toEpochMillis(millis, ZoneOffset.UTC))
    }

    @Test
    fun fromStorageValue_isoDateString() {
        val millis = MoneyManagerMmbakDate.fromStorageValue("2026-11-11", ZoneOffset.UTC)
        val parsed = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
        assertEquals(2026, parsed.year)
        assertEquals(11, parsed.monthValue)
        assertEquals(11, parsed.dayOfMonth)
    }

    @Test
    fun toEpochMillis_yyyymmdd() {
        val millis =
            MoneyManagerMmbakDate.toEpochMillis(20_241_101L, ZoneOffset.UTC)
        val parsed = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
        assertEquals(2024, parsed.year)
        assertEquals(11, parsed.monthValue)
        assertEquals(1, parsed.dayOfMonth)
    }
}
