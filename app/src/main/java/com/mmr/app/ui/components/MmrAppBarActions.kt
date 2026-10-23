package com.mmr.app.ui.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import com.mmr.app.testing.MmrTestTags
import com.mmr.app.ui.theme.MmrMmColors

@Composable
fun RowScope.MmrAppBarSaveIcon(
    onClick: () -> Unit,
    enabled: Boolean = true,
    contentDescription: String = "Save",
    tint: Color = MmrMmColors.HeaderIcon,
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier =
            Modifier
                .testTag(MmrTestTags.appBarSave)
                .size(MmrDimens.topBarIconButton),
    ) {
        Icon(
            imageVector = Icons.Outlined.Save,
            contentDescription = contentDescription,
            modifier = Modifier.size(MmrDimens.iconMd),
            tint = tint.copy(alpha = if (enabled) 1f else 0.38f),
        )
    }
}

@Composable
fun RowScope.MmrAppBarDeleteIcon(
    onClick: () -> Unit,
    enabled: Boolean = true,
    contentDescription: String = "Delete",
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(MmrDimens.topBarIconButton),
    ) {
        Icon(
            imageVector = Icons.Outlined.DeleteOutline,
            contentDescription = contentDescription,
            modifier = Modifier.size(MmrDimens.iconMd),
            tint = MmrMmColors.Active.copy(alpha = if (enabled) 1f else 0.38f),
        )
    }
}
