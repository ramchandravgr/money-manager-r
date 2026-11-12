package com.rrparallelstudio.miapp.budget

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import com.rrparallelstudio.miapp.R
import com.rrparallelstudio.miapp.core.common.display.MmrMoneyFormat
import com.rrparallelstudio.miapp.core.domain.model.BudgetProgressSnapshot
import com.rrparallelstudio.miapp.notification.MmrAppNotificationPoster
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BudgetAlertNotifier
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        fun notifyExceeded(snapshot: BudgetProgressSnapshot) {
            ensureChannel()
            val symbol = "₹"
            val body =
                "${snapshot.targetLabel}: ${MmrMoneyFormat.format(snapshot.spentAmount, symbol)} of " +
                    "${MmrMoneyFormat.format(snapshot.limitAmount, symbol)} (${snapshot.periodLabel})"
            val notification =
                NotificationCompat
                    .Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_launcher_monochrome)
                    .setContentTitle("Budget exceeded")
                    .setContentText(body)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                    .setAutoCancel(true)
                    .build()
            MmrAppNotificationPoster.notify(
                context,
                NOTIFICATION_ID_BASE + snapshot.budgetId.toInt(),
                notification,
            )
        }

        private fun ensureChannel() {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel =
                NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.notification_channel_budget_alerts),
                    NotificationManager.IMPORTANCE_DEFAULT,
                ).apply {
                    description = context.getString(R.string.notification_channel_budget_alerts_desc)
                }
            manager.createNotificationChannel(channel)
        }

        private companion object {
            const val CHANNEL_ID = "mmr_budget_alerts"
            const val NOTIFICATION_ID_BASE = 52_000
        }
    }
