package com.rrparallelstudio.miapp.permission

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import com.rrparallelstudio.miapp.core.data.preferences.LaunchPermissionStore
import com.rrparallelstudio.miapp.core.parser.permission.SmsPermissionHelper

/**
 * On first app entry, requests notification permission once (does not block the UI if denied).
 * SMS inbox/receive is not requested on Play builds ([com.rrparallelstudio.miapp.core.parser.policy.MmrSmsTelephonyPolicy]).
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

    val notificationsLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
            onResult = {
                launchPermissionStore.markLaunchPermissionFlowCompleted()
                onComplete()
            },
        )

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !SmsPermissionHelper.hasPostNotificationsPermission(context)
        ) {
            notificationsLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            launchPermissionStore.markLaunchPermissionFlowCompleted()
            onComplete()
        }
    }
}
