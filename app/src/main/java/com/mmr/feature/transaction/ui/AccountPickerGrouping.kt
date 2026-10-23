package com.mmr.feature.transaction.ui

import com.mmr.core.model.enums.AccountType
import com.mmr.feature.transaction.model.AccountUi

internal object AccountPickerGrouping {
    private val typeOrder =
        listOf(
            AccountType.CASH,
            AccountType.BANK,
            AccountType.DEBIT,
            AccountType.CREDIT,
            AccountType.WALLET,
            AccountType.OTHER,
        )

    fun sections(
        accounts: List<AccountUi>,
        excludedAccountId: Long? = null,
        includeLoanAccounts: Boolean = false,
    ): List<AccountPickerSection> {
        val filtered =
            accounts.filter { account ->
                account.id != excludedAccountId &&
                    (includeLoanAccounts || account.type != AccountType.LOAN)
            }
        return typeOrder.mapNotNull { type ->
            val items = filtered.filter { it.type == type }
            if (items.isEmpty()) return@mapNotNull null
            AccountPickerSection(
                title = items.first().typeLabel,
                accounts = items.sortedBy { it.name.lowercase() },
            )
        }
    }
}

internal data class AccountPickerSection(
    val title: String,
    val accounts: List<AccountUi>,
)
