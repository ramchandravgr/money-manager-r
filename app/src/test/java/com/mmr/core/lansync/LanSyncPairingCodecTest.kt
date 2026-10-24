package com.mmr.core.lansync

import org.junit.Assert.assertEquals
import org.junit.Test

class LanSyncPairingCodecTest {
    @Test
    fun encodeDecode_roundTrip_preservesOfferFields() {
        val offer =
            LanSyncPairingOffer(
                deviceId = "device-42",
                displayName = "Pixel",
                inboundAuthToken = "secret-token",
            )
        val encoded = LanSyncPairingCodec.encode(offer)
        val decoded = LanSyncPairingCodec.decode(encoded)
        assertEquals(offer, decoded)
        assertEquals(true, encoded.startsWith(LanSyncPairingCodec.LEGACY_PREFIX))
    }
}
