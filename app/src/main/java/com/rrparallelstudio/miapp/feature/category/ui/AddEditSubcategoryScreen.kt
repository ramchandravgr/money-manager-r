package com.rrparallelstudio.miapp.feature.category.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.rrparallelstudio.miapp.feature.category.model.AddEditSubcategoryUiEvent
import com.rrparallelstudio.miapp.feature.category.viewmodel.AddEditSubcategoryViewModel
import com.rrparallelstudio.miapp.ui.components.MmrConfirmDialog
import com.rrparallelstudio.miapp.ui.components.MmrIosSettingsEntityFormGate
import com.rrparallelstudio.miapp.ui.components.MmrSettingsGroup
import com.rrparallelstudio.miapp.ui.components.MmrSettingsGroupDestructiveSection
import com.rrparallelstudio.miapp.ui.components.MmrSettingsGroupError
import com.rrparallelstudio.miapp.ui.components.MmrSettingsGroupInset
import com.rrparallelstudio.miapp.ui.components.MmrSettingsGroupTextField
import com.rrparallelstudio.miapp.ui.components.MmrSettingsGroupValueRow
import com.rrparallelstudio.miapp.ui.components.MmrSettingsSaveTopBar
import com.rrparallelstudio.miapp.ui.components.MmrSettingsScreenLayout
import com.rrparallelstudio.miapp.ui.components.MmrSettingsScreenScaffold

@Composable
fun AddEditSubcategoryScreen(
    onNavigateBack: () -> Unit,
    viewModel: AddEditSubcategoryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDeleteConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            onNavigateBack()
        }
    }

    if (showDeleteConfirm && uiState.isEditMode) {
        MmrConfirmDialog(
            title = "Remove subcategory?",
            message =
                "\"${uiState.name}\" will be removed from pickers. Transactions and scheduled rules that used it stay on \"${uiState.parentCategoryName}\" with no subcategory.",
            confirmLabel = "Remove",
            onConfirm = {
                viewModel.onEvent(AddEditSubcategoryUiEvent.OnDeleteClick)
                showDeleteConfirm = false
            },
            onDismiss = { showDeleteConfirm = false },
            confirmIsDestructive = true,
            confirmEnabled = !uiState.isSaving,
        )
    }

    MmrSettingsScreenScaffold(
        title = if (uiState.isEditMode) "Edit subcategory" else "New subcategory",
        onNavigateBack = onNavigateBack,
        layout = MmrSettingsScreenLayout.InsetGrouped,
        topBarTrailing = {
            MmrSettingsSaveTopBar(
                onSave = { viewModel.onEvent(AddEditSubcategoryUiEvent.OnSaveClick) },
                enabled = uiState.canSave && !uiState.isSaving && uiState.loadErrorMessage == null,
            )
        },
    ) {
        MmrIosSettingsEntityFormGate(
            isLoading = uiState.isLoading,
            loadErrorMessage = uiState.loadErrorMessage,
        ) {
            uiState.errorMessage?.let { message ->
                MmrSettingsGroupError(message = message, modifier = Modifier.padding(top = 4.dp))
            }

            MmrSettingsGroup(
                title = "Category",
                isFirstInScreen = uiState.errorMessage == null,
            ) {
                MmrSettingsGroupValueRow(
                    label = uiState.parentCategoryName,
                    value = "",
                    showDividerBelow = false,
                )
            }

            MmrSettingsGroup(
                title = "Details",
                footer = "A smaller number appears first under this category. The icon uses the category color.",
            ) {
                MmrSettingsGroupTextField(
                    label = "Name",
                    value = uiState.name,
                    onValueChange = { viewModel.onEvent(AddEditSubcategoryUiEvent.OnNameChanged(it)) },
                    placeholder = "Required",
                )
                MmrSettingsGroupInset(showDividerBelow = true) {
                    CategoryIconPicker(
                        selectedIconName = uiState.iconName,
                        accentHex = uiState.parentColorHex,
                        onIconSelected = {
                            viewModel.onEvent(AddEditSubcategoryUiEvent.OnIconSelected(it))
                        },
                    )
                }
                MmrSettingsGroupTextField(
                    label = "Sort Order",
                    value = uiState.sortOrder,
                    onValueChange = { viewModel.onEvent(AddEditSubcategoryUiEvent.OnSortOrderChanged(it)) },
                    placeholder = "0",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    showDividerBelow = false,
                )
            }

            if (uiState.isEditMode) {
                MmrSettingsGroupDestructiveSection(
                    label = "Remove Subcategory",
                    onClick = { showDeleteConfirm = true },
                )
            }
        }
    }
}
