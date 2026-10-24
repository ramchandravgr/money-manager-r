package com.mmr.feature.home.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mmr.app.testing.MmrTestTags
import com.mmr.app.ui.components.MmrDimens
import com.mmr.app.ui.components.MmrGroupedFormCardCorner
import com.mmr.app.ui.components.mmrLedgerAmountTextStyle
import com.mmr.app.ui.components.mmrLedgerDayNumberTextStyle
import com.mmr.app.ui.components.mmrLedgerListTextStyle
import com.mmr.app.ui.theme.LocalMmrAmountColorScheme
import com.mmr.app.ui.theme.MmrBrandColors
import com.mmr.feature.home.model.TransactionGroupUi
import com.mmr.feature.home.model.TransactionRowUi

/** Vertical gap before a new day block in the transaction list. */
@Composable
fun TransactionDayGroupSeparator(modifier: Modifier = Modifier) {
    Spacer(
        modifier =
            modifier
                .fillMaxWidth()
                .height(MmrDimens.transactionDayGroupGap)
                .background(MmrBrandColors.ListBackground),
    )
}

fun formatRupeeAmount(amount: Double): String =
    com.mmr.core.common.display.MmrMoneyFormat
        .formatWithSpace(amount, "₹")

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TransactionRowPillShell(
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier,
    bottomGap: Dp = MmrDimens.transactionPillGap,
    leadingContent: (@Composable RowScope.() -> Unit)? = null,
    rowContent: @Composable RowScope.() -> Unit,
) {
    val pillShape = RoundedCornerShape(MmrGroupedFormCardCorner)
    val borderColor =
        if (selected) {
            MmrBrandColors.Active
        } else {
            MmrBrandColors.BorderMain.copy(alpha = 0.65f)
        }
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = MmrDimens.transactionPillHorizontalInset)
                .padding(bottom = bottomGap),
    ) {
        Surface(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .combinedClickable(onClick = onClick, onLongClick = onLongClick)
                    .testTag(testTag),
            shape = pillShape,
            color = MmrBrandColors.SurfaceWhite,
            shadowElevation = 1.dp,
            tonalElevation = 0.dp,
            border = BorderStroke(1.dp, borderColor),
        ) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = MmrDimens.transactionRowMinHeight)
                        .padding(
                            horizontal = MmrDimens.transactionPillContentPaddingHorizontal,
                            vertical = MmrDimens.transactionPillContentPaddingVertical,
                        ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                leadingContent?.invoke(this)
                rowContent()
            }
        }
    }
}

@Composable
fun TransactionDayHeaderRow(
    group: TransactionGroupUi,
    modifier: Modifier = Modifier,
) {
    val amountScheme = LocalMmrAmountColorScheme.current
    val dayPillBackground = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    val dayPillText = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.88f)

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = MmrDimens.transactionDayHeaderMinHeight)
                    .padding(
                        horizontal = MmrDimens.transactionPillHorizontalInset,
                        vertical = MmrDimens.transactionDayHeaderPaddingVertical,
                    ),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MmrDimens.spaceXs),
                modifier = Modifier.weight(1f, fill = false),
            ) {
                Text(
                    text = group.dayOfMonth.toString(),
                    modifier = Modifier.widthIn(min = 22.dp),
                    style = mmrLedgerDayNumberTextStyle(),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Start,
                )
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = dayPillBackground,
                ) {
                    Text(
                        text = group.weekdayShort,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.5.dp),
                        style = mmrLedgerListTextStyle().copy(fontSize = 10.sp, lineHeight = 12.sp),
                        color = dayPillText,
                        maxLines = 1,
                    )
                }
                Text(
                    text = group.monthYearLabel,
                    style = mmrLedgerListTextStyle().copy(fontSize = 10.sp, lineHeight = 12.sp),
                    color = MmrBrandColors.TextLight,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(1.dp),
                modifier = Modifier.padding(start = MmrDimens.spaceSm),
            ) {
                Text(
                    text = formatRupeeAmount(group.dayIncomeTotal),
                    style = mmrLedgerAmountTextStyle(),
                    color = MmrBrandColors.incomeColor(amountScheme),
                )
                Text(
                    text = formatRupeeAmount(group.dayExpenseTotal),
                    style = mmrLedgerAmountTextStyle(),
                    color = MmrBrandColors.expenseColor(amountScheme),
                )
            }
        }
    }
}

@Composable
private fun TransactionRowContent(
    item: TransactionRowUi,
    amountColor: Color,
    modifier: Modifier = Modifier,
) {
    val listMetaStyle = mmrLedgerListTextStyle()
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = item.categoryLabel,
            modifier = Modifier.width(MmrDimens.transactionCategoryColumnWidth),
            style = listMetaStyle,
            color = MmrBrandColors.TextLight,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .padding(start = MmrDimens.transactionRowColumnGap),
            verticalArrangement = Arrangement.spacedBy(MmrDimens.transactionTitleSubtitleGap),
        ) {
            Text(
                text = item.title,
                style = listMetaStyle,
                color = MmrBrandColors.TextStrong,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (item.detailLine.isNotBlank()) {
                Text(
                    text = item.detailLine,
                    style = listMetaStyle,
                    color = MmrBrandColors.TextLight,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = item.amountFormatted,
                style = mmrLedgerAmountTextStyle(),
                color = amountColor,
                textAlign = TextAlign.End,
            )
            if (item.timeLabel.isNotBlank()) {
                Text(
                    text = item.timeLabel,
                    style = mmrLedgerListTextStyle().copy(fontSize = 10.sp, lineHeight = 14.sp),
                    color = MmrBrandColors.TextLight,
                )
            }
        }
    }
}

@Composable
fun GroupedTransactionList(
    groups: List<com.mmr.feature.home.model.TransactionGroupUi>,
    onItemClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    contentBottomPadding: androidx.compose.ui.unit.Dp = MmrDimens.fabListBottomInset,
    selectionMode: Boolean = false,
    selectedIds: Set<Long> = emptySet(),
    onItemLongClick: (Long) -> Unit = onItemClick,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = contentBottomPadding, top = 0.dp),
    ) {
        groups.forEachIndexed { groupIndex, group ->
            if (groupIndex > 0) {
                item(key = "day-sep-${group.dateKey}") {
                    TransactionDayGroupSeparator()
                }
            }
            item(key = "header-${group.dateKey}") {
                TransactionDayHeaderRow(group = group)
            }
            items(
                count = group.items.size,
                key = { group.items[it].id },
            ) { index ->
                val item = group.items[index]
                val isLastInDay = index == group.items.lastIndex
                if (selectionMode) {
                    TransactionSelectionRowCard(
                        item = item,
                        selected = item.id in selectedIds,
                        onClick = { onItemClick(item.id) },
                        onLongClick = { onItemLongClick(item.id) },
                        isLastInDayGroup = isLastInDay,
                    )
                } else {
                    TransactionCompactRow(
                        item = item,
                        onClick = { onItemClick(item.id) },
                        onLongClick = { onItemLongClick(item.id) },
                        isLastInDayGroup = isLastInDay,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TransactionCompactRow(
    item: TransactionRowUi,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    isLastInDayGroup: Boolean = false,
) {
    val bottomGap =
        if (isLastInDayGroup) {
            MmrDimens.transactionPillDayEndGap
        } else {
            MmrDimens.transactionPillGap
        }
    val amountScheme = LocalMmrAmountColorScheme.current
    val amountColor =
        if (item.isIncome) {
            MmrBrandColors.incomeColor(amountScheme)
        } else {
            MmrBrandColors.expenseColor(amountScheme)
        }
    TransactionRowPillShell(
        selected = false,
        onClick = onClick,
        onLongClick = onLongClick,
        testTag = MmrTestTags.transactionRow(item.id),
        modifier = modifier,
        bottomGap = bottomGap,
        rowContent = {
            TransactionRowContent(
                item = item,
                amountColor = amountColor,
                modifier = Modifier.weight(1f),
            )
        },
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TransactionSelectionRowCard(
    item: TransactionRowUi,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    isLastInDayGroup: Boolean = false,
) {
    val bottomGap =
        if (isLastInDayGroup) {
            MmrDimens.transactionPillDayEndGap
        } else {
            MmrDimens.transactionPillGap
        }
    val amountScheme = LocalMmrAmountColorScheme.current
    val amountColor =
        if (item.isIncome) {
            MmrBrandColors.incomeColor(amountScheme)
        } else {
            MmrBrandColors.expenseColor(amountScheme)
        }
    TransactionRowPillShell(
        selected = selected,
        onClick = onClick,
        onLongClick = onLongClick,
        testTag = MmrTestTags.transactionRow(item.id),
        modifier = modifier,
        bottomGap = bottomGap,
        leadingContent = {
            Checkbox(
                checked = selected,
                onCheckedChange = { onClick() },
                colors = CheckboxDefaults.colors(checkedColor = MmrBrandColors.Active),
                modifier = Modifier.padding(end = MmrDimens.spaceXs),
            )
        },
        rowContent = {
            TransactionRowContent(
                item = item,
                amountColor = amountColor,
                modifier = Modifier.weight(1f),
            )
        },
    )
}
