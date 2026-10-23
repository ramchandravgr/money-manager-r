package com.mmr.feature.search.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mmr.app.ui.components.MmrDimens

@Composable
fun SearchFilterPickerRow(
    label: String,
    value: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onClear: (() -> Unit)? = null,
    menuContent: @Composable () -> Unit,
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        SearchFilterRow(
            label = label,
            value = value,
            showDropdownIndicator = true,
            onClick = { onExpandedChange(true) },
            onClear = onClear,
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) },
            modifier = Modifier.widthIn(min = 220.dp, max = 320.dp),
        ) {
            menuContent()
        }
    }
}

@Composable
fun SearchFilterRow(
    label: String,
    value: String,
    onClick: () -> Unit,
    onClear: (() -> Unit)? = null,
    showDropdownIndicator: Boolean = false,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onClick)
                    .padding(vertical = MmrDimens.listItemPaddingVertical),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(MmrDimens.formLabelWidth),
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (showDropdownIndicator) {
                Icon(
                    imageVector = Icons.Filled.ArrowDropDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (onClear != null) {
                TextButton(onClick = onClear) {
                    Text("Clear")
                }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    }
}

@Composable
fun SearchAmountFilterRow(
    amountMin: String,
    amountMax: String,
    onMinChange: (String) -> Unit,
    onMaxChange: (String) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = MmrDimens.spaceSm),
            verticalAlignment = Alignment.Top,
        ) {
            Text(
                text = "Amount",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier =
                    Modifier
                        .width(MmrDimens.formLabelWidth)
                        .padding(top = MmrDimens.spaceMd),
            )
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(MmrDimens.spaceSm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = amountMin,
                    onValueChange = onMinChange,
                    modifier = Modifier.weight(1f),
                    label = { Text("Min") },
                    placeholder = { Text("0") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                )
                Text(
                    text = "–",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = amountMax,
                    onValueChange = onMaxChange,
                    modifier = Modifier.weight(1f),
                    label = { Text("Max") },
                    placeholder = { Text("Any") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                )
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    }
}
