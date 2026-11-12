package com.rrparallelstudio.miapp.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.rrparallelstudio.miapp.R
import com.rrparallelstudio.miapp.core.model.settings.MmrAmountColorScheme
import com.rrparallelstudio.miapp.testing.MmrTestTags
import com.rrparallelstudio.miapp.ui.adaptive.currentMmrWidthSizeClass
import com.rrparallelstudio.miapp.ui.adaptive.isCompactWidth
import com.rrparallelstudio.miapp.ui.theme.MmrBrandColors
import com.rrparallelstudio.miapp.ui.theme.MmrChipPaints
import com.rrparallelstudio.miapp.ui.theme.mmrConfigSectionTitleStyle
import com.rrparallelstudio.miapp.ui.theme.mmrHeaderBadgeLabelStyle
import com.rrparallelstudio.miapp.ui.theme.mmrLedgerTabLabelStyle
import com.rrparallelstudio.miapp.ui.theme.mmrPeriodChipLabelStyle
import com.rrparallelstudio.miapp.ui.theme.mmrScreenIntroBulletStyle
import com.rrparallelstudio.miapp.ui.theme.mmrScreenIntroTitleStyle
import com.rrparallelstudio.miapp.ui.theme.mmrSearchNoteQueryStyle
import com.rrparallelstudio.miapp.ui.theme.mmrSettingsCompactSectionHeaderStyle
import com.rrparallelstudio.miapp.ui.theme.mmrSettingsGroupFooterStyle
import com.rrparallelstudio.miapp.ui.theme.mmrSettingsRowPrimaryStyle
import com.rrparallelstudio.miapp.ui.theme.mmrSettingsRowSecondaryStyle
import com.rrparallelstudio.miapp.ui.theme.mmrTitleMediumFixedStyle
import kotlin.math.roundToInt
import com.rrparallelstudio.miapp.ui.theme.mmrFilterChipTextStyle as mmrFilterChipTextStyleTheme
import com.rrparallelstudio.miapp.ui.theme.mmrLedgerAmountTextStyle as mmrLedgerAmountTextStyleTheme
import com.rrparallelstudio.miapp.ui.theme.mmrLedgerDayNumberTextStyle as mmrLedgerDayNumberTextStyleTheme
import com.rrparallelstudio.miapp.ui.theme.mmrLedgerListTextStyle as mmrLedgerListTextStyleTheme
import com.rrparallelstudio.miapp.ui.theme.mmrSearchFilterFieldTextStyle as mmrSearchFilterFieldTextStyleTheme

@Composable
fun MmrHomeTopBar(
    monthTitle: String,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onMonthTitleClick: () -> Unit,
    onSearchClick: () -> Unit,
    onApprovalsClick: () -> Unit,
    pendingApprovalCount: Int = 0,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .background(MmrBrandColors.Header)
                .statusBarsPadding(),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(MmrDimens.chromeTitleBarHeight)
                    .padding(horizontal = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.width(96.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MmrBrandColors.HeaderBrandTile,
                    shadowElevation = 0.dp,
                    modifier =
                        Modifier
                            .padding(start = 8.dp)
                            .size(28.dp),
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_launcher_foreground),
                        contentDescription = "App",
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .padding(3.dp),
                    )
                }
            }
            Row(
                modifier =
                    Modifier
                        .weight(1f)
                        .widthIn(min = 0.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = onPreviousMonth,
                    modifier = Modifier.size(MmrDimens.homeTopBarIconButton),
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "Previous month",
                        tint = MmrBrandColors.HeaderIcon,
                        modifier = Modifier.size(MmrDimens.chromeHeaderIconSize),
                    )
                }
                Text(
                    text = monthTitle,
                    style = mmrTitleMediumFixedStyle(MmrBrandColors.HeaderIcon, FontWeight.SemiBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier =
                        Modifier
                            .weight(1f, fill = false)
                            .widthIn(max = 140.dp)
                            .clickable(onClick = onMonthTitleClick)
                            .padding(horizontal = 2.dp),
                    textAlign = TextAlign.Center,
                )
                IconButton(
                    onClick = onNextMonth,
                    modifier = Modifier.size(MmrDimens.homeTopBarIconButton),
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Next month",
                        tint = MmrBrandColors.HeaderIcon,
                        modifier = Modifier.size(MmrDimens.chromeHeaderIconSize),
                    )
                }
            }
            Row(
                modifier = Modifier.width(106.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = onSearchClick,
                    modifier =
                        Modifier
                            .testTag(MmrTestTags.homeTopBarSearch)
                            .size(MmrDimens.homeTopBarIconButton),
                ) {
                    Icon(
                        Icons.Filled.Search,
                        contentDescription = "Search",
                        tint = MmrBrandColors.HeaderIcon,
                        modifier = Modifier.size(MmrDimens.chromeHeaderIconSize),
                    )
                }
                MmrHeaderApprovalsIconButton(
                    pendingApprovalCount = pendingApprovalCount,
                    onClick = onApprovalsClick,
                    modifier = Modifier.testTag(MmrTestTags.homeTopBarApprovals),
                )
            }
        }
        HorizontalDivider(color = Color.Black.copy(alpha = 0.12f), thickness = 0.5.dp)
    }
}

private fun headerApprovalBadgeLabel(count: Int): String =
    when {
        count <= 0 -> ""
        count > 99 -> "99+"
        else -> count.toString()
    }

@Composable
private fun MmrHeaderApprovalsIconButton(
    pendingApprovalCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .width(MmrDimens.homeTopBarIconButton + 10.dp)
                .height(MmrDimens.homeTopBarIconButton + 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier.size(MmrDimens.homeTopBarIconButton),
        ) {
            Icon(
                Icons.Outlined.Email,
                contentDescription = "Approvals",
                tint = MmrBrandColors.HeaderIcon,
                modifier = Modifier.size(MmrDimens.chromeHeaderIconSize),
            )
        }
        if (pendingApprovalCount > 0) {
            MmrHeaderCountBadge(
                label = headerApprovalBadgeLabel(pendingApprovalCount),
                modifier =
                    Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 1.dp, y = 0.dp),
            )
        }
    }
}

/** Corner badge: sits on the outer edge so ~70%+ of the mail icon stays visible. */
@Composable
private fun MmrHeaderCountBadge(
    label: String,
    modifier: Modifier = Modifier,
) {
    val compact = label.length > 2
    Surface(
        color = MmrBrandColors.Active,
        shape = RoundedCornerShape(7.dp),
        modifier = modifier,
        shadowElevation = 0.dp,
    ) {
        Text(
            text = label,
            style = mmrHeaderBadgeLabelStyle(Color.White, compact).copy(fontWeight = FontWeight.Bold),
            maxLines = 1,
            modifier = Modifier.padding(horizontal = if (compact) 4.dp else 3.dp, vertical = 1.dp),
        )
    }
}

@Composable
fun MmrHeaderBar(
    title: String,
    onPrevious: (() -> Unit)? = null,
    onNext: (() -> Unit)? = null,
    onTitleClick: (() -> Unit)? = null,
    titleContent: (@Composable () -> Unit)? = null,
    showBack: Boolean = false,
    onBack: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    /** Balanced side slots so prev/title/next sit in the visual center (Stats period chrome). */
    centerPeriodChrome: Boolean = false,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .background(MmrBrandColors.Header)
                .statusBarsPadding(),
    ) {
        if (centerPeriodChrome) {
            MmrHeaderBarCenteredPeriodRow(
                title = title,
                onPrevious = onPrevious,
                onNext = onNext,
                onTitleClick = onTitleClick,
                titleContent = titleContent,
                showBack = showBack,
                onBack = onBack,
                trailing = trailing,
            )
        } else {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(MmrDimens.chromeTitleBarHeight)
                        .padding(end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (showBack && onBack != null) {
                    IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "Back",
                            tint = MmrBrandColors.HeaderIcon,
                        )
                    }
                } else if (!showBack) {
                    Spacer(modifier = Modifier.width(12.dp))
                }
                if (onPrevious != null) {
                    IconButton(onClick = onPrevious, modifier = Modifier.size(36.dp)) {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "Previous",
                            tint = MmrBrandColors.HeaderIcon,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
                if (titleContent != null) {
                    Box(
                        modifier =
                            Modifier
                                .weight(1f, fill = false)
                                .padding(horizontal = 2.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        titleContent()
                    }
                } else {
                    Text(
                        text = title,
                        modifier =
                            Modifier
                                .then(
                                    if (onTitleClick != null) Modifier.clickable(onClick = onTitleClick) else Modifier,
                                ).padding(horizontal = 2.dp),
                        style = mmrTitleMediumFixedStyle(MmrBrandColors.HeaderIcon),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (onNext != null) {
                    IconButton(onClick = onNext, modifier = Modifier.size(36.dp)) {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Next",
                            tint = MmrBrandColors.HeaderIcon,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
                Box(modifier = Modifier.weight(1f))
                trailing?.invoke()
            }
        }
        HorizontalDivider(color = Color.Black.copy(alpha = 0.12f), thickness = 0.5.dp)
    }
}

@Composable
private fun MmrHeaderBarCenteredPeriodRow(
    title: String,
    onPrevious: (() -> Unit)?,
    onNext: (() -> Unit)?,
    onTitleClick: (() -> Unit)?,
    titleContent: (@Composable () -> Unit)?,
    showBack: Boolean,
    onBack: (() -> Unit)?,
    trailing: @Composable (() -> Unit)?,
) {
    val sideSlotWidth = 96.dp
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(MmrDimens.chromeTitleBarHeight)
                .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.width(sideSlotWidth),
            contentAlignment = Alignment.CenterStart,
        ) {
            if (showBack && onBack != null) {
                IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "Back",
                        tint = MmrBrandColors.HeaderIcon,
                    )
                }
            }
        }
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onPrevious != null) {
                IconButton(onClick = onPrevious, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "Previous",
                        tint = MmrBrandColors.HeaderIcon,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            if (titleContent != null) {
                Box(
                    modifier =
                        Modifier
                            .padding(horizontal = 2.dp)
                            .widthIn(max = 200.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    titleContent()
                }
            } else {
                Text(
                    text = title,
                    modifier =
                        Modifier
                            .widthIn(max = 200.dp)
                            .then(
                                if (onTitleClick != null) Modifier.clickable(onClick = onTitleClick) else Modifier,
                            ),
                    style = mmrTitleMediumFixedStyle(MmrBrandColors.HeaderIcon),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (onNext != null) {
                IconButton(onClick = onNext, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Next",
                        tint = MmrBrandColors.HeaderIcon,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
        Box(
            modifier = Modifier.width(sideSlotWidth),
            contentAlignment = Alignment.CenterEnd,
        ) {
            trailing?.invoke()
        }
    }
}

@Composable
fun MmrHeaderIconActions(
    onSearch: (() -> Unit)? = null,
    onFilter: (() -> Unit)? = null,
    onApprovals: (() -> Unit)? = null,
    pendingApprovals: Int = 0,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (onSearch != null) {
            IconButton(onClick = onSearch, modifier = Modifier.size(44.dp)) {
                Icon(Icons.Filled.Search, null, tint = MmrBrandColors.HeaderIcon, modifier = Modifier.size(18.dp))
            }
        }
        if (onApprovals != null) {
            Box(
                modifier =
                    Modifier
                        .width(48.dp)
                        .height(48.dp),
                contentAlignment = Alignment.Center,
            ) {
                IconButton(onClick = onApprovals, modifier = Modifier.size(44.dp)) {
                    Icon(
                        imageVector = Icons.Outlined.Email,
                        contentDescription = "Approvals",
                        tint = MmrBrandColors.HeaderIcon,
                        modifier = Modifier.size(18.dp),
                    )
                }
                if (pendingApprovals > 0) {
                    MmrHeaderCountBadge(
                        label = headerApprovalBadgeLabel(pendingApprovals),
                        modifier =
                            Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = 2.dp, y = 4.dp),
                    )
                }
            }
        }
        if (onFilter != null) {
            IconButton(onClick = onFilter, modifier = Modifier.size(44.dp)) {
                Icon(Icons.Filled.Tune, null, tint = MmrBrandColors.HeaderIcon, modifier = Modifier.size(18.dp))
            }
        }
    }
}

/** Where [MmrTabRow] is drawn — header chrome vs white ledger band (MM-style). */
enum class MmrTabRowSurface {
    /** Navy header; selected tab label is white. */
    HeaderChrome,

    /** White surface; dark labels + brand accent underline only. */
    Ledger,
}

@Composable
fun MmrTabRow(
    tabs: List<String>,
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    testTagForIndex: ((Int) -> String)? = null,
    surface: MmrTabRowSurface = MmrTabRowSurface.HeaderChrome,
) {
    val rowBackground =
        when (surface) {
            MmrTabRowSurface.HeaderChrome -> MmrBrandColors.Header
            MmrTabRowSurface.Ledger -> MmrBrandColors.expressiveChromeSurface()
        }
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .background(rowBackground),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(MmrDimens.homeTabRowHeight + 3.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            tabs.forEachIndexed { index, label ->
                val selected = index == selectedIndex
                val tabModifier =
                    Modifier
                        .weight(1f)
                        .then(
                            testTagForIndex?.invoke(index)?.let { tag ->
                                Modifier.testTag(tag)
                            } ?: Modifier,
                        ).clickable { onTabSelected(index) }
                Column(
                    modifier = tabModifier,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    val labelColor =
                        when (surface) {
                            MmrTabRowSurface.HeaderChrome ->
                                if (selected) {
                                    MmrBrandColors.TabTextSelected
                                } else {
                                    MmrBrandColors.TabText
                                }
                            MmrTabRowSurface.Ledger ->
                                if (selected) {
                                    MmrBrandColors.onCanvas()
                                } else {
                                    MmrBrandColors.onCanvasMuted()
                                }
                        }
                    Text(
                        text = label,
                        style = mmrLedgerTabLabelStyle(labelColor),
                        modifier = Modifier.padding(vertical = MmrDimens.homeTabLabelVerticalPadding),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                                .background(
                                    if (selected) {
                                        MmrBrandColors.TabIndicator
                                    } else {
                                        Color.Transparent
                                    },
                                ),
                    )
                }
            }
        }
        if (surface == MmrTabRowSurface.Ledger) {
            HorizontalDivider(color = MmrBrandColors.BorderMain, thickness = 0.5.dp)
        }
    }
}

/**
 * Centered ledger view-mode control (same band height as [MmrTabRow] on [MmrTabRowSurface.Ledger]).
 * Replaces the four-column tab row on Home.
 */
@Composable
fun MmrLedgerViewModeDropdown(
    options: List<String>,
    selectedIndex: Int,
    onOptionSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    triggerTestTag: String? = null,
    testTagForIndex: ((Int) -> String)? = null,
) {
    var expanded by remember { mutableStateOf(false) }
    var anchorWidthPx by remember { mutableIntStateOf(0) }
    val selectedLabel = options.getOrElse(selectedIndex) { "" }
    val rowBackground = MmrBrandColors.expressiveChromeSurface()
    val menuMinWidth = 168.dp
    val density = LocalDensity.current
    val menuOffsetX =
        with(density) {
            val menuPx = menuMinWidth.roundToPx()
            if (anchorWidthPx > 0) {
                DpOffset(x = ((anchorWidthPx - menuPx) / 2).toDp(), y = 0.dp)
            } else {
                DpOffset.Zero
            }
        }
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .background(rowBackground),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(MmrDimens.homeTabRowHeight + 3.dp),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Box(
                modifier =
                    Modifier
                        .wrapContentSize(Alignment.BottomCenter)
                        .onGloballyPositioned { anchorWidthPx = it.size.width },
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier =
                        Modifier
                            .then(
                                triggerTestTag?.let { Modifier.testTag(it) }
                                    ?: Modifier,
                            ).clickable { expanded = !expanded },
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier =
                            Modifier.padding(
                                horizontal = 20.dp,
                                vertical = MmrDimens.homeTabLabelVerticalPadding,
                            ),
                    ) {
                        Text(
                            text = selectedLabel,
                            style = mmrLedgerTabLabelStyle(MmrBrandColors.onCanvas(), FontWeight.SemiBold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Icon(
                            imageVector = Icons.Filled.ArrowDropDown,
                            contentDescription = null,
                            tint = MmrBrandColors.onCanvas(),
                            modifier = Modifier.size(16.dp),
                        )
                    }
                    Box(
                        modifier =
                            Modifier
                                .widthIn(min = 120.dp)
                                .height(3.dp)
                                .background(MmrBrandColors.TabIndicator),
                    )
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    offset = menuOffsetX,
                    modifier = Modifier.widthIn(min = menuMinWidth),
                ) {
                    options.forEachIndexed { index, label ->
                        val selected = index == selectedIndex
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = label,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    color =
                                        if (selected) {
                                            MmrBrandColors.onCanvas()
                                        } else {
                                            MaterialTheme.colorScheme.onSurface
                                        },
                                )
                            },
                            onClick = {
                                expanded = false
                                onOptionSelected(index)
                            },
                            modifier =
                                testTagForIndex?.invoke(index)?.let { tag ->
                                    Modifier.testTag(tag)
                                } ?: Modifier,
                        )
                    }
                }
            }
        }
        HorizontalDivider(color = MmrBrandColors.BorderMain, thickness = 0.5.dp)
    }
}

/** Trends Income/Expense sliding pill; labels include period totals. */
@Composable
fun MmrStatsTabRow(
    incomeTabLabel: String,
    expenseTabLabel: String,
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val fraction by animateFloatAsState(
        targetValue = selectedIndex.coerceIn(0, 1).toFloat(),
        animationSpec = tween(durationMillis = 180),
        label = "statsLedgerPill",
    )
    val trackColor =
        if (MmrBrandColors.canvasIsDark()) {
            MmrBrandColors.filterChipUnselectedFill()
        } else {
            Color(0xFFECECF3)
        }
    val thumbShape = RoundedCornerShape(percent = 50)
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .background(MmrBrandColors.SurfaceWhite)
                .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        BoxWithConstraints(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(MmrDimens.statsTabButtonHeight)
                    .clip(thumbShape)
                    .background(trackColor),
        ) {
            val inset = 3.dp
            val thumbWidth = (maxWidth - inset * 2) / 2
            Box(
                modifier =
                    Modifier
                        .padding(inset)
                        .offset {
                            IntOffset(x = (thumbWidth.toPx() * fraction).roundToInt(), y = 0)
                        }.width(thumbWidth)
                        .fillMaxHeight()
                        .shadow(1.dp, thumbShape)
                        .clip(thumbShape)
                        .background(MmrBrandColors.SurfaceWhite),
            )
            Row(Modifier.fillMaxSize()) {
                listOf(incomeTabLabel, expenseTabLabel).forEachIndexed { index, label ->
                    val selected = index == selectedIndex
                    val tabTag = if (index == 0) MmrTestTags.statsTabIncome else MmrTestTags.statsTabExpense
                    Box(
                        modifier =
                            Modifier
                                .testTag(tabTag)
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable { onTabSelected(index) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = label,
                            style =
                                mmrSettingsRowSecondaryStyle(
                                    if (selected) MmrBrandColors.TextStrong else MmrBrandColors.TextMedium,
                                ).copy(fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 8.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MmrDualSummaryStrip(
    leftLabel: String,
    leftAmount: String,
    leftAmountColor: Color,
    rightLabel: String,
    rightAmount: String,
    rightAmountColor: Color,
    modifier: Modifier = Modifier,
) {
    val wide = !currentMmrWidthSizeClass().isCompactWidth()
    Column(modifier = modifier.fillMaxWidth().background(MmrBrandColors.expressiveSummaryStripBackground())) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        vertical = MmrDimens.chromeSummaryStripVerticalPadding,
                        horizontal = MmrDimens.listItemPaddingHorizontal,
                    ),
            horizontalArrangement =
                if (wide) {
                    Arrangement.spacedBy(32.dp, Alignment.CenterHorizontally)
                } else {
                    Arrangement.SpaceEvenly
                },
        ) {
            val leftMod = if (wide) Modifier else Modifier.weight(1f)
            val rightMod = if (wide) Modifier else Modifier.weight(1f)
            MmrSummaryCell(leftLabel, leftAmount, leftAmountColor, leftMod)
            MmrSummaryCell(rightLabel, rightAmount, rightAmountColor, rightMod)
        }
        HorizontalDivider(color = MmrBrandColors.BorderMain, thickness = 0.5.dp)
    }
}

@Composable
fun MmrSummaryStrip(
    incomeAmount: String,
    expenseAmount: String,
    totalAmount: String,
    amountColorScheme: MmrAmountColorScheme = MmrAmountColorScheme.SET_B,
    carryOverAmount: String? = null,
    carryOverIsIncome: Boolean = true,
    totalIncludesCarryOver: Boolean = false,
    modifier: Modifier = Modifier,
    incomeAmountTestTag: String? = null,
    expenseAmountTestTag: String? = null,
    carryOverAmountTestTag: String? = null,
    totalAmountTestTag: String? = null,
) {
    val incomeColor = MmrBrandColors.incomeColor(amountColorScheme)
    val expenseColor = MmrBrandColors.expenseColor(amountColorScheme)
    val carryColor = if (carryOverIsIncome) incomeColor else expenseColor
    val wide = !currentMmrWidthSizeClass().isCompactWidth()
    Column(modifier = modifier.fillMaxWidth().background(MmrBrandColors.expressiveSummaryStripBackground())) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        vertical = MmrDimens.chromeSummaryStripVerticalPadding,
                        horizontal = MmrDimens.listItemPaddingHorizontal,
                    ),
            horizontalArrangement =
                if (wide) {
                    Arrangement.spacedBy(if (carryOverAmount != null) 16.dp else 28.dp, Alignment.CenterHorizontally)
                } else {
                    Arrangement.SpaceEvenly
                },
        ) {
            val cellMod = if (wide) Modifier else Modifier.weight(1f)
            MmrSummaryIconCell(
                icon = Icons.Filled.ArrowDownward,
                iconContentDescription = "Income",
                helpText = "↓ Income: money received in this period (not including carry-over).",
                iconTint = incomeColor,
                chipBackground = incomeColor.copy(alpha = 0.12f),
                amount = incomeAmount,
                amountColor = incomeColor,
                modifier = cellMod,
                amountTestTag = incomeAmountTestTag,
            )
            MmrSummaryIconCell(
                icon = Icons.Filled.ArrowUpward,
                iconContentDescription = "Expenses",
                helpText = "↑ Expenses: money spent in this period (not including carry-over).",
                iconTint = expenseColor,
                chipBackground = expenseColor.copy(alpha = 0.1f),
                amount = expenseAmount,
                amountColor = expenseColor,
                modifier = cellMod,
                amountTestTag = expenseAmountTestTag,
            )
            if (carryOverAmount != null) {
                MmrSummaryIconCell(
                    icon = MmrCarryOverCalendarArrow,
                    iconContentDescription = "Carry-over",
                    helpText =
                        "Calendar → Carry-over: balance from before this period.\n" +
                            "Surplus adds to total; minus (−) means you ended last period in the red.",
                    iconTint = MmrBrandColors.onCanvas(),
                    chipBackground = Color(0xFF7C6A9A).copy(alpha = 0.14f),
                    amount = carryOverAmount,
                    amountColor = carryColor,
                    modifier = cellMod,
                    amountTestTag = carryOverAmountTestTag,
                )
            }
            val totalA11y = if (totalIncludesCarryOver) "Total including carry-over" else "Total"
            val totalHelp =
                if (totalIncludesCarryOver) {
                    "Total: income minus expenses for this period, plus carry-over from before."
                } else {
                    "Total: income minus expenses for this period."
                }
            MmrSummaryIconCell(
                icon = MmrTotalReceiptSum,
                iconContentDescription = totalA11y,
                helpText = totalHelp,
                iconTint = MmrBrandColors.summaryNetColor(),
                chipBackground = MmrBrandColors.summaryNetColor().copy(alpha = 0.1f),
                amount = totalAmount,
                amountColor = MmrBrandColors.summaryNetColor(),
                modifier = cellMod,
                amountTestTag = totalAmountTestTag,
            )
        }
        HorizontalDivider(color = MmrBrandColors.BorderMain, thickness = 0.5.dp)
    }
}

@Composable
private fun MmrSummaryIconCell(
    icon: ImageVector,
    iconContentDescription: String,
    helpText: String,
    iconTint: Color,
    chipBackground: Color,
    amount: String,
    amountColor: Color,
    modifier: Modifier = Modifier,
    amountTestTag: String? = null,
) {
    val context = LocalContext.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Surface(
            onClick = {
                MmrToast.show(context, helpText)
            },
            color = chipBackground,
            shape = RoundedCornerShape(5.dp),
            modifier =
                Modifier
                    .size(22.dp)
                    .semantics {
                        contentDescription = "$iconContentDescription. Tap for explanation."
                    },
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
        Text(
            text = amount,
            modifier =
                if (amountTestTag != null) {
                    Modifier
                        .testTag(
                            amountTestTag,
                        ).semantics { contentDescription = "$iconContentDescription, $amount" }
                } else {
                    Modifier.semantics { contentDescription = "$iconContentDescription, $amount" }
                },
            style = mmrLedgerAmountTextStyle(),
            color = amountColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun MmrSummaryCell(
    label: String,
    amount: String,
    amountColor: Color,
    modifier: Modifier = Modifier,
    amountTestTag: String? = null,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        Text(
            text = label,
            style = mmrLedgerListTextStyle(),
            color = MmrBrandColors.onCanvas(),
            maxLines = 1,
        )
        Text(
            text = amount,
            modifier = if (amountTestTag != null) Modifier.testTag(amountTestTag) else Modifier,
            style = mmrLedgerAmountTextStyle(),
            color = amountColor,
            maxLines = 1,
            textAlign = TextAlign.Center,
        )
    }
}

/** Shared with home transaction rows (category + note) so list text matches filter chips. */
@Composable
fun mmrFilterChipTextStyle(): TextStyle = mmrFilterChipTextStyleTheme()

/** Home ledger list — same [MmrFontFamily] as app typography; compact sizes for list density. */
@Composable
fun mmrLedgerListTextStyle(): TextStyle = mmrLedgerListTextStyleTheme()

@Composable
fun mmrLedgerAmountTextStyle(): TextStyle = mmrLedgerAmountTextStyleTheme()

@Composable
fun mmrLedgerDayNumberTextStyle(): TextStyle = mmrLedgerDayNumberTextStyleTheme()

@Composable
fun MmrFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showDropdownCaret: Boolean = false,
) {
    val paint = MmrChipPaints.accentFilled(selected)
    val background = paint.fill
    val borderColor = paint.border
    val textColor = paint.content
    Box(
        modifier =
            modifier
                .height(MmrDimens.chromeFilterChipHeight)
                .clip(RoundedCornerShape(4.dp))
                .border(0.5.dp, borderColor, RoundedCornerShape(4.dp))
                .background(background)
                .clickable(onClick = onClick)
                .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = label,
                style = mmrFilterChipTextStyle(),
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (showDropdownCaret) {
                Icon(
                    imageVector = Icons.Filled.ArrowDropDown,
                    contentDescription = null,
                    tint = textColor,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

@Composable
fun MmrPeriodChip(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(7.dp),
        color = Color.Transparent,
        border = androidx.compose.foundation.BorderStroke(1.dp, MmrBrandColors.HeaderIcon.copy(alpha = 0.35f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = label, style = mmrPeriodChipLabelStyle(MmrBrandColors.HeaderIcon))
            Icon(
                Icons.Filled.ArrowDropDown,
                contentDescription = null,
                tint = MmrBrandColors.HeaderIcon,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
fun MmrSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    onClear: () -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = MmrDimens.spaceMd, vertical = MmrDimens.spaceSm)
                .clip(RoundedCornerShape(6.dp))
                .border(1.dp, MmrBrandColors.BorderMain, RoundedCornerShape(6.dp))
                .background(MmrBrandColors.SurfaceWhite)
                .padding(horizontal = MmrDimens.spaceMd, vertical = MmrDimens.spaceSm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Search, null, tint = MmrBrandColors.Reversal, modifier = Modifier.size(18.dp))
        val noteQueryStyle = mmrSearchNoteQueryStyle(MmrBrandColors.Reversal)
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier =
                Modifier
                    .testTag(MmrTestTags.searchNoteQuery)
                    .weight(1f)
                    .padding(horizontal = 8.dp),
            textStyle = noteQueryStyle,
            singleLine = true,
            cursorBrush = SolidColor(MmrBrandColors.Active),
            decorationBox = { inner ->
                if (value.isEmpty()) {
                    Text(
                        placeholder,
                        style = noteQueryStyle.copy(color = MmrBrandColors.TextLight),
                    )
                }
                inner()
            },
        )
        if (value.isNotEmpty()) {
            IconButton(onClick = onClear, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Filled.Clear, null, tint = MmrBrandColors.TextLight.copy(alpha = 0.5f))
            }
        }
    }
}

/** Shared label/value typography for Search filter rows (Period, Account, Category, Amount). */
@Composable
fun mmrSearchFilterFieldTextStyle(color: Color): TextStyle = mmrSearchFilterFieldTextStyleTheme(color)

@Composable
fun MmrSearchFilterRow(
    label: String,
    value: String,
    onClick: () -> Unit,
    onClear: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val valueInset = MmrDimens.spaceLg + MmrDimens.searchFilterLabelWidth
    val labelStyle = mmrSearchFilterFieldTextStyle(MmrBrandColors.onCanvasMuted())
    val valueStyle = mmrSearchFilterFieldTextStyle(MmrBrandColors.onCanvas())
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .background(MmrBrandColors.expressiveChromeSurface())
                .heightIn(min = 44.dp)
                .padding(horizontal = MmrDimens.spaceLg, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            modifier = Modifier.width(MmrDimens.searchFilterLabelWidth),
            style = labelStyle,
            textAlign = TextAlign.Start,
            maxLines = 1,
        )
        Text(
            text = value,
            modifier =
                Modifier
                    .weight(1f)
                    .padding(end = if (onClear != null) 0.dp else MmrDimens.spaceXs),
            style = valueStyle,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (onClear != null) {
            IconButton(
                onClick = onClear,
                modifier = Modifier.size(32.dp),
            ) {
                Icon(Icons.Filled.Clear, null, tint = MmrBrandColors.NavIcon, modifier = Modifier.size(16.dp))
            }
        }
    }
    HorizontalDivider(
        modifier = Modifier.padding(start = valueInset),
        color = MmrBrandColors.BorderMain,
        thickness = 0.5.dp,
    )
}

@Composable
fun MmrConfigSectionHeader(
    title: String,
    subtitle: String? = null,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(MmrDimens.settingsSectionHeaderCorner),
        color = MmrBrandColors.SettingsSectionHeaderFill,
        border = androidx.compose.foundation.BorderStroke(1.dp, MmrBrandColors.BorderMain),
        shadowElevation = 0.dp,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            Text(
                text = title,
                modifier = Modifier.fillMaxWidth(),
                style = mmrConfigSectionTitleStyle(MmrBrandColors.TextStrong),
            )
            subtitle?.let {
                MmrHelperText(
                    text = it,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp),
                )
            }
        }
    }
}

/**
 * Secondary explanatory copy on settings and backup screens. Uses the same 13sp / [MmrBrandColors.TextMedium]
 * treatment as [MmrConfigSectionHeader] subtitles (not [MaterialTheme.typography.bodySmall], which is 11sp).
 */
@Composable
fun MmrHelperText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MmrBrandColors.TextMedium,
) {
    Text(
        text = text,
        modifier = modifier,
        style = mmrSettingsGroupFooterStyle(color),
    )
}

@Composable
fun MmrScreenIntroPanel(
    title: String,
    bullets: List<String>,
    modifier: Modifier = Modifier,
    footer: String? = null,
) {
    if (LocalMmrSettingsVisualStyle.current == MmrSettingsVisualStyle.Compact) {
        Column(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = title,
                style =
                    mmrScreenIntroBulletStyle(MmrBrandColors.listAreaBodyText())
                        .copy(fontWeight = FontWeight.SemiBold),
            )
            bullets.forEach { line ->
                MmrHelperText(text = line, color = MmrBrandColors.listAreaCaptionText())
            }
            footer?.let { MmrHelperText(text = it, color = MmrBrandColors.listAreaCaptionText()) }
        }
        return
    }
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(MmrDimens.settingsSectionHeaderCorner),
        color = MmrBrandColors.SettingsSectionHeaderFill,
        border = androidx.compose.foundation.BorderStroke(1.dp, MmrBrandColors.BorderMain),
        shadowElevation = 0.dp,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = title,
                modifier = Modifier.fillMaxWidth(),
                style = mmrScreenIntroTitleStyle(MmrBrandColors.TextStrong),
            )
            bullets.forEach { line ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = "•",
                        style = mmrSettingsRowSecondaryStyle(MmrBrandColors.TextMedium),
                    )
                    MmrHelperText(text = line, modifier = Modifier.weight(1f))
                }
            }
            footer?.let { MmrHelperText(text = it, modifier = Modifier.fillMaxWidth()) }
        }
    }
}

@Composable
fun MmrSettingsSectionTitle(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    isFirstInScreen: Boolean = false,
) {
    val compact = LocalMmrSettingsVisualStyle.current == MmrSettingsVisualStyle.Compact
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(
                    top =
                        when {
                            isFirstInScreen -> 0.dp
                            compact -> MmrDimens.settingsSectionTopSpacingCompact
                            else -> MmrDimens.settingsSectionTopSpacing
                        },
                ),
    ) {
        if (compact) {
            Text(
                text = title.uppercase(),
                modifier = Modifier.fillMaxWidth(),
                style = mmrSettingsCompactSectionHeaderStyle(MmrBrandColors.listAreaCaptionText()),
            )
            subtitle?.let {
                MmrHelperText(
                    text = it,
                    modifier = Modifier.padding(top = 2.dp),
                    color = MmrBrandColors.listAreaCaptionText(),
                )
            }
            Spacer(modifier = Modifier.height(MmrDimens.settingsSectionContentGapCompact))
        } else {
            MmrConfigSectionHeader(title = title, subtitle = subtitle)
            Spacer(modifier = Modifier.height(MmrDimens.settingsSectionContentGap))
        }
    }
}

@Composable
fun MmrLightSwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val compact = LocalMmrSettingsVisualStyle.current == MmrSettingsVisualStyle.Compact
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .background(MmrBrandColors.SurfaceWhite)
                .padding(
                    horizontal = if (compact) 14.dp else 18.dp,
                    vertical = if (compact) 4.dp else 6.dp,
                ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = mmrSettingsRowPrimaryStyle(MmrBrandColors.Reversal),
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors =
                SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = MmrBrandColors.Active,
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = MmrBrandColors.TextLight,
                ),
        )
    }
    HorizontalDivider(color = MmrBrandColors.BorderMain, thickness = 0.5.dp)
}

@Composable
fun MmrLightNavRow(
    label: String,
    value: String = "",
    subtitle: String? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String? = null,
) {
    val compact = LocalMmrSettingsVisualStyle.current == MmrSettingsVisualStyle.Compact
    val rowPaddingVertical =
        when {
            compact && subtitle != null -> 8.dp
            compact -> 10.dp
            subtitle != null -> 12.dp
            else -> 14.dp
        }
    val rowPaddingHorizontal = if (compact) 14.dp else 18.dp
    Row(
        modifier =
            modifier
                .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .background(MmrBrandColors.SurfaceWhite)
                .padding(horizontal = rowPaddingHorizontal, vertical = rowPaddingVertical),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = label,
                style = mmrSettingsRowPrimaryStyle(MmrBrandColors.Reversal),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle != null) {
                MmrHelperText(
                    text = subtitle,
                    modifier = Modifier.padding(top = 3.dp),
                )
            }
        }
        if (value.isNotEmpty()) {
            Text(
                text = value,
                style = mmrSettingsRowPrimaryStyle(MmrBrandColors.Active),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
    HorizontalDivider(color = MmrBrandColors.BorderMain, thickness = 0.5.dp)
}

@Composable
fun MmrConfigValueRow(
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .background(MmrBrandColors.ConfigBackground)
                .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = mmrSettingsRowPrimaryStyle(MmrBrandColors.ConfigLabel),
        )
        Text(
            text = value,
            style = mmrSettingsRowPrimaryStyle(MmrBrandColors.ConfigValue),
            maxLines = 2,
            textAlign = TextAlign.End,
            modifier = Modifier.padding(start = 12.dp),
        )
    }
}

@Composable
fun MmrConfigSwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .background(MmrBrandColors.ConfigBackground)
                .padding(horizontal = 18.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = mmrSettingsRowPrimaryStyle(MmrBrandColors.ConfigLabel),
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors =
                SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = MmrBrandColors.Active,
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = MmrBrandColors.TextMedium,
                ),
        )
    }
}
