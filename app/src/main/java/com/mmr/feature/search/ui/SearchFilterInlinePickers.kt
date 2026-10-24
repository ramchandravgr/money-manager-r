package com.mmr.feature.search.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mmr.app.ui.components.MmrDimens
import com.mmr.app.ui.theme.MmrBrandColors
import com.mmr.feature.recurring.ui.RecurringPickerRow
import com.mmr.feature.search.model.TransactionSearchUiEvent
import com.mmr.feature.search.model.TransactionSearchUiState
import com.mmr.feature.transaction.model.AccountPickerMode
import com.mmr.feature.transaction.model.toAccountUi
import com.mmr.feature.transaction.ui.AccountPickerPanel
import com.mmr.feature.transaction.ui.CategoryPickerPanelState
import com.mmr.feature.transaction.ui.CategorySubcategoryPickerPanel
import com.mmr.feature.transaction.ui.searchCategoryPickerCallbacks

internal enum class SearchFilterExpandedField {
    None,
    Account,
    Category,
}

@Composable
internal fun MmrSearchFilterExpandableRow(
    label: String,
    value: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onClear: (() -> Unit)?,
    modifier: Modifier = Modifier,
    expandedContent: @Composable ColumnScope.() -> Unit,
) {
    val valueInset = MmrDimens.spaceLg + MmrDimens.searchFilterLabelWidth
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .background(MmrBrandColors.SurfaceWhite),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable { onExpandedChange(!expanded) }
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
                    Icon(
                        Icons.Filled.Clear,
                        contentDescription = "Clear $label",
                        tint = MmrBrandColors.NavIcon,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
            Icon(
                imageVector =
                    if (expanded) {
                        Icons.Filled.KeyboardArrowUp
                    } else {
                        Icons.Filled.KeyboardArrowDown
                    },
                contentDescription = null,
                tint = MmrBrandColors.TextLight,
                modifier = Modifier.size(20.dp),
            )
        }
        HorizontalDivider(
            modifier = Modifier.padding(start = valueInset),
            color = MmrBrandColors.BorderMain,
            thickness = 0.5.dp,
        )
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(),
            exit = shrinkVertically(),
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            start = valueInset,
                            end = MmrDimens.spaceLg,
                            top = MmrDimens.spaceSm,
                            bottom = MmrDimens.spaceMd,
                        ),
            ) {
                expandedContent()
            }
        }
    }
}

@Composable
internal fun SearchExpandableAccountFilter(
    state: TransactionSearchUiState,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onEvent: (TransactionSearchUiEvent) -> Unit,
    selectedAccountName: String,
) {
    MmrSearchFilterExpandableRow(
        label = "Account",
        value = selectedAccountName,
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        onClear =
            if (state.selectedAccountId != null) {
                { onEvent(TransactionSearchUiEvent.OnAccountSelected(null)) }
            } else {
                null
            },
    ) {
        AccountPickerPanel(
            accounts = state.accounts.map { it.toAccountUi() },
            loans = emptyList(),
            mode = AccountPickerMode.AccountsGrouped,
            selectedAccountId = state.selectedAccountId,
            selectedLoanId = null,
            excludedAccountId = null,
            showAllOption = true,
            allOptionSelected = state.selectedAccountId == null,
            onAllSelected = {
                onEvent(TransactionSearchUiEvent.OnAccountSelected(null))
                onExpandedChange(false)
            },
            onAccountSelected = { id ->
                onEvent(TransactionSearchUiEvent.OnAccountSelected(id))
                onExpandedChange(false)
            },
            onLoanSelected = null,
            listMaxHeight = 240.dp,
        )
    }
}

@Composable
internal fun SearchExpandableCategoryFilter(
    state: TransactionSearchUiState,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onEvent: (TransactionSearchUiEvent) -> Unit,
    categoryDisplay: String,
) {
    MmrSearchFilterExpandableRow(
        label = "Category",
        value = categoryDisplay,
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        onClear =
            if (state.selectedCategoryId != null) {
                { onEvent(TransactionSearchUiEvent.OnClearCategory) }
            } else {
                null
            },
    ) {
        val allSelected = state.selectedCategoryId == null
        RecurringPickerRow(
            label = "All categories",
            selected = allSelected,
            onClick = {
                onEvent(TransactionSearchUiEvent.OnClearCategory)
                onExpandedChange(false)
            },
        )
        CategorySubcategoryPickerPanel(
            panelState =
                CategoryPickerPanelState(
                    categories = state.categories,
                    pickerHighlightedCategoryId = state.pickerHighlightedCategoryId,
                ),
            callbacks = searchCategoryPickerCallbacks(onEvent),
            maxPanelHeight = 260.dp,
            onPickerFinished = { onExpandedChange(false) },
        )
    }
}
