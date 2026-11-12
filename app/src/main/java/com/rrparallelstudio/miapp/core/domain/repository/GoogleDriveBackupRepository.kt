package com.rrparallelstudio.miapp.core.domain.repository

import com.rrparallelstudio.miapp.core.drive.GoogleDriveFileRef

interface GoogleDriveBackupRepository {
    suspend fun uploadBackup(accessToken: String)

    suspend fun uploadBackupWithArchive(
        accessToken: String,
        archiveBytes: ByteArray,
    )

    suspend fun listRemoteBackups(accessToken: String): List<GoogleDriveFileRef>

    suspend fun downloadBackup(
        accessToken: String,
        fileId: String,
    ): ByteArray
}
