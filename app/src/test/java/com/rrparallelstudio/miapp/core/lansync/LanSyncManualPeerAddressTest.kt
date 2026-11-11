package com.rrparallelstudio.miapp.core.lansync

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LanSyncManualPeerAddressTest {
    @Test
    fun parse_hostPort() {
        val e = LanSyncManualPeerAddress.parse("192.168.0.138:40559")
        assertEquals("192.168.0.138", e?.host)
        assertEquals(40559, e?.port)
    }

    @Test
    fun parse_httpUrl() {
        val e = LanSyncManualPeerAddress.parse("http://192.168.0.141:38023")
        assertEquals("192.168.0.141", e?.host)
        assertEquals(38023, e?.port)
    }

    @Test
    fun parse_blank_isNull() {
        assertNull(LanSyncManualPeerAddress.parse("  "))
    }
}
