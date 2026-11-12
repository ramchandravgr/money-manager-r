package com.rrparallelstudio.miapp.feature.transaction.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rrparallelstudio.miapp.feature.category.ui.CategoryPickerInlineAdd
import com.rrparallelstudio.miapp.feature.category.ui.CategoryPickerParentRow
import com.rrparallelstudio.miapp.feature.category.ui.CategoryPickerSubRow
import com.rrparallelstudio.miapp.feature.transaction.model.AddEditTransactionUiEvent
import com.rrparallelstudio.miapp.feature.transaction.model.AddEditTransactionUiState
import com.rrparallelstudio.miapp.feature.transaction.model.CategoryUi
import com.rrparallelstudio.miapp.ui.components.MmrDimens
import com.rrparallelstudio.miapp.ui.components.MmrFormCompactSearchField
import com.rrparallelstudio.miapp.ui.components.mmrFillDownToFormBottom
import com.rrparallelstudio.miapp.ui.components.rememberMmrConsumeOverscrollConnection
import com.rrparallelstudio.miapp.ui.theme.MmrBrandColors

@Composable
fun CategorySubcategoryPickerPanel(
    state: AddEditTransactionUiState,
    onEvent: (AddEditTransactionUiEvent) -> Unit,
    modifier: Modifier = Modifier,
    maxPanelHeight: Dp = 300.dp,
    fillRemainingViewport: Boolean = false,
    onPickerFinished: () -> Unit = {},
    showDismissAction: Boolean = false,
    onDismiss: () -> Unit = {},
) {
    CategorySubcategoryPickerPanel(
        panelState =
            CategoryPickerPanelState(
                categories = state.categories,
                pickerHighlightedCategoryId = state.pickerHighlightedCategoryId,
            ),
        callbacks = transactionCategoryPickerCallbacks(onEvent),
        modifier = modifier,
        maxPanelHeight = maxPanelHeight,
        fillRemainingViewport = fillRemainingViewport,
        onPickerFinished = onPickerFinished,
        showDismissAction = showDismissAction,
        onDismiss = onDismiss,
    )
}

@Composable
fun CategorySubcategoryPickerPanel(
    panelState: CategoryPickerPanelState,
    callbacks: CategoryPickerPanelCallbacks,
    modifier: Modifier = Modifier,
    maxPanelHeight: Dp = 300.dp,
    fillRemainingViewport: Boolean = false,
    onPickerFinished: () -> Unit = {},
    showDismissAction: Boolean = false,
    onDismiss: () -> Unit = {},
) {
    val highlightedId = panelState.pickerHighlightedCategoryId
    val highlightedCategory = panelState.categories.firstOrNull { it.id == highlightedId }
    val subcategories = highlightedCategory?.subcategories ?: emptyList()
    var searchQuery by remember { mutableStateOf("") }
    val normalizedQuery = searchQuery.trim()
    val filteredCategories =
        remember(panelState.categories, normalizedQuery) {
            filterCategoriesForSearch(panelState.categories, normalizedQuery)
        }
    val visibleSubs =
        remember(subcategories, normalizedQuery, highlightedCategory) {
            if (highlightedCategory == null) {
                emptyList()
            } else if (normalizedQuery.isEmpty()) {
                subcategories
            } else {
                val matchingSubs =
                    subcategories.filter {
                        it.name.contains(normalizedQuery, ignoreCase = true)
                    }
                if (matchingSubs.isNotEmpty()) {
                    matchingSubs
                } else if (highlightedCategory.name.contains(normalizedQuery, ignoreCase = true)) {
                    subcategories
                } else {
                    emptyList()
                }
            }
        }

    LaunchedEffect(normalizedQuery, filteredCategories) {
        if (normalizedQuery.isEmpty()) return@LaunchedEffect
        val parentWithSubMatch =
            filteredCategories.firstOrNull { category ->
                category.subcategories.any {
                    it.name.contains(normalizedQuery, ignoreCase = true)
                }
            }
        val target =
            parentWithSubMatch
                ?: filteredCategories.firstOrNull {
                    it.name.contains(normalizedQuery, ignoreCase = true)
                }
                ?: filteredCategories.firstOrNull()
        if (target != null && target.id != highlightedId) {
            callbacks.onParentSelected(target.id)
        }
    }

    fun onParentOnlyConfirmed() {
        callbacks.onConfirmParentOnly()
        onPickerFinished()
    }

    fun onParentRowClick(categoryId: Long) {
        val category = panelState.categories.firstOrNull { it.id == categoryId } ?: return
        if (category.id == highlightedId) {
            onParentOnlyConfirmed()
            return
        }
        callbacks.onParentSelected(categoryId)
        if (category.subcategories.isEmpty()) {
            onPickerFinished()
        }
    }

    fun onSubcategoryPicked(subcategoryId: Long) {
        callbacks.onSubcategorySelected(subcategoryId)
        onPickerFinished()
    }

    val consumeOverscroll = rememberMmrConsumeOverscrollConnection()
    Column(modifier = modifier.fillMaxWidth()) {
        if (showDismissAction) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = MmrDimens.spaceSm),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Done", color = MmrBrandColors.Active, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        MmrFormCompactSearchField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = "Search…",
            onLightFormCard = true,
        )
        if (panelState.categories.isEmpty()) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(MmrDimens.space2xl),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "No categories for this transaction type.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MmrBrandColors.TextMedium,
                    textAlign = TextAlign.Center,
                )
                callbacks.onAddCategory?.let { onAdd ->
                    CategoryPickerInlineAdd(
                        label = "Add category",
                        onClick = onAdd,
                    )
                }
            }
        } else if (filteredCategories.isEmpty()) {
            Text(
                text = "No matches for \"$normalizedQuery\".",
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(MmrDimens.space2xl),
                style = MaterialTheme.typography.bodyMedium,
                color = MmrBrandColors.TextMedium,
                textAlign = TextAlign.Center,
            )
        } else {
            val listAreaModifier =
                if (fillRemainingViewport) {
                    Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = true)
                        .fillMaxHeight()
                } else {
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 200.dp, max = maxPanelHeight)
                }
            val listsColumnModifier =
                if (fillRemainingViewport) {
                    Modifier
                        .fillMaxWidth()
                        .mmrFillDownToFormBottom(
                            minHeight = 240.dp,
                            endInset = MmrDimens.spaceSm + 8.dp,
                        )
                } else {
                    Modifier.fillMaxWidth()
                }
            Column(modifier = listsColumnModifier) {
                Row(modifier = listAreaModifier) {
                    LazyColumn(
                        modifier =
                            Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .then(
                                    if (fillRemainingViewport) {
                                        Modifier.nestedScroll(consumeOverscroll)
                                    } else {
                                        Modifier
                                    },
                                ),
                    ) {
                        items(filteredCategories, key = { it.id }) { category ->
                            CategoryPickerParentRow(
                                name = category.name,
                                colorHex = category.colorHex,
                                iconName = category.iconName,
                                selected = category.id == highlightedId,
                                onClick = { onParentRowClick(category.id) },
                                onLongClick = { callbacks.onEditCategory?.invoke(category.id) },
                            )
                        }
                        callbacks.onAddCategory?.let { onAdd ->
                            item {
                                CategoryPickerInlineAdd(
                                    label = "Add category",
                                    onClick = onAdd,
                                )
                            }
                        }
                    }
                    Box(
                        modifier =
                            Modifier
                                .fillMaxHeight()
                                .width(1.dp)
                                .background(MmrBrandColors.BorderMain.copy(alpha = 0.5f)),
                    )
                    LazyColumn(
                        modifier =
                            Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .then(
                                    if (fillRemainingViewport) {
                                        Modifier.nestedScroll(consumeOverscroll)
                                    } else {
                                        Modifier
                                    },
                                ),
                    ) {
                        if (highlightedCategory == null) {
                            item {
                                Text(
                                    text = "Select a category",
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(MmrDimens.space2xl),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MmrBrandColors.TextLight,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                        when {
                            highlightedCategory == null -> Unit
                            subcategories.isEmpty() -> {
                                item {
                                    Text(
                                        text = "No subcategories — \"${highlightedCategory.name}\" will be used.",
                                        modifier =
                                            Modifier
                                                .fillMaxWidth()
                                                .padding(
                                                    horizontal = MmrDimens.spaceLg,
                                                    vertical = MmrDimens.spaceMd,
                                                ),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MmrBrandColors.TextMedium,
                                    )
                                }
                                callbacks.onAddSubcategory?.let { onAdd ->
                                    item {
                                        CategoryPickerInlineAdd(
                                            label = "Add subcategory",
                                            onClick = onAdd,
                                        )
                                    }
                                }
                            }
                            visibleSubs.isEmpty() && normalizedQuery.isNotEmpty() -> {
                                item {
                                    Text(
                                        text = "No subcategories match \"$normalizedQuery\".",
                                        modifier =
                                            Modifier
                                                .fillMaxWidth()
                                                .padding(MmrDimens.spaceLg),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MmrBrandColors.TextMedium,
                                    )
                                }
                            }
                            else -> {
                                items(visibleSubs, key = { it.id }) { sub ->
                                    CategoryPickerSubRow(
                                        name = sub.name,
                                        colorHex = highlightedCategory.colorHex,
                                        iconName = sub.iconName,
                                        onClick = { onSubcategoryPicked(sub.id) },
                                        onLongClick = { callbacks.onEditSubcategory?.invoke(sub.id) },
                                    )
                                }
                                callbacks.onAddSubcategory?.let { onAdd ->
                                    item {
                                        CategoryPickerInlineAdd(
                                            label = "Add subcategory",
                                            onClick = onAdd,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(MmrDimens.spaceSm))
    }
}

internal fun filterCategoriesForSearch(
    categories: List<CategoryUi>,
    query: String,
): List<CategoryUi> {
    if (query.isEmpty()) return categories
    return categories.filter { category ->
        category.name.contains(query, ignoreCase = true) ||
            category.subcategories.any { it.name.contains(query, ignoreCase = true) }
    }
}
