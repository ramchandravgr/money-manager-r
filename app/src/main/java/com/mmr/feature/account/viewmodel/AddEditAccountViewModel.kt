package com.mmr.feature.account.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mmr.app.ui.state.MmrUiMessages
import com.mmr.core.common.validation.MmrValidation
import com.mmr.core.domain.repository.AccountRepository
import com.mmr.feature.account.model.AddEditAccountUiEvent
import com.mmr.feature.account.model.AddEditAccountUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddEditAccountViewModel
    @Inject
    constructor(
        private val accountRepository: AccountRepository,
        savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        private val accountId: Long = savedStateHandle.get<Long>("accountId") ?: 0L

        private val _uiState = MutableStateFlow(AddEditAccountUiState(accountId = accountId.takeIf { it > 0L }))
        val uiState: StateFlow<AddEditAccountUiState> = _uiState.asStateFlow()

        init {
            if (accountId > 0L) {
                viewModelScope.launch {
                    val account = accountRepository.getAccountById(accountId)
                    if (account != null) {
                        _uiState.update {
                            it.copy(
                                name = account.name,
                                type = account.type,
                                openingBalance = account.openingBalance.toString(),
                                cardLastFour = account.cardLastFour ?: "",
                                isActive = account.isActive,
                                isPrimary = account.isDefault,
                                canSave = true,
                            )
                        }
                    }
                }
            }
        }

        fun onEvent(event: AddEditAccountUiEvent) {
            when (event) {
                is AddEditAccountUiEvent.OnNameChanged ->
                    _uiState.update {
                        it.copy(name = event.name, canSave = validate(it.copy(name = event.name)))
                    }
                is AddEditAccountUiEvent.OnTypeSelected -> _uiState.update { it.copy(type = event.type) }
                is AddEditAccountUiEvent.OnOpeningBalanceChanged -> {
                    val clean = event.value.filter { it.isDigit() || it == '.' }
                    _uiState.update { it.copy(openingBalance = clean) }
                }
                is AddEditAccountUiEvent.OnCardLastFourChanged -> {
                    val digits = event.value.filter(Char::isDigit).take(4)
                    _uiState.update { it.copy(cardLastFour = digits) }
                }
                is AddEditAccountUiEvent.OnActiveChanged -> _uiState.update { it.copy(isActive = event.active) }
                is AddEditAccountUiEvent.OnPrimaryChanged -> _uiState.update { it.copy(isPrimary = event.primary) }
                AddEditAccountUiEvent.OnSave -> save()
            }
        }

        private fun validate(state: AddEditAccountUiState): Boolean = state.name.trim().isNotEmpty()

        private fun save() {
            val state = _uiState.value
            if (!validate(state)) return
            val balance = MmrValidation.sanitizeDecimalInput(state.openingBalance).toDoubleOrNull() ?: 0.0
            viewModelScope.launch {
                _uiState.update { it.copy(isSaving = true, errorMessage = null) }
                try {
                    val last4 = state.cardLastFour.takeIf { it.length == 4 }
                    if (state.accountId != null && state.accountId > 0L) {
                        accountRepository.updateAccount(
                            id = state.accountId,
                            name = state.name,
                            type = state.type,
                            openingBalance = balance,
                            cardLastFour = last4,
                            isActive = state.isActive,
                        )
                        if (state.isPrimary) {
                            accountRepository.setPrimaryAccount(state.accountId)
                        }
                    } else {
                        val newId =
                            accountRepository.createAccount(
                                name = state.name,
                                type = state.type,
                                openingBalance = balance,
                                cardLastFour = last4,
                                isDefault = state.isPrimary,
                            )
                        if (state.isPrimary) {
                            accountRepository.setPrimaryAccount(newId)
                        }
                    }
                    _uiState.update { it.copy(isSaving = false, isSaved = true) }
                } catch (e: Exception) {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            errorMessage = MmrUiMessages.friendly(e.message, MmrUiMessages.GENERIC_SAVE),
                        )
                    }
                }
            }
        }
    }
