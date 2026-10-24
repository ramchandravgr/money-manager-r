package com.mmr.core.drive

import com.mmr.core.data.preferences.GoogleDriveBackupPreferencesStore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoogleDriveBackupCoordinator
    @Inject
    constructor(
        private val driveClient: GoogleDriveRestClient,
        private val preferencesStore: GoogleDriveBackupPreferencesStore,
    ) {
        suspend fun ensureBackupsFolderId(accessToken: String): String {
            val cached = preferencesStore.getBackupsFolderId()
            if (!cached.isNullOrBlank()) return cached
            val rootId = "root"
            val moneyInbox =
                driveClient.findFolderByName(accessToken, rootId, GoogleDriveBackupContracts.FOLDER_MONEY_INBOX)
                    ?: driveClient.createFolder(accessToken, rootId, GoogleDriveBackupContracts.FOLDER_MONEY_INBOX)
            val backups =
                driveClient.findFolderByName(
                    accessToken,
                    moneyInbox.id,
                    GoogleDriveBackupContracts.FOLDER_BACKUPS,
                )
                    ?: driveClient.createFolder(
                        accessToken,
                        moneyInbox.id,
                        GoogleDriveBackupContracts.FOLDER_BACKUPS,
                    )
            preferencesStore.setBackupsFolderId(backups.id)
            return backups.id
        }

        fun uploadWithRotation(
            accessToken: String,
            folderId: String,
            archiveBytes: ByteArray,
        ) {
            val files = driveClient.listFilesInFolder(accessToken, folderId)
            val current =
                files.find { it.name == GoogleDriveBackupContracts.FILE_CURRENT_NAME }
            if (current != null) {
                val datedName = datedBackupName(current.modifiedTimeMillis)
                driveClient.renameFile(accessToken, current.id, datedName)
            }
            driveClient.uploadFile(
                accessToken,
                folderId,
                GoogleDriveBackupContracts.FILE_CURRENT_NAME,
                archiveBytes,
            )
            pruneOldBackups(accessToken, folderId)
        }

        fun listBackupFiles(
            accessToken: String,
            folderId: String,
        ): List<GoogleDriveFileRef> =
            driveClient
                .listFilesInFolder(accessToken, folderId)
                .filter { it.name.endsWith(GoogleDriveBackupContracts.FILE_EXTENSION) }
                .sortedByDescending { it.modifiedTimeMillis }

        fun downloadBackup(
            accessToken: String,
            fileId: String,
        ): ByteArray = driveClient.downloadFile(accessToken, fileId)

        private fun pruneOldBackups(
            accessToken: String,
            folderId: String,
        ) {
            val backups =
                driveClient
                    .listFilesInFolder(accessToken, folderId)
                    .filter { it.name.endsWith(GoogleDriveBackupContracts.FILE_EXTENSION) }
                    .sortedByDescending { it.modifiedTimeMillis }
            if (backups.size <= GoogleDriveBackupContracts.MAX_RETAINED_FILES) return
            backups
                .drop(GoogleDriveBackupContracts.MAX_RETAINED_FILES)
                .forEach { driveClient.deleteFile(accessToken, it.id) }
        }

        private fun datedBackupName(modifiedTimeMillis: Long): String {
            val date =
                SimpleDateFormat("yyyy-MM-dd", Locale.US).format(
                    Date(if (modifiedTimeMillis > 0L) modifiedTimeMillis else System.currentTimeMillis()),
                )
            return GoogleDriveBackupContracts.FILE_DATED_PREFIX + date + GoogleDriveBackupContracts.FILE_EXTENSION
        }
    }
