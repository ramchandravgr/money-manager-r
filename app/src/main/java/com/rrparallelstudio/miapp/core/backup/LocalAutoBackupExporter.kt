package com.rrparallelstudio.miapp.core.backup

import com.rrparallelstudio.miapp.core.data.preferences.LocalAutoBackupPreferencesStore
import com.rrparallelstudio.miapp.core.domain.repository.BackupRepository
import com.rrparallelstudio.miapp.core.drive.GoogleDriveAutoBackupPolicy
import com.rrparallelstudio.miapp.core.drive.GoogleDriveAutoSchedule
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalAutoBackupExporter
    @Inject
    constructor(
        private val backupRepository: BackupRepository,
        private val downloadsSaver: LocalBackupDownloadsSaver,
        private val preferencesStore: LocalAutoBackupPreferencesStore,
        private val retention: LocalAutoBackupRetention,
    ) {
        data class Result(
            val savedFileName: String,
        )

        suspend fun exportScheduledBackup(): Result? {
            val schedule = preferencesStore.getAutoSchedule()
            if (schedule == GoogleDriveAutoSchedule.OFF) return null
            if (!GoogleDriveAutoBackupPolicy.isBackupDue(
                    schedule,
                    preferencesStore.getLastSuccessfulBackupMillis(),
                )
            ) {
                return null
            }
            return exportNow()
        }

        suspend fun exportNow(): Result {
            val archive = backupRepository.exportBackupArchive()
            val fileName = buildAutoBackupFileName()
            val saved = downloadsSaver.saveToDownloads(archive, fileName)
            preferencesStore.setLastSuccessfulBackupMillis(System.currentTimeMillis())
            retention.pruneRetainedAutoBackups()
            return Result(savedFileName = saved)
        }

        private fun buildAutoBackupFileName(): String {
            val stamp =
                SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
            return "${LocalAutoBackupContracts.FILE_NAME_PREFIX}$stamp.${BackupSignedZipCodec.SUGGESTED_FILE_EXTENSION}"
        }
    }
