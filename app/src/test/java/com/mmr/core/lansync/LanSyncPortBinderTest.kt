package com.mmr.core.lansync

import com.mmr.core.lansync.transport.LanSyncHttpConstants
import org.junit.Assert.assertTrue
import org.junit.Test

class LanSyncPortBinderTest {
    @Test
    fun findAvailablePort_returnsBindablePort() {
        val port = LanSyncPortBinder.findAvailablePort(LanSyncHttpConstants.DEFAULT_PORT)
        assertTrue(LanSyncPortBinder.canBind(port))
    }

    @Test
    fun candidatePorts_includesDefaultSyncPort() {
        assertTrue(LanSyncPortBinder.candidatePorts.contains(LanSyncHttpConstants.DEFAULT_PORT))
    }

    @Test
    fun ephemeralBind_assignsNonZeroPort() {
        java.net.ServerSocket().use { socket ->
            socket.bind(java.net.InetSocketAddress(LanSyncPortBinder.EPHEMERAL_BIND_PORT))
            assertTrue(socket.localPort > 0)
        }
    }
}
