package com.mmr.core.lansync

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LanSyncPairingPinTest {
    @Test
    fun generate_hasExpectedLengthAndAlphabet() {
        val pin = LanSyncPairingPin.generate()
        assertEquals(LanSyncPairingPin.LENGTH, pin.length)
        assertTrue(LanSyncPairingPin.isValidFormat(pin))
    }

    @Test
    fun normalize_stripsSpacesAndHyphens() {
        assertEquals("ABCDEFGHJKMN", LanSyncPairingPin.normalize("abcd-efgh jkmn"))
    }
}
