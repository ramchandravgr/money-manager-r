package com.mmr.app.ui.adaptive

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mmr.app.ui.components.MmrDimens

@Composable
fun MmrLabelValueRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueMaxLines: Int = 3,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = 28.dp)
                .padding(vertical = MmrDimens.spaceXs),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(MmrDimens.formLabelWidth),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Normal,
            modifier =
                Modifier
                    .weight(1f)
                    .padding(start = MmrDimens.labelValueGap),
            textAlign = TextAlign.Start,
            maxLines = valueMaxLines,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
