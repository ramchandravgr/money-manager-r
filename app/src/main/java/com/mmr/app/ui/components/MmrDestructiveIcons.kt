package com.mmr.app.ui.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp

/** Tint for delete / remove icons and other destructive affordances. */
@Composable
fun mmrDestructiveContentColor(enabled: Boolean = true): Color =
    MaterialTheme.colorScheme.error.copy(alpha = if (enabled) 1f else 0.38f)

/** Standard delete glyph (outlined trash). Always uses [mmrDestructiveContentColor]. */
@Composable
fun MmrDeleteIcon(
    contentDescription: String?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    iconSize: Dp = MmrDimens.iconMd,
) {
    Icon(
        imageVector = Icons.Outlined.DeleteOutline,
        contentDescription = contentDescription,
        modifier = modifier.size(iconSize),
        tint = mmrDestructiveContentColor(enabled),
    )
}

@Composable
fun MmrDeleteIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentDescription: String = "Delete",
    testTag: String? = null,
    buttonSize: Dp? = null,
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier =
            Modifier
                .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
                .then(if (buttonSize != null) Modifier.size(buttonSize) else Modifier)
                .then(modifier),
        colors =
            IconButtonDefaults.iconButtonColors(
                contentColor = MaterialTheme.colorScheme.error,
                disabledContentColor = MaterialTheme.colorScheme.error.copy(alpha = 0.38f),
            ),
    ) {
        MmrDeleteIcon(contentDescription = contentDescription, enabled = enabled)
    }
}

/** Delete action in app bars (brand header or default). */
@Composable
fun RowScope.MmrAppBarDeleteIcon(
    onClick: () -> Unit,
    enabled: Boolean = true,
    contentDescription: String = "Delete",
    testTag: String? = null,
) {
    MmrDeleteIconButton(
        onClick = onClick,
        enabled = enabled,
        contentDescription = contentDescription,
        testTag = testTag,
        buttonSize = MmrDimens.topBarIconButton,
    )
}
