package com.mmr.app.sms

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.mmr.app.MainActivity
import com.mmr.app.R
import com.mmr.app.navigation.Screen
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmsApprovalSummaryNotifier
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        fun showSummary(
            pendingCount: Int,
            latestLine: String?,
        ) {
            if (pendingCount <= 0) return
            if (!canPostNotifications()) return

            ensureChannel()

            val title =
                if (pendingCount == 1) {
                    "1 transaction to review"
                } else {
                    "$pendingCount transactions to review"
                }
            val text =
                latestLine?.let { "Latest: $it" }
                    ?: "Tap to open Approval Center"

            val intent =
                Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra(MainActivity.EXTRA_NAV_ROUTE, Screen.ApprovalCenter.createRoute())
                }
            val pendingIntent =
                PendingIntent.getActivity(
                    context,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )

            val notification =
                NotificationCompat
                    .Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_launcher_monochrome)
                    .setContentTitle(title)
                    .setContentText(text)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(text))
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                    .setOnlyAlertOnce(true)
                    .setNumber(pendingCount)
                    .setCategory(NotificationCompat.CATEGORY_STATUS)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .build()

            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        }

        private fun canPostNotifications(): Boolean {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (
                    ContextCompat.checkSelfPermission(
                        context,
                        android.Manifest.permission.POST_NOTIFICATIONS,
                    ) != android.content.pm.PackageManager.PERMISSION_GRANTED
                ) {
                    return false
                }
            }
            return NotificationManagerCompat.from(context).areNotificationsEnabled()
        }

        private fun ensureChannel() {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
            val manager = context.getSystemService(NotificationManager::class.java) ?: return
            val channel =
                NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.notification_channel_sms_approvals),
                    NotificationManager.IMPORTANCE_DEFAULT,
                ).apply {
                    description = context.getString(R.string.notification_channel_sms_approvals_desc)
                }
            manager.createNotificationChannel(channel)
        }

        companion object {
            const val NOTIFICATION_ID = 4101
            const val CHANNEL_ID = "mmr_sms_approvals"
        }
    }
