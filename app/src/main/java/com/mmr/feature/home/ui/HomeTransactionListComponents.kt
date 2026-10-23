package com.mmr.feature.home.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mmr.app.testing.MmrTestTags
import com.mmr.app.ui.components.MmrDimens
import com.mmr.app.ui.theme.MmrMmColors
import com.mmr.feature.home.model.TransactionGroupUi
import com.mmr.feature.home.model.TransactionRowUi
import java.text.NumberFormat
import java.util.Locale

private val MmrExpenseAmountColor = Color(0xFFD35151)
private val MmrIncomeAmountColor = Color(0xFF7DA7F5)

/** MM `list_view_item_gap` before a new day block. */
@Composable
fun TransactionDayGroupSeparator(modifier: Modifier = Modifier) {
    Spacer(
        modifier =
            modifier
                .fillMaxWidth()
                .height(MmrDimens.transactionDayGroupGap)
                .background(MmrMmColors.ListBackground),
    )
}

@Composable
private fun TransactionRowDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(
        modifier = modifier.fillMaxWidth(),
        color = MmrMmColors.TransactionListBorder,
        thickness = MmrDimens.transactionListDividerThickness,
    )
}

fun formatRupeeAmount(amount: Double): String {
    val formatter =
        NumberFormat.getNumberInstance(Locale.getDefault()).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }
    return "₹ ${formatter.format(amount)}"
}

@Composable
fun TransactionDayHeaderRow(
    group: TransactionGroupUi,
    modifier: Modifier = Modifier,
) {
    val dayPillBackground = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    val dayPillText = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.88f)

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = MmrDimens.transactionDayHeaderMinHeight)
                    .padding(
                        horizontal = MmrDimens.transactionListPaddingHorizontal,
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
                    style =
                        MaterialTheme.typography.titleLarge.copy(
                            fontSize = 19.sp,
                            lineHeight = 22.sp,
                        ),
                    fontWeight = FontWeight.Bold,
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
                        style =
                            MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                lineHeight = 12.sp,
                            ),
                        color = dayPillText,
                        maxLines = 1,
                    )
                }
                Text(
                    text = group.monthYearLabel,
                    style =
                        MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            lineHeight = 12.sp,
                        ),
                    color = MmrMmColors.TextLight,
                    fontWeight = FontWeight.Normal,
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
                    style =
                        MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 14.sp,
                            lineHeight = 16.sp,
                        ),
                    color = MmrIncomeAmountColor,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = formatRupeeAmount(group.dayExpenseTotal),
                    style =
                        MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 14.sp,
                            lineHeight = 16.sp,
                        ),
                    color = MmrExpenseAmountColor,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
        HorizontalDivider(
            modifier = Modifier.fillMaxWidth(),
            color = MmrMmColors.TransactionListBorder,
            thickness = MmrDimens.transactionDayHeaderDividerThickness,
        )
    }
}

@Composable
private fun TransactionRowContent(
    item: TransactionRowUi,
    amountColor: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = item.categoryLabel,
            modifier = Modifier.width(MmrDimens.transactionCategoryColumnWidth),
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp, lineHeight = 14.sp),
            color = MmrMmColors.TextLight,
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
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, lineHeight = 16.sp),
                fontWeight = FontWeight.Medium,
                color = MmrMmColors.TextStrong,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (item.detailLine.isNotBlank()) {
                Text(
                    text = item.detailLine,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, lineHeight = 12.sp),
                    color = MmrMmColors.TextLight,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = item.amountFormatted,
                style =
                    MaterialTheme.typography.labelMedium.copy(
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                    ),
                fontWeight = FontWeight.SemiBold,
                color = amountColor,
                textAlign = TextAlign.End,
            )
            if (item.timeLabel.isNotBlank()) {
                Text(
                    text = item.timeLabel,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, lineHeight = 14.sp),
                    color = MmrMmColors.TextLight,
                )
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
) {
    val amountColor = if (item.isIncome) MmrIncomeAmountColor else MmrExpenseAmountColor
    Column(modifier = modifier.fillMaxWidth()) {
        TransactionRowContent(
            item = item,
            amountColor = amountColor,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = MmrDimens.transactionRowMinHeight)
                    .testTag(MmrTestTags.transactionRow(item.id))
                    .combinedClickable(onClick = onClick, onLongClick = onLongClick)
                    .padding(horizontal = MmrDimens.transactionListPaddingHorizontal),
        )
        TransactionRowDivider()
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TransactionSelectionRowCard(
    item: TransactionRowUi,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val amountColor = if (item.isIncome) MmrIncomeAmountColor else MmrExpenseAmountColor
    val rowBackground =
        if (selected) {
            MmrMmColors.Active.copy(alpha = 0.1f)
        } else {
            Color.Transparent
        }
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = MmrDimens.transactionRowMinHeight)
                    .testTag(MmrTestTags.transactionRow(item.id))
                    .background(rowBackground)
                    .combinedClickable(onClick = onClick, onLongClick = onLongClick)
                    .padding(start = MmrDimens.spaceXs, end = MmrDimens.transactionListPaddingHorizontal),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = selected,
                onCheckedChange = { onClick() },
                colors = CheckboxDefaults.colors(checkedColor = MmrMmColors.Active),
            )
            TransactionRowContent(
                item = item,
                amountColor = amountColor,
                modifier = Modifier.weight(1f),
            )
        }
        TransactionRowDivider()
    }
}
