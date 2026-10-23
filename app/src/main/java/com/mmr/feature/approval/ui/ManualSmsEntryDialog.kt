package com.mmr.feature.approval.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mmr.app.ui.components.MmrContentDialog
import com.mmr.app.ui.components.MmrSecondaryActionButton
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
    onSubmit: (sender: String, body: String, timestampMillis: Long) -> Unit,
) {
    var sender by remember { mutableStateOf("Manual") }
    var body by remember { mutableStateOf("") }
    val zone = ZoneId.systemDefault()
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var selectedTime by remember { mutableStateOf(LocalTime.now().withSecond(0).withNano(0)) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val dateTimeLabel =
        remember(selectedDate, selectedTime) {
            val millis =
                selectedDate
                    .atTime(selectedTime)
                    .atZone(zone)
                    .toInstant()
                    .toEpochMilli()
            SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(millis))
        }

    MmrContentDialog(
        title = "Add SMS manually",
        message = "Paste bank SMS text to parse as a pending approval.",
        onDismiss = onDismiss,
        confirmLabel = "Add to pending",
        onConfirm = {
            val millis =
                selectedDate
                    .atTime(selectedTime)
                    .atZone(zone)
                    .toInstant()
                    .toEpochMilli()
            onSubmit(sender, body, millis)
        },
        confirmEnabled = body.isNotBlank(),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = sender,
                onValueChange = { sender = it },
                label = { Text("Sender") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = body,
                onValueChange = { body = it },
                label = { Text("SMS message") },
                minLines = 4,
                modifier = Modifier.fillMaxWidth(),
                isError = errorMessage != null,
                supportingText = errorMessage?.let { { Text(it) } },
            )
            MmrSecondaryActionButton(
                text = "Date & time: $dateTimeLabel",
                onClick = { showDatePicker = true },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    if (showDatePicker) {
        val state =
            rememberDatePickerState(
                initialSelectedDateMillis = selectedDate.atStartOfDay(zone).toInstant().toEpochMilli(),
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
                    },
                ) {
                    Text("Next")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            },
        ) {
            DatePicker(state = state)
        }
    }

    if (showTimePicker) {
        val timeState =
            rememberTimePickerState(
                initialHour = selectedTime.hour,
                initialMinute = selectedTime.minute,
                is24Hour = false,
            )
        MmrContentDialog(
            title = "Select time",
            onDismiss = { showTimePicker = false },
            confirmLabel = "OK",
            onConfirm = {
                selectedTime = LocalTime.of(timeState.hour, timeState.minute)
                showTimePicker = false
            },
        ) {
            TimePicker(state = timeState)
        }
    }
}

@Composable
fun ManualSmsDuplicateDialog(
    priorActionLabel: String,
    approvalId: Long,
    onRevertPrior: () -> Unit,
    onAddAsNew: () -> Unit,
    onDismiss: () -> Unit,
) {
    MmrContentDialog(
        title = "Duplicate SMS text",
        message = "This exact message was already $priorActionLabel (approval #$approvalId).",
        onDismiss = onDismiss,
        confirmLabel = "Revert & use new",
        onConfirm = onRevertPrior,
        dismissLabel = "Cancel",
    ) {
        MmrSecondaryActionButton(
            text = "Add as separate pending transaction",
            onClick = onAddAsNew,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
