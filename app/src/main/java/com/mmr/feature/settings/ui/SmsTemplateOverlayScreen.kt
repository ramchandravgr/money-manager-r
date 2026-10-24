package com.mmr.feature.settings.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.mmr.app.ui.components.MmrConfirmDialog
import com.mmr.app.ui.components.MmrIosSettingsDestructiveRow
import com.mmr.app.ui.components.MmrIosSettingsPrimaryActionSlot
import com.mmr.app.ui.components.MmrIosSettingsSection
import com.mmr.app.ui.components.MmrIosSettingsSectionBodyText
import com.mmr.app.ui.components.MmrSettingsScreenLayout
import com.mmr.app.ui.components.MmrSettingsScreenScaffold
import com.mmr.app.ui.components.MmrSettingsStatusBanners
import com.mmr.feature.settings.viewmodel.SmsTemplateOverlayUiEvent
import com.mmr.feature.settings.viewmodel.SmsTemplateOverlayViewModel

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
                "JSON overlay adds or overrides SMS parsing rules (same shape as sms_templates.json). " +
                    "Invalid regex is rejected. Inbox scan uses the merged list immediately.",
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
