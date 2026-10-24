package com.mmr.core.webmanage

import java.security.MessageDigest
import java.security.SecureRandom

object WebManagePasswordHasher {
    private const val SALT_BYTES = 16

    fun newSalt(): String {
        val bytes = ByteArray(SALT_BYTES)
        SecureRandom().nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun hash(
        password: String,
        salt: String,
    ): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest("$salt:$password".toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun verify(
        password: String,
        salt: String,
        expectedHash: String,
    ): Boolean = hash(password, salt) == expectedHash
}
