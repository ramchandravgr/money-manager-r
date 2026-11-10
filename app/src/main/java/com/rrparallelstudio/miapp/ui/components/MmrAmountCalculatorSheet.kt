package com.rrparallelstudio.miapp.ui.components

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rrparallelstudio.miapp.R
import com.rrparallelstudio.miapp.core.common.math.MmrAmountExpressionEvaluator
import com.rrparallelstudio.miapp.ui.copy.mmrStaticLabelCase
import com.rrparallelstudio.miapp.ui.theme.MmrBrandColors
import com.rrparallelstudio.miapp.ui.theme.mmrContrastingContent

@Composable
fun MmrAmountCalculatorIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.size(MmrDimens.touchTargetMin),
    ) {
        Icon(
            imageVector = Icons.Outlined.Calculate,
            contentDescription = stringResource(R.string.amount_calculator_open),
            tint = if (enabled) MmrBrandColors.Active else MmrBrandColors.TextLight,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MmrAmountCalculatorSheet(
    initialAmountFieldValue: String,
    onDismiss: () -> Unit,
    onApply: (String) -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
) {
    val context = LocalContext.current
    val invalidMessage = stringResource(R.string.amount_calculator_invalid_expression)
    val tooLargeMessage = stringResource(R.string.amount_calculator_too_large)
    var expression by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(Unit) {
        expression = MmrAmountExpressionEvaluator.seedExpressionFromAmountField(initialAmountFieldValue)
    }

    val preview =
        remember(expression, invalidMessage) {
            MmrAmountExpressionEvaluator.preview(expression, invalidMessage)
        }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MmrBrandColors.modalChromeSurface(),
        dragHandle = { BottomSheetDefaults.DragHandle() },
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MmrDimens.spaceXl)
                    .padding(bottom = MmrDimens.space2xl),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val headerMuted =
                    if (MmrBrandColors.canvasIsDark()) {
                        MmrBrandColors.onCanvasMuted()
                    } else {
                        MmrBrandColors.TextMedium
                    }
                val headerStrong =
                    if (MmrBrandColors.canvasIsDark()) {
                        MmrBrandColors.onCanvas()
                    } else {
                        MmrBrandColors.TextStrong
                    }
                Text(
                    text = stringResource(R.string.amount_calculator_title).mmrStaticLabelCase(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = headerStrong,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = stringResource(R.string.amount_calculator_close),
                        tint = headerMuted,
                    )
                }
            }

            val displayError = preview is MmrAmountExpressionEvaluator.Preview.Invalid
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color =
                    if (displayError) {
                        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)
                    } else if (MmrBrandColors.canvasIsDark()) {
                        Color.White.copy(alpha = 0.08f)
                    } else {
                        MmrBrandColors.ListBackground
                    },
                border =
                    androidx.compose.foundation.BorderStroke(
                        width = 1.dp,
                        color =
                            if (displayError) {
                                MaterialTheme.colorScheme.error
                            } else if (MmrBrandColors.canvasIsDark()) {
                                MmrBrandColors.onCanvasMuted().copy(alpha = 0.35f)
                            } else {
                                MmrBrandColors.BorderMain
                            },
                    ),
            ) {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = MmrDimens.spaceLg, vertical = MmrDimens.spaceMd),
                ) {
                    Text(
                        text = expression.ifEmpty { " " },
                        style = MaterialTheme.typography.bodyMedium,
                        color =
                            if (MmrBrandColors.canvasIsDark()) {
                                MmrBrandColors.onCanvasMuted()
                            } else {
                                MmrBrandColors.TextMedium
                            },
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    val resultText =
                        when (preview) {
                            MmrAmountExpressionEvaluator.Preview.Empty -> "0"
                            is MmrAmountExpressionEvaluator.Preview.Valid ->
                                MmrAmountExpressionEvaluator.formatPreviewResultLine(preview.value)
                            is MmrAmountExpressionEvaluator.Preview.Invalid -> "—"
                        }
                    Text(
                        text = resultText,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Medium,
                        color =
                            if (preview is MmrAmountExpressionEvaluator.Preview.Valid) {
                                if (MmrBrandColors.canvasIsDark()) {
                                    MmrBrandColors.onCanvas()
                                } else {
                                    MmrBrandColors.Reversal
                                }
                            } else if (MmrBrandColors.canvasIsDark()) {
                                MmrBrandColors.onCanvasMuted()
                            } else {
                                MmrBrandColors.TextLight
                            },
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.End,
                    )
                    if (displayError) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = invalidMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }

            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = MmrDimens.spaceMd),
                horizontalArrangement = Arrangement.spacedBy(MmrDimens.spaceSm),
            ) {
                CalculatorToolButton(
                    label = stringResource(R.string.amount_calculator_clear_all),
                    icon = Icons.Outlined.DeleteOutline,
                    onClick = { expression = "" },
                    modifier = Modifier.weight(1f),
                )
                CalculatorToolButton(
                    label = stringResource(R.string.amount_calculator_backspace),
                    icon = Icons.AutoMirrored.Outlined.Backspace,
                    onClick = {
                        if (expression.isNotEmpty()) {
                            expression = expression.dropLast(1)
                        }
                    },
                    modifier = Modifier.weight(1f),
                )
            }

            CalculatorKeypad(
                modifier = Modifier.padding(top = MmrDimens.spaceMd),
                onKey = { expression += it },
            )

            val doneEnabled = preview is MmrAmountExpressionEvaluator.Preview.Valid
            Button(
                onClick = {
                    when (val result = MmrAmountExpressionEvaluator.apply(expression, invalidMessage)) {
                        is MmrAmountExpressionEvaluator.ApplyResult.Ok -> {
                            onApply(result.amountPlain)
                            onDismiss()
                        }
                        MmrAmountExpressionEvaluator.ApplyResult.TooLarge ->
                            Toast.makeText(context, tooLargeMessage, Toast.LENGTH_LONG).show()
                        is MmrAmountExpressionEvaluator.ApplyResult.InvalidExpression -> Unit
                    }
                },
                enabled = doneEnabled,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = MmrDimens.spaceLg)
                        .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = MmrBrandColors.Active,
                        disabledContainerColor = MmrBrandColors.Active.copy(alpha = 0.45f),
                    ),
            ) {
                Text(
                    text = stringResource(R.string.amount_calculator_done).mmrStaticLabelCase(),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MmrBrandColors.Active.mmrContrastingContent(),
                )
            }
            Text(
                text = stringResource(R.string.amount_calculator_footnote),
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = MmrDimens.spaceSm),
                style = MaterialTheme.typography.labelSmall,
                color =
                    if (MmrBrandColors.canvasIsDark()) {
                        MmrBrandColors.onCanvasMuted()
                    } else {
                        MmrBrandColors.TextMedium
                    },
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun CalculatorToolButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TextButton(
        onClick = onClick,
        modifier = modifier.height(MmrDimens.touchTargetMin),
        shape = RoundedCornerShape(10.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint =
                if (MmrBrandColors.canvasIsDark()) {
                    MmrBrandColors.onCanvasMuted()
                } else {
                    MmrBrandColors.TextMedium
                },
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color =
                if (MmrBrandColors.canvasIsDark()) {
                    MmrBrandColors.onCanvas()
                } else {
                    MmrBrandColors.TextStrong
                },
            modifier = Modifier.padding(start = 6.dp),
        )
    }
}

@Composable
private fun CalculatorKeypad(
    onKey: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val keys =
        listOf(
            listOf("(", ")", "^", "/"),
            listOf("7", "8", "9", "*"),
            listOf("4", "5", "6", "-"),
            listOf("1", "2", "3", "+"),
            listOf("%", "0", ".", null),
        )
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        keys.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                row.forEach { key ->
                    if (key == null) {
                        Spacer(modifier = Modifier.weight(1f))
                    } else {
                        CalculatorKey(
                            label = calculatorKeyLabel(key),
                            isOperator = key in listOf("+", "-", "*", "/", "%", "^", "(", ")"),
                            onClick = { onKey(key) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

private fun calculatorKeyLabel(key: String): String =
    when (key) {
        "*" -> "×"
        "/" -> "÷"
        "-" -> "−"
        "^" -> "x^y"
        else -> key
    }

@Composable
private fun CalculatorKey(
    label: String,
    isOperator: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(MmrDimens.actionButtonCorner),
        color =
            if (isOperator) {
                MmrBrandColors.RowSelectionFill
            } else if (MmrBrandColors.canvasIsDark()) {
                Color.White.copy(alpha = 0.10f)
            } else {
                MmrBrandColors.ListBackground
            },
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                fontSize = if (label == "x^y") 13.sp else 18.sp,
                fontWeight = FontWeight.Medium,
                color =
                    if (isOperator) {
                        MmrBrandColors.Active
                    } else if (MmrBrandColors.canvasIsDark()) {
                        MmrBrandColors.onCanvas()
                    } else {
                        MmrBrandColors.Reversal
                    },
            )
        }
    }
}
