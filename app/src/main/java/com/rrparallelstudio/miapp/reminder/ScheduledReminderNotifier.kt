package com.rrparallelstudio.miapp.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.rrparallelstudio.miapp.R
import com.rrparallelstudio.miapp.core.database.entity.NotificationRuleEntity
import com.rrparallelstudio.miapp.core.model.enums.NotificationRuleType
import com.rrparallelstudio.miapp.navigation.Screen
import com.rrparallelstudio.miapp.navigation.buildMainActivityNavIntent
import com.rrparallelstudio.miapp.notification.MmrAppNotificationPoster
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScheduledReminderNotifier
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        fun show(rule: NotificationRuleEntity) {
            ensureChannel()

            val route =
                when (rule.type) {
                    NotificationRuleType.RECURRING_REMINDER -> {
                        val ruleId = rule.targetRecurringRuleId ?: return
                        Screen.RecurringDetail.createRoute(ruleId)
                    }
                    NotificationRuleType.LOAN_DUE -> {
                        val loanId = rule.targetLoanId ?: return
                        Screen.LoanDetail.createRoute(loanId)
                    }
                    NotificationRuleType.ASSET_VALUATION_REMINDER -> {
                        val assetId = rule.targetAssetId ?: return
                        Screen.AssetDetail.createRoute(assetId)
                    }
                    else -> return
                }

            val intent = buildMainActivityNavIntent(context, route)
            val pendingIntent =
                PendingIntent.getActivity(
                    context,
                    rule.id.toInt(),
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )

            val title = rule.title?.trim()?.takeIf { it.isNotEmpty() } ?: "Payment reminder"
            val body = rule.message?.trim()?.takeIf { it.isNotEmpty() } ?: "Tap to view details"

            val notification =
                NotificationCompat
                    .Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_launcher_monochrome)
                    .setContentTitle(title)
                    .setContentText(body)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                    .setCategory(NotificationCompat.CATEGORY_REMINDER)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .build()

            MmrAppNotificationPoster.notify(context, NOTIFICATION_ID_BASE + rule.id.toInt(), notification)
        }

        private fun ensureChannel() {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
            val manager = context.getSystemService(NotificationManager::class.java) ?: return
            val channel =
                NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.notification_channel_payment_reminders),
                    NotificationManager.IMPORTANCE_DEFAULT,
                ).apply {
                    description = context.getString(R.string.notification_channel_payment_reminders_desc)
                }
            manager.createNotificationChannel(channel)
        }

        private companion object {
            const val CHANNEL_ID = "mmr_payment_reminders"
            const val NOTIFICATION_ID_BASE = 53_000
        }
    }
