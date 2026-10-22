package com.mmr.core.parser.permission

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

object SmsPermissionHelper {

    val REQUIRED_PERMISSIONS = arrayOf(
        Manifest.permission.READ_SMS
    )

    fun hasSmsPermission(context: Context): Boolean {
        return REQUIRED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }
}
