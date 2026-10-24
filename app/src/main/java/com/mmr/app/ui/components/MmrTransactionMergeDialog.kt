package com.mmr.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

data class MmrMergeDateOption(
    val dateTimeMillis: Long,
    val label: String,
)

/**
 * Reusable merge flow dialog (home ledger merge, and similar name + optional date picks).
 */
@Composable
fun MmrTransactionMergeDialog(
    title: String,
    message: String,
    name: String,
    onNameChange: (String) -> Unit,
    nameLabel: String,
    namePlaceholder: String,
    dateOptions: List<MmrMergeDateOption>,
    selectedDateTimeMillis: Long?,
    onDateSelected: (Long) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    errorMessage: String? = null,
    confirmLabel: String = "Merge",
    confirmEnabled: Boolean = name.trim().isNotEmpty(),
    confirmTestTag: String? = null,
) {
    MmrContentDialog(
        title = title,
        message = message,
        onDismiss = onDismiss,
        confirmLabel = confirmLabel,
        onConfirm = onConfirm,
        confirmEnabled = confirmEnabled,
        confirmTestTag = confirmTestTag,
        showDismissButton = true,
        dismissLabel = "Cancel",
    ) {
        MmrDialogTextField(
            value = name,
            onValueChange = onNameChange,
            label = nameLabel,
            placeholder = namePlaceholder,
            singleLine = true,
            isError = errorMessage != null,
            errorMessage = errorMessage,
            modifier = Modifier.fillMaxWidth(),
        )
        if (dateOptions.size > 1) {
            MmrDialogFieldLabel("Use date")
            Column(modifier = Modifier.fillMaxWidth()) {
                dateOptions.forEach { option ->
                    MmrChoiceDialogRow(
                        label = option.label,
                        selected = selectedDateTimeMillis == option.dateTimeMillis,
                        onClick = { onDateSelected(option.dateTimeMillis) },
                    )
                }
            }
        }
    }
}
