package com.mmr.feature.transaction.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mmr.feature.transaction.model.PickerNameEditorKind
import com.mmr.feature.transaction.model.PickerNameEditorState

@Composable
fun TransactionPickerNameEditorDialog(
    editor: PickerNameEditorState,
    onNameChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    val title = when (editor.kind) {
        PickerNameEditorKind.ADD_CATEGORY -> "New category"
        PickerNameEditorKind.EDIT_CATEGORY -> "Edit category"
        PickerNameEditorKind.ADD_SUBCATEGORY -> "New subcategory"
        PickerNameEditorKind.EDIT_SUBCATEGORY -> "Edit subcategory"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = editor.name,
                    onValueChange = onNameChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Name") },
                    singleLine = true,
                    enabled = !editor.isSaving
                )
                editor.errorMessage?.let { message ->
                    Text(
                        text = message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onSave, enabled = !editor.isSaving) {
                Text(if (editor.isSaving) "Saving…" else "Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !editor.isSaving) {
                Text("Cancel")
            }
        }
    )
}
