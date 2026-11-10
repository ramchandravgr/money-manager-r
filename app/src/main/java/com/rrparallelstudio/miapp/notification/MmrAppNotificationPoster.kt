package com.rrparallelstudio.miapp.notification

import android.annotation.SuppressLint
import android.content.Context
import androidx.core.app.NotificationManagerCompat
import com.rrparallelstudio.miapp.core.parser.permission.SmsPermissionHelper

/** Posts notifications only when POST_NOTIFICATIONS (API 33+) and channel settings allow. */
object MmrAppNotificationPoster {
    @SuppressLint("MissingPermission")
    fun notify(
        context: Context,
        notificationId: Int,
        notification: android.app.Notification,
    ) {
        if (!SmsPermissionHelper.hasPostNotificationsPermission(context)) return
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return
        manager.notify(notificationId, notification)
    }

    fun cancel(
        context: Context,
        notificationId: Int,
    ) {
        NotificationManagerCompat.from(context).cancel(notificationId)
    }
}
