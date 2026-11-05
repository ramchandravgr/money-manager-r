package com.mmr.app.sms

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.mmr.app.R
import com.mmr.app.navigation.Screen
import com.mmr.app.navigation.buildMainActivityNavIntent
import com.mmr.app.notification.MmrAppNotificationPoster
import com.mmr.core.domain.repository.ApprovalRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmsApprovalSummaryNotifier
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val approvalRepository: ApprovalRepository,
    ) {
        fun showSummary(
            pendingCount: Int,
            latestLine: String?,
            quickApproveApprovalId: Long? = null,
        ) {
            if (pendingCount <= 0) {
                MmrAppNotificationPoster.cancel(context, NOTIFICATION_ID)
                return
            }

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

            val openIntent = buildMainActivityNavIntent(context, Screen.ApprovalCenter.createRoute())
            val contentPendingIntent =
                PendingIntent.getActivity(
                    context,
                    REQUEST_OPEN,
                    openIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )

            val builder =
                NotificationCompat
                    .Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_launcher_monochrome)
                    .setContentTitle(title)
                    .setContentText(text)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(text))
                    .setContentIntent(contentPendingIntent)
                    .setAutoCancel(true)
                    .setOnlyAlertOnce(true)
                    .setNumber(pendingCount)
                    .setCategory(NotificationCompat.CATEGORY_STATUS)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)

            builder.addAction(
                R.drawable.ic_launcher_monochrome,
                context.getString(R.string.notification_action_open_approvals),
                contentPendingIntent,
            )

            if (quickApproveApprovalId != null && quickApproveApprovalId > 0L) {
                val approveIntent =
                    Intent(context, SmsApprovalNotificationReceiver::class.java).apply {
                        action = SmsApprovalNotificationReceiver.ACTION_APPROVE
                        putExtra(SmsApprovalNotificationReceiver.EXTRA_APPROVAL_ID, quickApproveApprovalId)
                    }
                val approvePendingIntent =
                    PendingIntent.getBroadcast(
                        context,
                        quickApproveApprovalId.toInt(),
                        approveIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                    )
                builder.addAction(
                    R.drawable.ic_launcher_monochrome,
                    context.getString(R.string.notification_action_approve_latest),
                    approvePendingIntent,
                )
            }

            MmrAppNotificationPoster.notify(context, NOTIFICATION_ID, builder.build())
        }

        suspend fun refreshAfterPendingQueueChanged() {
            val pendingCount = approvalRepository.getPendingCountOnce()
            if (pendingCount <= 0) {
                MmrAppNotificationPoster.cancel(context, NOTIFICATION_ID)
                return
            }
            val quickId = approvalRepository.getLatestQuickApprovablePendingId()
            showSummary(
                pendingCount = pendingCount,
                latestLine = null,
                quickApproveApprovalId = quickId,
            )
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
            private const val REQUEST_OPEN = 41010
        }
    }
