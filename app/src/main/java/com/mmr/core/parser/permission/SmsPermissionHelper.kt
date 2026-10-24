package com.mmr.core.parser.permission

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

object SmsPermissionHelper {
    /** Inbox scan (Approval Center manual scan). */
    val REQUIRED_PERMISSIONS =
        arrayOf(
            Manifest.permission.READ_SMS,
        )

    /** Live `SMS_RECEIVED` → pending approval pipeline (Settings → notify toggle). */
    val LIVE_RECEIVE_SMS_PERMISSION = Manifest.permission.RECEIVE_SMS

    fun hasSmsPermission(context: Context): Boolean =
        REQUIRED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }

    fun hasReceiveSmsPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, LIVE_RECEIVE_SMS_PERMISSION) ==
            PackageManager.PERMISSION_GRANTED

    fun hasPostNotificationsPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    }

    /** Permissions required for Settings → live notify + `IncomingSmsReceiver`. */
    fun missingForLiveInbound(context: Context): List<LiveInboundPermission> {
        val missing = mutableListOf<LiveInboundPermission>()
        if (!hasReceiveSmsPermission(context)) {
            missing.add(LiveInboundPermission.RECEIVE_SMS)
        }
        if (!hasPostNotificationsPermission(context)) {
            missing.add(LiveInboundPermission.POST_NOTIFICATIONS)
        }
        return missing
    }

    fun isLiveInboundReady(context: Context): Boolean = missingForLiveInbound(context).isEmpty()

    enum class LiveInboundPermission {
        RECEIVE_SMS,
        POST_NOTIFICATIONS,
    }
}
