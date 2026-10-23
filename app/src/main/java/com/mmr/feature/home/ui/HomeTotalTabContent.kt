package com.mmr.feature.home.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mmr.app.testing.MmrTestTags
import com.mmr.app.ui.components.MmrDimens
import com.mmr.app.ui.components.MmrSecondaryActionButton
import com.mmr.app.ui.theme.MmrMmColors
import com.mmr.feature.home.model.HomeTotalAccountsSummaryUi
import com.mmr.feature.home.model.TransactionGroupUi

@Composable
fun HomeTotalTabContent(
    summary: HomeTotalAccountsSummaryUi,
    groups: List<TransactionGroupUi>,
    selectionMode: Boolean,
    selectedIds: Set<Long>,
    onItemClick: (Long) -> Unit,
    onItemLongClick: (Long) -> Unit,
    onExportClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding =
            PaddingValues(
                bottom = MmrDimens.fabListBottomInset,
                top = MmrDimens.spaceXs,
            ),
    ) {
        item(key = "total-accounts-header") {
            TotalAccountsSectionHeader(periodRangeLabel = summary.periodRangeLabel)
        }
        item(key = "total-accounts-card") {
            TotalAccountsSummaryCard(summary = summary)
        }
        item(key = "total-export") {
            MmrSecondaryActionButton(
                text = "Export data to Excel",
                onClick = onExportClick,
                modifier =
                    Modifier
                        .testTag(MmrTestTags.homeExportExcel)
                        .fillMaxWidth()
                        .padding(horizontal = MmrDimens.listItemPaddingHorizontal)
                        .padding(top = MmrDimens.spaceSm, bottom = MmrDimens.spaceMd),
            )
        }
        groups.forEachIndexed { groupIndex, group ->
            if (groupIndex > 0) {
                item(key = "day-sep-${group.dateKey}") {
                    TransactionDayGroupSeparator()
                }
            }
            item(key = "header-${group.dateKey}") {
                TransactionDayHeaderRow(group = group)
            }
            items(group.items, key = { it.id }) { item ->
                if (selectionMode) {
                    TransactionSelectionRowCard(
                        item = item,
                        selected = item.id in selectedIds,
                        onClick = { onItemClick(item.id) },
                        onLongClick = { onItemLongClick(item.id) },
                    )
                } else {
                    TransactionCompactRow(
                        item = item,
                        onClick = { onItemClick(item.id) },
                        onLongClick = { onItemLongClick(item.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun TotalAccountsSectionHeader(periodRangeLabel: String) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = MmrDimens.listItemPaddingHorizontal)
                .padding(bottom = MmrDimens.spaceXs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.AccountBalance,
                contentDescription = null,
                tint = MmrMmColors.TextMedium,
                modifier = Modifier.padding(end = 2.dp),
            )
            Text(
                text = "Accounts",
                style =
                    MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                    ),
                color = MmrMmColors.TextStrong,
            )
        }
        Text(
            text = periodRangeLabel,
            style = MaterialTheme.typography.labelSmall,
            color = MmrMmColors.TextMedium,
        )
    }
}

@Composable
private fun TotalAccountsSummaryCard(summary: HomeTotalAccountsSummaryUi) {
    val shape = RoundedCornerShape(10.dp)
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = MmrDimens.listItemPaddingHorizontal)
                .padding(bottom = MmrDimens.spaceSm)
                .border(1.dp, MmrMmColors.BorderMain.copy(alpha = 0.55f), shape)
                .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        TotalSummaryAnnotatedRow(
            label = comparedExpensesLabel(),
            value = summary.comparedExpensePercentLabel,
        )
        TotalSummaryRow(
            label = summary.cashExpenseLabel,
            value = formatRupeeAmount(summary.accountExpense),
        )
        if (summary.showCardExpenseRow) {
            TotalSummaryRow(
                label = summary.cardExpenseLabel,
                value = formatRupeeAmount(summary.cardExpense),
            )
        }
        TotalSummaryRow(
            label = summary.transferExpenseLabel,
            value = formatRupeeAmount(summary.transferAmount),
        )
    }
}

@Composable
private fun comparedExpensesLabel() =
    buildAnnotatedString {
        append("Compared Expenses ")
        withStyle(SpanStyle(color = MmrMmColors.TextLight)) {
            append("(Last month)")
        }
    }

@Composable
private fun TotalSummaryAnnotatedRow(
    label: androidx.compose.ui.text.AnnotatedString,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MmrMmColors.TextMedium,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = MmrMmColors.TextStrong,
        )
    }
}

@Composable
private fun TotalSummaryRow(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MmrMmColors.TextMedium,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = MmrMmColors.TextStrong,
        )
    }
}
