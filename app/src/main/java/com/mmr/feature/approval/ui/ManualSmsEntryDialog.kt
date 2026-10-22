package com.mmr.feature.approval.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualSmsEntryDialog(
    errorMessage: String?,
    onDismiss: () -> Unit,
    onSubmit: (sender: String, body: String, timestampMillis: Long) -> Unit
) {
    var sender by remember { mutableStateOf("Manual") }
    var body by remember { mutableStateOf("") }
    val zone = ZoneId.systemDefault()
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var selectedTime by remember { mutableStateOf(LocalTime.now().withSecond(0).withNano(0)) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val dateTimeLabel = remember(selectedDate, selectedTime) {
        val millis = selectedDate.atTime(selectedTime).atZone(zone).toInstant().toEpochMilli()
        SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(millis))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add SMS manually") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = sender,
                    onValueChange = { sender = it },
                    label = { Text("Sender") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = body,
                    onValueChange = { body = it },
                    label = { Text("SMS message") },
                    minLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Date & time: $dateTimeLabel")
                }
                errorMessage?.let { msg ->
                    Text(
                        text = msg,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val millis = selectedDate.atTime(selectedTime).atZone(zone).toInstant().toEpochMilli()
                    onSubmit(sender, body, millis)
                },
                enabled = body.isNotEmpty()
            ) {
                Text("Add to pending")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )

    if (showDatePicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate.atStartOfDay(zone).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        state.selectedDateMillis?.let { millis ->
                            selectedDate = Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()
                        }
                        showDatePicker = false
                        showTimePicker = true
                    }
                ) {
                    Text("Next")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = state)
        }
    }

    if (showTimePicker) {
        val timeState = rememberTimePickerState(
            initialHour = selectedTime.hour,
            initialMinute = selectedTime.minute,
            is24Hour = false
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Select time") },
            text = {
                TimePicker(state = timeState)
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        selectedTime = LocalTime.of(timeState.hour, timeState.minute)
                        showTimePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun ManualSmsDuplicateDialog(
    priorActionLabel: String,
    approvalId: Long,
    onRevertPrior: () -> Unit,
    onAddAsNew: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Duplicate SMS text") },
        text = {
            Text(
                "This exact message was already $priorActionLabel (approval #$approvalId). " +
                    "Do you want to revert that action and use this new entry, or add a separate pending transaction?"
            )
        },
        confirmButton = {
            TextButton(onClick = onRevertPrior) {
                Text("Revert & use new")
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onAddAsNew) {
                    Text("Add as new")
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}
