package com.mmr.core.attachment

import java.io.File

object MmrAttachmentSyncId {
    private val SAFE_FILENAME = Regex("^[a-zA-Z0-9._-]{1,128}$")

    fun sanitizeForPath(syncId: String): String =
        syncId
            .trim()
            .replace(Regex("[^a-zA-Z0-9._-]"), "_")
            .ifBlank { "attachment" }

    fun requireSafeSyncId(syncId: String): String {
        val trimmed = syncId.trim()
        require(trimmed.isNotEmpty()) { "Empty attachment syncId" }
        require(!trimmed.contains("..")) { "Invalid attachment syncId" }
        require(SAFE_FILENAME.matches(trimmed)) { "Invalid attachment syncId" }
        return trimmed
    }

    fun fileInRoot(
        rootDir: File,
        syncId: String,
    ): File {
        val safe = requireSafeSyncId(syncId)
        val root = rootDir.canonicalFile
        val file = File(root, safe).canonicalFile
        val rootPath = root.path
        val filePath = file.path
        require(filePath == rootPath || filePath.startsWith("$rootPath${File.separator}")) {
            "Attachment path outside storage root"
        }
        return file
    }
}
