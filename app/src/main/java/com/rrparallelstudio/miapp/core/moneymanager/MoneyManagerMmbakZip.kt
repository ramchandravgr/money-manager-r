package com.rrparallelstudio.miapp.core.moneymanager

import java.io.ByteArrayInputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

internal object MoneyManagerMmbakZip {
    private const val MAX_UNCOMPRESSED_BYTES = 64L * 1024L * 1024L

    fun extractDatabaseFromZip(
        zipBytes: ByteArray,
        dest: File,
    ) {
        val member = selectLargestDatabaseZipEntry(zipBytes) ?: error("mmbak database entry missing")
        extractZipEntryToFile(zipBytes, member, dest)
    }

    private fun selectLargestDatabaseZipEntry(zipBytes: ByteArray): String? {
        ZipInputStream(ByteArrayInputStream(zipBytes)).use { zip ->
            var bestDbLikeName: String? = null
            var bestDbLikeSize = -1L
            var bestAnyName: String? = null
            var bestAnySize = -1L
            while (true) {
                val entry = zip.nextEntry ?: break
                if (entry.isDirectory) {
                    zip.closeEntry()
                    continue
                }
                val size = entrySize(zip, entry)
                if (size > bestAnySize) {
                    bestAnySize = size
                    bestAnyName = entry.name
                }
                if (isDatabaseZipEntryName(entry.name) && size > bestDbLikeSize) {
                    bestDbLikeSize = size
                    bestDbLikeName = entry.name
                }
                zip.closeEntry()
            }
            return bestDbLikeName ?: bestAnyName
        }
    }

    private fun extractZipEntryToFile(
        zipBytes: ByteArray,
        entryName: String,
        dest: File,
    ) {
        ZipInputStream(ByteArrayInputStream(zipBytes)).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                if (entry.name != entryName) {
                    zip.closeEntry()
                    continue
                }
                if (entry.size > MAX_UNCOMPRESSED_BYTES) error("mmbak too large")
                writeZipEntryContents(zip, dest)
                zip.closeEntry()
                return
            }
        }
        error("mmbak database entry missing")
    }

    private fun writeZipEntryContents(
        zip: ZipInputStream,
        dest: File,
    ) {
        var total = 0L
        dest.outputStream().use { out ->
            val buffer = ByteArray(16_384)
            while (true) {
                val read = zip.read(buffer)
                if (read <= 0) break
                total += read
                if (total > MAX_UNCOMPRESSED_BYTES) error("mmbak too large")
                out.write(buffer, 0, read)
            }
        }
    }

    private fun isDatabaseZipEntryName(name: String): Boolean {
        val lower = name.lowercase()
        return lower.endsWith(".db") ||
            lower.endsWith(".sqlite") ||
            lower.endsWith(".sqlite3") ||
            lower.endsWith(".mmbak")
    }

    private fun entrySize(
        zip: ZipInputStream,
        entry: ZipEntry,
    ): Long {
        if (entry.size >= 0L) return entry.size
        var drained = 0L
        val buffer = ByteArray(16_384)
        while (true) {
            val read = zip.read(buffer)
            if (read <= 0) break
            drained += read
        }
        return drained
    }
}
