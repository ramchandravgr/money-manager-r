package com.rrparallelstudio.miapp.core.lansync

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
        assertEquals("ABCDEFG", LanSyncPairingPin.normalize("abc-defg"))
    }

    @Test
    fun sanitizeEntry_uppercasesAndDropsInvalidChars() {
        assertEquals("ABC23", LanSyncPairingPin.sanitizeEntry("abc2i3"))
        assertEquals("ABCDEFG", LanSyncPairingPin.sanitizeEntry("abcdefgH"))
    }
}
