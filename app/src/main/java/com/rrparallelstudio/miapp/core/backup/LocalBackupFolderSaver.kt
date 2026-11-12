package com.rrparallelstudio.miapp.core.backup

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import com.rrparallelstudio.miapp.core.data.preferences.LocalBackupFolderStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalBackupFolderSaver
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val folderStore: LocalBackupFolderStore,
    ) {
        fun saveToConfiguredFolder(
            archiveBytes: ByteArray,
            displayName: String,
        ): String {
            val treeUri =
                folderStore.getFolderTreeUri()
                    ?: throw IllegalStateException("Choose a backup folder first.")
            val safeName = displayName.replace(Regex("[/\\\\?*\\[\\]:]"), "-")
            val root =
                DocumentFile.fromTreeUri(context, treeUri)
                    ?: throw IllegalStateException("Backup folder is no longer available.")
            val existing = root.findFile(safeName)
            if (existing != null && existing.isFile) {
                existing.delete()
            }
            val created =
                root.createFile("application/zip", safeName)
                    ?: throw IllegalStateException("Could not create backup file in the selected folder.")
            context.contentResolver.openOutputStream(created.uri)?.use { out ->
                out.write(archiveBytes)
            } ?: throw IllegalStateException("Could not write backup file.")
            return safeName
        }

        fun readLatestBackupBytes(): ByteArray? {
            val treeUri = folderStore.getFolderTreeUri() ?: return null
            val root = DocumentFile.fromTreeUri(context, treeUri) ?: return null
            val latest =
                root
                    .listFiles()
                    .filter { it.isFile && it.name.orEmpty().startsWith(LocalAutoBackupContracts.FILE_NAME_PREFIX) }
                    .maxByOrNull { it.lastModified() }
                    ?: return null
            return context.contentResolver.openInputStream(latest.uri)?.use { it.readBytes() }
        }

        fun pruneRetainedInFolder() {
            val treeUri = folderStore.getFolderTreeUri() ?: return
            val root = DocumentFile.fromTreeUri(context, treeUri) ?: return
            val files =
                root
                    .listFiles()
                    .filter { it.isFile && it.name.orEmpty().startsWith(LocalAutoBackupContracts.FILE_NAME_PREFIX) }
                    .sortedByDescending { it.lastModified() }
            files.drop(LocalAutoBackupContracts.MAX_RETAINED_FILES).forEach { it.delete() }
        }
    }
