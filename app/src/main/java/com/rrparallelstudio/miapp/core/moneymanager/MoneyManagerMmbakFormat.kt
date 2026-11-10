package com.rrparallelstudio.miapp.core.moneymanager

import com.rrparallelstudio.miapp.core.backup.BackupSignedZipCodec
import java.io.ByteArrayInputStream
import java.util.zip.ZipInputStream

object MoneyManagerMmbakFormat {
    private const val MANIFEST = "manifest.json"

    fun isProbablyMoneyManagerBackup(
        bytes: ByteArray,
        displayName: String?,
    ): Boolean {
        if (displayName.orEmpty().endsWith(".mmbak", ignoreCase = true)) return true
        if (looksLikeSqlite(bytes)) return true
        if (!BackupSignedZipCodec.isZipArchive(bytes)) return false
        return !zipContainsEntry(bytes, MANIFEST)
    }

    private fun looksLikeSqlite(bytes: ByteArray): Boolean =
        bytes.size >= 16 && bytes.copyOf(16).decodeToString().startsWith("SQLite format")

    private fun zipContainsEntry(
        bytes: ByteArray,
        entryName: String,
    ): Boolean =
        runCatching {
            ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
                generateSequence { zip.nextEntry }.any { it.name == entryName }
            }
        }.getOrDefault(false)
}
