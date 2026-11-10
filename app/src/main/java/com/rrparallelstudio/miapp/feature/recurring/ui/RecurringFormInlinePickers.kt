package com.rrparallelstudio.miapp.feature.recurring.ui

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
import com.rrparallelstudio.miapp.feature.recurring.model.AddEditRecurringRuleUiEvent
import com.rrparallelstudio.miapp.feature.recurring.model.AddEditRecurringRuleUiState
import com.rrparallelstudio.miapp.feature.transaction.ui.CategoryPickerPanelState
import com.rrparallelstudio.miapp.feature.transaction.ui.CategorySubcategoryPickerPanel
import com.rrparallelstudio.miapp.feature.transaction.ui.recurringCategoryPickerCallbacks
import com.rrparallelstudio.miapp.ui.components.MmrTransactionFormExpandablePicker
import com.rrparallelstudio.miapp.ui.components.MmrTransactionFormIcons
import com.rrparallelstudio.miapp.ui.theme.MmrBrandColors
import com.rrparallelstudio.miapp.ui.theme.MmrChipPaints

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
        emptyHint = "Select reminder",
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
                .background(MmrChipPaints.rowBackground(selected))
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = MmrBrandColors.formCardPrimaryText(),
        )
    }
    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
}
