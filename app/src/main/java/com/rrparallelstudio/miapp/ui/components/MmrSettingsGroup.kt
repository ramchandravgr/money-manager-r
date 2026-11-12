package com.rrparallelstudio.miapp.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.rrparallelstudio.miapp.ui.theme.MmrBrandColors
import com.rrparallelstudio.miapp.ui.theme.mmrSettingsCardRowPrimaryStyle
import com.rrparallelstudio.miapp.ui.theme.mmrSettingsCardRowSecondaryStyle
import com.rrparallelstudio.miapp.ui.theme.mmrSettingsGroupActionStyle
import com.rrparallelstudio.miapp.ui.theme.mmrSettingsGroupCodeStyle
import com.rrparallelstudio.miapp.ui.theme.mmrSettingsGroupFooterStyle
import com.rrparallelstudio.miapp.ui.theme.mmrSettingsGroupHeadingStyle
import com.rrparallelstudio.miapp.ui.theme.mmrSettingsGroupTextFieldStyle
import com.rrparallelstudio.miapp.ui.theme.mmrSettingsRowSecondaryStyle
import java.util.Locale

/**
 * Settings body that matches `docs/design/settings-screens-suggested.html`.
 *
 * One uppercase heading, one card, help text under the card. Row text is shown as written.
 * The rose (or other theme) canvas stays on [MmrSettingsScreenScaffold]; this kit does not paint it.
 */
@Composable
fun MmrSettingsGroup(
    title: String?,
    modifier: Modifier = Modifier,
    footer: String? = null,
    isFirstInScreen: Boolean = false,
    showCard: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(top = if (isFirstInScreen) 4.dp else 14.dp),
    ) {
        if (!title.isNullOrBlank()) {
            Text(
                text = title.uppercase(Locale.getDefault()),
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(start = 8.dp, end = 8.dp, bottom = 6.dp),
                style = mmrSettingsGroupHeadingStyle(MmrBrandColors.listAreaCaptionText()),
            )
        }
        if (showCard) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MmrBrandColors.SurfaceWhite,
                shadowElevation = 0.dp,
                tonalElevation = 0.dp,
            ) {
                Column(modifier = Modifier.fillMaxWidth(), content = content)
            }
        }
        if (!footer.isNullOrBlank()) {
            Text(
                text = footer,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(start = 8.dp, end = 8.dp, top = 6.dp),
                style = mmrSettingsGroupFooterStyle(MmrBrandColors.listAreaCaptionText()),
            )
        }
    }
}

/** Standalone warning card on the canvas. Not a section heading. */
@Composable
fun MmrSettingsGroupWarn(
    text: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFFFF7ED),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            style = mmrSettingsRowSecondaryStyle(Color(0xFFB54708)),
        )
    }
}

@Composable
fun MmrSettingsGroupNavRow(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    value: String = "",
    subtitle: String? = null,
    showDividerBelow: Boolean = true,
    testTag: String? = null,
) {
    MmrSettingsGroupRowFrame(
        showDividerBelow = showDividerBelow,
        onClick = onClick,
        modifier = modifier.then(if (testTag != null) Modifier.testTag(testTag) else Modifier),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = mmrSettingsCardRowPrimaryStyle(MmrBrandColors.formCardPrimaryText()),
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = mmrSettingsCardRowSecondaryStyle(MmrBrandColors.formCardSecondaryText()),
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
        if (value.isNotEmpty()) {
            Text(
                text = value,
                modifier = Modifier.padding(start = 12.dp),
                style = mmrSettingsCardRowPrimaryStyle(MmrBrandColors.formCardSecondaryText()),
                textAlign = TextAlign.End,
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MmrBrandColors.formCardDisclosureIcon(),
            modifier = Modifier.padding(start = 4.dp),
        )
    }
}

@Composable
fun MmrSettingsGroupValueRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    showDividerBelow: Boolean = true,
    onClick: (() -> Unit)? = null,
    valueTestTag: String? = null,
) {
    MmrSettingsGroupRowFrame(
        showDividerBelow = showDividerBelow,
        modifier = modifier,
        onClick = onClick,
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = mmrSettingsCardRowPrimaryStyle(MmrBrandColors.formCardPrimaryText()),
        )
        Text(
            text = value,
            modifier =
                Modifier
                    .padding(start = 12.dp)
                    .then(if (valueTestTag != null) Modifier.testTag(valueTestTag) else Modifier),
            style = mmrSettingsCardRowPrimaryStyle(MmrBrandColors.formCardSecondaryText()),
            textAlign = TextAlign.End,
        )
    }
}

@Composable
fun MmrSettingsGroupLabelRow(
    label: String,
    modifier: Modifier = Modifier,
    showDividerBelow: Boolean = true,
) {
    MmrSettingsGroupRowFrame(showDividerBelow = showDividerBelow, modifier = modifier) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = mmrSettingsCardRowPrimaryStyle(MmrBrandColors.formCardPrimaryText()),
        )
    }
}

@Composable
fun MmrSettingsGroupSwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    showDividerBelow: Boolean = true,
    switchTestTag: String? = null,
) {
    MmrSettingsGroupRowFrame(showDividerBelow = showDividerBelow, modifier = modifier) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = mmrSettingsCardRowPrimaryStyle(MmrBrandColors.formCardPrimaryText()),
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            modifier = if (switchTestTag != null) Modifier.testTag(switchTestTag) else Modifier,
            colors =
                SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = MmrBrandColors.Active,
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = MmrBrandColors.formCardSecondaryText(),
                ),
        )
    }
}

/** Muted paragraph inside the card, under a row or under the pairing code. */
@Composable
fun MmrSettingsGroupNote(
    text: String,
    modifier: Modifier = Modifier,
    showDividerBelow: Boolean = true,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            style = mmrSettingsRowSecondaryStyle(MmrBrandColors.formCardSecondaryText()),
        )
        if (showDividerBelow) {
            MmrSettingsGroupDivider()
        }
    }
}

/** Large centered code, as on Peer Device Sync in the mock. */
@Composable
fun MmrSettingsGroupCode(
    code: String,
    modifier: Modifier = Modifier,
    showDividerBelow: Boolean = true,
    onClick: (() -> Unit)? = null,
    testTag: String? = null,
) {
    val compact = code.filter { !it.isWhitespace() }
    val shown =
        when {
            compact.isEmpty() -> "—"
            compact.length <= 3 -> compact
            else -> compact.take(3) + " " + compact.drop(3)
        }
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = shown,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                    .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
                    .padding(start = 14.dp, end = 14.dp, top = 8.dp, bottom = 14.dp),
            style = mmrSettingsGroupCodeStyle(MmrBrandColors.formCardPrimaryText()),
            textAlign = TextAlign.Center,
        )
        if (showDividerBelow) {
            MmrSettingsGroupDivider()
        }
    }
}

@Composable
fun MmrSettingsGroupAction(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    danger: Boolean = false,
    enabled: Boolean = true,
    showDividerBelow: Boolean = true,
    testTag: String? = null,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = text,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable(enabled = enabled, onClick = onClick)
                    .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
            style =
                mmrSettingsGroupActionStyle(
                    color =
                        when {
                            !enabled -> MmrBrandColors.formCardSecondaryText()
                            danger -> Color(0xFFB42318)
                            else -> MmrBrandColors.Active
                        },
                ),
            textAlign = TextAlign.Center,
        )
        if (showDividerBelow) {
            MmrSettingsGroupDivider()
        }
    }
}

@Composable
private fun MmrSettingsGroupRowFrame(
    showDividerBelow: Boolean,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 48.dp)
                    .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
        if (showDividerBelow) {
            MmrSettingsGroupDivider()
        }
    }
}

@Composable
private fun MmrSettingsGroupDivider() {
    HorizontalDivider(
        modifier = Modifier.fillMaxWidth(),
        thickness = 0.5.dp,
        color = MmrBrandColors.formCardDivider(),
    )
}

@Composable
fun MmrSettingsGroupChoiceRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showDividerBelow: Boolean = true,
    testTag: String? = null,
    labelContent: (@Composable () -> Unit)? = null,
) {
    MmrSettingsGroupRowFrame(
        showDividerBelow = showDividerBelow,
        onClick = onClick,
        modifier = modifier.then(if (testTag != null) Modifier.testTag(testTag) else Modifier),
    ) {
        if (labelContent != null) {
            Box(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                labelContent()
            }
        } else {
            Text(
                text = label,
                modifier = Modifier.weight(1f),
                style =
                    mmrSettingsCardRowPrimaryStyle(
                        color = MmrBrandColors.formCardPrimaryText(),
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    ),
            )
        }
        if (selected) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = MmrBrandColors.Active,
            )
        }
    }
}

@Composable
fun MmrSettingsGroupRadioRow(
    label: String,
    selected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
    showDividerBelow: Boolean = true,
) {
    MmrSettingsGroupRowFrame(
        showDividerBelow = showDividerBelow,
        onClick = onSelect,
        modifier = modifier,
    ) {
        RadioButton(
            selected = selected,
            onClick = onSelect,
            colors =
                RadioButtonDefaults.colors(
                    selectedColor = MmrBrandColors.Active,
                ),
        )
        Text(
            text = label,
            style = mmrSettingsCardRowPrimaryStyle(MmrBrandColors.formCardPrimaryText()),
        )
    }
}

@Composable
fun MmrSettingsGroupDestructiveRow(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showDividerBelow: Boolean = true,
    enabled: Boolean = true,
) {
    MmrSettingsGroupRowFrame(
        showDividerBelow = showDividerBelow,
        onClick = if (enabled) onClick else null,
        modifier = modifier,
    ) {
        Text(
            text = label,
            modifier = Modifier.fillMaxWidth(),
            style =
                mmrSettingsCardRowPrimaryStyle(
                    color = if (enabled) Color(0xFFB42318) else MmrBrandColors.formCardSecondaryText(),
                ),
        )
    }
}

@Composable
fun MmrSettingsGroupSecureTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    enabled: Boolean = true,
    showDividerBelow: Boolean = true,
    testTag: String? = null,
) {
    MmrSettingsGroupTextField(
        label = label,
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        placeholder = placeholder,
        enabled = enabled,
        showDividerBelow = showDividerBelow,
        testTag = testTag,
        keyboardOptions = KeyboardOptions.Default,
        visualTransformation = PasswordVisualTransformation(),
    )
}

@Composable
fun MmrSettingsGroupTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    enabled: Boolean = true,
    showDividerBelow: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    testTag: String? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
) {
    MmrSettingsGroupRowFrame(showDividerBelow = showDividerBelow, modifier = modifier) {
        Text(
            text = label,
            modifier = Modifier.weight(0.42f),
            style = mmrSettingsCardRowPrimaryStyle(MmrBrandColors.formCardPrimaryText()),
        )
        Box(modifier = Modifier.weight(0.58f), contentAlignment = Alignment.CenterEnd) {
            val fieldStyle = mmrSettingsGroupTextFieldStyle(MmrBrandColors.formCardPrimaryText())
            val placeholderStyle = mmrSettingsGroupTextFieldStyle(MmrBrandColors.formCardSecondaryText())
            if (value.isEmpty() && placeholder.isNotEmpty()) {
                Text(
                    text = placeholder,
                    modifier = Modifier.fillMaxWidth(),
                    style = placeholderStyle,
                    textAlign = TextAlign.End,
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .then(if (testTag != null) Modifier.testTag(testTag) else Modifier),
                textStyle = fieldStyle,
                singleLine = true,
                enabled = enabled,
                keyboardOptions = keyboardOptions,
                visualTransformation = visualTransformation,
            )
        }
    }
}

@Composable
fun MmrSettingsGroupSlider(
    caption: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    minLabel: String,
    maxLabel: String,
    modifier: Modifier = Modifier,
    showDividerBelow: Boolean = true,
    sliderTestTag: String? = null,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
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
        if (showDividerBelow) {
            MmrSettingsGroupDivider()
        }
    }
}

/** Padding-aligned block inside a group (color pickers, monospace URLs). */
@Composable
fun MmrSettingsGroupInset(
    modifier: Modifier = Modifier,
    showDividerBelow: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = content,
        )
        if (showDividerBelow) {
            MmrSettingsGroupDivider()
        }
    }
}

@Composable
fun MmrSettingsGroupEmphasis(
    text: String,
    modifier: Modifier = Modifier,
    helperBelow: String? = null,
    showDividerBelow: Boolean = true,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Text(
                text = text,
                style =
                    mmrSettingsCardRowPrimaryStyle(
                        color = MmrBrandColors.formCardPrimaryText(),
                        fontWeight = FontWeight.SemiBold,
                    ),
            )
            helperBelow?.let {
                Text(
                    text = it,
                    style = mmrSettingsRowSecondaryStyle(MmrBrandColors.formCardSecondaryText()),
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
        if (showDividerBelow) {
            MmrSettingsGroupDivider()
        }
    }
}

/** Standalone destructive section (no heading). */
@Composable
fun MmrSettingsGroupDestructiveSection(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    MmrSettingsGroup(title = null, modifier = modifier) {
        MmrSettingsGroupDestructiveRow(
            label = label,
            onClick = onClick,
            showDividerBelow = false,
        )
    }
}

@Composable
fun MmrSettingsGroupError(
    message: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFFEF3F2),
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            style = mmrSettingsRowSecondaryStyle(Color(0xFFB42318)),
        )
    }
}
