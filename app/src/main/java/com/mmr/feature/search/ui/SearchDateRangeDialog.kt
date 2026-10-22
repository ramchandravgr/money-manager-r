package com.mmr.feature.search.ui

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchDateRangeDialog(
    initialStart: LocalDate?,
    initialEnd: LocalDate?,
    onConfirm: (LocalDate, LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    val zone = ZoneId.systemDefault()
    val startMillis = initialStart?.atStartOfDay(zone)?.toInstant()?.toEpochMilli()
    val endMillis = initialEnd?.atStartOfDay(zone)?.toInstant()?.toEpochMilli()

    val state = rememberDateRangePickerState(
        initialSelectedStartDateMillis = startMillis,
        initialSelectedEndDateMillis = endMillis
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val start = state.selectedStartDateMillis?.toLocalDate() ?: return@TextButton
                    val end = state.selectedEndDateMillis?.toLocalDate() ?: start
                    onConfirm(start, end)
                }
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    ) {
        DateRangePicker(state = state, showModeToggle = false)
    }
}

private fun Long.toLocalDate(): LocalDate {
    return Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()
}
