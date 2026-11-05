package com.mmr.core.data.catalog

import com.mmr.core.database.entity.AccountEntity
import com.mmr.core.model.enums.AccountType

object MmrBuiltInAccounts {
    const val DEFAULT_ACCOUNT_NAME = "Savings"

    fun defaultAccount(): AccountEntity =
        AccountEntity(
            name = DEFAULT_ACCOUNT_NAME,
            type = AccountType.DEBIT,
            isDefault = true,
        )
}
