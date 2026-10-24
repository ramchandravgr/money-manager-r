package com.mmr.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mmr.app.ui.theme.MmrBrandColors

/**
 * Shared chrome for account / category bottom sheets on transaction entry.
 * Place inside [androidx.compose.material3.ModalBottomSheet] content.
 */
@Composable
fun MmrLedgerPickerSheetShell(
    title: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    dismissLabel: String = "Done",
    secondaryActionLabel: String? = null,
    onSecondaryAction: (() -> Unit)? = null,
    searchQuery: String? = null,
    onSearchQueryChange: ((String) -> Unit)? = null,
    searchPlaceholder: String = "Search",
    chipRow: @Composable (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val maxSheetHeight = (LocalConfiguration.current.screenHeightDp * 0.85f).dp
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(max = maxSheetHeight)
                .navigationBarsPadding(),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MmrDimens.spaceLg, vertical = MmrDimens.spaceSm),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MmrBrandColors.TextStrong,
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MmrBrandColors.TextMedium,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                if (secondaryActionLabel != null && onSecondaryAction != null) {
                    TextButton(onClick = onSecondaryAction) {
                        Text(
                            text = secondaryActionLabel,
                            color = MmrBrandColors.TextMedium,
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text(dismissLabel, color = MmrBrandColors.Active, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        if (searchQuery != null && onSearchQueryChange != null) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = MmrDimens.spaceLg),
                singleLine = true,
                label = { Text("Search") },
                placeholder = { Text(searchPlaceholder) },
            )
        }
        chipRow?.invoke()
        content()
    }
}
