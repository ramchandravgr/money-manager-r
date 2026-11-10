package com.rrparallelstudio.miapp.core.attachment

import android.content.Context
import com.rrparallelstudio.miapp.core.attachment.MmrAttachmentSyncId
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AssetAttachmentStore
    @Inject
    constructor(
        @ApplicationContext context: Context,
    ) {
        private val rootDir: File =
            File(context.filesDir, "asset_attachments").apply { mkdirs() }

        fun fileForSyncId(syncId: String): File = MmrAttachmentSyncId.fileInRoot(rootDir, syncId)

        fun readBytes(syncId: String): ByteArray? {
            val file = runCatching { fileForSyncId(syncId) }.getOrNull() ?: return null
            if (!file.isFile) return null
            return runCatching { file.readBytes() }.getOrNull()
        }

        fun writeBytes(
            syncId: String,
            bytes: ByteArray,
        ) {
            fileForSyncId(syncId).writeBytes(bytes)
        }

        fun delete(syncId: String) {
            runCatching { fileForSyncId(syncId).delete() }
        }

        fun clearAllFiles() {
            rootDir.listFiles()?.forEach { runCatching { it.delete() } }
        }
    }
