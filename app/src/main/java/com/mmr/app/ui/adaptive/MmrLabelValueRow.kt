package com.mmr.app.ui.adaptive

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun MmrLabelValueRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueMaxLines: Int = 3
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.weight(0.38f, fill = false)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .weight(0.62f)
                .padding(start = 8.dp),
            textAlign = TextAlign.End,
            maxLines = valueMaxLines,
            overflow = TextOverflow.Ellipsis
        )
    }
}
