package com.rrparallelstudio.miapp.feature.category.ui

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.navigation.compose.hiltViewModel
import com.rrparallelstudio.miapp.feature.category.model.AddEditSubcategoryUiEvent
import com.rrparallelstudio.miapp.feature.category.viewmodel.AddEditSubcategoryViewModel
import com.rrparallelstudio.miapp.ui.components.MmrConfirmDialog
import com.rrparallelstudio.miapp.ui.components.MmrIosSettingsDestructiveActionSection
import com.rrparallelstudio.miapp.ui.components.MmrIosSettingsEntityFormGate
import com.rrparallelstudio.miapp.ui.components.MmrIosSettingsInlineErrorSection
import com.rrparallelstudio.miapp.ui.components.MmrIosSettingsInlineTextRow
import com.rrparallelstudio.miapp.ui.components.MmrIosSettingsReadOnlyValueRow
import com.rrparallelstudio.miapp.ui.components.MmrIosSettingsSection
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
            message = "\"${uiState.name}\" under \"${uiState.parentCategoryName}\" will be removed from pickers.",
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
                MmrIosSettingsInlineErrorSection(message = message, isFirstInScreen = true)
            }

            MmrIosSettingsSection(
                title = "Parent",
                isFirstInScreen = uiState.errorMessage == null,
            ) {
                MmrIosSettingsReadOnlyValueRow(
                    label = uiState.parentCategoryName,
                    showDividerBelow = false,
                )
            }

            MmrIosSettingsSection(
                title = "Details",
                footer = "Order among siblings under the same parent.",
            ) {
                MmrIosSettingsInlineTextRow(
                    label = "Name",
                    value = uiState.name,
                    onValueChange = { viewModel.onEvent(AddEditSubcategoryUiEvent.OnNameChanged(it)) },
                    placeholder = "Required",
                )
                MmrIosSettingsInlineTextRow(
                    label = "Sort order",
                    value = uiState.sortOrder,
                    onValueChange = { viewModel.onEvent(AddEditSubcategoryUiEvent.OnSortOrderChanged(it)) },
                    placeholder = "0",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    showDividerBelow = false,
                )
            }

            if (uiState.isEditMode) {
                MmrIosSettingsDestructiveActionSection(
                    label = "Remove subcategory",
                    onClick = { showDeleteConfirm = true },
                )
            }
        }
    }
}
