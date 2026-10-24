package com.mmr.feature.transaction.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.mmr.app.testing.MmrTestTags
import com.mmr.app.ui.components.MmrTransactionFormAttachIconButton
import com.mmr.app.ui.components.MmrTransactionFormChipOption
import com.mmr.app.ui.components.MmrTransactionFormChoiceChips
import com.mmr.app.ui.components.MmrTransactionFormIcons
import com.mmr.app.ui.components.MmrTransactionFormPickerRow
import com.mmr.app.ui.components.MmrTransactionFormPickerRowLabeled
import com.mmr.app.ui.components.MmrTransactionFormTextRow
import com.mmr.core.model.enums.RecurrenceInterval
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
    icon: androidx.compose.ui.graphics.vector.ImageVector = MmrTransactionFormIcons.category,
    emptyHint: String = "Select ${label.lowercase()}",
    useLabeledRow: Boolean = false,
) {
    if (useLabeledRow) {
        MmrTransactionFormPickerRowLabeled(
            label = label,
            value = value,
            emptyHint = emptyHint,
            icon = icon,
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
        )
    } else {
        MmrTransactionFormPickerRow(
            icon = icon,
            placeholder = label,
            value = value.ifBlank { emptyHint },
            emptyHint = emptyHint,
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
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
    val formatted =
        remember(dateTimeMillis) {
            SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(dateTimeMillis))
        }

    TransactionFormPickerField(
        label = "Date",
        value = formatted,
        emptyHint = "",
        onClick = { showDatePicker = true },
        modifier = modifier,
        icon = MmrTransactionFormIcons.date,
        useLabeledRow = false,
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
fun TransactionNameField(
    name: String,
    onNameChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = MmrTestTags.recurringFieldName,
    placeholder: String = "Name",
) {
    MmrTransactionFormTextRow(
        value = name,
        onValueChange = onNameChange,
        placeholder = placeholder,
        modifier = modifier,
        leadingIcon = MmrTransactionFormIcons.name,
        testTag = testTag,
        singleLine = true,
    )
}

@Composable
fun TransactionAmountField(
    amount: String,
    onAmountChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    MmrTransactionFormTextRow(
        value = amount,
        onValueChange = onAmountChange,
        placeholder = "Amount",
        modifier = modifier,
        leadingIcon = MmrTransactionFormIcons.amount,
        testTag = MmrTestTags.transactionAmount,
        keyboardType = KeyboardType.Decimal,
        singleLine = true,
        groupedAmountDisplay = true,
    )
}

@Composable
fun TransactionNoteField(
    note: String,
    onNoteChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    suggestions: List<String> = emptyList(),
    onAttachPhotoClick: (() -> Unit)? = null,
    canAttachMore: Boolean = true,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        MmrTransactionFormTextRow(
            value = note,
            onValueChange = onNoteChange,
            placeholder = "Note",
            leadingIcon = MmrTransactionFormIcons.note,
            testTag = MmrTestTags.transactionNote,
            singleLine = false,
            minLines = 2,
            trailingContent =
                if (onAttachPhotoClick != null) {
                    {
                        MmrTransactionFormAttachIconButton(
                            onClick = onAttachPhotoClick,
                            enabled = canAttachMore,
                        )
                    }
                } else {
                    null
                },
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

private val recurrenceIntervalOptions =
    listOf(
        MmrTransactionFormChipOption(RecurrenceInterval.DAILY, "Daily"),
        MmrTransactionFormChipOption(RecurrenceInterval.WEEKLY, "Weekly"),
        MmrTransactionFormChipOption(RecurrenceInterval.MONTHLY, "Monthly"),
        MmrTransactionFormChipOption(RecurrenceInterval.YEARLY, "Yearly"),
    )

@Composable
fun TransactionCategoryPickerField(
    value: String,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    TransactionFormPickerField(
        label = "Category",
        value = value,
        emptyHint = "Select category",
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        icon = MmrTransactionFormIcons.category,
    )
}

@Composable
fun TransactionAccountPickerField(
    value: String,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    label: String = "Account",
    emptyHint: String = "Select account",
) {
    TransactionFormPickerField(
        label = label,
        value = value,
        emptyHint = emptyHint,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        icon = MmrTransactionFormIcons.account,
    )
}

@Composable
fun TransactionTransferFromPickerField(
    value: String,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    TransactionFormPickerField(
        label = "From account",
        value = value,
        emptyHint = "Select account",
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        icon = MmrTransactionFormIcons.transferFrom,
    )
}

@Composable
fun TransactionTransferToPickerField(
    value: String,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    emptyHint: String = "Select destination",
) {
    TransactionFormPickerField(
        label = "To",
        value = value,
        emptyHint = emptyHint,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        icon = MmrTransactionFormIcons.transferTo,
    )
}

@Composable
fun TransactionReminderPickerField(
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TransactionFormPickerField(
        label = "Reminder",
        value = value,
        emptyHint = "None",
        onClick = onClick,
        modifier = modifier,
        icon = MmrTransactionFormIcons.reminder,
    )
}

@Composable
fun TransactionRecurrenceIntervalChips(
    selected: RecurrenceInterval,
    onSelected: (RecurrenceInterval) -> Unit,
    modifier: Modifier = Modifier,
) {
    MmrTransactionFormChoiceChips(
        options = recurrenceIntervalOptions,
        selected = selected,
        onSelected = onSelected,
        modifier = modifier,
    )
}
