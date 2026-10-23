package com.mmr.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.mmr.app.ui.theme.MmrMmColors

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
    borderColor: Color = MmrMmColors.BorderMain,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = MmrDimens.actionButtonMinHeight),
        shape = actionShape,
        colors =
            ButtonDefaults.outlinedButtonColors(
                contentColor = MmrMmColors.TextStrong,
            ),
        border = BorderStroke(1.dp, borderColor),
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
}
