package com.mmr.app.ui.components

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation

/** Outlined amount field with locale grouping commas while typing. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MmrAmountOutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    singleLine: Boolean = true,
    enabled: Boolean = true,
    isError: Boolean = false,
    supportingText: @Composable (() -> Unit)? = null,
    showCalculator: Boolean = true,
) {
    var calculatorOpen by remember { mutableStateOf(false) }
    val amountTransformation = remember { MmrLocaleAmountVisualTransformation() }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        label = label,
        placeholder = placeholder,
        singleLine = singleLine,
        enabled = enabled,
        isError = isError,
        supportingText = supportingText,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        visualTransformation =
            if (value.isEmpty()) {
                VisualTransformation.None
            } else {
                amountTransformation
            },
        trailingIcon =
            if (showCalculator && enabled) {
                {
                    MmrAmountCalculatorIconButton(onClick = { calculatorOpen = true })
                }
            } else {
                null
            },
    )
    if (calculatorOpen) {
        MmrAmountCalculatorSheet(
            initialAmountFieldValue = value,
            onDismiss = { calculatorOpen = false },
            onApply = onValueChange,
        )
    }
}
