package com.mmr.core.domain.repository

import com.mmr.core.drive.GoogleDriveFileRef

interface GoogleDriveBackupRepository {
    suspend fun uploadBackup(accessToken: String)

    suspend fun listRemoteBackups(accessToken: String): List<GoogleDriveFileRef>

    suspend fun downloadBackup(
        accessToken: String,
        fileId: String,
    ): ByteArray
}
