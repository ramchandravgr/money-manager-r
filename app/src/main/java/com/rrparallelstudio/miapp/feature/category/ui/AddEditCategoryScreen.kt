package com.rrparallelstudio.miapp.feature.category.ui

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.navigation.compose.hiltViewModel
import com.rrparallelstudio.miapp.core.model.enums.CategoryType
import com.rrparallelstudio.miapp.feature.category.model.AddEditCategoryUiEvent
import com.rrparallelstudio.miapp.feature.category.viewmodel.AddEditCategoryViewModel
import com.rrparallelstudio.miapp.testing.MmrTestTags
import com.rrparallelstudio.miapp.ui.components.CategoryDeleteDialog
import com.rrparallelstudio.miapp.ui.components.EntityPickerOption
import com.rrparallelstudio.miapp.ui.components.MmrIosSettingsChoiceRow
import com.rrparallelstudio.miapp.ui.components.MmrIosSettingsColorPickerSlot
import com.rrparallelstudio.miapp.ui.components.MmrIosSettingsDestructiveActionSection
import com.rrparallelstudio.miapp.ui.components.MmrIosSettingsEntityFormGate
import com.rrparallelstudio.miapp.ui.components.MmrIosSettingsInlineErrorSection
import com.rrparallelstudio.miapp.ui.components.MmrIosSettingsInlineTextRow
import com.rrparallelstudio.miapp.ui.components.MmrIosSettingsNavRow
import com.rrparallelstudio.miapp.ui.components.MmrIosSettingsSection
import com.rrparallelstudio.miapp.ui.components.MmrIosSettingsSingleChoiceSheet
import com.rrparallelstudio.miapp.ui.components.MmrSettingsSaveTopBar
import com.rrparallelstudio.miapp.ui.components.MmrSettingsScreenLayout
import com.rrparallelstudio.miapp.ui.components.MmrSettingsScreenScaffold

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditCategoryScreen(
    onNavigateBack: () -> Unit,
    onNavigateToAddSubcategory: (Long) -> Unit,
    onNavigateToEditSubcategory: (Long) -> Unit,
    viewModel: AddEditCategoryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDeleteCategory by remember { mutableStateOf(false) }
    var showTypeSheet by remember { mutableStateOf(false) }
    val typeSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var categoryTxnCount by remember { mutableStateOf(0) }
    var migrateOptions by remember { mutableStateOf<List<EntityPickerOption>>(emptyList()) }

    LaunchedEffect(showDeleteCategory, uiState.categoryId) {
        if (!showDeleteCategory || uiState.categoryId <= 0L) return@LaunchedEffect
        categoryTxnCount = viewModel.countTransactionsForCategory()
        migrateOptions =
            viewModel.migrationTargetOptions().map { EntityPickerOption(it.first, it.second) }
    }

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            onNavigateBack()
        }
    }

    if (showTypeSheet && !uiState.isDefaultCategory) {
        val typeOptions = listOf(CategoryType.EXPENSE, CategoryType.INCOME)
        MmrIosSettingsSingleChoiceSheet(
            title = "Category type",
            onDismiss = { showTypeSheet = false },
            sheetState = typeSheetState,
        ) {
            typeOptions.forEachIndexed { index, type ->
                MmrIosSettingsChoiceRow(
                    label = if (type == CategoryType.EXPENSE) "Expense" else "Income",
                    selected = uiState.type == type,
                    onClick = {
                        viewModel.onEvent(AddEditCategoryUiEvent.OnTypeSelected(type))
                        showTypeSheet = false
                    },
                    showDividerBelow = index < typeOptions.lastIndex,
                )
            }
        }
    }

    if (showDeleteCategory && uiState.isEditMode) {
        CategoryDeleteDialog(
            categoryName = uiState.name,
            transactionCount = categoryTxnCount,
            migrateOptions = migrateOptions,
            onConfirm = { mode, migrateToId ->
                viewModel.deleteCategory(mode, migrateToId)
                showDeleteCategory = false
            },
            onDismiss = { showDeleteCategory = false },
            working = uiState.isSaving,
        )
    }

    MmrSettingsScreenScaffold(
        title = if (uiState.isEditMode) "Edit category" else "New category",
        onNavigateBack = onNavigateBack,
        layout = MmrSettingsScreenLayout.InsetGrouped,
        topBarTrailing = {
            MmrSettingsSaveTopBar(
                onSave = { viewModel.onEvent(AddEditCategoryUiEvent.OnSaveClick) },
                enabled = uiState.canSave && !uiState.isSaving && uiState.loadErrorMessage == null,
            )
        },
    ) {
        MmrIosSettingsEntityFormGate(
            isLoading = uiState.isLoading,
            loadErrorMessage = uiState.loadErrorMessage,
        ) {
            uiState.errorMessage?.let { message ->
                MmrIosSettingsInlineErrorSection(message = message, isFirstInScreen = true)
            }

            MmrIosSettingsSection(
                title = "Details",
                isFirstInScreen = uiState.errorMessage == null,
                footer =
                    if (uiState.isDefaultCategory) {
                        "Built-in categories keep their type."
                    } else {
                        "Lower sort order values appear first in lists."
                    },
            ) {
                MmrIosSettingsInlineTextRow(
                    label = "Name",
                    value = uiState.name,
                    onValueChange = { viewModel.onEvent(AddEditCategoryUiEvent.OnNameChanged(it)) },
                    placeholder = "Required",
                    testTag = MmrTestTags.categoryFieldName,
                )
                if (uiState.isDefaultCategory) {
                    com.rrparallelstudio.miapp.ui.components.MmrIosSettingsValueRow(
                        label = "Type",
                        value = if (uiState.type == CategoryType.EXPENSE) "Expense" else "Income",
                    )
                } else {
                    MmrIosSettingsNavRow(
                        label = "Type",
                        value = if (uiState.type == CategoryType.EXPENSE) "Expense" else "Income",
                        onClick = { showTypeSheet = true },
                    )
                }
                MmrIosSettingsColorPickerSlot(showDividerBelow = true) {
                    CategoryColorPicker(
                        selectedColorHex = uiState.colorHex,
                        onColorSelected = {
                            viewModel.onEvent(AddEditCategoryUiEvent.OnColorSelected(it))
                        },
                    )
                }
                MmrIosSettingsInlineTextRow(
                    label = "Sort order",
                    value = uiState.sortOrder,
                    onValueChange = { viewModel.onEvent(AddEditCategoryUiEvent.OnSortOrderChanged(it)) },
                    placeholder = "0",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    showDividerBelow = false,
                )
            }

            if (uiState.isEditMode && uiState.categoryId > 0L) {
                SubcategoryListSection(
                    subcategories = uiState.subcategories,
                    onAddSubcategory = { onNavigateToAddSubcategory(uiState.categoryId) },
                    onEditSubcategory = onNavigateToEditSubcategory,
                )
            }

            if (uiState.isEditMode) {
                MmrIosSettingsDestructiveActionSection(
                    label = "Remove category",
                    onClick = { showDeleteCategory = true },
                )
            }
        }
    }
}
