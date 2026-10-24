package com.mmr.feature.account.ui

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.navigation.compose.hiltViewModel
import com.mmr.app.testing.MmrTestTags
import com.mmr.app.ui.components.AccountDeleteDialog
import com.mmr.app.ui.components.EntityPickerOption
import com.mmr.app.ui.components.MmrIosSettingsDestructiveActionSection
import com.mmr.app.ui.components.MmrIosSettingsInlineErrorSection
import com.mmr.app.ui.components.MmrIosSettingsInlineTextRow
import com.mmr.app.ui.components.MmrIosSettingsNavRow
import com.mmr.app.ui.components.MmrIosSettingsSection
import com.mmr.app.ui.components.MmrIosSettingsSingleChoiceSheet
import com.mmr.app.ui.components.MmrIosSettingsSwitchRow
import com.mmr.app.ui.components.MmrSettingsSaveTopBar
import com.mmr.app.ui.components.MmrSettingsScreenLayout
import com.mmr.app.ui.components.MmrSettingsScreenScaffold
import com.mmr.core.domain.model.AccountDeletionPreview
import com.mmr.core.model.enums.AccountType
import com.mmr.feature.account.model.AddEditAccountUiEvent
import com.mmr.feature.account.viewmodel.AddEditAccountViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditAccountScreen(
    onNavigateBack: () -> Unit,
    viewModel: AddEditAccountViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val accountId = state.accountId
    val isEdit = accountId != null && accountId > 0L
    var showTypeSheet by remember { mutableStateOf(false) }
    val typeSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var showDeleteDialog by remember { mutableStateOf(false) }
    var deletePreview by remember { mutableStateOf<AccountDeletionPreview?>(null) }
    var replacementOptions by remember { mutableStateOf<List<EntityPickerOption>>(emptyList()) }
    var selectedReplacementId by remember { mutableLongStateOf(0L) }

    LaunchedEffect(showDeleteDialog, accountId) {
        if (!showDeleteDialog || accountId == null) return@LaunchedEffect
        deletePreview = viewModel.previewDeletion()
        replacementOptions =
            viewModel.replacementAccountOptions().map { EntityPickerOption(it.first, it.second) }
        selectedReplacementId = replacementOptions.firstOrNull()?.id ?: 0L
    }

    LaunchedEffect(state.isSaved) {
        if (state.isSaved) onNavigateBack()
    }

    if (showTypeSheet) {
        MmrIosSettingsSingleChoiceSheet(
            title = "Account type",
            onDismiss = { showTypeSheet = false },
            sheetState = typeSheetState,
        ) {
            AccountType.entries.forEachIndexed { index, type ->
                com.mmr.app.ui.components.MmrIosSettingsChoiceRow(
                    label = type.name,
                    selected = state.type == type,
                    onClick = {
                        viewModel.onEvent(AddEditAccountUiEvent.OnTypeSelected(type))
                        showTypeSheet = false
                    },
                    showDividerBelow = index < AccountType.entries.lastIndex,
                )
            }
        }
    }

    if (showDeleteDialog && deletePreview != null && accountId != null) {
        AccountDeleteDialog(
            accountName = state.name,
            preview = deletePreview!!,
            replacementOptions = replacementOptions,
            selectedReplacementId = selectedReplacementId.takeIf { it > 0L },
            onSelectReplacement = { selectedReplacementId = it },
            onConfirm = {
                viewModel.onEvent(AddEditAccountUiEvent.OnDelete(selectedReplacementId.takeIf { it > 0L }))
                showDeleteDialog = false
            },
            onDismiss = { showDeleteDialog = false },
            working = state.isSaving,
        )
    }

    MmrSettingsScreenScaffold(
        title = if (isEdit) "Edit account" else "New account",
        onNavigateBack = onNavigateBack,
        layout = MmrSettingsScreenLayout.InsetGrouped,
        topBarTrailing = {
            MmrSettingsSaveTopBar(
                onSave = { viewModel.onEvent(AddEditAccountUiEvent.OnSave) },
                enabled = state.canSave && !state.isSaving,
            )
        },
    ) {
        MmrIosSettingsSection(
            title = "Details",
            isFirstInScreen = true,
            footer = "Opening balance is used when you first add the account.",
        ) {
            MmrIosSettingsInlineTextRow(
                label = "Name",
                value = state.name,
                onValueChange = { viewModel.onEvent(AddEditAccountUiEvent.OnNameChanged(it)) },
                placeholder = "Required",
                testTag = MmrTestTags.accountFieldName,
            )
            MmrIosSettingsNavRow(
                label = "Type",
                value = state.type.name,
                onClick = { showTypeSheet = true },
            )
            MmrIosSettingsInlineTextRow(
                label = "Opening balance",
                value = state.openingBalance,
                onValueChange = { viewModel.onEvent(AddEditAccountUiEvent.OnOpeningBalanceChanged(it)) },
                placeholder = "₹",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            )
            MmrIosSettingsInlineTextRow(
                label = "Card last 4",
                value = state.cardLastFour,
                onValueChange = { viewModel.onEvent(AddEditAccountUiEvent.OnCardLastFourChanged(it)) },
                placeholder = "Optional",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                showDividerBelow = false,
            )
        }

        MmrIosSettingsSection(
            title = "Options",
            footer = "Primary account is used for loan repayments from your main cash or bank account.",
        ) {
            MmrIosSettingsSwitchRow(
                label = "Primary account",
                checked = state.isPrimary,
                onCheckedChange = { viewModel.onEvent(AddEditAccountUiEvent.OnPrimaryChanged(it)) },
                showDividerBelow = isEdit,
            )
            if (isEdit) {
                MmrIosSettingsSwitchRow(
                    label = "Active",
                    checked = state.isActive,
                    onCheckedChange = { viewModel.onEvent(AddEditAccountUiEvent.OnActiveChanged(it)) },
                    showDividerBelow = false,
                )
            }
        }

        if (isEdit) {
            MmrIosSettingsDestructiveActionSection(
                label = "Remove account",
                onClick = { showDeleteDialog = true },
            )
        }

        state.errorMessage?.let { message ->
            MmrIosSettingsInlineErrorSection(message = message)
        }
    }
}
