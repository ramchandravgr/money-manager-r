package com.mmr.app.ui.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import com.mmr.app.testing.MmrTestTags
import com.mmr.app.ui.theme.MmrBrandColors

@Composable
fun RowScope.MmrAppBarAddIcon(
    onClick: () -> Unit,
    enabled: Boolean = true,
    contentDescription: String = "Add",
    testTag: String? = null,
    tint: Color = MmrBrandColors.HeaderIcon,
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier =
            Modifier
                .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
                .size(MmrDimens.topBarIconButton),
    ) {
        Icon(
            imageVector = Icons.Outlined.Add,
            contentDescription = contentDescription,
            modifier = Modifier.size(MmrDimens.iconMd),
            tint = tint.copy(alpha = if (enabled) 1f else 0.38f),
        )
    }
}

@Composable
fun RowScope.MmrAppBarSaveIcon(
    onClick: () -> Unit,
    enabled: Boolean = true,
    contentDescription: String = "Save",
    tint: Color = MmrBrandColors.HeaderIcon,
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
