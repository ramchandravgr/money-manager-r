package com.mmr.app.drive

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.mmr.app.MainActivity
import com.mmr.app.R
import com.mmr.app.navigation.Screen
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
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        }

        private fun ensureChannel() {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
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
            val intent =
                Intent(context, MainActivity::class.java).apply {
                    putExtra(MainActivity.EXTRA_NAV_ROUTE, Screen.GoogleDriveBackup.route)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
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
