package com.mmr.core.backup

import android.util.Base64
import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * HMAC integrity for backup archives. Protects against accidental edits only; key is in the APK.
 */
object BackupIntegrity {
    const val SIGNATURE_ALGORITHM = "HmacSHA256"

    /** Not a user secret — deters casual tampering with exported files. */
    private val INTEGRITY_KEY =
        "com.mmr.app.backup.integrity.v1"
            .toByteArray(Charsets.UTF_8)

    fun sha256Hex(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(bytes)
        return hash.toHex()
    }

    fun sign(
        payloadSha256: String,
        attachmentHashes: List<Pair<String, String>>,
    ): String {
        val canonical = canonicalSigningPayload(payloadSha256, attachmentHashes)
        val mac = Mac.getInstance(SIGNATURE_ALGORITHM)
        mac.init(SecretKeySpec(INTEGRITY_KEY, SIGNATURE_ALGORITHM))
        val raw = mac.doFinal(canonical.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(raw, Base64.NO_WRAP)
    }

    fun verify(
        payloadSha256: String,
        attachmentHashes: List<Pair<String, String>>,
        signatureBase64: String,
    ): Boolean {
        val expected = sign(payloadSha256, attachmentHashes)
        return expected == signatureBase64
    }

    fun canonicalSigningPayload(
        payloadSha256: String,
        attachmentHashes: List<Pair<String, String>>,
    ): String =
        buildString {
            append(payloadSha256)
            attachmentHashes.sortedBy { it.first }.forEach { (syncId, hash) ->
                append('\n')
                append(syncId)
                append(':')
                append(hash)
            }
        }

    private fun ByteArray.toHex(): String =
        joinToString(separator = "") { byte ->
            "%02x".format(byte)
        }
}
