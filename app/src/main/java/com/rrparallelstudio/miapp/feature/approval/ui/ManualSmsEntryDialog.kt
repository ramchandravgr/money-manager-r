package com.rrparallelstudio.miapp.feature.approval.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rrparallelstudio.miapp.ui.components.MmrContentDialog
import com.rrparallelstudio.miapp.ui.components.MmrDialogPickerField
import com.rrparallelstudio.miapp.ui.components.MmrDialogTextField
import com.rrparallelstudio.miapp.ui.theme.mmrDatePickerColors
import com.rrparallelstudio.miapp.ui.theme.mmrDatePickerDialogActionContentColor
import com.rrparallelstudio.miapp.ui.theme.mmrTimePickerColors
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
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            MmrDialogTextField(
                value = sender,
                onValueChange = { sender = it },
                label = "Sender",
                singleLine = true,
            )
            MmrDialogTextField(
                value = body,
                onValueChange = { body = it },
                label = "SMS message",
                singleLine = false,
                minLines = 4,
                maxLines = 8,
                isError = errorMessage != null,
                errorMessage = errorMessage,
            )
            MmrDialogPickerField(
                label = "Date & time",
                value = dateTimeLabel,
                onClick = { showDatePicker = true },
            )
        }
    }

    if (showDatePicker) {
        val state =
            rememberDatePickerState(
                initialSelectedDateMillis = selectedDate.atStartOfDay(zone).toInstant().toEpochMilli(),
            )
        val pickerColors = mmrDatePickerColors()
        val actionColor = mmrDatePickerDialogActionContentColor()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            colors = pickerColors,
            confirmButton = {
                TextButton(
                    onClick = {
                        state.selectedDateMillis?.let { millis ->
                            selectedDate = Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()
                        }
                        showDatePicker = false
                        showTimePicker = true
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = actionColor),
                ) {
                    Text("Next")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDatePicker = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = actionColor),
                ) {
                    Text("Cancel")
                }
            },
        ) {
            DatePicker(state = state, colors = pickerColors)
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
            TimePicker(state = timeState, colors = mmrTimePickerColors())
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
        secondaryConfirmLabel = "Add as separate pending transaction",
        onSecondaryConfirm = onAddAsNew,
        dismissLabel = "Cancel",
    )
}
