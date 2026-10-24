package com.mmr.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.mmr.app.ui.theme.MmrBrandColors

private val actionShape = RoundedCornerShape(MmrDimens.actionButtonCorner)

@Composable
fun MmrPrimaryActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = MmrDimens.actionButtonMinHeight),
        shape = actionShape,
        elevation =
            ButtonDefaults.buttonElevation(
                defaultElevation = 1.dp,
                pressedElevation = 2.dp,
                disabledElevation = 0.dp,
            ),
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun MmrSecondaryActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    borderColor: Color = MmrBrandColors.BorderMain,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = MmrDimens.actionButtonMinHeight),
        shape = actionShape,
        colors =
            ButtonDefaults.outlinedButtonColors(
                contentColor = MmrBrandColors.TextStrong,
            ),
        border = BorderStroke(1.dp, borderColor),
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun MmrDestructivePrimaryActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = MmrDimens.actionButtonMinHeight),
        shape = actionShape,
        colors =
            ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError,
                disabledContainerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.38f),
                disabledContentColor = MaterialTheme.colorScheme.onError.copy(alpha = 0.38f),
            ),
        elevation =
            ButtonDefaults.buttonElevation(
                defaultElevation = 1.dp,
                pressedElevation = 2.dp,
                disabledElevation = 0.dp,
            ),
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
}

/** List / ledger FAB — accent square with optically centered “+” (Home, Loans, Recurring, Approval). */
@Composable
fun MmrAddListFab(
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    SmallFloatingActionButton(
        onClick = onClick,
        modifier = modifier,
        containerColor = MmrBrandColors.Active,
        contentColor = Color.White,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = contentDescription,
                modifier = Modifier.size(MmrDimens.listAddFabPlusIcon),
            )
        }
    }
}
