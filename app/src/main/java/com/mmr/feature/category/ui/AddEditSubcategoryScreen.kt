package com.mmr.feature.category.ui

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
import com.mmr.app.ui.components.MmrConfirmDialog
import com.mmr.app.ui.components.MmrIosSettingsDestructiveActionSection
import com.mmr.app.ui.components.MmrIosSettingsEntityFormGate
import com.mmr.app.ui.components.MmrIosSettingsInlineErrorSection
import com.mmr.app.ui.components.MmrIosSettingsInlineTextRow
import com.mmr.app.ui.components.MmrIosSettingsReadOnlyValueRow
import com.mmr.app.ui.components.MmrIosSettingsSection
import com.mmr.app.ui.components.MmrSettingsSaveTopBar
import com.mmr.app.ui.components.MmrSettingsScreenLayout
import com.mmr.app.ui.components.MmrSettingsScreenScaffold
import com.mmr.feature.category.model.AddEditSubcategoryUiEvent
import com.mmr.feature.category.viewmodel.AddEditSubcategoryViewModel

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
