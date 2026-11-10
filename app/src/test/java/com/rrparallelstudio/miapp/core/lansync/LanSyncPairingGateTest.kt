package com.rrparallelstudio.miapp.core.lansync

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LanSyncPairingGateTest {
    @Test
    fun pairingCodeMatches_acceptsNormalizedCode() {
        val pin = LanSyncPairingPin.generate()
        assertTrue(LanSyncPairingGate.pairingCodeMatches(pin.lowercase(), pin))
    }

    @Test
    fun pairingCodeMatches_rejectsWrongCode() {
        val pin = LanSyncPairingPin.generate()
        assertFalse(LanSyncPairingGate.pairingCodeMatches("WRONGCODE12", pin))
    }

    @Test
    fun isPinExpired_afterTtl() {
        val issued = System.currentTimeMillis() - LanSyncPairingGate.ACTIVE_PIN_TTL_MS - 1
        assertTrue(LanSyncPairingGate.isPinExpired(issued))
    }
}
