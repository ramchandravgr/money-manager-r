package com.rrparallelstudio.miapp.core.lansync

import java.security.SecureRandom

/**
 * Short human-entered pairing code for LAN device trust (shown on the host device).
 * Full [LanSyncPairingOffer] is fetched over the local HTTP pairing endpoint when the code matches.
 */
object LanSyncPairingPin {
    const val LENGTH = 7
    const val MIN_LENGTH = 7
    const val MAX_LENGTH = 7

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

    /** Live pairing field: upper case, Crockford charset only, max [LENGTH] characters. */
    fun sanitizeEntry(raw: String): String =
        buildString {
            for (c in normalize(raw)) {
                if (c !in ALPHABET) continue
                append(c)
                if (length >= LENGTH) break
            }
        }

    fun isValidFormat(normalized: String): Boolean =
        normalized.length in MIN_LENGTH..MAX_LENGTH &&
            normalized.all { it in ALPHABET }
}
