package com.rrparallelstudio.miapp.feature.account.ui

import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.rrparallelstudio.miapp.core.domain.model.AccountDeletionPreview
import com.rrparallelstudio.miapp.core.model.enums.AccountType
import com.rrparallelstudio.miapp.feature.account.model.AddEditAccountUiEvent
import com.rrparallelstudio.miapp.feature.account.viewmodel.AddEditAccountViewModel
import com.rrparallelstudio.miapp.testing.MmrTestTags
import com.rrparallelstudio.miapp.ui.components.AccountDeleteDialog
import com.rrparallelstudio.miapp.ui.components.EntityPickerOption
import com.rrparallelstudio.miapp.ui.components.MmrIosSettingsSingleChoiceSheet
import com.rrparallelstudio.miapp.ui.components.MmrSettingsGroup
import com.rrparallelstudio.miapp.ui.components.MmrSettingsGroupDestructiveSection
import com.rrparallelstudio.miapp.ui.components.MmrSettingsGroupError
import com.rrparallelstudio.miapp.ui.components.MmrSettingsGroupNavRow
import com.rrparallelstudio.miapp.ui.components.MmrSettingsGroupSwitchRow
import com.rrparallelstudio.miapp.ui.components.MmrSettingsGroupTextField
import com.rrparallelstudio.miapp.ui.components.MmrSettingsGroupValueRow
import com.rrparallelstudio.miapp.ui.components.MmrSettingsSaveTopBar
import com.rrparallelstudio.miapp.ui.components.MmrSettingsScreenLayout
import com.rrparallelstudio.miapp.ui.components.MmrSettingsScreenScaffold

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
                com.rrparallelstudio.miapp.ui.components.MmrIosSettingsChoiceRow(
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
        MmrSettingsGroup(
            title = "Details",
            isFirstInScreen = true,
            footer =
                "Opening balance is what was already in this account when you started tracking it here. " +
                    "Card last 4 helps match a bank message to this account.",
        ) {
            MmrSettingsGroupTextField(
                label = "Name",
                value = state.name,
                onValueChange = { viewModel.onEvent(AddEditAccountUiEvent.OnNameChanged(it)) },
                placeholder = "Required",
                testTag = MmrTestTags.accountFieldName,
            )
            val smsOrigin = state.smsOriginLabel
            if (smsOrigin != null) {
                MmrSettingsGroupValueRow(
                    label = "SMS provisioning",
                    value = smsOrigin,
                )
            }
            MmrSettingsGroupNavRow(
                label = "Type",
                value = state.type.name,
                onClick = { showTypeSheet = true },
            )
            MmrSettingsGroupTextField(
                label = "Opening balance",
                value = state.openingBalance,
                onValueChange = { viewModel.onEvent(AddEditAccountUiEvent.OnOpeningBalanceChanged(it)) },
                placeholder = "₹",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            )
            MmrSettingsGroupTextField(
                label = "Card last 4",
                value = state.cardLastFour,
                onValueChange = { viewModel.onEvent(AddEditAccountUiEvent.OnCardLastFourChanged(it)) },
                placeholder = "Optional",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                showDividerBelow = false,
            )
        }

        MmrSettingsGroup(
            title = "Options",
            footer =
                "Loan payments use the primary account when you do not pick another one. " +
                    "Turn Active off to hide an account without deleting it.",
        ) {
            MmrSettingsGroupSwitchRow(
                label = "Primary account",
                checked = state.isPrimary,
                onCheckedChange = { viewModel.onEvent(AddEditAccountUiEvent.OnPrimaryChanged(it)) },
                showDividerBelow = isEdit,
            )
            if (isEdit) {
                MmrSettingsGroupSwitchRow(
                    label = "Active",
                    checked = state.isActive,
                    onCheckedChange = { viewModel.onEvent(AddEditAccountUiEvent.OnActiveChanged(it)) },
                    showDividerBelow = false,
                )
            }
        }

        if (isEdit) {
            MmrSettingsGroupDestructiveSection(
                label = "Remove Account",
                onClick = { showDeleteDialog = true },
            )
        }

        state.errorMessage?.let { message ->
            MmrSettingsGroupError(message = message, modifier = Modifier.padding(top = 14.dp))
        }
    }
}
