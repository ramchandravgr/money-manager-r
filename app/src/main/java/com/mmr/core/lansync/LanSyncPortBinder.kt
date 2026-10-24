package com.mmr.core.lansync

import com.mmr.core.lansync.transport.LanSyncHttpConstants
import java.net.InetSocketAddress
import java.net.ServerSocket

object LanSyncPortBinder {
    /** Passed to [java.net.ServerSocket.bind] to obtain a random available port from the OS. */
    const val EPHEMERAL_BIND_PORT = 0

    val candidatePorts: List<Int> =
        listOf(
            LanSyncHttpConstants.DEFAULT_PORT,
            8888,
            9999,
            19682,
            19683,
        )

    fun findAvailablePort(preferred: Int = LanSyncHttpConstants.DEFAULT_PORT): Int {
        val ordered = listOf(preferred) + candidatePorts.filter { it != preferred }
        for (port in ordered.distinct()) {
            if (canBind(port)) return port
        }
        return preferred
    }

    fun canBind(port: Int): Boolean =
        runCatching {
            ServerSocket().use { socket ->
                socket.bind(InetSocketAddress(port))
            }
        }.isSuccess
}
