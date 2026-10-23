package com.mmr.feature.transaction.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.mmr.app.testing.MmrTestTags
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.util.Date
import java.util.Locale

@Composable
fun TransactionFormPickerField(
    label: String,
    value: String,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val interactive = enabled && onClick != null
    val pickerColors =
        OutlinedTextFieldDefaults.colors(
            disabledTextColor = MaterialTheme.colorScheme.onSurface,
            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            disabledBorderColor = MaterialTheme.colorScheme.outline,
            disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .then(
                    if (interactive) {
                        Modifier.clickable(onClick = onClick!!)
                    } else {
                        Modifier
                    },
                ),
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            enabled = false,
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth(),
            colors =
                if (interactive) {
                    pickerColors
                } else {
                    OutlinedTextFieldDefaults.colors()
                },
            trailingIcon =
                if (interactive) {
                    {
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = null)
                    }
                } else {
                    null
                },
            singleLine = true,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDateTimeField(
    dateTimeMillis: Long,
    onDateTimeMillisChange: (Long) -> Unit,
    modifier: Modifier = Modifier,
    zoneId: ZoneId = ZoneId.systemDefault(),
) {
    var showDatePicker by remember { mutableStateOf(false) }
    val label =
        remember(dateTimeMillis) {
            SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(dateTimeMillis))
        }

    TransactionFormPickerField(
        label = "Date",
        value = label,
        onClick = { showDatePicker = true },
        modifier = modifier,
    )

    if (showDatePicker) {
        val state =
            rememberDatePickerState(
                initialSelectedDateMillis = dateTimeMillis,
            )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        state.selectedDateMillis?.let { pickerMillis ->
                            val selectedDate =
                                Instant
                                    .ofEpochMilli(pickerMillis)
                                    .atZone(zoneId)
                                    .toLocalDate()
                            val millis =
                                selectedDate
                                    .atTime(LocalTime.now())
                                    .atZone(zoneId)
                                    .toInstant()
                                    .toEpochMilli()
                            onDateTimeMillisChange(millis)
                        }
                        showDatePicker = false
                    },
                ) {
                    Text("OK")
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
}

@Composable
fun TransactionAmountField(
    amount: String,
    onAmountChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = amount,
        onValueChange = onAmountChange,
        modifier =
            modifier
                .testTag(MmrTestTags.transactionAmount)
                .fillMaxWidth(),
        label = { Text("Amount") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
    )
}

@Composable
fun TransactionNoteField(
    note: String,
    onNoteChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    suggestions: List<String> = emptyList(),
) {
    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = note,
            onValueChange = onNoteChange,
            modifier =
                Modifier
                    .testTag(MmrTestTags.transactionNote)
                    .fillMaxWidth(),
            label = { Text("Note") },
            minLines = 2,
        )
        if (suggestions.isNotEmpty()) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
            ) {
                suggestions.forEach { suggestion ->
                    TextButton(
                        onClick = { onNoteChange(suggestion) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = suggestion,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}
