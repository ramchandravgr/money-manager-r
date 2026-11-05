package com.mmr.core.backup

import com.mmr.core.attachment.MmrAttachmentSyncId
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

    const val SUGGESTED_FILE_EXTENSION = "mibxbac"

    /** Pre–0.9.0 export suffix; zip import still accepts these files. */
    const val LEGACY_FILE_EXTENSION = "mmrbackup"

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
                payloadUncompressedSize = payloadBytes.size,
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
        if (bytes.size > BackupArchiveLimits.MAX_COMPRESSED_BYTES) {
            throw IllegalArgumentException("Backup archive file is too large.")
        }

        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            var uncompressedTotal = 0L

            val manifestBytes =
                readRequiredEntry(
                    zip = zip,
                    expectedName = ENTRY_MANIFEST,
                    maxEntryBytes = BackupArchiveLimits.MAX_MANIFEST_BYTES,
                    totalUncompressedSoFar = uncompressedTotal,
                )
            uncompressedTotal += manifestBytes.size

            val payloadBytes =
                readRequiredEntry(
                    zip = zip,
                    expectedName = ENTRY_PAYLOAD,
                    maxEntryBytes = BackupArchiveLimits.MAX_PAYLOAD_BYTES,
                    totalUncompressedSoFar = uncompressedTotal,
                )
            uncompressedTotal += payloadBytes.size

            val manifest = JSONObject(String(manifestBytes, Charsets.UTF_8))
            validateManifestHeader(manifest)
            assertDeclaredPayloadSize(manifest, payloadBytes.size)

            val expectedPayloadSha = manifest.getString("payloadSha256")
            val actualPayloadSha = BackupIntegrity.sha256Hex(payloadBytes)
            if (expectedPayloadSha != actualPayloadSha) {
                throw IllegalArgumentException("Backup data was modified or is corrupt (payload checksum mismatch).")
            }

            val attachmentManifest = parseAttachmentManifest(manifest)
            val attachmentBytesByPath = mutableMapOf<String, ByteArray>()
            val attachmentHashes = mutableListOf<Pair<String, String>>()

            var entry = zip.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    val name = entry.name
                    if (!name.startsWith(ATTACHMENTS_DIR)) {
                        throw IllegalArgumentException("Unexpected backup archive entry: $name")
                    }
                    val meta =
                        attachmentManifest.byPath[name]
                            ?: throw IllegalArgumentException("Unexpected backup attachment entry: $name")
                    val maxEntry =
                        meta.declaredUncompressedSize?.coerceAtMost(BackupArchiveLimits.MAX_ATTACHMENT_ENTRY_BYTES)
                            ?: BackupArchiveLimits.MAX_ATTACHMENT_ENTRY_BYTES
                    val fileBytes =
                        readEntryBytesCapped(
                            zip = zip,
                            maxEntryBytes = maxEntry,
                            totalUncompressedSoFar = uncompressedTotal,
                        )
                    uncompressedTotal += fileBytes.size
                    if (meta.declaredUncompressedSize != null && fileBytes.size != meta.declaredUncompressedSize) {
                        throw IllegalArgumentException("Backup attachment size mismatch: ${meta.syncId}")
                    }
                    val actualSha = BackupIntegrity.sha256Hex(fileBytes)
                    if (actualSha != meta.sha256) {
                        throw IllegalArgumentException("Backup attachment was modified or is corrupt: ${meta.syncId}")
                    }
                    attachmentBytesByPath[name] = fileBytes
                    attachmentHashes.add(meta.syncId to meta.sha256)
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }

            if (attachmentBytesByPath.size != attachmentManifest.byPath.size) {
                throw IllegalArgumentException("Backup archive is missing one or more attachments.")
            }

            val signature = manifest.getString("signature")
            if (!BackupIntegrity.verify(expectedPayloadSha, attachmentHashes, signature)) {
                throw IllegalArgumentException("Backup file failed integrity check. It may have been edited.")
            }

            val attachmentBytesBySyncId =
                attachmentManifest.byPath
                    .map { (path, meta) ->
                        meta.syncId to attachmentBytesByPath.getValue(path)
                    }.toMap()

            return ParsedArchive(
                payloadJson = String(payloadBytes, Charsets.UTF_8),
                attachmentBytesBySyncId = attachmentBytesBySyncId,
            )
        }
    }

    fun isZipArchive(bytes: ByteArray): Boolean =
        bytes.size >= 4 && bytes[0] == 0x50.toByte() && bytes[1] == 0x4B.toByte()

    fun attachmentZipPath(syncId: String): String = ATTACHMENTS_DIR + MmrAttachmentSyncId.sanitizeForPath(syncId)

    private data class AttachmentEntry(
        val syncId: String,
        val zipPath: String,
        val sha256: String,
        val bytes: ByteArray,
    )

    private data class AttachmentManifestMeta(
        val syncId: String,
        val sha256: String,
        val declaredUncompressedSize: Int?,
    )

    private data class AttachmentManifest(
        val byPath: Map<String, AttachmentManifestMeta>,
    )

    private fun buildManifestJson(
        payloadSha256: String,
        payloadUncompressedSize: Int,
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
                put("payloadUncompressedSize", payloadUncompressedSize)
                put(
                    "attachments",
                    JSONArray().apply {
                        attachmentEntries.forEach { entry ->
                            put(
                                JSONObject().apply {
                                    put("syncId", entry.syncId)
                                    put("path", entry.zipPath)
                                    put("sha256", entry.sha256)
                                    put("uncompressedSize", entry.bytes.size)
                                },
                            )
                        }
                    },
                )
                put("signature", signature)
            }.toString(2)

    private fun validateManifestHeader(manifest: JSONObject) {
        val archiveVersion = manifest.getInt("archiveFormatVersion")
        if (archiveVersion != ARCHIVE_FORMAT_VERSION) {
            throw IllegalArgumentException("Unsupported backup archive version: $archiveVersion")
        }
        if (manifest.getString("appId") != BackupJsonCodec.EXPECTED_APP_ID) {
            throw IllegalArgumentException("This backup file is not for Money Inbox.")
        }
    }

    private fun assertDeclaredPayloadSize(
        manifest: JSONObject,
        actualSize: Int,
    ) {
        if (!manifest.has("payloadUncompressedSize") || manifest.isNull("payloadUncompressedSize")) return
        val declared = manifest.getInt("payloadUncompressedSize")
        if (declared > BackupArchiveLimits.MAX_PAYLOAD_BYTES) {
            throw IllegalArgumentException("Backup payload exceeds maximum allowed size.")
        }
        if (actualSize != declared) {
            throw IllegalArgumentException("Backup payload size does not match manifest.")
        }
    }

    private fun parseAttachmentManifest(manifest: JSONObject): AttachmentManifest {
        val byPath = linkedMapOf<String, AttachmentManifestMeta>()
        val attachmentArray = manifest.optJSONArray("attachments") ?: JSONArray()
        for (i in 0 until attachmentArray.length()) {
            val item = attachmentArray.getJSONObject(i)
            val syncId = item.getString("syncId")
            val path = item.getString("path")
            val expectedSha = item.getString("sha256")
            val declaredSize =
                if (item.has("uncompressedSize") && !item.isNull("uncompressedSize")) {
                    item.getInt("uncompressedSize").also { size ->
                        if (size > BackupArchiveLimits.MAX_ATTACHMENT_ENTRY_BYTES) {
                            throw IllegalArgumentException("Backup attachment exceeds maximum allowed size: $syncId")
                        }
                    }
                } else {
                    null
                }
            byPath[path] =
                AttachmentManifestMeta(
                    syncId = syncId,
                    sha256 = expectedSha,
                    declaredUncompressedSize = declaredSize,
                )
        }
        return AttachmentManifest(byPath = byPath)
    }

    private fun readRequiredEntry(
        zip: ZipInputStream,
        expectedName: String,
        maxEntryBytes: Int,
        totalUncompressedSoFar: Long,
    ): ByteArray {
        val entry =
            zip.nextEntry ?: throw IllegalArgumentException("Backup archive is missing $expectedName.")
        if (entry.isDirectory || entry.name != expectedName) {
            throw IllegalArgumentException("Backup archive is missing $expectedName.")
        }
        val data =
            readEntryBytesCapped(
                zip = zip,
                maxEntryBytes = maxEntryBytes,
                totalUncompressedSoFar = totalUncompressedSoFar,
            )
        zip.closeEntry()
        return data
    }

    private fun readEntryBytesCapped(
        zip: ZipInputStream,
        maxEntryBytes: Int,
        totalUncompressedSoFar: Long,
    ): ByteArray {
        val buffer = ByteArrayOutputStream()
        val chunk = ByteArray(8192)
        while (true) {
            val read = zip.read(chunk)
            if (read <= 0) break
            val nextTotal = totalUncompressedSoFar + buffer.size() + read
            if (buffer.size() + read > maxEntryBytes) {
                throw IllegalArgumentException("Backup archive entry is too large.")
            }
            if (nextTotal > BackupArchiveLimits.MAX_TOTAL_UNCOMPRESSED_BYTES) {
                throw IllegalArgumentException("Backup archive exceeds maximum uncompressed size.")
            }
            buffer.write(chunk, 0, read)
        }
        return buffer.toByteArray()
    }
}
