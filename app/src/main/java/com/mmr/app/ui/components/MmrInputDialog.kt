package com.mmr.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mmr.app.ui.theme.MmrMmColors

@Composable
fun MmrContentDialog(
    title: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    message: String? = null,
    confirmLabel: String? = null,
    onConfirm: (() -> Unit)? = null,
    dismissLabel: String = "Cancel",
    showDismissButton: Boolean = true,
    confirmEnabled: Boolean = true,
    dismissEnabled: Boolean = true,
    confirmTestTag: String? = null,
    content: @Composable ColumnScope.() -> Unit = {},
) {
    MmrDialogShell(onDismiss = onDismiss, modifier = modifier) {
        MmrDialogHeader(title = title, message = message)
        content()
        MmrDialogActions(
            onDismiss = onDismiss,
            onConfirm = onConfirm,
            confirmLabel = confirmLabel,
            dismissLabel = dismissLabel,
            showDismissButton = showDismissButton,
            confirmEnabled = confirmEnabled,
            dismissEnabled = dismissEnabled,
            confirmTestTag = confirmTestTag,
        )
    }
}

@Composable
fun MmrConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    dismissLabel: String = "Cancel",
    @Suppress("UNUSED_PARAMETER") confirmIsDestructive: Boolean = false,
    confirmEnabled: Boolean = true,
    dismissEnabled: Boolean = true,
) {
    MmrContentDialog(
        title = title,
        message = message,
        onDismiss = onDismiss,
        modifier = modifier,
        confirmLabel = confirmLabel,
        onConfirm = onConfirm,
        dismissLabel = dismissLabel,
        confirmEnabled = confirmEnabled,
        dismissEnabled = dismissEnabled,
    )
}

@Composable
fun MmrInputDialog(
    title: String,
    value: String,
    onValueChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    message: String? = null,
    placeholder: String? = null,
    suffix: String? = null,
    errorMessage: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    confirmLabel: String = "Save",
    dismissLabel: String = "Cancel",
    fieldEnabled: Boolean = true,
    confirmEnabled: Boolean? = null,
) {
    val canConfirm = confirmEnabled ?: (fieldEnabled && value.isNotBlank())
    MmrContentDialog(
        title = title,
        message = message,
        onDismiss = onDismiss,
        modifier = modifier,
        confirmLabel = confirmLabel,
        onConfirm = onConfirm,
        dismissLabel = dismissLabel,
        confirmEnabled = canConfirm,
        dismissEnabled = fieldEnabled,
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            placeholder = placeholder?.let { { Text(it) } },
            suffix = suffix?.let { { Text(it, color = MmrMmColors.TextMedium) } },
            singleLine = true,
            enabled = fieldEnabled,
            isError = errorMessage != null,
            supportingText =
                errorMessage?.let { msg ->
                    { Text(msg, color = MaterialTheme.colorScheme.error) }
                },
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun MmrDialogShell(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier =
                modifier
                    .fillMaxWidth()
                    .padding(horizontal = MmrDimens.space2xl),
            shape = RoundedCornerShape(20.dp),
            color = MmrMmColors.SurfaceWhite,
            shadowElevation = 10.dp,
            tonalElevation = 2.dp,
        ) {
            Column(
                modifier = Modifier.padding(MmrDimens.cardPadding),
                verticalArrangement = Arrangement.spacedBy(MmrDimens.spaceMd),
                content = content,
            )
        }
    }
}

@Composable
private fun ColumnScope.MmrDialogHeader(
    title: String,
    message: String?,
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.SemiBold,
        color = MmrMmColors.Reversal,
    )
    message?.let {
        Text(
            text = it,
            style = MaterialTheme.typography.bodyMedium,
            color = MmrMmColors.TextMedium,
        )
    }
}

@Composable
private fun MmrDialogActions(
    onDismiss: () -> Unit,
    onConfirm: (() -> Unit)?,
    confirmLabel: String?,
    dismissLabel: String,
    showDismissButton: Boolean,
    confirmEnabled: Boolean,
    dismissEnabled: Boolean,
    confirmTestTag: String? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
    ) {
        if (showDismissButton) {
            TextButton(onClick = onDismiss, enabled = dismissEnabled) {
                Text(dismissLabel, color = MmrMmColors.TextMedium)
            }
        }
        if (confirmLabel != null && onConfirm != null) {
            TextButton(
                onClick = onConfirm,
                enabled = confirmEnabled,
                modifier = confirmTestTag?.let { Modifier.testTag(it) } ?: Modifier,
            ) {
                Text(
                    confirmLabel,
                    fontWeight = FontWeight.SemiBold,
                    color = if (confirmEnabled) MmrMmColors.Active else MmrMmColors.TextMedium,
                )
            }
        }
    }
}
