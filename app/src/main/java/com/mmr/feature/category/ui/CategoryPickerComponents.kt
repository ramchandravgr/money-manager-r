package com.mmr.feature.category.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mmr.app.ui.components.MmrDimens
import com.mmr.app.ui.theme.MmrMmColors

@Composable
fun CategoryPickerSheetHeader(
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    parentOnlyVisible: Boolean = false,
    onParentOnly: (() -> Unit)? = null,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = MmrDimens.spaceLg, vertical = MmrDimens.spaceSm),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Category",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MmrMmColors.TextStrong,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(0.dp)) {
            if (parentOnlyVisible && onParentOnly != null) {
                TextButton(onClick = onParentOnly) {
                    Text("Parent only", color = MmrMmColors.Active)
                }
            }
            TextButton(onClick = onClose) {
                Text("Close", color = MmrMmColors.TextMedium)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CategoryPickerParentRow(
    name: String,
    colorHex: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
) {
    val accent = parseColorHex(colorHex)
    val clickModifier =
        if (onLongClick != null) {
            Modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick)
        } else {
            Modifier.combinedClickable(onClick = onClick)
        }
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .then(clickModifier)
                .padding(start = if (selected) 0.dp else 3.dp)
                .padding(vertical = 10.dp, horizontal = MmrDimens.spaceMd),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MmrDimens.spaceSm),
    ) {
        if (selected) {
            Box(
                modifier =
                    Modifier
                        .size(width = 3.dp, height = 22.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(accent),
            )
        }
        Box(
            modifier =
                Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(accent),
        )
        Text(
            text = name,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) MmrMmColors.TextStrong else MmrMmColors.TextMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CategoryPickerSubRow(
    name: String,
    colorHex: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
) {
    val clickModifier =
        if (onLongClick != null) {
            Modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick)
        } else {
            Modifier.combinedClickable(onClick = onClick)
        }
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .then(clickModifier)
                .padding(horizontal = MmrDimens.spaceLg, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MmrDimens.spaceSm),
    ) {
        Box(
            modifier =
                Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(parseColorHex(colorHex)),
        )
        Text(
            text = name,
            style = MaterialTheme.typography.bodyLarge,
            color = MmrMmColors.TextStrong,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun CategoryPickerInlineAdd(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TextButton(
        onClick = onClick,
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = MmrDimens.spaceSm, vertical = MmrDimens.spaceXs),
    ) {
        Icon(
            imageVector = Icons.Filled.Add,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = MmrMmColors.Active,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MmrMmColors.Active,
            modifier = Modifier.padding(start = 4.dp),
        )
    }
}

@Composable
fun CategoryPickerPaneLabel(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MmrMmColors.TextLight,
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = MmrDimens.spaceLg, vertical = MmrDimens.spaceSm),
    )
}
