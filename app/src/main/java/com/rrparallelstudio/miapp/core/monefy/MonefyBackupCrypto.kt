package com.rrparallelstudio.miapp.core.monefy

import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

/**
 * Monefy Android backups are an SQLite database encrypted with AES-128-ECB.
 * The key is the constant stored in the Monefy APK (`DatabaseBackupHelper.BACKUP_KEY`).
 */
internal object MonefyBackupCrypto {
    private val key = SecretKeySpec("MyDifficultPassw".toByteArray(Charsets.UTF_8), "AES")

    fun decrypt(encrypted: ByteArray): ByteArray {
        require(encrypted.isNotEmpty() && encrypted.size % BLOCK == 0) {
            "Not a Monefy backup (unexpected file size)."
        }
        val cipher = Cipher.getInstance("AES/ECB/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key)
        val plain = cipher.doFinal(encrypted)
        require(
            plain.size >= SQLITE_HEADER.size && plain.copyOfRange(0, SQLITE_HEADER.size).contentEquals(SQLITE_HEADER),
        ) {
            "Not a Monefy backup (could not read the ledger database)."
        }
        return plain
    }

    private const val BLOCK = 16

    private val SQLITE_HEADER = "SQLite format 3\u0000".toByteArray(Charsets.US_ASCII)
}
