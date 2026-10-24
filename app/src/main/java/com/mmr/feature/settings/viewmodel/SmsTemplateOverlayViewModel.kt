package com.mmr.feature.settings.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mmr.core.common.dispatcher.IoDispatcher
import com.mmr.core.parser.template.SmsTemplateOverlayImportResult
import com.mmr.core.parser.template.SmsTemplateOverlayRepository
import com.mmr.core.parser.template.SmsTemplateOverlayStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class SmsTemplateOverlayUiState(
    val status: SmsTemplateOverlayStatus = SmsTemplateOverlayStatus(),
    val isBusy: Boolean = false,
    val message: String? = null,
    val messageIsError: Boolean = true,
    val showResetConfirm: Boolean = false,
)

sealed class SmsTemplateOverlayUiEvent {
    data class OnImportUri(
        val uri: Uri,
    ) : SmsTemplateOverlayUiEvent()

    data object OnConfirmReset : SmsTemplateOverlayUiEvent()

    data object OnDismissReset : SmsTemplateOverlayUiEvent()

    data object OnDismissMessage : SmsTemplateOverlayUiEvent()
}

@HiltViewModel
class SmsTemplateOverlayViewModel
    @Inject
    constructor(
        private val overlayRepository: SmsTemplateOverlayRepository,
        @ApplicationContext private val context: Context,
        @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(SmsTemplateOverlayUiState())
        val uiState: StateFlow<SmsTemplateOverlayUiState> = _uiState.asStateFlow()

        init {
            viewModelScope.launch {
                overlayRepository.status.collect { status ->
                    _uiState.update { it.copy(status = status) }
                }
            }
        }

        fun onEvent(event: SmsTemplateOverlayUiEvent) {
            when (event) {
                is SmsTemplateOverlayUiEvent.OnImportUri -> importUri(event.uri)
                SmsTemplateOverlayUiEvent.OnConfirmReset -> resetOverlay()
                SmsTemplateOverlayUiEvent.OnDismissReset ->
                    _uiState.update { it.copy(showResetConfirm = false) }
                SmsTemplateOverlayUiEvent.OnDismissMessage ->
                    _uiState.update { it.copy(message = null, messageIsError = true) }
            }
        }

        fun requestReset() {
            _uiState.update { it.copy(showResetConfirm = true) }
        }

        private fun importUri(uri: Uri) {
            viewModelScope.launch {
                _uiState.update { it.copy(isBusy = true, message = null) }
                val json =
                    withContext(ioDispatcher) {
                        runCatching {
                            context.contentResolver.openInputStream(uri)?.use { stream ->
                                stream.bufferedReader().readText()
                            }
                        }.getOrNull()
                    }
                if (json.isNullOrBlank()) {
                    _uiState.update {
                        it.copy(isBusy = false, message = "Could not read the selected file.")
                    }
                    return@launch
                }
                when (val result = overlayRepository.importOverlayJson(json)) {
                    is SmsTemplateOverlayImportResult.Success -> {
                        val overrides =
                            if (result.overrideBuiltInCount > 0) {
                                " (${result.overrideBuiltInCount} override built-in)"
                            } else {
                                ""
                            }
                        _uiState.update {
                            it.copy(
                                isBusy = false,
                                message = "Imported ${result.overlayCount} template(s)$overrides.",
                                messageIsError = false,
                            )
                        }
                    }
                    is SmsTemplateOverlayImportResult.ValidationFailed -> {
                        val detail =
                            result.errors
                                .take(3)
                                .joinToString("\n") { err ->
                                    val id = err.templateId ?: "?"
                                    "• $id: ${err.message}"
                                }
                        val more =
                            if (result.errors.size > 3) "\n…and ${result.errors.size - 3} more." else ""
                        _uiState.update {
                            it.copy(
                                isBusy = false,
                                message = "Invalid overlay file.\n$detail$more",
                            )
                        }
                    }
                }
            }
        }

        private fun resetOverlay() {
            viewModelScope.launch {
                _uiState.update { it.copy(isBusy = true, showResetConfirm = false) }
                overlayRepository.clearOverlay()
                _uiState.update {
                    it.copy(
                        isBusy = false,
                        message = "Overlay cleared. Using built-in templates only.",
                        messageIsError = false,
                    )
                }
            }
        }
    }
