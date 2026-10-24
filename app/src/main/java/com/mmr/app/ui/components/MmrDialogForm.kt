package com.mmr.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mmr.app.ui.theme.MmrBrandColors

/** Title + optional message — shared body chrome for [MmrContentDialog] (Design 12 mockup `.t` / `.m`). */
@Composable
fun MmrDialogHeader(
    title: String,
    message: String?,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MmrBrandColors.TextStrong,
            lineHeight = 22.sp,
        )
        message?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MmrBrandColors.TextMedium,
                lineHeight = 18.sp,
            )
        }
    }
}

@Composable
fun MmrDialogFieldLabel(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        modifier = modifier.padding(bottom = 4.dp),
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = MmrBrandColors.TextStrong,
    )
}

private val dialogFieldShape = RoundedCornerShape(8.dp)
private val dialogFieldTextStyle =
    TextStyle(
        fontSize = 13.sp,
        lineHeight = 18.sp,
        color = MmrBrandColors.Reversal,
    )

@Composable
fun MmrDialogTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    singleLine: Boolean = true,
    minLines: Int = 1,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    enabled: Boolean = true,
    isError: Boolean = false,
    errorMessage: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        label?.let { MmrDialogFieldLabel(it) }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color =
                            when {
                                isError -> MaterialTheme.colorScheme.error
                                else -> MmrBrandColors.BorderMain
                            },
                        shape = dialogFieldShape,
                    ).padding(horizontal = 10.dp, vertical = 10.dp),
            enabled = enabled,
            textStyle =
                dialogFieldTextStyle.copy(
                    color = if (enabled) MmrBrandColors.Reversal else MmrBrandColors.TextMedium,
                ),
            singleLine = singleLine,
            minLines = minLines,
            maxLines = maxLines,
            keyboardOptions = keyboardOptions,
            visualTransformation = visualTransformation,
            cursorBrush = SolidColor(MmrBrandColors.Active),
            decorationBox = { inner ->
                Box {
                    if (value.isEmpty() && placeholder != null) {
                        Text(
                            text = placeholder,
                            style = dialogFieldTextStyle,
                            color = MmrBrandColors.TextMedium,
                        )
                    }
                    inner()
                }
            },
        )
        errorMessage?.let {
            Text(
                text = it,
                modifier = Modifier.padding(top = 4.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
fun MmrDialogAmountTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    enabled: Boolean = true,
    isError: Boolean = false,
    errorMessage: String? = null,
) {
    val amountTransformation = remember { MmrLocaleAmountVisualTransformation() }
    MmrDialogTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        label = label,
        placeholder = placeholder,
        enabled = enabled,
        isError = isError,
        errorMessage = errorMessage,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        visualTransformation =
            if (value.isEmpty()) {
                VisualTransformation.None
            } else {
                amountTransformation
            },
    )
}

/** Tappable row styled like mockup `.field` (date pickers, etc.). */
@Composable
fun MmrDialogPickerField(
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String = "Select",
    enabled: Boolean = true,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        label?.let { MmrDialogFieldLabel(it) }
        Surface(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
            shape = dialogFieldShape,
            border = BorderStroke(1.dp, MmrBrandColors.BorderMain),
            color = MmrBrandColors.SurfaceWhite,
        ) {
            Text(
                text = value.ifBlank { placeholder },
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                style = dialogFieldTextStyle,
                color = if (value.isBlank()) MmrBrandColors.TextMedium else MmrBrandColors.Reversal,
            )
        }
    }
}

@Composable
fun MmrDialogRadioRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick,
            colors =
                RadioButtonDefaults.colors(
                    selectedColor = MmrBrandColors.Active,
                ),
        )
        Text(
            text = label,
            modifier = Modifier.padding(start = 2.dp),
            fontSize = 13.sp,
            lineHeight = 18.sp,
            color = MmrBrandColors.Reversal,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Mockup `.sec-btn` — in-body actions before footer (e.g. loan unlink / reverse). */
@Composable
fun MmrDialogOptionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, MmrBrandColors.BorderMain),
        color = MmrBrandColors.SurfaceWhite,
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            fontSize = 13.sp,
            color = MmrBrandColors.Reversal,
        )
    }
}

@Composable
fun MmrDialogSmsPreview(
    fromLine: String,
    body: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = fromLine,
            fontSize = 12.sp,
            color = MmrBrandColors.TextMedium,
        )
        Text(
            text = body,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            color = MmrBrandColors.TextStrong,
        )
    }
}

@Composable
fun MmrDialogDetailLine(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = MmrBrandColors.TextMedium,
        )
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = MmrBrandColors.Reversal,
        )
    }
}

@Composable
fun MmrDialogBodyNote(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        modifier = modifier.fillMaxWidth(),
        fontSize = 13.sp,
        lineHeight = 18.sp,
        color = MmrBrandColors.TextMedium,
    )
}
