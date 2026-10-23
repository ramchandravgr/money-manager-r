package com.mmr.feature.transaction.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.mmr.app.ui.components.MmrDimens

@Composable
fun TransactionFormRow(
    label: String,
    value: String,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    valueContent: (@Composable () -> Unit)? = null,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .then(
                        if (onClick != null) {
                            Modifier.clickable(onClick = onClick)
                        } else {
                            Modifier
                        },
                    ).padding(vertical = MmrDimens.listItemPaddingVertical),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(MmrDimens.formLabelWidth),
            )
            if (valueContent != null) {
                Box(modifier = Modifier.weight(1f)) {
                    valueContent()
                }
            } else {
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}
