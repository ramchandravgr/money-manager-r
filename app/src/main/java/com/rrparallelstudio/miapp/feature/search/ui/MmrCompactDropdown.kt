package com.rrparallelstudio.miapp.feature.search.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rrparallelstudio.miapp.ui.theme.MmrBrandColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MmrCompactDropdown(
    label: String,
    options: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val safeIndex = selectedIndex.coerceIn(0, (options.size - 1).coerceAtLeast(0))
    val display = options.getOrElse(safeIndex) { "" }
    val dark = MmrBrandColors.canvasIsDark()
    val textColor = if (dark) MmrBrandColors.onCanvas() else MmrBrandColors.TextStrong
    val labelColor = if (dark) MmrBrandColors.onCanvasMuted() else MmrBrandColors.TextMedium

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = display,
            onValueChange = {},
            readOnly = true,
            label = {
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall,
                    color = labelColor,
                )
            },
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = textColor),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier =
                Modifier
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth()
                    .heightIn(min = 52.dp, max = 56.dp),
            singleLine = true,
            colors =
                OutlinedTextFieldDefaults.colors(
                    focusedTextColor = textColor,
                    unfocusedTextColor = textColor,
                    focusedLabelColor = labelColor,
                    unfocusedLabelColor = labelColor,
                    focusedBorderColor = MmrBrandColors.Active,
                    unfocusedBorderColor =
                        if (dark) {
                            MmrBrandColors.onCanvasMuted().copy(alpha = 0.35f)
                        } else {
                            MmrBrandColors.BorderMain
                        },
                    focusedContainerColor = if (dark) Color.White.copy(alpha = 0.06f) else Color.Transparent,
                    unfocusedContainerColor = if (dark) Color.White.copy(alpha = 0.06f) else Color.Transparent,
                    focusedTrailingIconColor = labelColor,
                    unfocusedTrailingIconColor = labelColor,
                ),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            options.forEachIndexed { index, option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = option,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    onClick = {
                        onSelected(index)
                        expanded = false
                    },
                )
            }
        }
    }
}
