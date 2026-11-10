package com.rrparallelstudio.miapp.core.parser.permission

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import com.rrparallelstudio.miapp.core.parser.policy.MmrSmsTelephonyPolicy

/** Whether this APK may use device SMS inbox intake (Play release vs debug / sideload). */
object SmsIntakeCapabilities {
    fun isReadSmsDeclared(context: Context): Boolean {
        val packageInfo =
            context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.GET_PERMISSIONS,
            )
        return packageInfo.requestedPermissions?.contains(Manifest.permission.READ_SMS) == true
    }

    fun inboxScanAllowed(context: Context): Boolean =
        MmrSmsTelephonyPolicy.INBOX_SCAN_AND_RECEIVE_ENABLED || isReadSmsDeclared(context)
}
