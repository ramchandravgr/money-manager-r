package com.mmr.app.ui.security

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.mmr.app.security.MmrDeviceCredentialGate
import com.mmr.app.ui.theme.MmrBrandColors

@Composable
fun MmrAppLockGate(
    activity: FragmentActivity,
    onUnlocked: () -> Unit,
) {
    LaunchedEffect(activity) {
        MmrDeviceCredentialGate.authenticate(
            activity = activity,
            title = "Unlock Money Inbox",
            subtitle = "Use your device screen lock or biometrics",
            onSuccess = onUnlocked,
            onFailure = { /* stay on gate; user can tap Unlock to retry */ },
        )
    }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Money Inbox is locked",
            style = MaterialTheme.typography.titleLarge,
            color = MmrBrandColors.TextStrong,
            textAlign = TextAlign.Center,
        )
        Text(
            text = "Confirm your identity to view balances and approvals.",
            style = MaterialTheme.typography.bodyMedium,
            color = MmrBrandColors.TextMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 12.dp),
        )
        TextButton(
            onClick = {
                MmrDeviceCredentialGate.authenticate(
                    activity = activity,
                    title = "Unlock Money Inbox",
                    subtitle = "Use your device screen lock or biometrics",
                    onSuccess = onUnlocked,
                    onFailure = { },
                )
            },
            modifier = Modifier.padding(top = 24.dp),
        ) {
            Text("Unlock")
        }
    }
}
