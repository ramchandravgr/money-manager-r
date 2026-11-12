package com.rrparallelstudio.miapp.core.backup

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.rrparallelstudio.miapp.core.data.preferences.GoogleDriveBackupPreferencesStore
import com.rrparallelstudio.miapp.core.data.preferences.LocalAutoBackupPreferencesStore
import com.rrparallelstudio.miapp.core.data.preferences.LocalBackupFolderStore
import com.rrparallelstudio.miapp.core.domain.repository.BackupRepository
import com.rrparallelstudio.miapp.core.domain.repository.GoogleDriveBackupRepository
import com.rrparallelstudio.miapp.core.drive.GoogleDriveAutoBackupPolicy
import com.rrparallelstudio.miapp.core.drive.GoogleDriveAutoSchedule
import com.rrparallelstudio.miapp.drive.GoogleDriveAuthGateway
import dagger.hilt.android.qualifiers.ApplicationContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UnifiedBackupRunner
    @Inject
    constructor(
        private val backupRepository: BackupRepository,
        private val folderSaver: LocalBackupFolderSaver,
        private val folderStore: LocalBackupFolderStore,
        private val localPreferencesStore: LocalAutoBackupPreferencesStore,
        private val drivePreferencesStore: GoogleDriveBackupPreferencesStore,
        private val driveBackupRepository: GoogleDriveBackupRepository,
        private val authGateway: GoogleDriveAuthGateway,
        private val retention: LocalAutoBackupRetention,
        @ApplicationContext private val context: Context,
    ) {
        data class Result(
            val savedFileName: String,
            val driveUploaded: Boolean = false,
            val driveDeferredWifi: Boolean = false,
        )

        suspend fun exportScheduledBackup(): Result? {
            val schedule = localPreferencesStore.getAutoSchedule()
            if (schedule == GoogleDriveAutoSchedule.OFF) return null
            if (!folderStore.isConfigured()) return null
            if (
                !GoogleDriveAutoBackupPolicy.isBackupDue(
                    schedule,
                    localPreferencesStore.getLastSuccessfulBackupMillis(),
                )
            ) {
                return null
            }
            return exportNow(uploadToDrive = true)
        }

        suspend fun exportNow(uploadToDrive: Boolean): Result {
            if (!folderStore.isConfigured()) {
                throw IllegalStateException("Choose a backup folder first.")
            }
            val archive = backupRepository.exportBackupArchive()
            val fileName = buildAutoBackupFileName()
            val saved = folderSaver.saveToConfiguredFolder(archive, fileName)
            val now = System.currentTimeMillis()
            localPreferencesStore.setLastSuccessfulBackupMillis(now)
            folderSaver.pruneRetainedInFolder()
            retention.pruneRetainedAutoBackups()

            var driveUploaded = false
            var driveDeferredWifi = false
            if (uploadToDrive && drivePreferencesStore.isLinked() && authGateway.isConfigured()) {
                if (drivePreferencesStore.isWifiOnly() && !isOnWifi(context)) {
                    driveDeferredWifi = true
                } else {
                    val token =
                        authGateway.fetchAccessToken()
                            ?: throw IllegalStateException("Connect Google Drive again.")
                    driveBackupRepository.uploadBackupWithArchive(token, archive)
                    driveUploaded = true
                }
            }
            return Result(
                savedFileName = saved,
                driveUploaded = driveUploaded,
                driveDeferredWifi = driveDeferredWifi,
            )
        }

        private fun buildAutoBackupFileName(): String {
            val stamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
            return "${LocalAutoBackupContracts.FILE_NAME_PREFIX}$stamp.${BackupSignedZipCodec.SUGGESTED_FILE_EXTENSION}"
        }

        private fun isOnWifi(context: Context): Boolean {
            val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = manager.activeNetwork ?: return false
            val caps = manager.getNetworkCapabilities(network) ?: return false
            return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
        }
    }
