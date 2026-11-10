package com.rrparallelstudio.miapp.feature.approval.ui

import androidx.compose.runtime.Composable
import com.rrparallelstudio.miapp.ui.components.MmrContentDialog
import com.rrparallelstudio.miapp.ui.components.MmrDialogSmsPreview

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
