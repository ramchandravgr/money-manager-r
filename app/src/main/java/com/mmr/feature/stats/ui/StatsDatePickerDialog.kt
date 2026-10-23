package com.mmr.feature.stats.ui

import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable

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
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { state.selectedDateMillis?.let(onConfirm) }) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    ) {
        DatePicker(state = state)
    }
}
