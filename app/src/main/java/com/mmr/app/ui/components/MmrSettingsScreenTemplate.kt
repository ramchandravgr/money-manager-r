package com.mmr.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mmr.app.ui.state.MmrStatusBanner
import com.mmr.app.ui.theme.MmrBrandColors

/**
 * Layout for settings flows:
 * - [MmrSettingsScreenLayout.Hub] — full-bleed white rows (legacy).
 * - [MmrSettingsScreenLayout.InsetGrouped] — iOS Settings–style inset groups ([MmrIosSettingsSection]).
 * - [MmrSettingsScreenLayout.Form] — padded scroll body (sub-screens: backup, LAN sync, etc.).
 */
enum class MmrSettingsScreenLayout {
    Hub,
    InsetGrouped,
    Form,
}

/**
 * Standard shell for Settings tab and all nested settings screens. Prefer this over raw [Scaffold].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MmrSettingsScreenScaffold(
    title: String,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    layout: MmrSettingsScreenLayout = MmrSettingsScreenLayout.Form,
    useBrandTopBar: Boolean = false,
    showBackNavigation: Boolean = true,
    topBarTrailing: @Composable (() -> Unit)? = null,
    visualStyle: MmrSettingsVisualStyle? = null,
    fieldGrouping: MmrSettingsFieldGrouping = MmrSettingsFieldGrouping.Separated,
    content: @Composable ColumnScope.() -> Unit,
) {
    val resolvedStyle =
        visualStyle
            ?: when (layout) {
                MmrSettingsScreenLayout.Hub -> MmrSettingsVisualStyle.Standard
                MmrSettingsScreenLayout.InsetGrouped -> MmrSettingsVisualStyle.Compact
                MmrSettingsScreenLayout.Form -> MmrSettingsVisualStyle.Compact
            }
    CompositionLocalProvider(
        LocalMmrSettingsVisualStyle provides resolvedStyle,
        LocalMmrSettingsFieldGrouping provides fieldGrouping,
    ) {
        SettingsScreenScaffoldBody(
            title = title,
            onNavigateBack = onNavigateBack,
            modifier = modifier,
            layout = layout,
            useBrandTopBar = useBrandTopBar,
            showBackNavigation = showBackNavigation,
            topBarTrailing = topBarTrailing,
            visualStyle = resolvedStyle,
            content = content,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreenScaffoldBody(
    title: String,
    onNavigateBack: () -> Unit,
    modifier: Modifier,
    layout: MmrSettingsScreenLayout,
    useBrandTopBar: Boolean,
    showBackNavigation: Boolean,
    topBarTrailing: @Composable (() -> Unit)?,
    visualStyle: MmrSettingsVisualStyle,
    content: @Composable ColumnScope.() -> Unit,
) {
    Scaffold(
        modifier = modifier,
        containerColor = MmrBrandColors.ListBackground,
        contentWindowInsets = MmrNestedScaffoldInsets,
        topBar = {
            if (useBrandTopBar) {
                MmrTopAppBar(
                    title = title,
                    showBackNavigation = showBackNavigation,
                    onNavigateBack = onNavigateBack,
                    useBrandHeaderStyle = true,
                )
            } else {
                MmrHeaderBar(
                    title = title,
                    showBack = showBackNavigation,
                    onBack = onNavigateBack,
                    trailing = topBarTrailing,
                )
            }
        },
    ) { innerPadding ->
        val scrollState = rememberScrollState()
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .imePadding()
                    .verticalScroll(scrollState)
                    .then(
                        when (layout) {
                            MmrSettingsScreenLayout.Hub -> Modifier
                            MmrSettingsScreenLayout.InsetGrouped ->
                                Modifier.padding(
                                    horizontal = MmrDimens.iosSettingsHorizontalInset,
                                    vertical = MmrDimens.iosSettingsSectionFirstTop,
                                )
                            MmrSettingsScreenLayout.Form ->
                                when (visualStyle) {
                                    MmrSettingsVisualStyle.Standard ->
                                        Modifier.padding(
                                            horizontal = MmrDimens.spaceLg,
                                            vertical = MmrDimens.spaceMd,
                                        )
                                    MmrSettingsVisualStyle.Compact ->
                                        Modifier.padding(
                                            horizontal = MmrDimens.settingsFormPaddingHorizontalCompact,
                                            vertical = MmrDimens.settingsFormPaddingVerticalCompact,
                                        )
                                }
                        },
                    ),
            verticalArrangement =
                when (layout) {
                    MmrSettingsScreenLayout.Hub -> Arrangement.Top
                    MmrSettingsScreenLayout.InsetGrouped -> Arrangement.Top
                    else ->
                        when (visualStyle) {
                            MmrSettingsVisualStyle.Compact ->
                                Arrangement.spacedBy(MmrDimens.settingsFormItemGapCompact)
                            MmrSettingsVisualStyle.Standard ->
                                Arrangement.spacedBy(MmrDimens.spaceMd)
                        }
                },
            content = content,
        )
    }
}

@Composable
fun ColumnScope.MmrSettingsStatusBanners(
    statusMessage: String?,
    errorMessage: String?,
    onDismissStatus: () -> Unit = {},
    onDismissError: () -> Unit = {},
) {
    statusMessage?.let {
        MmrStatusBanner(message = it, onDismiss = onDismissStatus, isError = false)
    }
    errorMessage?.let {
        MmrStatusBanner(message = it, onDismiss = onDismissError, isError = true)
    }
}

@Composable
fun MmrSettingsPanelCard(
    modifier: Modifier = Modifier,
    title: String? = null,
    helper: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val compact = LocalMmrSettingsVisualStyle.current == MmrSettingsVisualStyle.Compact
    val innerPadding = if (compact) MmrDimens.settingsPanelPaddingCompact else MmrDimens.cardPadding
    MmrGroupedFormCard(modifier = modifier) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(innerPadding),
            verticalArrangement =
                Arrangement.spacedBy(if (compact) MmrDimens.spaceXs else MmrDimens.spaceSm),
        ) {
            title?.let {
                Text(
                    text = it,
                    fontSize = if (compact) 14.sp else 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MmrBrandColors.TextStrong,
                )
            }
            helper?.let { MmrHelperText(text = it) }
            content()
        }
    }
}

/** Switch row with optional subtitle — same colors as [MmrLightSwitchRow]. */
@Composable
fun MmrSettingsToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    enabled: Boolean = true,
    switchTestTag: String? = null,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(vertical = if (subtitle != null) 4.dp else 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                color = MmrBrandColors.Reversal,
                fontSize = 14.sp,
            )
            subtitle?.let {
                MmrHelperText(
                    text = it,
                    modifier = Modifier.padding(top = 3.dp),
                )
            }
        }
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

/** List-style settings screen (accounts, categories) with FAB and list background. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MmrSettingsListScaffold(
    title: String,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    showBackNavigation: Boolean = true,
    topBarTrailing: @Composable (() -> Unit)? = null,
    floatingActionButton: @Composable () -> Unit = {},
    snackbarHostState: SnackbarHostState? = null,
    visualStyle: MmrSettingsVisualStyle = MmrSettingsVisualStyle.Compact,
    fieldGrouping: MmrSettingsFieldGrouping = MmrSettingsFieldGrouping.Separated,
    insetGrouped: Boolean = true,
    reserveFabBottomPadding: Boolean = false,
    content: @Composable (PaddingValues) -> Unit,
) {
    CompositionLocalProvider(
        LocalMmrSettingsVisualStyle provides visualStyle,
        LocalMmrSettingsFieldGrouping provides fieldGrouping,
    ) {
        SettingsListScaffoldBody(
            title = title,
            onNavigateBack = onNavigateBack,
            modifier = modifier,
            showBackNavigation = showBackNavigation,
            topBarTrailing = topBarTrailing,
            floatingActionButton = floatingActionButton,
            snackbarHostState = snackbarHostState,
            insetGrouped = insetGrouped,
            reserveFabBottomPadding = reserveFabBottomPadding,
            content = content,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsListScaffoldBody(
    title: String,
    onNavigateBack: () -> Unit,
    modifier: Modifier,
    showBackNavigation: Boolean,
    topBarTrailing: @Composable (() -> Unit)?,
    floatingActionButton: @Composable () -> Unit,
    snackbarHostState: SnackbarHostState?,
    insetGrouped: Boolean,
    reserveFabBottomPadding: Boolean,
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        modifier = modifier,
        containerColor = MmrBrandColors.ListBackground,
        contentWindowInsets = MmrNestedScaffoldInsets,
        floatingActionButton = floatingActionButton,
        floatingActionButtonPosition = FabPosition.End,
        snackbarHost = snackbarHostState?.let { { SnackbarHost(it) } } ?: {},
        topBar = {
            MmrHeaderBar(
                title = title,
                showBack = showBackNavigation,
                onBack = onNavigateBack,
                trailing = topBarTrailing,
            )
        },
    ) { innerPadding ->
        val listPadding =
            if (insetGrouped) {
                PaddingValues(
                    start = MmrDimens.iosSettingsHorizontalInset,
                    end = MmrDimens.iosSettingsHorizontalInset,
                    top = innerPadding.calculateTopPadding() + MmrDimens.iosSettingsSectionFirstTop,
                    bottom =
                        innerPadding.calculateBottomPadding() +
                            if (reserveFabBottomPadding) {
                                MmrDimens.fabListBottomInset
                            } else {
                                MmrDimens.spaceMd
                            },
                )
            } else {
                innerPadding
            }
        content(listPadding)
    }
}

@Composable
fun MmrSettingsMonospaceText(
    text: String,
    modifier: Modifier = Modifier,
    accent: Boolean = false,
) {
    Text(
        text = text,
        modifier = modifier.fillMaxWidth(),
        fontFamily = FontFamily.Monospace,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        color = if (accent) MmrBrandColors.Active else MmrBrandColors.TextMedium,
    )
}
