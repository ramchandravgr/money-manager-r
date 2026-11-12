package com.rrparallelstudio.miapp.ui.components

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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rrparallelstudio.miapp.ui.copy.mmrStaticLabelCase
import com.rrparallelstudio.miapp.ui.theme.MmrBrandColors
import com.rrparallelstudio.miapp.ui.theme.mmrDialogFieldCaptionStyle
import com.rrparallelstudio.miapp.ui.theme.mmrFormFieldLabelStyle
import com.rrparallelstudio.miapp.ui.theme.mmrFormFieldStyle

/** Title + optional message — shared body chrome for [MmrContentDialog] (Design 12 mockup `.t` / `.m`). */
@Composable
fun MmrDialogHeader(
    title: String,
    message: String?,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = title.mmrStaticLabelCase(),
            style =
                MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = MmrBrandColors.dialogTitleText(),
                ),
        )
        message?.let {
            Text(
                text = it,
                style = mmrFormFieldStyle(MmrBrandColors.dialogMessageText()),
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
        text = text.mmrStaticLabelCase(),
        modifier = modifier.padding(bottom = 4.dp),
        style = mmrFormFieldLabelStyle(MmrBrandColors.dialogTitleText()),
    )
}

private val dialogFieldShape = RoundedCornerShape(8.dp)

@Composable
private fun dialogFieldTextStyle(): TextStyle = mmrFormFieldStyle(MmrBrandColors.dialogFieldValueText())

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
                dialogFieldTextStyle().copy(
                    color =
                        if (enabled) {
                            MmrBrandColors.dialogFieldValueText()
                        } else {
                            MmrBrandColors.dialogFieldMutedText()
                        },
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
                            style = dialogFieldTextStyle(),
                            color = MmrBrandColors.dialogFieldMutedText(),
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

@OptIn(ExperimentalMaterial3Api::class)
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
    showCalculator: Boolean = true,
) {
    var calculatorOpen by remember { mutableStateOf(false) }
    val amountTransformation = remember { MmrLocaleAmountVisualTransformation() }
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MmrDialogTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
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
        if (showCalculator && enabled) {
            MmrAmountCalculatorIconButton(
                onClick = { calculatorOpen = true },
                modifier = Modifier.padding(start = 4.dp),
            )
        }
    }
    if (calculatorOpen) {
        MmrAmountCalculatorSheet(
            initialAmountFieldValue = value,
            onDismiss = { calculatorOpen = false },
            onApply = onValueChange,
        )
    }
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
            color = MmrBrandColors.dialogFieldFill(),
        ) {
            Text(
                text = value.ifBlank { placeholder },
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                style = dialogFieldTextStyle(),
                color =
                    if (value.isBlank()) {
                        MmrBrandColors.dialogFieldMutedText()
                    } else {
                        MmrBrandColors.dialogFieldValueText()
                    },
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
            style = mmrFormFieldStyle(MmrBrandColors.dialogFieldValueText()),
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
        color = MmrBrandColors.dialogFieldFill(),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            style = mmrFormFieldStyle(MmrBrandColors.dialogFieldValueText()),
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
            style = mmrDialogFieldCaptionStyle(MmrBrandColors.dialogFieldMutedText()),
        )
        Text(
            text = body,
            style = mmrFormFieldStyle(MmrBrandColors.dialogTitleText()),
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
            style = mmrFormFieldStyle(MmrBrandColors.dialogFieldMutedText()),
        )
        Text(
            text = value,
            style = mmrFormFieldStyle(MmrBrandColors.dialogFieldValueText()).copy(fontWeight = FontWeight.Medium),
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
        style = mmrFormFieldStyle(MmrBrandColors.dialogFieldMutedText()),
    )
}
