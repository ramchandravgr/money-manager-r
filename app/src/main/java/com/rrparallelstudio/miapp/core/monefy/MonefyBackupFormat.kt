package com.rrparallelstudio.miapp.core.monefy

import com.rrparallelstudio.miapp.core.backup.BackupSignedZipCodec

object MonefyBackupFormat {
    fun matchesDisplayName(displayName: String?): Boolean = displayName.orEmpty().contains("monefy", ignoreCase = true)

    fun isProbablyMonefyBackup(
        bytes: ByteArray,
        displayName: String?,
    ): Boolean {
        if (BackupSignedZipCodec.isZipArchive(bytes)) return false
        if (bytes.size < 16 || bytes.size % 16 != 0) return false
        if (!matchesDisplayName(displayName) && bytes.size > MAX_PROBE_BYTES) return false
        return runCatching { MonefyBackupCrypto.decrypt(bytes) }.isSuccess
    }

    private const val MAX_PROBE_BYTES = 32 * 1024 * 1024
}
