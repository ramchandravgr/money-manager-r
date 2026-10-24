package com.mmr.feature.transaction.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mmr.app.ui.theme.MmrBrandColors
import com.mmr.feature.transaction.model.AddEditTransactionUiEvent
import com.mmr.feature.transaction.model.AddEditTransactionUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategorySubcategoryPickerSheet(
    state: AddEditTransactionUiState,
    onEvent: (AddEditTransactionUiEvent) -> Unit,
    sheetState: SheetState,
    onDismiss: () -> Unit,
) {
    state.pickerNameEditor?.let { editor ->
        TransactionPickerNameEditorDialog(
            editor = editor,
            onNameChange = { onEvent(AddEditTransactionUiEvent.OnPickerNameEditorChanged(it)) },
            onSave = { onEvent(AddEditTransactionUiEvent.OnPickerNameEditorSave) },
            onDismiss = { onEvent(AddEditTransactionUiEvent.OnPickerNameEditorDismiss) },
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MmrBrandColors.SurfaceWhite,
        dragHandle = { BottomSheetDefaults.DragHandle() },
    ) {
        CategorySubcategoryPickerPanel(
            state = state,
            onEvent = onEvent,
            modifier = Modifier.fillMaxWidth(),
            maxPanelHeight = 380.dp,
            onPickerFinished = onDismiss,
            showDismissAction = true,
            onDismiss = onDismiss,
        )
    }
}
