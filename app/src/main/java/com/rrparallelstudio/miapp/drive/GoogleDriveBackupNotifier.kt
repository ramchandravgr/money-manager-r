package com.rrparallelstudio.miapp.drive

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import androidx.core.app.NotificationCompat
import com.rrparallelstudio.miapp.R
import com.rrparallelstudio.miapp.navigation.Screen
import com.rrparallelstudio.miapp.navigation.buildMainActivityNavIntent
import com.rrparallelstudio.miapp.notification.MmrAppNotificationPoster
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoogleDriveBackupNotifier
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        fun notifyAutoBackupFailed(failureCount: Int) {
            ensureChannel()
            val notification =
                NotificationCompat
                    .Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle("Google Drive backup failed")
                    .setContentText(
                        "Automatic backup failed $failureCount times. Open Backup settings to reconnect or try again.",
                    ).setContentIntent(openDriveBackupPendingIntent())
                    .setAutoCancel(true)
                    .build()
            MmrAppNotificationPoster.notify(context, NOTIFICATION_ID, notification)
        }

        private fun ensureChannel() {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel =
                NotificationChannel(
                    CHANNEL_ID,
                    "Backup",
                    NotificationManager.IMPORTANCE_DEFAULT,
                ).apply {
                    description = "Backup and Google Drive sync status"
                }
            manager.createNotificationChannel(channel)
        }

        private fun openDriveBackupPendingIntent(): PendingIntent {
            val intent = buildMainActivityNavIntent(context, Screen.GoogleDriveBackup.route)
            return PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }

        private companion object {
            const val CHANNEL_ID = "mmr_backup"
            const val NOTIFICATION_ID = 4001
        }
    }
