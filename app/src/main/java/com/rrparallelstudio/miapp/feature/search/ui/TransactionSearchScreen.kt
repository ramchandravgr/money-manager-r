package com.rrparallelstudio.miapp.feature.search.ui

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
import androidx.hilt.navigation.compose.hiltViewModel
import com.rrparallelstudio.miapp.feature.home.HomePeriodUtils
import com.rrparallelstudio.miapp.feature.home.model.HomeSummaryUi
import com.rrparallelstudio.miapp.feature.home.ui.HomePeriodSummaryStrip
import com.rrparallelstudio.miapp.feature.home.ui.TransactionCompactRow
import com.rrparallelstudio.miapp.feature.home.ui.TransactionDayHeaderRow
import com.rrparallelstudio.miapp.feature.search.SearchHeaderPeriodMode
import com.rrparallelstudio.miapp.feature.search.model.TransactionSearchUiEvent
import com.rrparallelstudio.miapp.feature.search.viewmodel.TransactionSearchViewModel
import com.rrparallelstudio.miapp.testing.MmrTestTags
import com.rrparallelstudio.miapp.ui.components.MmrDimens
import com.rrparallelstudio.miapp.ui.components.MmrHeaderBar
import com.rrparallelstudio.miapp.ui.components.MmrLocaleAmountVisualTransformation
import com.rrparallelstudio.miapp.ui.components.MmrPeriodChip
import com.rrparallelstudio.miapp.ui.components.MmrSearchField
import com.rrparallelstudio.miapp.ui.components.MmrSearchFilterRow
import com.rrparallelstudio.miapp.ui.components.mmrSearchFilterFieldTextStyle
import com.rrparallelstudio.miapp.ui.state.MmrEmptyState
import com.rrparallelstudio.miapp.ui.state.MmrErrorState
import com.rrparallelstudio.miapp.ui.state.MmrLoadingState
import com.rrparallelstudio.miapp.ui.theme.MmrBrandColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionSearchScreen(
    onNavigateBack: () -> Unit,
    onNavigateToEditTransaction: (Long) -> Unit,
    viewModel: TransactionSearchViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    var periodMenuExpanded by remember { mutableStateOf(false) }
    var showCategoryDialog by remember { mutableStateOf(false) }
    var showAccountDialog by remember { mutableStateOf(false) }

    if (showCategoryDialog) {
        SearchCategoryFilterDialog(
            categories = state.categories,
            picks = state.categoryPicks,
            onToggle = { categoryId, subcategoryId ->
                viewModel.onEvent(
                    TransactionSearchUiEvent.OnToggleCategoryPick(categoryId, subcategoryId),
                )
            },
            onDismiss = { showCategoryDialog = false },
        )
    }
    if (showAccountDialog) {
        SearchAccountFilterDialog(
            accounts = state.accounts,
            matchingIds = state.accountsMatchingCategories,
            selectedIds = state.selectedAccountIds,
            onToggle = { viewModel.onEvent(TransactionSearchUiEvent.OnToggleAccount(it)) },
            onClear = { viewModel.onEvent(TransactionSearchUiEvent.OnClearAccounts) },
            onDismiss = { showAccountDialog = false },
        )
    }

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
        containerColor = MmrBrandColors.ScreenContainer,
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
                    val selectedAccountName = accountLabel(state)
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

                        Column(
                            modifier = Modifier.fillMaxWidth().background(MmrBrandColors.expressiveChromeSurface()),
                        ) {
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
                                MmrSearchFilterRow(
                                    label = "Account",
                                    value = selectedAccountName,
                                    onClick = { showAccountDialog = true },
                                    onClear =
                                        if (state.selectedAccountIds.isNotEmpty()) {
                                            { viewModel.onEvent(TransactionSearchUiEvent.OnClearAccounts) }
                                        } else {
                                            null
                                        },
                                )
                                MmrSearchFilterRow(
                                    label = "Category",
                                    value = categoryDisplay,
                                    onClick = { showCategoryDialog = true },
                                    onClear =
                                        if (state.categoryPicks.isNotEmpty()) {
                                            { viewModel.onEvent(TransactionSearchUiEvent.OnClearCategoryPicks) }
                                        } else {
                                            null
                                        },
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
                                    tint = MmrBrandColors.onCanvasMuted(),
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
                                    color = MmrBrandColors.onCanvasMuted(),
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
                                    .background(MmrBrandColors.ListAreaBackground),
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
                .background(MmrBrandColors.expressiveChromeSurface())
                .heightIn(min = 44.dp)
                .padding(horizontal = MmrDimens.spaceLg, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Amount",
            modifier = Modifier.width(MmrDimens.searchFilterLabelWidth),
            textAlign = TextAlign.Start,
            style = mmrSearchFilterFieldTextStyle(MmrBrandColors.onCanvasMuted()),
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
            style = mmrSearchFilterFieldTextStyle(MmrBrandColors.onCanvasMuted()),
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
    val fieldStyle = mmrSearchFilterFieldTextStyle(MmrBrandColors.onCanvas())
    val placeholderStyle = mmrSearchFilterFieldTextStyle(MmrBrandColors.onCanvasMuted())
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
        textStyle = fieldStyle,
        cursorBrush = SolidColor(MmrBrandColors.Active),
        decorationBox = { inner ->
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (value.isEmpty()) {
                    Text(text = placeholder, style = placeholderStyle)
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

private fun accountLabel(state: com.rrparallelstudio.miapp.feature.search.model.TransactionSearchUiState): String {
    if (state.selectedAccountIds.isEmpty()) return "All"
    return state.accounts
        .filter { it.id in state.selectedAccountIds }
        .joinToString(", ") { account ->
            "${account.name} · ${searchAccountTypeLabel(account.type)}"
        }.ifBlank { "All" }
}

private fun searchAccountTypeLabel(type: com.rrparallelstudio.miapp.core.model.enums.AccountType): String =
    when (type) {
        com.rrparallelstudio.miapp.core.model.enums.AccountType.BANK -> "Bank"
        com.rrparallelstudio.miapp.core.model.enums.AccountType.CASH -> "Cash"
        com.rrparallelstudio.miapp.core.model.enums.AccountType.CREDIT -> "Card"
        com.rrparallelstudio.miapp.core.model.enums.AccountType.DEBIT -> "Debit"
        com.rrparallelstudio.miapp.core.model.enums.AccountType.WALLET -> "Wallet"
        com.rrparallelstudio.miapp.core.model.enums.AccountType.LOAN -> "Loan"
        com.rrparallelstudio.miapp.core.model.enums.AccountType.OTHER -> "Other"
    }

private fun categoryLabel(state: com.rrparallelstudio.miapp.feature.search.model.TransactionSearchUiState): String {
    if (state.categoryPicks.isEmpty()) return "All"
    return state.categoryPicks.joinToString(", ") { pick ->
        val category = state.categories.firstOrNull { it.id == pick.categoryId }
        val name = category?.name ?: "Category"
        val sub = pick.subcategoryId?.let { id -> category?.subcategories?.firstOrNull { it.id == id }?.name }
        if (sub != null) "$name · $sub" else name
    }
}

private fun filterSummary(state: com.rrparallelstudio.miapp.feature.search.model.TransactionSearchUiState): String {
    val parts =
        buildList {
            if (state.noteQuery.isNotBlank()) add("Note")
            if (state.periodStart != null) add("Period")
            if (state.selectedAccountIds.isNotEmpty()) add("Account")
            if (state.categoryPicks.isNotEmpty()) add("Category")
            if (state.amountMin.isNotBlank() || state.amountMax.isNotBlank()) add("Amount")
        }
    return if (parts.isEmpty()) "All transactions" else parts.joinToString(" · ")
}
