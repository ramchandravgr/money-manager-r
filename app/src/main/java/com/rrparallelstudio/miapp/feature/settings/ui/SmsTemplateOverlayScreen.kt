package com.rrparallelstudio.miapp.feature.settings.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.rrparallelstudio.miapp.feature.settings.viewmodel.SmsTemplateOverlayUiEvent
import com.rrparallelstudio.miapp.feature.settings.viewmodel.SmsTemplateOverlayViewModel
import com.rrparallelstudio.miapp.ui.components.MmrConfirmDialog
import com.rrparallelstudio.miapp.ui.components.MmrIosSettingsDestructiveRow
import com.rrparallelstudio.miapp.ui.components.MmrIosSettingsPrimaryActionSlot
import com.rrparallelstudio.miapp.ui.components.MmrIosSettingsSection
import com.rrparallelstudio.miapp.ui.components.MmrIosSettingsSectionBodyText
import com.rrparallelstudio.miapp.ui.components.MmrSettingsScreenLayout
import com.rrparallelstudio.miapp.ui.components.MmrSettingsScreenScaffold
import com.rrparallelstudio.miapp.ui.components.MmrSettingsStatusBanners

@Composable
fun SmsTemplateOverlayScreen(
    onNavigateBack: () -> Unit,
    viewModel: SmsTemplateOverlayViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    val importLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocument(),
        ) { uri ->
            if (uri != null) {
                viewModel.onEvent(SmsTemplateOverlayUiEvent.OnImportUri(uri))
            }
        }

    if (uiState.showResetConfirm) {
        MmrConfirmDialog(
            title = "Clear SMS template overlay?",
            message = "Built-in templates from the app will be used again. This does not delete your transactions.",
            confirmLabel = "Clear overlay",
            onConfirm = { viewModel.onEvent(SmsTemplateOverlayUiEvent.OnConfirmReset) },
            onDismiss = { viewModel.onEvent(SmsTemplateOverlayUiEvent.OnDismissReset) },
        )
    }

    MmrSettingsScreenScaffold(
        title = "SMS templates",
        onNavigateBack = onNavigateBack,
        layout = MmrSettingsScreenLayout.InsetGrouped,
    ) {
        MmrSettingsStatusBanners(
            statusMessage = if (uiState.messageIsError) null else uiState.message,
            errorMessage = if (uiState.messageIsError) uiState.message else null,
            onDismissStatus = { viewModel.onEvent(SmsTemplateOverlayUiEvent.OnDismissMessage) },
            onDismissError = { viewModel.onEvent(SmsTemplateOverlayUiEvent.OnDismissMessage) },
        )
        MmrIosSettingsSection(
            title = "Template overlay",
            isFirstInScreen = true,
            footer =
                "Import a JSON file to add or replace SMS parsing rules. " +
                    "Invalid patterns are rejected. Manual paste uses the updated list immediately.",
        ) {
            MmrIosSettingsSectionBodyText(
                text =
                    when (uiState.status.overlayCount) {
                        0 -> "No overlay loaded (${uiState.status.mergedCount} templates active)."
                        else ->
                            "${uiState.status.overlayCount} overlay; " +
                                "${uiState.status.overrideBuiltInCount} override built-in; " +
                                "${uiState.status.mergedCount} total active."
                    },
            )
            MmrIosSettingsPrimaryActionSlot(
                text = if (uiState.isBusy) "Working…" else "Import overlay JSON",
                onClick = { importLauncher.launch(arrayOf("application/json", "application/*", "text/*")) },
                enabled = !uiState.isBusy,
                showDividerBelow = uiState.status.overlayCount > 0,
            )
            if (uiState.status.overlayCount > 0) {
                MmrIosSettingsDestructiveRow(
                    label = "Clear overlay",
                    onClick = { viewModel.requestReset() },
                    showDividerBelow = false,
                )
            }
        }
    }
}
