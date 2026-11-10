package com.rrparallelstudio.miapp.core.data.catalog

import com.rrparallelstudio.miapp.core.database.entity.AccountEntity
import com.rrparallelstudio.miapp.core.model.enums.AccountType

object MmrBuiltInAccounts {
    const val DEFAULT_ACCOUNT_NAME = "Savings"

    fun defaultAccount(): AccountEntity =
        AccountEntity(
            name = DEFAULT_ACCOUNT_NAME,
            type = AccountType.DEBIT,
            isDefault = true,
        )
}
