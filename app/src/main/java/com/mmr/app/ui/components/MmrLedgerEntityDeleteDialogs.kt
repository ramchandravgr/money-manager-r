package com.mmr.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mmr.core.domain.model.AccountDeletionPreview
import com.mmr.core.domain.model.CategoryDeleteTransactionMode

data class EntityPickerOption(
    val id: Long,
    val label: String,
)

@Composable
fun AccountDeleteDialog(
    accountName: String,
    preview: AccountDeletionPreview,
    replacementOptions: List<EntityPickerOption>,
    selectedReplacementId: Long?,
    onSelectReplacement: (Long) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    working: Boolean,
) {
    val needsReassign =
        !preview.isBlocked &&
            (preview.linkedTransactionCount > 0 || preview.linkedRecurringRuleCount > 0)
    val canConfirm =
        when {
            preview.isBlocked -> false
            needsReassign -> selectedReplacementId != null && replacementOptions.isNotEmpty()
            else -> true
        }

    MmrContentDialog(
        title = "Remove account?",
        message =
            when {
                preview.isBlocked ->
                    preview.blockReason
                        ?: "This account cannot be removed."
                needsReassign ->
                    buildString {
                        append("\"$accountName\" is used on ")
                        if (preview.linkedTransactionCount > 0) {
                            append("${preview.linkedTransactionCount} transaction(s)")
                        }
                        if (preview.linkedTransactionCount > 0 && preview.linkedRecurringRuleCount > 0) {
                            append(" and ")
                        }
                        if (preview.linkedRecurringRuleCount > 0) {
                            append("${preview.linkedRecurringRuleCount} recurring rule(s)")
                        }
                        append(". Move them to another active account, then remove this one.")
                    }
                else ->
                    "\"$accountName\" will be removed from pickers. You can add a new account with the same name later."
            },
        onDismiss = onDismiss,
        confirmLabel = if (preview.isBlocked) "Close" else "Remove account",
        onConfirm = if (preview.isBlocked) onDismiss else onConfirm,
        confirmEnabled = if (preview.isBlocked) true else canConfirm && !working,
        confirmIsDestructive = !preview.isBlocked,
        showDismissButton = !preview.isBlocked,
        dismissLabel = "Cancel",
    ) {
        if (needsReassign) {
            if (replacementOptions.isEmpty()) {
                MmrHelperText(
                    text = "Add another active account before removing this one.",
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            } else {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    MmrDialogFieldLabel("Move to")
                    replacementOptions.forEach { option ->
                        MmrChoiceDialogRow(
                            label = option.label,
                            selected = option.id == selectedReplacementId,
                            onClick = { onSelectReplacement(option.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryDeleteDialog(
    categoryName: String,
    transactionCount: Int,
    migrateOptions: List<EntityPickerOption>,
    onConfirm: (CategoryDeleteTransactionMode, Long?) -> Unit,
    onDismiss: () -> Unit,
    working: Boolean,
) {
    var mode by remember(transactionCount) {
        mutableStateOf(
            if (transactionCount > 0) CategoryDeleteTransactionMode.MIGRATE else CategoryDeleteTransactionMode.NONE,
        )
    }
    var selectedTargetId by remember(migrateOptions) {
        mutableStateOf(migrateOptions.firstOrNull()?.id)
    }

    val canConfirm =
        when {
            transactionCount == 0 -> true
            mode == CategoryDeleteTransactionMode.UNCATEGORIZE -> true
            mode == CategoryDeleteTransactionMode.MIGRATE ->
                selectedTargetId != null && migrateOptions.isNotEmpty()
            else -> false
        }

    MmrContentDialog(
        title = "Remove category?",
        message =
            if (transactionCount > 0) {
                "\"$categoryName\" is used on $transactionCount transaction(s). " +
                    "Choose what to do with those tags before removing the category."
            } else {
                "\"$categoryName\" and its subcategories will be removed from pickers."
            },
        onDismiss = onDismiss,
        confirmLabel = "Remove category",
        onConfirm = {
            onConfirm(
                if (transactionCount > 0) mode else CategoryDeleteTransactionMode.NONE,
                if (mode == CategoryDeleteTransactionMode.MIGRATE) selectedTargetId else null,
            )
        },
        confirmEnabled = canConfirm && !working,
        confirmIsDestructive = true,
        showDismissButton = true,
        dismissLabel = "Cancel",
    ) {
        if (transactionCount > 0) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                MmrDialogRadioRow(
                    label = "Move transactions to another category",
                    selected = mode == CategoryDeleteTransactionMode.MIGRATE,
                    onClick = { mode = CategoryDeleteTransactionMode.MIGRATE },
                )
                if (mode == CategoryDeleteTransactionMode.MIGRATE) {
                    if (migrateOptions.isEmpty()) {
                        MmrHelperText(
                            text = "No other category of the same type is available. Add one or mark as uncategorized.",
                            modifier = Modifier.padding(start = 48.dp),
                        )
                    } else {
                        migrateOptions.forEach { option ->
                            MmrChoiceDialogRow(
                                label = option.label,
                                selected = option.id == selectedTargetId,
                                onClick = { selectedTargetId = option.id },
                                modifier = Modifier.padding(start = 40.dp),
                            )
                        }
                    }
                }
                MmrDialogRadioRow(
                    label = "Mark all as uncategorized",
                    selected = mode == CategoryDeleteTransactionMode.UNCATEGORIZE,
                    onClick = { mode = CategoryDeleteTransactionMode.UNCATEGORIZE },
                )
            }
        }
    }
}
