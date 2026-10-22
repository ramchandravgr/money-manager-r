package com.mmr.core.parser.duplicate

import com.mmr.core.parser.util.SmsNormalizer
import java.security.MessageDigest

object SmsMessageHasher {

    /**
     * Generates a deterministic SHA-256 hash for an SMS entry based on sender, timestamp, and normalized body.
     */
    fun generateHash(
        sender: String,
        body: String,
        timestamp: Long,
        uniquenessNonce: String = ""
    ): String {
        val payload = SmsNormalizer.normalizeForHashing(sender, body, timestamp) + uniquenessNonce
        val digest = MessageDigest.getInstance("SHA-256").digest(payload.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }
}
