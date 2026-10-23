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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mmr.app.R
import com.mmr.app.testing.MmrTestTags
import com.mmr.app.ui.theme.MmrMmColors
import com.mmr.core.model.settings.MmrAmountColorScheme

@Composable
fun MmrMmHomeTopBar(
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
                .background(MmrMmColors.Header)
                .statusBarsPadding(),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(MmrDimens.mmTitleBarHeight)
                    .padding(horizontal = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.width(44.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MmrMmColors.HeaderBrandTile,
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
                        tint = MmrMmColors.HeaderIcon,
                        modifier = Modifier.size(MmrDimens.mmHeaderIconSize),
                    )
                }
                Text(
                    text = monthTitle,
                    color = MmrMmColors.HeaderIcon,
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
                        tint = MmrMmColors.HeaderIcon,
                        modifier = Modifier.size(MmrDimens.mmHeaderIconSize),
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
                        tint = MmrMmColors.HeaderIcon,
                        modifier = Modifier.size(MmrDimens.mmHeaderIconSize),
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
                            tint = MmrMmColors.HeaderIcon,
                            modifier = Modifier.size(MmrDimens.mmHeaderIconSize),
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
        color = MmrMmColors.Active,
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
fun MmrMmHeaderBar(
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
                .background(MmrMmColors.Header)
                .statusBarsPadding(),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .padding(end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (showBack && onBack != null) {
                IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "Back",
                        tint = MmrMmColors.HeaderIcon,
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
                        tint = MmrMmColors.HeaderIcon,
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
                    color = MmrMmColors.HeaderIcon,
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
                        tint = MmrMmColors.HeaderIcon,
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
fun MmrMmHeaderIconActions(
    onSearch: (() -> Unit)? = null,
    onFilter: (() -> Unit)? = null,
    onApprovals: (() -> Unit)? = null,
    pendingApprovals: Int = 0,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (onSearch != null) {
            IconButton(onClick = onSearch, modifier = Modifier.size(44.dp)) {
                Icon(Icons.Filled.Search, null, tint = MmrMmColors.HeaderIcon, modifier = Modifier.size(18.dp))
            }
        }
        if (onApprovals != null) {
            Box {
                IconButton(onClick = onApprovals, modifier = Modifier.size(44.dp)) {
                    Icon(
                        imageVector = Icons.Outlined.Email,
                        contentDescription = "Approvals",
                        tint = MmrMmColors.HeaderIcon,
                        modifier = Modifier.size(18.dp),
                    )
                }
                if (pendingApprovals > 0) {
                    Surface(
                        color = MmrMmColors.Active,
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
                Icon(Icons.Filled.Tune, null, tint = MmrMmColors.HeaderIcon, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
fun MmrMmTabRow(
    tabs: List<String>,
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    testTagForIndex: ((Int) -> String)? = null,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .background(MmrMmColors.Header),
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
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (selected) MmrMmColors.TabTextSelected else MmrMmColors.TabText,
                        modifier = Modifier.padding(vertical = MmrDimens.homeTabLabelVerticalPadding),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                                .background(if (selected) MmrMmColors.TabIndicator else Color.Transparent),
                    )
                }
            }
        }
    }
}

/** MM Stats Income/Expense toggle row (`stats.xml`: 43dp bar, 13sp labels, 3dp indicator). */
@Composable
fun MmrMmStatsTabRow(
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
                .background(MmrMmColors.Header),
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
                        color = if (selected) MmrMmColors.TabTextSelected else MmrMmColors.TabText,
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
                            .background(if (selected) MmrMmColors.TabIndicator else Color.Transparent),
                )
            }
        }
    }
}

@Composable
fun MmrMmDualSummaryStrip(
    leftLabel: String,
    leftAmount: String,
    leftAmountColor: Color,
    rightLabel: String,
    rightAmount: String,
    rightAmountColor: Color,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth().background(MmrMmColors.SurfaceWhite)) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        vertical = MmrDimens.mmSummaryStripVerticalPadding,
                        horizontal = MmrDimens.listItemPaddingHorizontal,
                    ),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            MmrMmSummaryCell(leftLabel, leftAmount, leftAmountColor, Modifier.weight(1f))
            MmrMmSummaryCell(rightLabel, rightAmount, rightAmountColor, Modifier.weight(1f))
        }
        HorizontalDivider(color = MmrMmColors.BorderMain, thickness = 0.5.dp)
    }
}

@Composable
fun MmrMmSummaryStrip(
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
    val incomeColor = MmrMmColors.incomeColor(amountColorScheme)
    val expenseColor = MmrMmColors.expenseColor(amountColorScheme)
    Column(modifier = modifier.fillMaxWidth().background(MmrMmColors.SurfaceWhite)) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        vertical = MmrDimens.mmSummaryStripVerticalPadding,
                        horizontal = MmrDimens.listItemPaddingHorizontal,
                    ),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            MmrMmSummaryCell(
                incomeLabel,
                incomeAmount,
                incomeColor,
                Modifier.weight(1f),
                amountTestTag = incomeAmountTestTag,
            )
            MmrMmSummaryCell(
                expenseLabel,
                expenseAmount,
                expenseColor,
                Modifier.weight(1f),
                amountTestTag = expenseAmountTestTag,
            )
            MmrMmSummaryCell(
                totalLabel,
                totalAmount,
                MmrMmColors.Reversal,
                Modifier.weight(1f),
                amountTestTag = totalAmountTestTag,
            )
        }
        HorizontalDivider(color = MmrMmColors.BorderMain, thickness = 0.5.dp)
    }
}

@Composable
private fun MmrMmSummaryCell(
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
            fontSize = 11.sp,
            lineHeight = 13.sp,
            color = MmrMmColors.TextStrong,
            maxLines = 1,
        )
        Text(
            text = amount,
            modifier = if (amountTestTag != null) Modifier.testTag(amountTestTag) else Modifier,
            fontSize = 13.sp,
            lineHeight = 15.sp,
            color = amountColor,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun MmrMmFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val background = if (selected) MmrMmColors.Active else Color.Transparent
    val borderColor = if (selected) MmrMmColors.Active else MmrMmColors.BorderMain
    val textColor = if (selected) Color.White else MmrMmColors.TextStrong
    Box(
        modifier =
            modifier
                .height(MmrDimens.mmFilterChipHeight)
                .clip(RoundedCornerShape(4.dp))
                .border(0.5.dp, borderColor, RoundedCornerShape(4.dp))
                .background(background)
                .clickable(onClick = onClick)
                .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            lineHeight = 13.sp,
            color = textColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun MmrMmPeriodChip(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(7.dp),
        color = Color.Transparent,
        border = androidx.compose.foundation.BorderStroke(1.dp, MmrMmColors.HeaderIcon.copy(alpha = 0.35f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = label, color = MmrMmColors.HeaderIcon, fontSize = 14.sp)
            Icon(
                Icons.Filled.ArrowDropDown,
                contentDescription = null,
                tint = MmrMmColors.HeaderIcon,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
fun MmrMmSearchField(
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
                .border(1.dp, MmrMmColors.BorderMain, RoundedCornerShape(6.dp))
                .background(MmrMmColors.SurfaceWhite)
                .padding(horizontal = MmrDimens.spaceMd, vertical = MmrDimens.spaceSm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Search, null, tint = MmrMmColors.Reversal, modifier = Modifier.size(18.dp))
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
                    color = MmrMmColors.Reversal,
                ),
            singleLine = true,
            cursorBrush = SolidColor(MmrMmColors.Active),
            decorationBox = { inner ->
                if (value.isEmpty()) {
                    Text(placeholder, color = MmrMmColors.TextLight, fontSize = 15.sp)
                }
                inner()
            },
        )
        if (value.isNotEmpty()) {
            IconButton(onClick = onClear, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Filled.Clear, null, tint = MmrMmColors.TextLight.copy(alpha = 0.5f))
            }
        }
    }
}

@Composable
fun MmrMmSearchFilterRow(
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
                .background(MmrMmColors.SurfaceWhite)
                .heightIn(min = 44.dp)
                .padding(horizontal = MmrDimens.spaceLg, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            modifier = Modifier.width(MmrDimens.searchFilterLabelWidth),
            fontSize = 14.sp,
            color = MmrMmColors.TextMedium,
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
            color = MmrMmColors.Reversal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (onClear != null) {
            IconButton(
                onClick = onClear,
                modifier = Modifier.size(32.dp),
            ) {
                Icon(Icons.Filled.Clear, null, tint = MmrMmColors.NavIcon, modifier = Modifier.size(16.dp))
            }
        }
    }
    HorizontalDivider(
        modifier = Modifier.padding(start = valueInset),
        color = MmrMmColors.BorderMain,
        thickness = 0.5.dp,
    )
}

@Composable
fun MmrMmConfigSectionHeader(title: String) {
    Text(
        text = title,
        modifier =
            Modifier
                .fillMaxWidth()
                .background(MmrMmColors.ConfigBackground)
                .padding(horizontal = 18.dp, vertical = 12.dp),
        color = MmrMmColors.ConfigTitle,
        fontSize = 16.sp,
        fontWeight = FontWeight.Medium,
    )
}

@Composable
fun MmrMmSettingsSectionTitle(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .background(MmrMmColors.ListBackground)
                .padding(horizontal = 18.dp, vertical = 10.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MmrMmColors.TextStrong,
        )
        subtitle?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MmrMmColors.TextMedium,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

@Composable
fun MmrMmLightSwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .background(MmrMmColors.SurfaceWhite)
                .padding(horizontal = 18.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            color = MmrMmColors.Reversal,
            fontSize = 14.sp,
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors =
                SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = MmrMmColors.Active,
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = MmrMmColors.TextLight,
                ),
        )
    }
    HorizontalDivider(color = MmrMmColors.BorderMain, thickness = 0.5.dp)
}

@Composable
fun MmrMmLightNavRow(
    label: String,
    value: String = "",
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String? = null,
) {
    Row(
        modifier =
            modifier
                .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .background(MmrMmColors.SurfaceWhite)
                .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            color = MmrMmColors.Reversal,
            fontSize = 14.sp,
        )
        if (value.isNotEmpty()) {
            Text(
                text = value,
                color = MmrMmColors.Active,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
    HorizontalDivider(color = MmrMmColors.BorderMain, thickness = 0.5.dp)
}

@Composable
fun MmrMmConfigValueRow(
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
                .background(MmrMmColors.ConfigBackground)
                .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            color = MmrMmColors.ConfigLabel,
            fontSize = 14.sp,
        )
        Text(
            text = value,
            color = MmrMmColors.ConfigValue,
            fontSize = 14.sp,
            maxLines = 2,
            textAlign = TextAlign.End,
            modifier = Modifier.padding(start = 12.dp),
        )
    }
}

@Composable
fun MmrMmConfigSwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .background(MmrMmColors.ConfigBackground)
                .padding(horizontal = 18.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            color = MmrMmColors.ConfigLabel,
            fontSize = 14.sp,
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors =
                SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = MmrMmColors.Active,
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = MmrMmColors.TextMedium,
                ),
        )
    }
}
