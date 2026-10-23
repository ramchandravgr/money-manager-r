package com.mmr.feature.transaction.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.input.KeyboardType
import com.mmr.app.ui.components.MmrInputDialog
import com.mmr.feature.transaction.model.PickerNameEditorKind
import com.mmr.feature.transaction.model.PickerNameEditorState

@Composable
fun TransactionPickerNameEditorDialog(
    editor: PickerNameEditorState,
    onNameChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    val title =
        when (editor.kind) {
            PickerNameEditorKind.ADD_CATEGORY -> "New category"
            PickerNameEditorKind.EDIT_CATEGORY -> "Edit category"
            PickerNameEditorKind.ADD_SUBCATEGORY -> "New subcategory"
            PickerNameEditorKind.EDIT_SUBCATEGORY -> "Edit subcategory"
        }

    MmrInputDialog(
        title = title,
        value = editor.name,
        onValueChange = onNameChange,
        onConfirm = onSave,
        onDismiss = onDismiss,
        label = "Name",
        placeholder = "e.g. Groceries",
        keyboardType = KeyboardType.Text,
        confirmLabel = if (editor.isSaving) "Saving…" else "Save",
        fieldEnabled = !editor.isSaving,
        confirmEnabled = !editor.isSaving && editor.name.isNotBlank(),
        errorMessage = editor.errorMessage,
    )
}
