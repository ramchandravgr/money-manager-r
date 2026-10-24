package com.mmr.feature.category.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mmr.app.testing.MmrTestTags
import com.mmr.app.ui.components.MmrIosSettingsFilterChipRow
import com.mmr.app.ui.components.MmrIosSettingsListEmptySection
import com.mmr.app.ui.components.MmrIosSettingsNavRow
import com.mmr.app.ui.components.MmrIosSettingsSection
import com.mmr.app.ui.components.MmrSettingsListAddTopBar
import com.mmr.app.ui.components.MmrSettingsListScaffold
import com.mmr.app.ui.components.MmrSettingsStatusBanners
import com.mmr.app.ui.state.MmrErrorState
import com.mmr.app.ui.state.MmrLoadingState
import com.mmr.core.model.enums.CategoryType
import com.mmr.feature.category.model.CategoryUiEvent
import com.mmr.feature.category.viewmodel.CategoryViewModel

@Composable
fun CategoryListScreen(
    onNavigateBack: () -> Unit,
    onNavigateToAddCategory: () -> Unit,
    onNavigateToEditCategory: (Long) -> Unit,
    @Suppress("UNUSED_PARAMETER") onNavigateToAddSubcategory: (Long) -> Unit,
    @Suppress("UNUSED_PARAMETER") onNavigateToEditSubcategory: (Long) -> Unit,
    viewModel: CategoryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    MmrSettingsListScaffold(
        title = "Categories",
        onNavigateBack = onNavigateBack,
        insetGrouped = true,
        topBarTrailing = {
            MmrSettingsListAddTopBar(
                onAdd = onNavigateToAddCategory,
                testTag = MmrTestTags.settingsListAddCategories,
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = padding,
        ) {
            item {
                Column {
                    MmrSettingsStatusBanners(
                        statusMessage = uiState.statusMessage,
                        errorMessage = uiState.errorMessage,
                        onDismissStatus = { viewModel.onEvent(CategoryUiEvent.OnDismissMessage) },
                        onDismissError = { viewModel.onEvent(CategoryUiEvent.OnDismissMessage) },
                    )
                }
            }
            item {
                val selectedChipIndex =
                    when (uiState.typeFilter) {
                        null -> 0
                        CategoryType.EXPENSE -> 1
                        CategoryType.INCOME -> 2
                        else -> 0
                    }
                MmrIosSettingsFilterChipRow(
                    labels = listOf("All", "Expense", "Income"),
                    selectedIndex = selectedChipIndex,
                    onSelectedIndex = { index ->
                        val filter =
                            when (index) {
                                1 -> CategoryType.EXPENSE
                                2 -> CategoryType.INCOME
                                else -> null
                            }
                        viewModel.onEvent(CategoryUiEvent.OnTypeFilterSelected(filter))
                    },
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }

            when {
                uiState.isLoading && uiState.categories.isEmpty() -> {
                    item { MmrLoadingState() }
                }
                uiState.errorMessage != null && uiState.categories.isEmpty() -> {
                    item {
                        MmrErrorState(
                            message = uiState.errorMessage!!,
                            onRetry = { viewModel.onEvent(CategoryUiEvent.OnRetryLoad) },
                        )
                    }
                }
                uiState.isEmpty -> {
                    item {
                        MmrIosSettingsListEmptySection(
                            message = "No categories yet. Tap + in the toolbar to add one.",
                        )
                    }
                }
                else -> {
                    val sections =
                        when (uiState.typeFilter) {
                            null ->
                                listOf(
                                    CategoryType.EXPENSE to
                                        uiState.categories.filter { it.type == CategoryType.EXPENSE },
                                    CategoryType.INCOME to
                                        uiState.categories.filter { it.type == CategoryType.INCOME },
                                )
                            else -> listOf(uiState.typeFilter!! to uiState.categories)
                        }
                    sections.forEachIndexed { sectionIndex, (type, categories) ->
                        if (categories.isEmpty()) return@forEachIndexed
                        item(key = type.name) {
                            val sectionTitle =
                                when (type) {
                                    CategoryType.EXPENSE -> "Expense"
                                    CategoryType.INCOME -> "Income"
                                    CategoryType.TRANSFER,
                                    CategoryType.ALL,
                                    -> "Categories"
                                }
                            MmrIosSettingsSection(
                                title = sectionTitle,
                                isFirstInScreen = sectionIndex == 0,
                                footer = "Tap a category to edit or manage subcategories.",
                            ) {
                                categories.forEachIndexed { index, category ->
                                    val subLabel =
                                        when (category.subcategories.size) {
                                            0 ->
                                                if (category.isDefault) {
                                                    "Default"
                                                } else {
                                                    "No subcategories"
                                                }
                                            1 -> "1 subcategory"
                                            else -> "${category.subcategories.size} subcategories"
                                        }
                                    MmrIosSettingsNavRow(
                                        label = category.name,
                                        value = subLabel,
                                        onClick = { onNavigateToEditCategory(category.id) },
                                        showDividerBelow = index < categories.lastIndex,
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
