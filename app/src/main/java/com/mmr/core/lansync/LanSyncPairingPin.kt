package com.mmr.core.lansync

import java.security.SecureRandom

/**
 * Short human-entered pairing code for LAN device trust (shown on the host device).
 * Full [LanSyncPairingOffer] is fetched over the local HTTP pairing endpoint when the code matches.
 */
object LanSyncPairingPin {
    const val LENGTH = 12
    const val MIN_LENGTH = 10
    const val MAX_LENGTH = 15

    /** Crockford-style alphabet (no I, L, O, 0, 1) for easier reading aloud. */
    private const val ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789"

    private val random = SecureRandom()

    fun generate(length: Int = LENGTH): String {
        require(length in MIN_LENGTH..MAX_LENGTH)
        return buildString(length) {
            repeat(length) {
                append(ALPHABET[random.nextInt(ALPHABET.length)])
            }
        }
    }

    fun normalize(raw: String): String =
        raw
            .trim()
            .uppercase()
            .replace("-", "")
            .replace(" ", "")

    fun isValidFormat(normalized: String): Boolean =
        normalized.length in MIN_LENGTH..MAX_LENGTH &&
            normalized.all { it in ALPHABET }
}
