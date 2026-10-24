package com.mmr.feature.search.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mmr.app.testing.MmrTestTags
import com.mmr.app.ui.components.MmrDimens
import com.mmr.app.ui.components.MmrHeaderBar
import com.mmr.app.ui.components.MmrLocaleAmountVisualTransformation
import com.mmr.app.ui.components.MmrPeriodChip
import com.mmr.app.ui.components.MmrSearchField
import com.mmr.app.ui.components.MmrSearchFilterRow
import com.mmr.app.ui.state.MmrEmptyState
import com.mmr.app.ui.state.MmrErrorState
import com.mmr.app.ui.state.MmrLoadingState
import com.mmr.app.ui.theme.MmrBrandColors
import com.mmr.feature.home.HomePeriodUtils
import com.mmr.feature.home.model.HomeSummaryUi
import com.mmr.feature.home.ui.HomePeriodSummaryStrip
import com.mmr.feature.home.ui.TransactionCompactRow
import com.mmr.feature.home.ui.TransactionDayHeaderRow
import com.mmr.feature.search.SearchHeaderPeriodMode
import com.mmr.feature.search.model.TransactionSearchUiEvent
import com.mmr.feature.search.viewmodel.TransactionSearchViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionSearchScreen(
    onNavigateBack: () -> Unit,
    onNavigateToEditTransaction: (Long) -> Unit,
    viewModel: TransactionSearchViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    var periodMenuExpanded by remember { mutableStateOf(false) }
    var expandedFilterField by remember { mutableStateOf(SearchFilterExpandedField.None) }

    if (state.showDateRangePicker) {
        SearchDateRangeDialog(
            initialStart = state.periodStart,
            initialEnd = state.periodEnd,
            onConfirm = { start, end ->
                viewModel.onEvent(TransactionSearchUiEvent.OnDateRangeSelected(start, end))
            },
            onDismiss = { viewModel.onEvent(TransactionSearchUiEvent.OnDismissDateRangePicker) },
        )
    }

    Scaffold(
        containerColor = MmrBrandColors.ListBackground,
        topBar = {
            MmrHeaderBar(
                title = "Search",
                showBack = true,
                onBack = onNavigateBack,
                trailing = {
                    Box {
                        MmrPeriodChip(
                            label = state.headerPeriodMode.label,
                            onClick = { periodMenuExpanded = true },
                            modifier = Modifier.testTag(MmrTestTags.searchPeriodChip),
                        )
                        androidx.compose.material3.DropdownMenu(
                            expanded = periodMenuExpanded,
                            onDismissRequest = { periodMenuExpanded = false },
                        ) {
                            SearchHeaderPeriodMode.entries.forEach { mode ->
                                androidx.compose.material3.DropdownMenuItem(
                                    text = { Text(mode.label) },
                                    onClick = {
                                        periodMenuExpanded = false
                                        viewModel.onEvent(
                                            TransactionSearchUiEvent.OnHeaderPeriodModeSelected(mode),
                                        )
                                    },
                                )
                            }
                        }
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding),
        ) {
            when {
                state.isLoading -> MmrLoadingState(modifier = Modifier.fillMaxSize())
                state.errorMessage != null ->
                    MmrErrorState(
                        message = state.errorMessage ?: "Something went wrong.",
                        onRetry = { viewModel.onEvent(TransactionSearchUiEvent.OnClearFilters) },
                    )
                else -> {
                    val selectedAccountName =
                        state.accounts
                            .firstOrNull { it.id == state.selectedAccountId }
                            ?.name ?: "All"
                    val categoryDisplay = categoryLabel(state)

                    Column(modifier = Modifier.fillMaxSize()) {
                        Column(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .background(MmrBrandColors.Header),
                        ) {
                            MmrSearchField(
                                value = state.noteQuery,
                                onValueChange = {
                                    viewModel.onEvent(TransactionSearchUiEvent.OnNoteQueryChanged(it))
                                },
                                onClear = {
                                    viewModel.onEvent(TransactionSearchUiEvent.OnNoteQueryChanged(""))
                                },
                                placeholder = "Search",
                            )
                        }

                        Column(modifier = Modifier.fillMaxWidth().background(MmrBrandColors.SurfaceWhite)) {
                            if (state.filtersExpanded) {
                                MmrSearchFilterRow(
                                    label = "Period",
                                    value = periodLabel(state.periodStart, state.periodEnd),
                                    onClick = { viewModel.onEvent(TransactionSearchUiEvent.OnPeriodClick) },
                                    onClear =
                                        if (state.periodStart != null) {
                                            { viewModel.onEvent(TransactionSearchUiEvent.OnClearPeriod) }
                                        } else {
                                            null
                                        },
                                )
                                SearchExpandableAccountFilter(
                                    state = state,
                                    expanded = expandedFilterField == SearchFilterExpandedField.Account,
                                    onExpandedChange = { open ->
                                        expandedFilterField =
                                            if (open) {
                                                SearchFilterExpandedField.Account
                                            } else {
                                                SearchFilterExpandedField.None
                                            }
                                    },
                                    onEvent = viewModel::onEvent,
                                    selectedAccountName = selectedAccountName,
                                )
                                SearchExpandableCategoryFilter(
                                    state = state,
                                    expanded = expandedFilterField == SearchFilterExpandedField.Category,
                                    onExpandedChange = { open ->
                                        if (open) {
                                            viewModel.onEvent(TransactionSearchUiEvent.OnCategoryRowClick)
                                            expandedFilterField = SearchFilterExpandedField.Category
                                        } else {
                                            expandedFilterField = SearchFilterExpandedField.None
                                        }
                                    },
                                    onEvent = viewModel::onEvent,
                                    categoryDisplay = categoryDisplay,
                                )
                                SearchAmountMmRow(
                                    amountMin = state.amountMin,
                                    amountMax = state.amountMax,
                                    onMinChange = {
                                        viewModel.onEvent(TransactionSearchUiEvent.OnAmountMinChanged(it))
                                    },
                                    onMaxChange = {
                                        viewModel.onEvent(TransactionSearchUiEvent.OnAmountMaxChanged(it))
                                    },
                                    onClear =
                                        if (state.amountMin.isNotBlank() || state.amountMax.isNotBlank()) {
                                            {
                                                viewModel.onEvent(TransactionSearchUiEvent.OnAmountMinChanged(""))
                                                viewModel.onEvent(TransactionSearchUiEvent.OnAmountMaxChanged(""))
                                            }
                                        } else {
                                            null
                                        },
                                )
                            }

                            Row(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.onEvent(TransactionSearchUiEvent.OnToggleFiltersExpanded)
                                        }.padding(vertical = 10.dp, horizontal = MmrDimens.spaceLg),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector =
                                        if (state.filtersExpanded) {
                                            Icons.Filled.KeyboardArrowUp
                                        } else {
                                            Icons.Filled.KeyboardArrowDown
                                        },
                                    contentDescription = null,
                                    tint = MmrBrandColors.TextLight,
                                    modifier = Modifier.size(20.dp),
                                )
                                Text(
                                    text =
                                        if (state.filtersExpanded) {
                                            "Hide filters · ${filterSummary(state)}"
                                        } else {
                                            "Show filters · ${filterSummary(state)}"
                                        },
                                    modifier = Modifier.padding(start = MmrDimens.spaceSm),
                                    color = MmrBrandColors.TextMedium,
                                    style = MaterialTheme.typography.labelMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            HorizontalDivider(color = MmrBrandColors.BorderMain, thickness = 0.5.dp)
                        }

                        HomePeriodSummaryStrip(
                            summary =
                                HomeSummaryUi(
                                    totalIncome = state.totalIncome,
                                    totalExpense = state.totalExpense,
                                    netBalance = state.totalIncome - state.totalExpense,
                                ),
                            amountColorScheme = state.amountColorScheme,
                        )

                        LazyColumn(
                            modifier =
                                Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .background(MmrBrandColors.ListBackground),
                            contentPadding = PaddingValues(bottom = MmrDimens.space2xl),
                        ) {
                            if (state.isEmpty) {
                                item(key = "empty") {
                                    MmrEmptyState(message = "No transactions match your filters.")
                                }
                            } else {
                                state.groupedResults.forEach { group ->
                                    item(key = "header-${group.dateKey}") {
                                        TransactionDayHeaderRow(group = group)
                                    }
                                    items(
                                        count = group.items.size,
                                        key = { group.items[it].id },
                                    ) { index ->
                                        val item = group.items[index]
                                        TransactionCompactRow(
                                            item = item,
                                            onClick = { onNavigateToEditTransaction(item.id) },
                                            onLongClick = { onNavigateToEditTransaction(item.id) },
                                            isLastInDayGroup = index == group.items.lastIndex,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchAmountMmRow(
    amountMin: String,
    amountMax: String,
    onMinChange: (String) -> Unit,
    onMaxChange: (String) -> Unit,
    onClear: (() -> Unit)?,
) {
    val valueInset = MmrDimens.spaceLg + MmrDimens.searchFilterLabelWidth
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(MmrBrandColors.SurfaceWhite)
                .heightIn(min = 44.dp)
                .padding(horizontal = MmrDimens.spaceLg, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Amount",
            modifier = Modifier.width(MmrDimens.searchFilterLabelWidth),
            color = MmrBrandColors.TextMedium,
            textAlign = TextAlign.Start,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
        )
        SearchAmountCompactField(
            value = amountMin,
            onValueChange = onMinChange,
            placeholder = "Min",
            modifier = Modifier.weight(1f),
        )
        Text(
            text = "~",
            modifier = Modifier.padding(horizontal = 4.dp),
            color = MmrBrandColors.TextLight,
            style = MaterialTheme.typography.bodyMedium,
        )
        SearchAmountCompactField(
            value = amountMax,
            onValueChange = onMaxChange,
            placeholder = "Max",
            modifier = Modifier.weight(1f),
        )
        if (onClear != null) {
            IconButton(onClick = onClear, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Filled.Clear, contentDescription = "Clear amount", tint = MmrBrandColors.NavIcon)
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
private fun SearchAmountCompactField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    val amountTransformation = remember { MmrLocaleAmountVisualTransformation() }
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier =
            modifier
                .height(36.dp)
                .border(1.dp, MmrBrandColors.BorderMain, RoundedCornerShape(4.dp))
                .padding(horizontal = 8.dp),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        visualTransformation =
            if (value.isEmpty()) {
                VisualTransformation.None
            } else {
                amountTransformation
            },
        textStyle =
            MaterialTheme.typography.bodyMedium.copy(
                fontSize = 14.sp,
                color = MmrBrandColors.Reversal,
            ),
        cursorBrush = SolidColor(MmrBrandColors.Active),
        decorationBox = { inner ->
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        color = MmrBrandColors.TextLight,
                        fontSize = 13.sp,
                    )
                }
                inner()
            }
        },
    )
}

private fun periodLabel(
    start: java.time.LocalDate?,
    end: java.time.LocalDate?,
): String {
    if (start == null || end == null) return "Any time"
    val s = HomePeriodUtils.formatShortDate(minOf(start, end))
    val e = HomePeriodUtils.formatShortDate(maxOf(start, end))
    return "$s — $e"
}

private fun categoryLabel(state: com.mmr.feature.search.model.TransactionSearchUiState): String {
    val category = state.categories.firstOrNull { it.id == state.selectedCategoryId }
    if (category == null) return "All"
    val sub = category.subcategories.firstOrNull { it.id == state.selectedSubcategoryId }
    return if (sub != null) "${category.name} › ${sub.name}" else category.name
}

private fun filterSummary(state: com.mmr.feature.search.model.TransactionSearchUiState): String {
    val parts =
        buildList {
            if (state.noteQuery.isNotBlank()) add("Note")
            if (state.periodStart != null) add("Period")
            if (state.selectedAccountId != null) add("Account")
            if (state.selectedCategoryId != null) add("Category")
            if (state.amountMin.isNotBlank() || state.amountMax.isNotBlank()) add("Amount")
        }
    return if (parts.isEmpty()) "All transactions" else parts.joinToString(" · ")
}
