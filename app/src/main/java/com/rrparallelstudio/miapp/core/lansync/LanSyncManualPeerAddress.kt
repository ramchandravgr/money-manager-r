package com.rrparallelstudio.miapp.core.lansync

/**
 * Parses the "This phone: http://host:port" line from the other device's Peer Device Sync screen.
 */
object LanSyncManualPeerAddress {
    data class Endpoint(
        val host: String,
        val port: Int,
    )

    fun parse(raw: String?): Endpoint? {
        val text = raw?.trim().orEmpty()
        if (text.isBlank()) return null
        val lower = text.lowercase()
        val withoutScheme =
            when {
                lower.startsWith("http://") -> text.substring(7)
                lower.startsWith("https://") -> text.substring(8)
                else -> text
            }.substringBefore('/').trim()
        val lastColon = withoutScheme.lastIndexOf(':')
        if (lastColon <= 0) return null
        val host = withoutScheme.substring(0, lastColon).trim()
        val port = withoutScheme.substring(lastColon + 1).toIntOrNull() ?: return null
        if (host.isBlank() || port !in 1..65535) return null
        return Endpoint(host, port)
    }
}
