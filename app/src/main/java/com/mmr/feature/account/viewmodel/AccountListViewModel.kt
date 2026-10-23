package com.mmr.feature.account.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mmr.core.domain.repository.AccountRepository
import com.mmr.feature.account.model.AccountItemUi
import com.mmr.feature.account.model.AccountListUiEvent
import com.mmr.feature.account.model.AccountListUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class AccountListViewModel
    @Inject
    constructor(
        private val accountRepository: AccountRepository,
    ) : ViewModel() {
        private val statusMessage = MutableStateFlow<String?>(null)

        val uiState: StateFlow<AccountListUiState> =
            combine(
                accountRepository.getAccounts().map { accounts ->
                    accounts.map { entity ->
                        AccountItemUi(
                            id = entity.id,
                            name = entity.name,
                            type = entity.type,
                            typeLabel =
                                entity.type.name
                                    .lowercase(Locale.getDefault())
                                    .replaceFirstChar { it.titlecase(Locale.getDefault()) },
                            balanceFormatted = "₹${"%.2f".format(entity.currentBalance)}",
                            cardLastFour = entity.cardLastFour,
                            isActive = entity.isActive,
                            isPrimary = entity.isDefault,
                        )
                    }
                },
                statusMessage,
            ) { accounts, message ->
                AccountListUiState(
                    isLoading = false,
                    accounts = accounts,
                    statusMessage = message,
                )
            }.catch {
                emit(AccountListUiState(isLoading = false, errorMessage = it.localizedMessage))
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AccountListUiState(isLoading = true))

        fun onEvent(event: AccountListUiEvent) {
            when (event) {
                AccountListUiEvent.OnRetry -> statusMessage.value = null
                is AccountListUiEvent.OnSetPrimary -> setPrimary(event.id)
                is AccountListUiEvent.OnEditAccount -> Unit
            }
        }

        private fun setPrimary(accountId: Long) {
            viewModelScope.launch {
                try {
                    accountRepository.setPrimaryAccount(accountId)
                    statusMessage.value = "Primary account updated."
                } catch (e: Exception) {
                    statusMessage.value = e.message ?: "Could not set primary account."
                }
            }
        }
    }
