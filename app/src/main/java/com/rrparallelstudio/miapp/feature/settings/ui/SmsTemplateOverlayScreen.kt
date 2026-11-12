package com.rrparallelstudio.miapp.feature.settings.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import com.rrparallelstudio.miapp.feature.settings.model.SmsTemplateOverlayPrompt
import com.rrparallelstudio.miapp.feature.settings.viewmodel.SmsTemplateOverlayUiEvent
import com.rrparallelstudio.miapp.feature.settings.viewmodel.SmsTemplateOverlayViewModel
import com.rrparallelstudio.miapp.ui.components.MmrConfirmDialog
import com.rrparallelstudio.miapp.ui.components.MmrSettingsGroup
import com.rrparallelstudio.miapp.ui.components.MmrSettingsGroupAction
import com.rrparallelstudio.miapp.ui.components.MmrSettingsGroupDestructiveRow
import com.rrparallelstudio.miapp.ui.components.MmrSettingsGroupNote
import com.rrparallelstudio.miapp.ui.components.MmrSettingsScreenLayout
import com.rrparallelstudio.miapp.ui.components.MmrSettingsScreenScaffold
import com.rrparallelstudio.miapp.ui.components.MmrSettingsStatusBanners

@Composable
fun SmsTemplateOverlayScreen(
    onNavigateBack: () -> Unit,
    viewModel: SmsTemplateOverlayViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

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
        title = "SMS Templates",
        onNavigateBack = onNavigateBack,
        layout = MmrSettingsScreenLayout.InsetGrouped,
    ) {
        MmrSettingsStatusBanners(
            statusMessage = if (uiState.messageIsError) null else uiState.message,
            errorMessage = if (uiState.messageIsError) uiState.message else null,
            onDismissStatus = { viewModel.onEvent(SmsTemplateOverlayUiEvent.OnDismissMessage) },
            onDismissError = { viewModel.onEvent(SmsTemplateOverlayUiEvent.OnDismissMessage) },
        )
        MmrSettingsGroup(
            title = "Build Your Own",
            isFirstInScreen = true,
            footer =
                "Paste the prompt and your SMS export into any AI you choose. " +
                    "Save the JSON after ---JSON--- as a file, then import it below. " +
                    "Your messages go only to that AI. This app reads the file you pick.",
        ) {
            MmrSettingsGroupAction(
                text = "Copy Prompt",
                onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(
                        ClipData.newPlainText(SmsTemplateOverlayPrompt.CLIPBOARD_LABEL, SmsTemplateOverlayPrompt.text),
                    )
                    viewModel.onEvent(SmsTemplateOverlayUiEvent.OnPromptCopied)
                },
                enabled = !uiState.isBusy,
                showDividerBelow = false,
            )
        }
        MmrSettingsGroup(
            title = "Your Rules",
            footer = "An extra file can recognize more bank messages. Removing it does not delete transactions.",
        ) {
            MmrSettingsGroupNote(
                text =
                    when (uiState.status.overlayCount) {
                        0 -> "Using the built-in bank messages. No extra rules file is loaded."
                        else ->
                            "${uiState.status.overlayCount} extra rules are loaded " +
                                "(${uiState.status.mergedCount} rules in use)."
                    },
            )
            MmrSettingsGroupAction(
                text = if (uiState.isBusy) "Working…" else "Import JSON",
                onClick = { importLauncher.launch(arrayOf("application/json", "application/*", "text/*")) },
                enabled = !uiState.isBusy,
                showDividerBelow = uiState.status.overlayCount > 0,
            )
            if (uiState.status.overlayCount > 0) {
                MmrSettingsGroupDestructiveRow(
                    label = "Remove Extra Rules",
                    onClick = { viewModel.requestReset() },
                    showDividerBelow = false,
                )
            }
        }
    }
}
