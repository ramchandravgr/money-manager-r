package com.mmr.core.lansync

/**
 * Validates an inbound LAN pair attempt against the host's active pairing code.
 */
object LanSyncPairingGate {
    /** Host pairing codes expire so stale codes cannot be replayed indefinitely. */
    const val ACTIVE_PIN_TTL_MS = 15 * 60 * 1000L

    fun pairingCodeMatches(
        submittedRaw: String,
        activePin: String?,
    ): Boolean {
        if (activePin.isNullOrBlank()) return false
        val submitted = LanSyncPairingPin.normalize(submittedRaw)
        if (!LanSyncPairingPin.isValidFormat(submitted)) return false
        return submitted.equals(activePin, ignoreCase = true)
    }

    fun isPinExpired(
        issuedAtMillis: Long,
        nowMillis: Long = System.currentTimeMillis(),
    ): Boolean = nowMillis - issuedAtMillis > ACTIVE_PIN_TTL_MS
}
