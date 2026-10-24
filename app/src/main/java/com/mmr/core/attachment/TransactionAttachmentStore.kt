package com.mmr.core.attachment

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionAttachmentStore
    @Inject
    constructor(
        @ApplicationContext context: Context,
    ) {
        private val rootDir: File =
            File(context.filesDir, "transaction_attachments").apply { mkdirs() }

        fun fileForSyncId(syncId: String): File = File(rootDir, syncId)

        fun readBytes(syncId: String): ByteArray? {
            val file = fileForSyncId(syncId)
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
