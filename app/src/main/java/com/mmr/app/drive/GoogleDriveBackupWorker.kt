package com.mmr.app.drive

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.mmr.core.data.preferences.GoogleDriveBackupPreferencesStore
import com.mmr.core.domain.repository.GoogleDriveBackupRepository
import com.mmr.core.drive.GoogleDriveBackupContracts
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class GoogleDriveBackupWorker
    @AssistedInject
    constructor(
        @Assisted context: Context,
        @Assisted params: WorkerParameters,
        private val authGateway: GoogleDriveAuthGateway,
        private val driveBackupRepository: GoogleDriveBackupRepository,
        private val preferencesStore: GoogleDriveBackupPreferencesStore,
        private val notifier: GoogleDriveBackupNotifier,
    ) : CoroutineWorker(context, params) {
        override suspend fun doWork(): Result {
            if (!authGateway.isConfigured() || !preferencesStore.isLinked()) {
                return Result.success()
            }
            if (preferencesStore.getAutoSchedule() == com.mmr.core.drive.GoogleDriveAutoSchedule.OFF) {
                return Result.success()
            }
            if (preferencesStore.isWifiOnly() && !isOnWifi(applicationContext)) {
                return Result.retry()
            }
            val token = authGateway.fetchAccessToken()
            if (token.isNullOrBlank()) {
                return recordFailure(Result.retry())
            }
            return try {
                driveBackupRepository.uploadBackup(token)
                preferencesStore.setConsecutiveFailureCount(0)
                Result.success()
            } catch (e: Exception) {
                recordFailure(Result.retry())
            }
        }

        private fun recordFailure(default: Result): Result {
            val failures = preferencesStore.getConsecutiveFailureCount() + 1
            preferencesStore.setConsecutiveFailureCount(failures)
            if (failures >= GoogleDriveBackupContracts.AUTO_FAILURE_NOTIFY_THRESHOLD) {
                notifier.notifyAutoBackupFailed(failures)
            }
            return default
        }

        private fun isOnWifi(context: Context): Boolean {
            val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = manager.activeNetwork ?: return false
            val caps = manager.getNetworkCapabilities(network) ?: return false
            return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
        }
    }
