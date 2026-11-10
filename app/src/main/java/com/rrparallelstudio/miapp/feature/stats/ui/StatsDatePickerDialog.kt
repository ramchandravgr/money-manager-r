package com.rrparallelstudio.miapp.feature.stats.ui

import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import com.rrparallelstudio.miapp.ui.theme.mmrDatePickerColors
import com.rrparallelstudio.miapp.ui.theme.mmrDatePickerDialogActionContentColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsDatePickerDialog(
    initialMillis: Long?,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit,
) {
    val state =
        rememberDatePickerState(
            initialSelectedDateMillis = initialMillis ?: System.currentTimeMillis(),
        )
    val pickerColors = mmrDatePickerColors()
    val actionColor = mmrDatePickerDialogActionContentColor()
    DatePickerDialog(
        onDismissRequest = onDismiss,
        colors = pickerColors,
        confirmButton = {
            TextButton(
                onClick = { state.selectedDateMillis?.let(onConfirm) },
                colors = ButtonDefaults.textButtonColors(contentColor = actionColor),
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = actionColor),
            ) {
                Text("Cancel")
            }
        },
    ) {
        DatePicker(state = state, colors = pickerColors)
    }
}
