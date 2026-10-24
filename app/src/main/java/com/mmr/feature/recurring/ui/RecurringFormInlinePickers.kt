package com.mmr.feature.recurring.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mmr.app.ui.components.MmrTransactionFormExpandablePicker
import com.mmr.app.ui.components.MmrTransactionFormIcons
import com.mmr.app.ui.theme.MmrBrandColors
import com.mmr.feature.recurring.model.AddEditRecurringRuleUiEvent
import com.mmr.feature.recurring.model.AddEditRecurringRuleUiState
import com.mmr.feature.transaction.ui.CategoryPickerPanelState
import com.mmr.feature.transaction.ui.CategorySubcategoryPickerPanel
import com.mmr.feature.transaction.ui.recurringCategoryPickerCallbacks

internal enum class RecurringFormExpandedField {
    None,
    Category,
    Account,
    TransferFrom,
    TransferTo,
    Reminder,
}

@Composable
fun RecurringExpandableCategoryField(
    state: AddEditRecurringRuleUiState,
    displayValue: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onEvent: (AddEditRecurringRuleUiEvent) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    MmrTransactionFormExpandablePicker(
        label = "Category",
        value = displayValue,
        emptyHint = "Select category",
        icon = MmrTransactionFormIcons.category,
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        modifier = modifier,
        enabled = enabled,
    ) {
        CategorySubcategoryPickerPanel(
            panelState =
                CategoryPickerPanelState(
                    categories = state.categories,
                    pickerHighlightedCategoryId = state.pickerHighlightedCategoryId,
                ),
            callbacks = recurringCategoryPickerCallbacks(onEvent),
            maxPanelHeight = 300.dp,
            onPickerFinished = { onExpandedChange(false) },
        )
    }
}

@Composable
fun RecurringExpandableReminderField(
    displayValue: String,
    selectedMinutes: Int,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    reminderOptions: List<Pair<Int, String>>,
) {
    MmrTransactionFormExpandablePicker(
        label = "Reminder",
        value = displayValue,
        emptyHint = "None",
        icon = MmrTransactionFormIcons.reminder,
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        modifier = modifier,
    ) {
        Column {
            reminderOptions.forEach { (minutes, label) ->
                RecurringPickerRow(
                    label = label,
                    selected = minutes == selectedMinutes,
                    onClick = {
                        onSelected(minutes)
                        onExpandedChange(false)
                    },
                )
            }
        }
    }
}

@Composable
internal fun RecurringPickerRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(if (selected) MmrBrandColors.ListBackground else MmrBrandColors.SurfaceWhite)
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = MmrBrandColors.Reversal,
        )
    }
    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
}
