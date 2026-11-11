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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rrparallelstudio.miapp.ui.copy.mmrStaticLabelCase
import com.rrparallelstudio.miapp.ui.theme.MmrBrandColors

@Composable
internal fun MmrIosSettingsUsesSeparatedFieldCards(): Boolean =
    LocalMmrSettingsFieldGrouping.current.separatedFieldCards()

/**
 * Wraps one settings row or control: single elevated card when [MmrSettingsFieldGrouping.Separated],
 * or a flat row with optional divider inside a section group.
 */
@Composable
internal fun MmrIosSettingsRowContainer(
    showDividerBelow: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    if (MmrIosSettingsUsesSeparatedFieldCards()) {
        MmrGroupedFormCard(modifier = modifier) {
            Box(Modifier.fillMaxWidth()) {
                content()
            }
        }
    } else {
        Column(modifier = modifier.fillMaxWidth()) {
            content()
            if (showDividerBelow) {
                MmrIosSettingsRowDivider()
            }
        }
    }
}

/**
 * iOS Settings–style **inset grouped** section: small header, rounded white card, optional footer.
 * Matches UITableView.Style.insetGrouped / SwiftUI inset grouped List.
 */
@Composable
fun MmrIosSettingsSection(
    title: String?,
    modifier: Modifier = Modifier,
    footer: String? = null,
    isFirstInScreen: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(
                    top =
                        when {
                            isFirstInScreen -> MmrDimens.iosSettingsSectionFirstTop
                            title != null -> MmrDimens.iosSettingsSectionTop
                            else -> MmrDimens.iosSettingsSectionTop
                        },
                ),
    ) {
        val separated = MmrIosSettingsUsesSeparatedFieldCards()
        title?.let {
            if (separated) {
                MmrFormSectionLabel(
                    text = it,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(
                                start = MmrDimens.iosSettingsSectionHeaderInset - 4.dp,
                                end = MmrDimens.iosSettingsSectionHeaderInset,
                                bottom = MmrDimens.iosSettingsSectionHeaderBottom,
                            ),
                )
            } else {
                Text(
                    text = it.mmrStaticLabelCase().uppercase(),
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(
                                start = MmrDimens.iosSettingsSectionHeaderInset,
                                end = MmrDimens.iosSettingsSectionHeaderInset,
                                bottom = MmrDimens.iosSettingsSectionHeaderBottom,
                            ),
                    color = MmrBrandColors.listAreaCaptionText(),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal,
                    letterSpacing = 0.2.sp,
                    lineHeight = 16.sp,
                )
            }
        }
        if (separated) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(MmrDimens.settingsSectionContentGap),
                content = content,
            )
        } else {
            MmrGroupedFormCard {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    content = content,
                )
            }
        }
        footer?.let {
            MmrHelperText(
                text = it,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            start = MmrDimens.iosSettingsSectionHeaderInset,
                            end = MmrDimens.iosSettingsSectionHeaderInset,
                            top = MmrDimens.iosSettingsSectionFooterTop,
                        ),
                color = MmrBrandColors.listAreaCaptionText(),
            )
        }
    }
}

@Composable
fun MmrIosSettingsRowDivider() {
    HorizontalDivider(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(start = MmrDimens.iosSettingsRowHorizontalPadding),
        color = MmrBrandColors.BorderMain,
        thickness = 0.5.dp,
    )
}

internal const val MMR_IOS_SETTINGS_LABEL_MAX_LINES = 10
internal const val MMR_IOS_SETTINGS_VALUE_MAX_LINES = 8
internal val MmrIosSettingsLabelValueGap = 8.dp

@Composable
internal fun MmrIosSettingsRowLabelText(
    text: String,
    modifier: Modifier = Modifier,
    useStaticLabelCase: Boolean = true,
) {
    Text(
        text = if (useStaticLabelCase) text.mmrStaticLabelCase() else text,
        modifier = modifier,
        color = MmrBrandColors.formCardPrimaryText(),
        fontSize = 16.sp,
        maxLines = MMR_IOS_SETTINGS_LABEL_MAX_LINES,
        overflow = TextOverflow.Ellipsis,
        softWrap = true,
    )
}

@Composable
internal fun MmrIosSettingsRowValueText(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        modifier = modifier,
        color = MmrBrandColors.TextMedium,
        fontSize = 16.sp,
        maxLines = MMR_IOS_SETTINGS_VALUE_MAX_LINES,
        overflow = TextOverflow.Ellipsis,
        softWrap = true,
        textAlign = TextAlign.End,
    )
}

/** Row separator inside [MmrIosSettingsSingleChoiceSheet] grouped list (readable on dark modal chrome). */
@Composable
fun MmrIosSettingsSheetRowDivider() {
    HorizontalDivider(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(start = MmrDimens.iosSettingsRowHorizontalPadding),
        color = MmrBrandColors.settingsSheetListDivider(),
        thickness = 0.5.dp,
    )
}

/** Navigation row with optional value and disclosure chevron (iOS Settings style). */
@Composable
fun MmrIosSettingsNavRow(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    value: String = "",
    /** Secondary line under [label] (e.g. SMS provisioning status); not title-cased again. */
    labelSubtitle: String? = null,
    valueContent: (@Composable () -> Unit)? = null,
    showDividerBelow: Boolean = true,
    testTag: String? = null,
) {
    MmrIosSettingsRowContainer(
        showDividerBelow = showDividerBelow,
        modifier =
            modifier.then(if (testTag != null) Modifier.testTag(testTag) else Modifier),
    ) {
        val hasTrailingValue = valueContent != null || value.isNotEmpty()
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = MmrDimens.iosSettingsRowMinHeight)
                    .clickable(onClick = onClick)
                    .padding(
                        horizontal = MmrDimens.iosSettingsRowHorizontalPadding,
                        vertical = 11.dp,
                    ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier =
                    Modifier
                        .weight(1f, fill = true)
                        .padding(
                            end =
                                if (hasTrailingValue) {
                                    MmrIosSettingsLabelValueGap
                                } else {
                                    0.dp
                                },
                        ),
            ) {
                MmrIosSettingsRowLabelText(text = label)
                if (!labelSubtitle.isNullOrBlank()) {
                    Text(
                        text = labelSubtitle,
                        color = MmrBrandColors.TextMedium,
                        fontSize = 13.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (valueContent != null) {
                Box(
                    modifier =
                        Modifier
                            .weight(1f, fill = true)
                            .padding(end = 6.dp),
                    contentAlignment = Alignment.CenterEnd,
                ) {
                    valueContent()
                }
            } else if (value.isNotEmpty()) {
                MmrIosSettingsRowValueText(
                    text = value,
                    modifier = Modifier.weight(1f, fill = true).padding(end = 6.dp),
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MmrBrandColors.TextLight,
                modifier = Modifier.padding(start = 2.dp),
            )
        }
    }
}

@Composable
fun MmrIosSettingsSwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    showDividerBelow: Boolean = true,
    switchTestTag: String? = null,
) {
    MmrIosSettingsRowContainer(
        showDividerBelow = showDividerBelow,
        modifier = modifier,
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = MmrDimens.iosSettingsRowMinHeight)
                    .padding(
                        horizontal = MmrDimens.iosSettingsRowHorizontalPadding,
                        vertical = 8.dp,
                    ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MmrIosSettingsRowLabelText(
                text = label,
                modifier = Modifier.weight(1f, fill = true),
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
                        uncheckedTrackColor = MmrBrandColors.TextLight,
                    ),
            )
        }
    }
}

/** Non-interactive label row (e.g. slider caption inside a group). */
@Composable
fun MmrIosSettingsCaptionRow(
    text: String,
    modifier: Modifier = Modifier,
    showDividerBelow: Boolean = false,
) {
    MmrIosSettingsRowContainer(
        showDividerBelow = showDividerBelow,
        modifier = modifier,
    ) {
        Text(
            text = text.mmrStaticLabelCase(),
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = MmrDimens.iosSettingsRowHorizontalPadding,
                        vertical = 10.dp,
                    ),
            color = MmrBrandColors.TextMedium,
            fontSize = 15.sp,
        )
    }
}

/** Read-only row: label left, value right (no chevron). */
@Composable
fun MmrIosSettingsValueRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    showDividerBelow: Boolean = true,
) {
    MmrIosSettingsRowContainer(
        showDividerBelow = showDividerBelow,
        modifier = modifier,
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = MmrDimens.iosSettingsRowMinHeight)
                    .padding(
                        horizontal = MmrDimens.iosSettingsRowHorizontalPadding,
                        vertical = 11.dp,
                    ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MmrIosSettingsRowLabelText(
                text = label,
                modifier =
                    Modifier
                        .weight(1f, fill = true)
                        .padding(end = MmrIosSettingsLabelValueGap),
            )
            MmrIosSettingsRowValueText(
                text = value,
                modifier = Modifier.weight(1f, fill = true),
            )
        }
    }
}

/** Destructive action row (e.g. Remove, Clear). */
@Composable
fun MmrIosSettingsDestructiveRow(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showDividerBelow: Boolean = false,
) {
    MmrIosSettingsRowContainer(
        showDividerBelow = showDividerBelow,
        modifier =
            modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
    ) {
        Text(
            text = label.mmrStaticLabelCase(),
            modifier =
                Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = MmrDimens.iosSettingsRowMinHeight)
                    .padding(
                        horizontal = MmrDimens.iosSettingsRowHorizontalPadding,
                        vertical = 11.dp,
                    ),
            color = MaterialTheme.colorScheme.error,
            fontSize = 16.sp,
        )
    }
}

/** Slot for controls (slider, text fields, buttons) inside a group. */
@Composable
fun MmrIosSettingsControlSlot(
    modifier: Modifier = Modifier,
    showDividerBelow: Boolean = true,
    verticalPadding: Dp = 8.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    MmrIosSettingsRowContainer(showDividerBelow = showDividerBelow, modifier = modifier) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = MmrDimens.iosSettingsRowHorizontalPadding,
                        vertical = verticalPadding,
                    ),
            content = content,
        )
    }
}
