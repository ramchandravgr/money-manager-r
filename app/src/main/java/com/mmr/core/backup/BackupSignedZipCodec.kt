package com.mmr.core.backup

import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Signed ZIP backup container (archive format v1).
 *
 * Entries: [manifest.json], [backup.json], [attachments/<syncId>]…
 */
object BackupSignedZipCodec {
    const val ARCHIVE_FORMAT_VERSION = 1
    const val ENTRY_MANIFEST = "manifest.json"
    const val ENTRY_PAYLOAD = "backup.json"
    const val ATTACHMENTS_DIR = "attachments/"

    const val SUGGESTED_FILE_EXTENSION = "mmrbackup"

    data class ParsedArchive(
        val payloadJson: String,
        val attachmentBytesBySyncId: Map<String, ByteArray>,
    )

    fun encode(
        payloadJson: String,
        attachmentBytesBySyncId: Map<String, ByteArray>,
    ): ByteArray {
        val payloadBytes = payloadJson.toByteArray(Charsets.UTF_8)
        val payloadSha256 = BackupIntegrity.sha256Hex(payloadBytes)
        val attachmentEntries =
            attachmentBytesBySyncId
                .map { (syncId, bytes) ->
                    AttachmentEntry(
                        syncId = syncId,
                        zipPath = attachmentZipPath(syncId),
                        sha256 = BackupIntegrity.sha256Hex(bytes),
                        bytes = bytes,
                    )
                }.sortedBy { it.syncId }

        val attachmentHashes = attachmentEntries.map { it.syncId to it.sha256 }
        val signature = BackupIntegrity.sign(payloadSha256, attachmentHashes)
        val manifestJson =
            buildManifestJson(
                payloadSha256 = payloadSha256,
                attachmentEntries = attachmentEntries,
                signature = signature,
            )

        return ByteArrayOutputStream().use { buffer ->
            ZipOutputStream(buffer).use { zip ->
                zip.putNextEntry(ZipEntry(ENTRY_MANIFEST))
                zip.write(manifestJson.toByteArray(Charsets.UTF_8))
                zip.closeEntry()

                zip.putNextEntry(ZipEntry(ENTRY_PAYLOAD))
                zip.write(payloadBytes)
                zip.closeEntry()

                attachmentEntries.forEach { entry ->
                    zip.putNextEntry(ZipEntry(entry.zipPath))
                    zip.write(entry.bytes)
                    zip.closeEntry()
                }
            }
            buffer.toByteArray()
        }
    }

    fun decode(bytes: ByteArray): ParsedArchive {
        if (bytes.size < 4 || bytes[0] != 0x50.toByte() || bytes[1] != 0x4B.toByte()) {
            throw IllegalArgumentException("Not a valid MMR backup archive.")
        }

        val entries = mutableMapOf<String, ByteArray>()
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    entries[entry.name] = zip.readBytes()
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }

        val manifestBytes =
            entries[ENTRY_MANIFEST]
                ?: throw IllegalArgumentException("Backup archive is missing manifest.json.")
        val payloadBytes =
            entries[ENTRY_PAYLOAD]
                ?: throw IllegalArgumentException("Backup archive is missing backup.json.")

        val manifest = JSONObject(String(manifestBytes, Charsets.UTF_8))
        val archiveVersion = manifest.getInt("archiveFormatVersion")
        if (archiveVersion != ARCHIVE_FORMAT_VERSION) {
            throw IllegalArgumentException("Unsupported backup archive version: $archiveVersion")
        }
        if (manifest.getString("appId") != BackupJsonCodec.EXPECTED_APP_ID) {
            throw IllegalArgumentException("This backup file is not for Money Inbox.")
        }

        val expectedPayloadSha = manifest.getString("payloadSha256")
        val actualPayloadSha = BackupIntegrity.sha256Hex(payloadBytes)
        if (expectedPayloadSha != actualPayloadSha) {
            throw IllegalArgumentException("Backup data was modified or is corrupt (payload checksum mismatch).")
        }

        val attachmentHashes = mutableListOf<Pair<String, String>>()
        val attachmentArray = manifest.optJSONArray("attachments") ?: JSONArray()
        for (i in 0 until attachmentArray.length()) {
            val item = attachmentArray.getJSONObject(i)
            val syncId = item.getString("syncId")
            val path = item.getString("path")
            val expectedSha = item.getString("sha256")
            val fileBytes =
                entries[path]
                    ?: throw IllegalArgumentException("Backup archive is missing attachment: $path")
            val actualSha = BackupIntegrity.sha256Hex(fileBytes)
            if (actualSha != expectedSha) {
                throw IllegalArgumentException("Backup attachment was modified or is corrupt: $syncId")
            }
            attachmentHashes.add(syncId to expectedSha)
        }

        val signature = manifest.getString("signature")
        if (!BackupIntegrity.verify(expectedPayloadSha, attachmentHashes, signature)) {
            throw IllegalArgumentException("Backup file failed integrity check. It may have been edited.")
        }

        val attachmentBytesBySyncId = mutableMapOf<String, ByteArray>()
        for (i in 0 until attachmentArray.length()) {
            val item = attachmentArray.getJSONObject(i)
            val syncId = item.getString("syncId")
            val path = item.getString("path")
            attachmentBytesBySyncId[syncId] = entries[path]!!
        }

        return ParsedArchive(
            payloadJson = String(payloadBytes, Charsets.UTF_8),
            attachmentBytesBySyncId = attachmentBytesBySyncId,
        )
    }

    fun isZipArchive(bytes: ByteArray): Boolean =
        bytes.size >= 4 && bytes[0] == 0x50.toByte() && bytes[1] == 0x4B.toByte()

    fun attachmentZipPath(syncId: String): String = ATTACHMENTS_DIR + sanitizeSyncIdForPath(syncId)

    private fun sanitizeSyncIdForPath(syncId: String): String =
        syncId
            .trim()
            .replace(Regex("[^a-zA-Z0-9._-]"), "_")
            .ifBlank { "attachment" }

    private data class AttachmentEntry(
        val syncId: String,
        val zipPath: String,
        val sha256: String,
        val bytes: ByteArray,
    )

    private fun buildManifestJson(
        payloadSha256: String,
        attachmentEntries: List<AttachmentEntry>,
        signature: String,
    ): String =
        JSONObject()
            .apply {
                put("archiveFormatVersion", ARCHIVE_FORMAT_VERSION)
                put("appId", BackupJsonCodec.EXPECTED_APP_ID)
                put("innerFormatVersion", BackupJsonCodec.CURRENT_FORMAT_VERSION)
                put("payloadFile", ENTRY_PAYLOAD)
                put("payloadSha256", payloadSha256)
                put(
                    "attachments",
                    JSONArray().apply {
                        attachmentEntries.forEach { entry ->
                            put(
                                JSONObject().apply {
                                    put("syncId", entry.syncId)
                                    put("path", entry.zipPath)
                                    put("sha256", entry.sha256)
                                },
                            )
                        }
                    },
                )
                put("signature", signature)
            }.toString(2)
}
