package com.mmr.app.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mmr.app.ui.state.MmrErrorState
import com.mmr.app.ui.state.MmrLoadingState
import com.mmr.app.ui.theme.MmrBrandColors

/** Helper / status copy inside an inset grouped section (standard horizontal inset). */
@Composable
fun MmrIosSettingsSectionBodyText(
    text: String,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
) {
    MmrIosSettingsRowContainer(showDividerBelow = false, modifier = modifier) {
        val padded =
            Modifier.padding(
                horizontal = MmrDimens.iosSettingsRowHorizontalPadding,
                vertical = 10.dp,
            )
        if (isError) {
            Text(
                text = text,
                modifier = padded,
                color = MaterialTheme.colorScheme.error,
                fontSize = 13.sp,
                lineHeight = 18.sp,
            )
        } else {
            MmrHelperText(text = text, modifier = padded)
        }
    }
}

/** One log/meta line inside a grouped section (e.g. LAN sync history). */
@Composable
fun MmrIosSettingsLogLine(
    text: String,
    isError: Boolean = false,
) {
    MmrIosSettingsSectionBodyText(text = text, isError = isError)
}

@Composable
fun MmrIosSettingsListEmptySection(
    message: String,
    isFirstInScreen: Boolean = true,
) {
    MmrIosSettingsSection(
        title = null,
        isFirstInScreen = isFirstInScreen,
    ) {
        MmrIosSettingsSectionBodyText(text = message)
    }
}

@Composable
fun MmrIosSettingsBusyIndicator(modifier: Modifier = Modifier) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
    ) {
        androidx.compose.material3.CircularProgressIndicator(
            modifier = Modifier.size(28.dp),
            strokeWidth = 2.5.dp,
        )
    }
}

@Composable
fun MmrIosSettingsReadOnlyValueRow(
    label: String,
    value: String? = null,
    showDividerBelow: Boolean = false,
) {
    MmrIosSettingsControlSlot(showDividerBelow = showDividerBelow, verticalPadding = 0.dp) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = MmrDimens.iosSettingsRowMinHeight)
                    .padding(vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                modifier = Modifier.weight(1f),
                color = MmrBrandColors.Reversal,
                fontSize = 16.sp,
            )
            value?.let {
                Text(
                    text = it,
                    color = MmrBrandColors.TextMedium,
                    fontSize = 16.sp,
                )
            }
        }
    }
}

@Composable
fun MmrIosSettingsColorPickerSlot(
    showDividerBelow: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    MmrIosSettingsControlSlot(showDividerBelow = showDividerBelow) {
        content()
    }
}

@Composable
fun MmrSettingsListAddTopBar(
    onAdd: () -> Unit,
    testTag: String? = null,
) {
    Row {
        MmrAppBarAddIcon(onClick = onAdd, testTag = testTag)
    }
}

/**
 * Compact inset-grouped text field (caption label + single-line body).
 * Avoids Material [OutlinedTextField] min-height so rows stay ~iOS Settings density.
 */
@Composable
fun MmrIosSettingsTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    enabled: Boolean = true,
    password: Boolean = false,
    visualTransformation: VisualTransformation? = null,
    supportingText: String? = null,
    testTag: String? = null,
) {
    val fieldColor = if (enabled) MmrBrandColors.Reversal else MmrBrandColors.TextMedium
    val transformation =
        visualTransformation ?: if (password) PasswordVisualTransformation() else VisualTransformation.None
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .then(if (testTag != null) Modifier.testTag(testTag) else Modifier),
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = MmrBrandColors.TextMedium,
        )
        Spacer(modifier = Modifier.height(4.dp))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = if (singleLine) 22.dp else 56.dp)
                    .padding(vertical = 4.dp),
            textStyle =
                TextStyle(
                    fontSize = 16.sp,
                    lineHeight = 20.sp,
                    color = fieldColor,
                ),
            singleLine = singleLine,
            maxLines = if (singleLine) 1 else Int.MAX_VALUE,
            keyboardOptions = keyboardOptions,
            enabled = enabled,
            visualTransformation = transformation,
        )
        supportingText?.let {
            MmrHelperText(text = it, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

/**
 * Single-row field: label left, entry right (~44dp). Use for account/category form rows.
 */
@Composable
fun MmrIosSettingsInlineTextRow(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    enabled: Boolean = true,
    showDividerBelow: Boolean = true,
    singleLine: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    testTag: String? = null,
) {
    MmrIosSettingsInlineFieldRow(
        label = label,
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        placeholder = placeholder,
        enabled = enabled,
        showDividerBelow = showDividerBelow,
        singleLine = singleLine,
        keyboardOptions = keyboardOptions,
        password = false,
        testTag = testTag,
    )
}

/** Single-row **secure** field (see [MmrIosSettingsInlineTextRow]). */
@Composable
fun MmrIosSettingsSecureTextRow(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    enabled: Boolean = true,
    showDividerBelow: Boolean = true,
    testTag: String? = null,
) {
    MmrIosSettingsInlineFieldRow(
        label = label,
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        placeholder = placeholder,
        enabled = enabled,
        showDividerBelow = showDividerBelow,
        singleLine = true,
        keyboardOptions = KeyboardOptions.Default,
        password = true,
        testTag = testTag,
    )
}

@Composable
private fun MmrIosSettingsInlineFieldRow(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier,
    placeholder: String,
    enabled: Boolean,
    showDividerBelow: Boolean,
    singleLine: Boolean,
    keyboardOptions: KeyboardOptions,
    password: Boolean,
    testTag: String?,
) {
    val fieldColor = if (enabled) MmrBrandColors.Reversal else MmrBrandColors.TextMedium
    val transformation = if (password) PasswordVisualTransformation() else VisualTransformation.None
    MmrIosSettingsControlSlot(
        showDividerBelow = showDividerBelow,
        verticalPadding = 0.dp,
        modifier = modifier,
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = MmrDimens.iosSettingsRowMinHeight),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                modifier = Modifier.weight(0.42f),
                color = MmrBrandColors.Reversal,
                fontSize = 16.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Box(
                modifier =
                    Modifier
                        .weight(0.58f)
                        .clip(RectangleShape),
                contentAlignment = Alignment.CenterEnd,
            ) {
                if (value.isEmpty() && placeholder.isNotEmpty()) {
                    Text(
                        text = placeholder,
                        color = MmrBrandColors.TextMedium,
                        fontSize = 16.sp,
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier),
                    textStyle =
                        TextStyle(
                            fontSize = 16.sp,
                            color = fieldColor,
                            textAlign = TextAlign.End,
                        ),
                    singleLine = singleLine,
                    maxLines = if (singleLine) 1 else Int.MAX_VALUE,
                    enabled = enabled,
                    visualTransformation = transformation,
                    keyboardOptions = keyboardOptions,
                )
            }
        }
    }
}

/** Text field row inside a grouped section (control slot + divider). */
@Composable
fun MmrIosSettingsTextFieldSlot(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    showDividerBelow: Boolean = true,
    singleLine: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    enabled: Boolean = true,
    password: Boolean = false,
    supportingText: String? = null,
    testTag: String? = null,
) {
    MmrIosSettingsControlSlot(
        showDividerBelow = showDividerBelow,
        verticalPadding = 10.dp,
    ) {
        MmrIosSettingsTextField(
            value = value,
            onValueChange = onValueChange,
            label = label,
            singleLine = singleLine,
            keyboardOptions = keyboardOptions,
            enabled = enabled,
            password = password,
            supportingText = supportingText,
            testTag = testTag,
        )
    }
}

@Composable
fun MmrIosSettingsPrimaryActionSlot(
    text: String,
    onClick: () -> Unit,
    showDividerBelow: Boolean = true,
    enabled: Boolean = true,
    testTag: String? = null,
) {
    MmrIosSettingsControlSlot(showDividerBelow = showDividerBelow) {
        MmrPrimaryActionButton(
            text = text,
            onClick = onClick,
            enabled = enabled,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .then(if (testTag != null) Modifier.testTag(testTag) else Modifier),
        )
    }
}

@Composable
fun MmrIosSettingsSecondaryActionSlot(
    text: String,
    onClick: () -> Unit,
    showDividerBelow: Boolean = true,
    enabled: Boolean = true,
    testTag: String? = null,
) {
    MmrIosSettingsControlSlot(showDividerBelow = showDividerBelow) {
        MmrSecondaryActionButton(
            text = text,
            onClick = onClick,
            enabled = enabled,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .then(if (testTag != null) Modifier.testTag(testTag) else Modifier),
        )
    }
}

/** Radio choice row matching inset grouped list style. */
@Composable
fun MmrIosSettingsRadioRow(
    label: String,
    selected: Boolean,
    onSelect: () -> Unit,
    showDividerBelow: Boolean = true,
    modifier: Modifier = Modifier,
) {
    MmrIosSettingsRowContainer(
        showDividerBelow = showDividerBelow,
        modifier =
            modifier
                .fillMaxWidth()
                .clickable(onClick = onSelect),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = MmrDimens.iosSettingsRowHorizontalPadding,
                        vertical = 4.dp,
                    ),
        ) {
            RadioButton(selected = selected, onClick = onSelect)
            Text(label, fontSize = 16.sp, color = MmrBrandColors.Reversal)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun <T> MmrIosSettingsChipChoiceSlot(
    fieldLabel: String,
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    optionLabel: (T) -> String,
    showDividerBelow: Boolean = true,
    enabled: Boolean = true,
) {
    MmrIosSettingsControlSlot(showDividerBelow = showDividerBelow) {
        Text(
            text = fieldLabel,
            fontSize = 14.sp,
            color = MmrBrandColors.TextMedium,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { option ->
                FilterChip(
                    selected = selected == option,
                    onClick = { onSelect(option) },
                    label = { Text(optionLabel(option)) },
                    enabled = enabled,
                )
            }
        }
    }
}

/** Small field label + monospace value (pairing codes, URLs in forms). */
@Composable
fun MmrIosSettingsMonospaceFieldSlot(
    fieldLabel: String,
    value: String,
    showDividerBelow: Boolean = true,
    accentValue: Boolean = false,
) {
    MmrIosSettingsControlSlot(showDividerBelow = showDividerBelow) {
        Text(text = fieldLabel, style = MaterialTheme.typography.labelMedium)
        MmrSettingsMonospaceText(text = value, accent = accentValue)
    }
}

/** Pairing code / short secret: monospace value + copy action in the row. */
@Composable
fun MmrIosSettingsCopyableCodeSlot(
    fieldLabel: String,
    value: String,
    showDividerBelow: Boolean = true,
    copyContentDescription: String = "Copy",
    copyTestTag: String? = null,
) {
    val context = LocalContext.current
    MmrIosSettingsControlSlot(showDividerBelow = showDividerBelow, verticalPadding = 10.dp) {
        Text(text = fieldLabel, style = MaterialTheme.typography.labelMedium)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = value.ifBlank { "—" },
                modifier = Modifier.weight(1f),
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.2.sp,
                color = if (value.isNotBlank()) MmrBrandColors.Active else MmrBrandColors.TextMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (value.isNotBlank()) {
                IconButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText(fieldLabel, value))
                    },
                    modifier = if (copyTestTag != null) Modifier.testTag(copyTestTag) else Modifier,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ContentCopy,
                        contentDescription = copyContentDescription,
                        tint = MmrBrandColors.TextMedium,
                    )
                }
            }
        }
    }
}

@Composable
fun MmrIosSettingsInlineErrorSection(
    message: String,
    isFirstInScreen: Boolean = false,
) {
    MmrIosSettingsSection(title = null, isFirstInScreen = isFirstInScreen) {
        MmrIosSettingsControlSlot(showDividerBelow = false) {
            Text(message, color = MaterialTheme.colorScheme.error, fontSize = 14.sp)
        }
    }
}

@Composable
fun MmrIosSettingsDestructiveActionSection(
    label: String,
    onClick: () -> Unit,
) {
    MmrIosSettingsSection(title = null) {
        MmrIosSettingsDestructiveRow(label = label, onClick = onClick)
    }
}

/** Standard save action for inset grouped add/edit settings screens. */
@Composable
fun MmrSettingsSaveTopBar(
    onSave: () -> Unit,
    enabled: Boolean,
) {
    Row {
        MmrAppBarSaveIcon(onClick = onSave, enabled = enabled)
    }
}

/** Loading / load-error gate for entity add/edit settings screens. */
@Composable
fun MmrIosSettingsEntityFormGate(
    isLoading: Boolean,
    loadErrorMessage: String?,
    onRetry: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    when {
        isLoading -> MmrLoadingState(modifier = Modifier.fillMaxWidth())
        loadErrorMessage != null ->
            MmrErrorState(
                message = loadErrorMessage,
                onRetry = onRetry,
                modifier = Modifier.fillMaxWidth(),
            )
        else -> Column(modifier = Modifier.fillMaxWidth(), content = content)
    }
}

@Composable
fun MmrIosSettingsSliderSlot(
    caption: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    minLabel: String,
    maxLabel: String,
    showDividerBelow: Boolean = true,
    sliderTestTag: String? = null,
) {
    MmrIosSettingsControlSlot(showDividerBelow = showDividerBelow, verticalPadding = 12.dp) {
        MmrSettingsRangeSlider(
            caption = caption,
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            minLabel = minLabel,
            maxLabel = maxLabel,
            sliderTestTag = sliderTestTag,
        )
    }
}

/** Horizontal filter chips (e.g. category list type filter). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MmrIosSettingsFilterChipRow(
    labels: List<String>,
    selectedIndex: Int,
    onSelectedIndex: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        labels.forEachIndexed { index, label ->
            FilterChip(
                selected = selectedIndex == index,
                onClick = { onSelectedIndex(index) },
                label = { Text(label) },
            )
        }
    }
}

/** Emphasis line inside a section (e.g. linked account email). */
@Composable
fun MmrIosSettingsEmphasisTextSlot(
    text: String,
    showDividerBelow: Boolean = false,
    helperBelow: String? = null,
) {
    MmrIosSettingsControlSlot(showDividerBelow = showDividerBelow) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MmrBrandColors.TextStrong,
        )
        helperBelow?.let { MmrHelperText(text = it, modifier = Modifier.padding(top = 6.dp)) }
    }
}
