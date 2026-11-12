package com.rrparallelstudio.miapp.feature.settings.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import com.rrparallelstudio.miapp.feature.settings.viewmodel.ConfigurationViewModel
import com.rrparallelstudio.miapp.security.MmrDeviceCredentialGate
import com.rrparallelstudio.miapp.ui.components.MmrSettingsGroup
import com.rrparallelstudio.miapp.ui.components.MmrSettingsGroupSwitchRow
import com.rrparallelstudio.miapp.ui.components.MmrSettingsScreenLayout
import com.rrparallelstudio.miapp.ui.components.MmrSettingsScreenScaffold

@Composable
fun SecuritySettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: ConfigurationViewModel = hiltViewModel(),
) {
    val appLockOnLaunch by viewModel.appLockOnLaunch.collectAsState()
    val context = LocalContext.current
    val activity = context as? FragmentActivity

    MmrSettingsScreenScaffold(
        title = "Security",
        onNavigateBack = onNavigateBack,
        layout = MmrSettingsScreenLayout.InsetGrouped,
    ) {
        MmrSettingsGroup(
            title = "App Lock",
            isFirstInScreen = true,
            footer =
                "When you open the app, or come back to it, unlock with this phone's PIN, pattern, or " +
                    "fingerprint. The phone needs a screen lock first. Turning this on asks you to confirm once.",
        ) {
            MmrSettingsGroupSwitchRow(
                label = "Lock App On Launch",
                checked = appLockOnLaunch,
                onCheckedChange = { enabled ->
                    if (!enabled) {
                        viewModel.setAppLockOnLaunch(false)
                        return@MmrSettingsGroupSwitchRow
                    }
                    val host = activity
                    if (host == null) {
                        viewModel.setAppLockOnLaunch(true)
                        return@MmrSettingsGroupSwitchRow
                    }
                    MmrDeviceCredentialGate.authenticate(
                        activity = host,
                        title = "Turn on app lock",
                        subtitle = "Confirm with your device screen lock or biometrics",
                        onSuccess = { viewModel.setAppLockOnLaunch(true) },
                        onFailure = { },
                    )
                },
                showDividerBelow = false,
            )
        }
    }
}
