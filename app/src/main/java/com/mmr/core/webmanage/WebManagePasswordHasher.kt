package com.mmr.core.webmanage

import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object WebManagePasswordHasher {
    private const val SALT_BYTES = 16
    private const val PBKDF2_PREFIX = "pbkdf2:"
    private const val PBKDF2_ITERATIONS = 210_000
    private const val PBKDF2_KEY_BITS = 256

    fun newSalt(): String {
        val bytes = ByteArray(SALT_BYTES)
        SecureRandom().nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun hash(
        password: String,
        salt: String,
    ): String {
        val saltBytes = decodeSaltHex(salt)
        val spec = PBEKeySpec(password.toCharArray(), saltBytes, PBKDF2_ITERATIONS, PBKDF2_KEY_BITS)
        val derived =
            SecretKeyFactory
                .getInstance("PBKDF2WithHmacSHA256")
                .generateSecret(spec)
                .encoded
        return "$PBKDF2_PREFIX$PBKDF2_ITERATIONS:${derived.toHex()}"
    }

    fun verify(
        password: String,
        salt: String,
        expectedHash: String,
    ): Boolean {
        require(expectedHash.startsWith(PBKDF2_PREFIX)) { "Unsupported password hash format" }
        val computed = hash(password, salt)
        return constantTimeEquals(computed, expectedHash)
    }

    private fun decodeSaltHex(salt: String): ByteArray {
        require(salt.length >= SALT_BYTES * 2) { "Invalid salt" }
        return salt.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    }

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

    private fun constantTimeEquals(
        a: String,
        b: String,
    ): Boolean {
        if (a.length != b.length) return false
        var diff = 0
        for (index in a.indices) {
            diff = diff or (a[index].code xor b[index].code)
        }
        return diff == 0
    }
}
