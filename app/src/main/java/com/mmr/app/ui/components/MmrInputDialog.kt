package com.mmr.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mmr.app.ui.theme.MmrBrandColors

/**
 * Centered modal dialog — **Design 12**:
 * white body (mockup title/message/fields via [MmrDialogForm]), gray footer band,
 * full-width primary + optional secondary + dismiss text.
 */
@Composable
fun MmrContentDialog(
    title: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    message: String? = null,
    confirmLabel: String? = null,
    onConfirm: (() -> Unit)? = null,
    secondaryConfirmLabel: String? = null,
    onSecondaryConfirm: (() -> Unit)? = null,
    dismissLabel: String = "Cancel",
    showDismissButton: Boolean = true,
    confirmEnabled: Boolean = true,
    secondaryConfirmEnabled: Boolean = true,
    dismissEnabled: Boolean = true,
    confirmIsDestructive: Boolean = false,
    confirmTestTag: String? = null,
    secondaryConfirmTestTag: String? = null,
    content: @Composable ColumnScope.() -> Unit = {},
) {
    MmrDialogShell(onDismiss = onDismiss, modifier = modifier) {
        MmrDialogScrollBody {
            MmrDialogHeader(title = title, message = message)
            content()
        }
        MmrDialogFooterCtaBar(
            onDismiss = onDismiss,
            confirmLabel = confirmLabel,
            onConfirm = onConfirm,
            secondaryConfirmLabel = secondaryConfirmLabel,
            onSecondaryConfirm = onSecondaryConfirm,
            dismissLabel = dismissLabel,
            showDismissButton = showDismissButton,
            confirmEnabled = confirmEnabled,
            secondaryConfirmEnabled = secondaryConfirmEnabled,
            dismissEnabled = dismissEnabled,
            confirmIsDestructive = confirmIsDestructive,
            confirmTestTag = confirmTestTag,
            secondaryConfirmTestTag = secondaryConfirmTestTag,
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
    confirmIsDestructive: Boolean = false,
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
        confirmIsDestructive = confirmIsDestructive,
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
        MmrDialogTextField(
            value = value,
            onValueChange = onValueChange,
            label = label,
            placeholder = placeholder,
            singleLine = true,
            enabled = fieldEnabled,
            isError = errorMessage != null,
            errorMessage = errorMessage,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            modifier = Modifier.fillMaxWidth(),
        )
        suffix?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.labelSmall,
                color = MmrBrandColors.TextMedium,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

/** List-style single choice inside the standard MMR dialog shell (settings pickers). */
@Composable
fun MmrSingleChoiceDialog(
    title: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    message: String? = null,
    dismissLabel: String = "Close",
    content: @Composable ColumnScope.() -> Unit,
) {
    MmrDialogShell(onDismiss = onDismiss, modifier = modifier) {
        MmrDialogScrollBody {
            MmrDialogHeader(title = title, message = message)
            content()
        }
        MmrDialogFooterCtaBar(
            onDismiss = onDismiss,
            confirmLabel = dismissLabel,
            onConfirm = onDismiss,
            showDismissButton = false,
        )
    }
}

@Composable
fun MmrChoiceDialogRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    MmrDialogRadioRow(
        label = label,
        selected = selected,
        onClick = onClick,
        modifier = modifier,
    )
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
                    .padding(horizontal = MmrDimens.space2xl)
                    .imePadding(),
            shape = RoundedCornerShape(16.dp),
            color = MmrBrandColors.SurfaceWhite,
            shadowElevation = 10.dp,
            tonalElevation = 0.dp,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                content = content,
            )
        }
    }
}

@Composable
private fun MmrDialogScrollBody(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(max = 420.dp)
                .verticalScroll(rememberScrollState())
                .padding(
                    start = MmrDimens.cardPadding,
                    end = MmrDimens.cardPadding,
                    top = MmrDimens.cardPadding,
                    bottom = MmrDimens.spaceSm,
                ),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        content = content,
    )
}

@Composable
private fun MmrDialogFooterCtaBar(
    onDismiss: () -> Unit,
    confirmLabel: String? = null,
    onConfirm: (() -> Unit)? = null,
    secondaryConfirmLabel: String? = null,
    onSecondaryConfirm: (() -> Unit)? = null,
    dismissLabel: String = "Cancel",
    showDismissButton: Boolean = true,
    confirmEnabled: Boolean = true,
    secondaryConfirmEnabled: Boolean = true,
    dismissEnabled: Boolean = true,
    confirmIsDestructive: Boolean = false,
    confirmTestTag: String? = null,
    secondaryConfirmTestTag: String? = null,
) {
    MmrFooterCtaBar(
        bandBackground = MmrBrandColors.SurfaceWhite,
        primaryLabel = confirmLabel,
        onPrimary = onConfirm,
        primaryEnabled = confirmEnabled,
        primaryIsDestructive = confirmIsDestructive,
        primaryTestTag = confirmTestTag,
        secondaryLabel = secondaryConfirmLabel,
        onSecondary = onSecondaryConfirm,
        secondaryEnabled = secondaryConfirmEnabled,
        secondaryTestTag = secondaryConfirmTestTag,
        textLabel = if (showDismissButton) dismissLabel else null,
        onText = if (showDismissButton) onDismiss else null,
        textEnabled = dismissEnabled,
    )
}
