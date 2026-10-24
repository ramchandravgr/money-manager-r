package com.mmr.feature.account.model

import com.mmr.core.model.enums.AccountType

data class AccountItemUi(
    val id: Long,
    val name: String,
    val type: AccountType,
    val typeLabel: String,
    val balanceFormatted: String,
    val cardLastFour: String?,
    val isActive: Boolean,
    val isPrimary: Boolean = false,
)

data class AccountListUiState(
    val accounts: List<AccountItemUi> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val statusMessage: String? = null,
)

data class AddEditAccountUiState(
    val accountId: Long? = null,
    val name: String = "",
    val type: AccountType = AccountType.BANK,
    val openingBalance: String = "0",
    val cardLastFour: String = "",
    val isActive: Boolean = true,
    val isPrimary: Boolean = false,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val errorMessage: String? = null,
    val canSave: Boolean = false,
)

sealed interface AccountListUiEvent {
    object OnRetry : AccountListUiEvent

    data class OnEditAccount(
        val id: Long,
    ) : AccountListUiEvent

    data class OnSetPrimary(
        val id: Long,
    ) : AccountListUiEvent

    data class OnDeleteAccount(
        val id: Long,
        val reassignToAccountId: Long?,
    ) : AccountListUiEvent
}

sealed interface AddEditAccountUiEvent {
    data class OnNameChanged(
        val name: String,
    ) : AddEditAccountUiEvent

    data class OnTypeSelected(
        val type: AccountType,
    ) : AddEditAccountUiEvent

    data class OnOpeningBalanceChanged(
        val value: String,
    ) : AddEditAccountUiEvent

    data class OnCardLastFourChanged(
        val value: String,
    ) : AddEditAccountUiEvent

    data class OnActiveChanged(
        val active: Boolean,
    ) : AddEditAccountUiEvent

    data class OnPrimaryChanged(
        val primary: Boolean,
    ) : AddEditAccountUiEvent

    object OnSave : AddEditAccountUiEvent

    data class OnDelete(
        val reassignToAccountId: Long?,
    ) : AddEditAccountUiEvent
}
