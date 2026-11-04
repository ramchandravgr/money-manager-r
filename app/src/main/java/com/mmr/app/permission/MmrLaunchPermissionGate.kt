package com.mmr.app.permission

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.mmr.core.data.preferences.LaunchPermissionStore
import com.mmr.core.parser.permission.SmsPermissionHelper

/**
 * On first app entry, requests notification + SMS permissions once (does not block the UI if denied).
 */
@Composable
fun MmrLaunchPermissionGate(
    launchPermissionStore: LaunchPermissionStore,
    onComplete: () -> Unit,
) {
    if (launchPermissionStore.hasCompletedLaunchPermissionFlow()) {
        LaunchedEffect(Unit) { onComplete() }
        return
    }

    val context = LocalContext.current
    var step by remember { mutableStateOf(PermissionStep.Notifications) }

    val smsPermissionsLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestMultiplePermissions(),
            onResult = {
                launchPermissionStore.markLaunchPermissionFlowCompleted()
                onComplete()
            },
        )

    val notificationsLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
            onResult = {
                step = PermissionStep.Sms
            },
        )

    LaunchedEffect(step) {
        when (step) {
            PermissionStep.Notifications -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    !SmsPermissionHelper.hasPostNotificationsPermission(context)
                ) {
                    notificationsLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    step = PermissionStep.Sms
                }
            }
            PermissionStep.Sms -> {
                val toRequest =
                    buildList {
                        if (!SmsPermissionHelper.hasReceiveSmsPermission(context)) {
                            add(SmsPermissionHelper.LIVE_RECEIVE_SMS_PERMISSION)
                        }
                        if (!SmsPermissionHelper.hasSmsPermission(context)) {
                            add(Manifest.permission.READ_SMS)
                        }
                    }
                if (toRequest.isEmpty()) {
                    launchPermissionStore.markLaunchPermissionFlowCompleted()
                    onComplete()
                } else {
                    smsPermissionsLauncher.launch(toRequest.toTypedArray())
                }
            }
        }
    }
}

private enum class PermissionStep {
    Notifications,
    Sms,
}
