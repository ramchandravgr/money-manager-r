package com.mmr.feature.approval.ui

import androidx.compose.runtime.Composable
import com.mmr.app.ui.components.MmrContentDialog
import com.mmr.app.ui.components.MmrDialogSmsPreview

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
        MmrDialogSmsPreview(
            fromLine = "From: $sender",
            body = body.ifBlank { "(No message body stored.)" },
        )
    }
}
