package com.mmr.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mmr.app.R
import com.mmr.app.testing.MmrTestTags
import com.mmr.app.ui.theme.MmrBrandColors
import com.mmr.core.model.settings.MmrAmountColorScheme

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
                    color = MmrBrandColors.HeaderIcon,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
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
                modifier = Modifier.width(96.dp),
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
                IconButton(
                    onClick = onApprovalsClick,
                    modifier =
                        Modifier
                            .testTag(MmrTestTags.homeTopBarApprovals)
                            .size(MmrDimens.homeTopBarIconButton),
                ) {
                    Box(
                        modifier = Modifier.size(22.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Outlined.Email,
                            contentDescription = "Approvals",
                            tint = MmrBrandColors.HeaderIcon,
                            modifier = Modifier.size(MmrDimens.chromeHeaderIconSize),
                        )
                        if (pendingApprovalCount > 0) {
                            MmrHeaderCountBadge(
                                count = pendingApprovalCount,
                                modifier =
                                    Modifier
                                        .align(Alignment.TopEnd)
                                        .offset(x = 6.dp, y = (-4).dp),
                            )
                        }
                    }
                }
            }
        }
        HorizontalDivider(color = Color.Black.copy(alpha = 0.12f), thickness = 0.5.dp)
    }
}

/** Corner badge: sits on the outer edge so ~70%+ of the mail icon stays visible. */
@Composable
private fun MmrHeaderCountBadge(
    count: Int,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = MmrBrandColors.Active,
        shape = RoundedCornerShape(7.dp),
        modifier = modifier,
        shadowElevation = 0.dp,
    ) {
        Text(
            text = count.coerceAtMost(99).toString(),
            color = Color.White,
            fontSize = 9.sp,
            lineHeight = 10.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp),
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
                    color = MmrBrandColors.HeaderIcon,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Normal,
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
        HorizontalDivider(color = Color.Black.copy(alpha = 0.12f), thickness = 0.5.dp)
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
            Box {
                IconButton(onClick = onApprovals, modifier = Modifier.size(44.dp)) {
                    Icon(
                        imageVector = Icons.Outlined.Email,
                        contentDescription = "Approvals",
                        tint = MmrBrandColors.HeaderIcon,
                        modifier = Modifier.size(18.dp),
                    )
                }
                if (pendingApprovals > 0) {
                    Surface(
                        color = MmrBrandColors.Active,
                        shape = RoundedCornerShape(9.dp),
                        modifier =
                            Modifier
                                .align(Alignment.TopEnd)
                                .padding(top = 6.dp, end = 8.dp),
                    ) {
                        Text(
                            text = pendingApprovals.coerceAtMost(99).toString(),
                            color = Color.White,
                            fontSize = 9.sp,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                        )
                    }
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
            MmrTabRowSurface.Ledger -> MmrBrandColors.SurfaceWhite
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
                                    MmrBrandColors.TextStrong
                                } else {
                                    MmrBrandColors.TextLight
                                }
                        }
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Normal,
                        color = labelColor,
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

/** Stats Income/Expense toggle row (43dp bar, 13sp labels, 3dp indicator). */
@Composable
fun MmrStatsTabRow(
    incomeTabLabel: String,
    expenseTabLabel: String,
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .background(MmrBrandColors.Header),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(MmrDimens.statsTabRowHeight),
            verticalAlignment = Alignment.Bottom,
        ) {
            listOf(incomeTabLabel, expenseTabLabel).forEachIndexed { index, label ->
                val selected = index == selectedIndex
                val tabTag = if (index == 0) MmrTestTags.statsTabIncome else MmrTestTags.statsTabExpense
                Column(
                    modifier =
                        Modifier
                            .testTag(tabTag)
                            .weight(1f)
                            .height(MmrDimens.statsTabButtonHeight)
                            .clickable { onTabSelected(index) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = label,
                        fontSize = 13.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (selected) MmrBrandColors.TabTextSelected else MmrBrandColors.TabText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 4.dp),
                    )
                }
            }
        }
        Row(Modifier.fillMaxWidth()) {
            repeat(2) { index ->
                val selected = index == selectedIndex
                Box(
                    modifier =
                        Modifier
                            .weight(1f)
                            .height(MmrDimens.statsTabIndicatorHeight)
                            .background(if (selected) MmrBrandColors.TabIndicator else Color.Transparent),
                )
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
    Column(modifier = modifier.fillMaxWidth().background(MmrBrandColors.SurfaceWhite)) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        vertical = MmrDimens.chromeSummaryStripVerticalPadding,
                        horizontal = MmrDimens.listItemPaddingHorizontal,
                    ),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            MmrSummaryCell(leftLabel, leftAmount, leftAmountColor, Modifier.weight(1f))
            MmrSummaryCell(rightLabel, rightAmount, rightAmountColor, Modifier.weight(1f))
        }
        HorizontalDivider(color = MmrBrandColors.BorderMain, thickness = 0.5.dp)
    }
}

@Composable
fun MmrSummaryStrip(
    incomeLabel: String,
    incomeAmount: String,
    expenseLabel: String,
    expenseAmount: String,
    totalLabel: String,
    totalAmount: String,
    amountColorScheme: MmrAmountColorScheme = MmrAmountColorScheme.SET_B,
    modifier: Modifier = Modifier,
    incomeAmountTestTag: String? = null,
    expenseAmountTestTag: String? = null,
    totalAmountTestTag: String? = null,
) {
    val incomeColor = MmrBrandColors.incomeColor(amountColorScheme)
    val expenseColor = MmrBrandColors.expenseColor(amountColorScheme)
    Column(modifier = modifier.fillMaxWidth().background(MmrBrandColors.SurfaceWhite)) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        vertical = MmrDimens.chromeSummaryStripVerticalPadding,
                        horizontal = MmrDimens.listItemPaddingHorizontal,
                    ),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            MmrSummaryCell(
                incomeLabel,
                incomeAmount,
                incomeColor,
                Modifier.weight(1f),
                amountTestTag = incomeAmountTestTag,
            )
            MmrSummaryCell(
                expenseLabel,
                expenseAmount,
                expenseColor,
                Modifier.weight(1f),
                amountTestTag = expenseAmountTestTag,
            )
            MmrSummaryCell(
                totalLabel,
                totalAmount,
                MmrBrandColors.Reversal,
                Modifier.weight(1f),
                amountTestTag = totalAmountTestTag,
            )
        }
        HorizontalDivider(color = MmrBrandColors.BorderMain, thickness = 0.5.dp)
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
            color = MmrBrandColors.TextStrong,
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
fun mmrFilterChipTextStyle(): TextStyle =
    MaterialTheme.typography.bodySmall.copy(
        fontSize = 11.sp,
        lineHeight = 13.sp,
        fontWeight = FontWeight.Normal,
    )

/** Home ledger list: Roboto/system sans, single normal weight (MM-like flat list). */
@Composable
fun mmrLedgerListTextStyle(): TextStyle =
    TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 11.sp,
        lineHeight = 13.sp,
        fontWeight = FontWeight.Normal,
    )

@Composable
fun mmrLedgerAmountTextStyle(): TextStyle =
    TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 12.sp,
        lineHeight = 14.sp,
        fontWeight = FontWeight.Normal,
    )

@Composable
fun mmrLedgerDayNumberTextStyle(): TextStyle =
    TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 19.sp,
        lineHeight = 22.sp,
        fontWeight = FontWeight.Normal,
    )

@Composable
fun MmrFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val background = if (selected) MmrBrandColors.Active else Color.Transparent
    val borderColor = if (selected) MmrBrandColors.Active else MmrBrandColors.BorderMain
    val textColor = if (selected) Color.White else MmrBrandColors.TextStrong
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
        Text(
            text = label,
            style = mmrFilterChipTextStyle(),
            color = textColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
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
            Text(text = label, color = MmrBrandColors.HeaderIcon, fontSize = 14.sp)
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
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier =
                Modifier
                    .testTag(MmrTestTags.searchNoteQuery)
                    .weight(1f)
                    .padding(horizontal = 8.dp),
            textStyle =
                MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 15.sp,
                    color = MmrBrandColors.Reversal,
                ),
            singleLine = true,
            cursorBrush = SolidColor(MmrBrandColors.Active),
            decorationBox = { inner ->
                if (value.isEmpty()) {
                    Text(placeholder, color = MmrBrandColors.TextLight, fontSize = 15.sp)
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

@Composable
fun MmrSearchFilterRow(
    label: String,
    value: String,
    onClick: () -> Unit,
    onClear: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val valueInset = MmrDimens.spaceLg + MmrDimens.searchFilterLabelWidth
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .background(MmrBrandColors.SurfaceWhite)
                .heightIn(min = 44.dp)
                .padding(horizontal = MmrDimens.spaceLg, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            modifier = Modifier.width(MmrDimens.searchFilterLabelWidth),
            fontSize = 14.sp,
            color = MmrBrandColors.TextMedium,
            textAlign = TextAlign.Start,
            maxLines = 1,
        )
        Text(
            text = value,
            modifier =
                Modifier
                    .weight(1f)
                    .padding(end = if (onClear != null) 0.dp else MmrDimens.spaceXs),
            fontSize = 14.sp,
            color = MmrBrandColors.Reversal,
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
                color = MmrBrandColors.TextStrong,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 20.sp,
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
) {
    Text(
        text = text,
        modifier = modifier,
        color = MmrBrandColors.TextMedium,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        fontFamily = MaterialTheme.typography.bodyMedium.fontFamily,
        fontWeight = FontWeight.Normal,
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
                color = MmrBrandColors.TextStrong,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 18.sp,
            )
            bullets.forEach { line ->
                MmrHelperText(text = line)
            }
            footer?.let { MmrHelperText(text = it) }
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
                color = MmrBrandColors.TextStrong,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 20.sp,
            )
            bullets.forEach { line ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = "•",
                        color = MmrBrandColors.TextMedium,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
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
                color = MmrBrandColors.TextMedium,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.4.sp,
                lineHeight = 16.sp,
            )
            subtitle?.let {
                MmrHelperText(
                    text = it,
                    modifier = Modifier.padding(top = 2.dp),
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
            color = MmrBrandColors.Reversal,
            fontSize = 14.sp,
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
                color = MmrBrandColors.Reversal,
                fontSize = 14.sp,
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
                color = MmrBrandColors.Active,
                fontSize = 14.sp,
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
            color = MmrBrandColors.ConfigLabel,
            fontSize = 14.sp,
        )
        Text(
            text = value,
            color = MmrBrandColors.ConfigValue,
            fontSize = 14.sp,
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
            color = MmrBrandColors.ConfigLabel,
            fontSize = 14.sp,
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
