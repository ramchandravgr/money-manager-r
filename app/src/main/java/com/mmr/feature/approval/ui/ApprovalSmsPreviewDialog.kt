package com.mmr.feature.approval.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.mmr.app.ui.components.MmrContentDialog
import com.mmr.app.ui.components.MmrDimens

@Composable
fun ApprovalSmsPreviewDialog(
    sender: String,
    body: String,
    onDismiss: () -> Unit,
) {
    MmrContentDialog(
        title = "SMS message",
        onDismiss = onDismiss,
        confirmLabel = "Close",
        onConfirm = onDismiss,
        showDismissButton = false,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(MmrDimens.spaceSm),
        ) {
            Text(
                text = "From: $sender",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = body.ifBlank { "(No message body stored.)" },
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
