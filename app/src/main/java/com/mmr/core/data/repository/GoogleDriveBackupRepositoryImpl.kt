package com.mmr.core.data.repository

import com.mmr.core.common.dispatcher.IoDispatcher
import com.mmr.core.data.preferences.GoogleDriveBackupPreferencesStore
import com.mmr.core.domain.repository.BackupRepository
import com.mmr.core.domain.repository.GoogleDriveBackupRepository
import com.mmr.core.drive.GoogleDriveBackupCoordinator
import com.mmr.core.drive.GoogleDriveFileRef
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoogleDriveBackupRepositoryImpl
    @Inject
    constructor(
        private val backupRepository: BackupRepository,
        private val coordinator: GoogleDriveBackupCoordinator,
        private val preferencesStore: GoogleDriveBackupPreferencesStore,
        @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) : GoogleDriveBackupRepository {
        override suspend fun uploadBackup(accessToken: String) =
            withContext(ioDispatcher) {
                val archive = backupRepository.exportBackupArchive()
                val folderId = coordinator.ensureBackupsFolderId(accessToken)
                coordinator.uploadWithRotation(accessToken, folderId, archive)
                preferencesStore.setLastSuccessfulBackupMillis(System.currentTimeMillis())
                preferencesStore.setConsecutiveFailureCount(0)
            }

        override suspend fun listRemoteBackups(accessToken: String): List<GoogleDriveFileRef> =
            withContext(ioDispatcher) {
                val folderId = coordinator.ensureBackupsFolderId(accessToken)
                coordinator.listBackupFiles(accessToken, folderId)
            }

        override suspend fun downloadBackup(
            accessToken: String,
            fileId: String,
        ): ByteArray =
            withContext(ioDispatcher) {
                coordinator.downloadBackup(accessToken, fileId)
            }
    }
