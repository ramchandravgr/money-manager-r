package com.rrparallelstudio.miapp.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/**
 * Named text roles for static UI chrome. Sizes are product-fixed (not a redesign layer).
 *
 * | Role | Size | Primary consumers |
 * |------|------|-------------------|
 * | [mmrSettingsGroupHeadingStyle] | 12sp semibold | `MmrSettingsGroup` heading |
 * | [mmrSettingsGroupFooterStyle] | 13sp | Group footers, [com.rrparallelstudio.miapp.ui.components.MmrHelperText] |
 * | [mmrSettingsCardRowPrimaryStyle] | 15sp | [MmrSettingsGroup] card rows |
 * | [mmrSettingsRowPrimaryStyle] | 14sp | iOS settings list / nav rows |
 * | [mmrSettingsRowSecondaryStyle] | 13sp | Row subtitles, secondary labels |
 * | [mmrFormFieldStyle] | 13sp | Dialog fields, grouped form inputs |
 * | [mmrConfigSectionTitleStyle] | 15sp semibold | [com.rrparallelstudio.miapp.ui.components.MmrConfigSectionHeader] |
 * | [mmrChromeCaptionStyle] | 11–12sp | Compact meta, filter chips, ledger list |
 * | [mmrStatsHeroAmountStyle] | 28sp | Stats category hero amount |
 * | [mmrStatsChartLabelStyle] | 10–12sp | Cylinder inspect, chart callouts |
 *
 * See `.cursor/rules/settings-ui-system.mdc` for settings screen contracts.
 */
@Composable
fun mmrSettingsGroupHeadingStyle(color: Color): TextStyle =
    MaterialTheme.typography.labelMedium.copy(
        fontFamily = MmrFontFamily,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.6.sp,
        color = color,
    )

@Composable
fun mmrSettingsGroupFooterStyle(color: Color): TextStyle =
    MaterialTheme.typography.bodyMedium.copy(
        fontFamily = MmrFontFamily,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        fontWeight = FontWeight.Normal,
        color = color,
    )

@Composable
fun mmrSettingsCardRowPrimaryStyle(
    color: Color,
    fontWeight: FontWeight = FontWeight.Normal,
): TextStyle =
    MaterialTheme.typography.titleMedium.copy(
        fontFamily = MmrFontFamily,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        fontWeight = fontWeight,
        color = color,
    )

@Composable
fun mmrSettingsCardRowSecondaryStyle(color: Color): TextStyle = mmrSettingsRowSecondaryStyle(color)

@Composable
fun mmrSettingsGroupCodeStyle(color: Color): TextStyle =
    MaterialTheme.typography.headlineLarge.copy(
        fontFamily = MmrFontFamily,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 5.sp,
        color = color,
    )

@Composable
fun mmrSettingsGroupActionStyle(
    color: Color,
    fontWeight: FontWeight = FontWeight.SemiBold,
): TextStyle = mmrSettingsCardRowPrimaryStyle(color, fontWeight)

@Composable
fun mmrSettingsRowPrimaryStyle(color: Color): TextStyle =
    MaterialTheme.typography.bodyMedium.copy(
        fontFamily = MmrFontFamily,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.Normal,
        color = color,
    )

@Composable
fun mmrSettingsRowSecondaryStyle(color: Color): TextStyle =
    MaterialTheme.typography.bodyMedium.copy(
        fontFamily = MmrFontFamily,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        fontWeight = FontWeight.Normal,
        color = color,
    )

@Composable
fun mmrFormFieldStyle(color: Color): TextStyle =
    MaterialTheme.typography.bodyMedium.copy(
        fontFamily = MmrFontFamily,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        fontWeight = FontWeight.Normal,
        color = color,
    )

@Composable
fun mmrFormFieldLabelStyle(color: Color): TextStyle =
    MaterialTheme.typography.bodyMedium.copy(
        fontFamily = MmrFontFamily,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        fontWeight = FontWeight.SemiBold,
        color = color,
    )

@Composable
fun mmrDialogFieldCaptionStyle(color: Color): TextStyle =
    MaterialTheme.typography.labelMedium.copy(
        fontFamily = MmrFontFamily,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.Normal,
        color = color,
    )

@Composable
fun mmrConfigSectionTitleStyle(color: Color): TextStyle =
    MaterialTheme.typography.titleMedium.copy(
        fontFamily = MmrFontFamily,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.SemiBold,
        color = color,
    )

@Composable
fun mmrChromeCaptionStyle(
    color: Color,
    fontSize: TextUnit = 11.sp,
    lineHeight: TextUnit = 13.sp,
): TextStyle =
    MaterialTheme.typography.bodySmall.copy(
        fontFamily = MmrFontFamily,
        fontSize = fontSize,
        lineHeight = lineHeight,
        fontWeight = FontWeight.Normal,
        color = color,
    )

@Composable
fun mmrLedgerListTextStyle(color: Color = Color.Unspecified): TextStyle =
    mmrChromeCaptionStyle(color, fontSize = 11.sp, lineHeight = 13.sp)

@Composable
fun mmrLedgerAmountTextStyle(color: Color = Color.Unspecified): TextStyle =
    mmrChromeCaptionStyle(color, fontSize = 12.sp, lineHeight = 14.sp)

@Composable
fun mmrFilterChipTextStyle(): TextStyle = mmrChromeCaptionStyle(Color.Unspecified, fontSize = 11.sp, lineHeight = 13.sp)

@Composable
fun mmrLedgerDayNumberTextStyle(color: Color = Color.Unspecified): TextStyle =
    MaterialTheme.typography.titleLarge.copy(
        fontFamily = MmrFontFamily,
        fontSize = 19.sp,
        lineHeight = 22.sp,
        fontWeight = FontWeight.Normal,
        color = color,
    )

@Composable
fun mmrPeriodChipLabelStyle(color: Color): TextStyle = mmrSettingsRowPrimaryStyle(color)

@Composable
fun mmrSearchNoteQueryStyle(color: Color): TextStyle =
    MaterialTheme.typography.bodyMedium.copy(
        fontFamily = MmrFontFamily,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.Normal,
        color = color,
    )

@Composable
fun mmrSearchFilterFieldTextStyle(color: Color): TextStyle = mmrSettingsRowPrimaryStyle(color)

@Composable
fun mmrScreenIntroBulletStyle(color: Color): TextStyle = mmrSettingsRowPrimaryStyle(color)

@Composable
fun mmrScreenIntroTitleStyle(color: Color): TextStyle = mmrConfigSectionTitleStyle(color)

@Composable
fun mmrLightNavRowTitleStyle(color: Color): TextStyle = mmrSettingsRowPrimaryStyle(color)

@Composable
fun mmrStatsHeroAmountStyle(color: Color): TextStyle =
    MaterialTheme.typography.headlineLarge.copy(
        fontFamily = MmrFontFamily,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        fontWeight = FontWeight.Normal,
        color = color,
    )

@Composable
fun mmrStatsChartLabelStyle(
    color: Color,
    fontSize: TextUnit = 10.sp,
): TextStyle =
    MaterialTheme.typography.labelSmall.copy(
        fontFamily = MmrFontFamily,
        fontSize = fontSize,
        lineHeight = (fontSize.value + 4).sp,
        fontWeight = FontWeight.Normal,
        color = color,
    )

@Composable fun mmrStatsChartLabel10Style(color: Color): TextStyle = mmrStatsChartLabelStyle(color, 10.sp)

@Composable fun mmrStatsChartLabel11Style(color: Color): TextStyle = mmrStatsChartLabelStyle(color, 11.sp)

@Composable fun mmrStatsChartLabel12Style(color: Color): TextStyle = mmrStatsChartLabelStyle(color, 12.sp)

@Composable
fun mmrLedgerList10Style(color: Color = Color.Unspecified): TextStyle =
    mmrChromeCaptionStyle(color, fontSize = 10.sp, lineHeight = 12.sp)

@Composable
fun mmrLedgerList10TightStyle(color: Color = Color.Unspecified): TextStyle =
    mmrChromeCaptionStyle(color, fontSize = 10.sp, lineHeight = 14.sp)

@Composable
fun mmrCalendarWeekdayStyle(color: Color): TextStyle = mmrLedgerTabLabelStyle(color)

@Composable
fun mmrCalendarDayAmountStyle(color: Color): TextStyle = mmrLedgerAmountTextStyle(color)

@Composable
fun mmrCalendarDayMicroStyle(color: Color): TextStyle =
    mmrNavTabLabelStyle(color, compact = true).copy(lineHeight = 11.sp)

@Composable
fun mmrHomeMonthlyCaptionStyle(color: Color): TextStyle = mmrSettingsRowSecondaryStyle(color)

@Composable
fun mmrHomeMonthlyBadgeStyle(color: Color): TextStyle = mmrStatsChartLabel10Style(color)

@Composable
fun mmrHomeMonthlyAmountStyle(
    color: Color,
    compact: Boolean,
): TextStyle =
    mmrChromeCaptionStyle(
        color = color,
        fontSize = if (compact) 11.sp else 12.sp,
        lineHeight = if (compact) 13.sp else 14.sp,
    )

@Composable
fun mmrApprovalStatusMicroStyle(color: Color): TextStyle =
    mmrNavTabLabelStyle(color, compact = true).copy(lineHeight = 11.sp)

@Composable
fun mmrApprovalMetaStyle(color: Color): TextStyle = mmrStatsChartLabel12Style(color)

@Composable
fun mmrLanSyncHistoryKickerStyle(color: Color): TextStyle = mmrLedgerTabLabelStyle(color, FontWeight.Bold)

@Composable
fun mmrApprovalAmountStyle(color: Color): TextStyle =
    mmrSettingsRowPrimaryStyle(color).copy(fontWeight = FontWeight.Bold, lineHeight = 16.sp)

@Composable
fun mmrStatsSectionTitleStyle(color: Color): TextStyle =
    MaterialTheme.typography.titleMedium.copy(
        fontFamily = MmrFontFamily,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.SemiBold,
        color = color,
    )

@Composable
fun mmrTitleMediumFixedStyle(
    color: Color,
    fontWeight: FontWeight = FontWeight.Normal,
): TextStyle =
    MaterialTheme.typography.titleMedium.copy(
        fontFamily = MmrFontFamily,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        fontWeight = fontWeight,
        color = color,
    )

@Composable
fun mmrBody15Style(color: Color): TextStyle =
    MaterialTheme.typography.bodyLarge.copy(
        fontFamily = MmrFontFamily,
        fontSize = 15.sp,
        lineHeight = 21.sp,
        fontWeight = FontWeight.Normal,
        color = color,
    )

@Composable
fun mmrNavTabLabelStyle(
    color: Color,
    compact: Boolean,
): TextStyle =
    mmrChromeCaptionStyle(
        color = color,
        fontSize = if (compact) 8.sp else 9.sp,
        lineHeight = if (compact) 10.sp else 11.sp,
    )

@Composable
fun mmrSettingsGroupTextFieldStyle(color: Color): TextStyle =
    mmrSettingsCardRowPrimaryStyle(color).copy(textAlign = TextAlign.End)

/** Inset grouped settings rows (legacy iOS list density). */
@Composable
fun mmrSettingsInsetRowPrimaryStyle(color: Color): TextStyle =
    MaterialTheme.typography.bodyLarge.copy(
        fontFamily = MmrFontFamily,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        fontWeight = FontWeight.Normal,
        color = color,
    )

@Composable
fun mmrBottomSheetTitleStyle(color: Color): TextStyle =
    MaterialTheme.typography.titleLarge.copy(
        fontFamily = MmrFontFamily,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        fontWeight = FontWeight.SemiBold,
        color = color,
    )

@Composable
fun mmrSettingsSliderCaptionStyle(color: Color): TextStyle = mmrSettingsInsetRowPrimaryStyle(color)

@Composable
fun mmrSettingsSliderEndpointStyle(color: Color): TextStyle = mmrSettingsGroupHeadingStyle(color).copy(fontSize = 12.sp)

@Composable
fun mmrSettingsInsetFieldStyle(color: Color): TextStyle =
    mmrSettingsInsetRowPrimaryStyle(color).copy(lineHeight = 20.sp, textAlign = TextAlign.End)

@Composable
fun mmrSettingsStackedFieldValueStyle(color: Color): TextStyle =
    mmrSettingsInsetRowPrimaryStyle(color).copy(lineHeight = 20.sp)

@Composable
fun mmrSettingsGateCodeValueStyle(color: Color): TextStyle =
    mmrBottomSheetTitleStyle(color).copy(
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.2.sp,
    )

@Composable
fun mmrSettingsCompactSectionHeaderStyle(color: Color): TextStyle =
    mmrSettingsGroupHeadingStyle(color).copy(
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.4.sp,
        lineHeight = 16.sp,
    )

@Composable
fun mmrHeaderBadgeLabelStyle(
    color: Color,
    compact: Boolean,
): TextStyle = mmrNavTabLabelStyle(color, compact)

@Composable
fun mmrLedgerTabLabelStyle(
    color: Color,
    fontWeight: FontWeight = FontWeight.Normal,
): TextStyle = mmrChromeCaptionStyle(color, fontSize = 11.sp, lineHeight = 13.sp).copy(fontWeight = fontWeight)

@Composable
fun mmrIosSettingsSectionHeaderStyle(color: Color): TextStyle =
    MaterialTheme.typography.labelLarge.copy(
        fontFamily = MmrFontFamily,
        fontSize = 13.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.2.sp,
        color = color,
    )
