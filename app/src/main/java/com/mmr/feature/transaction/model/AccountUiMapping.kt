package com.mmr.feature.transaction.model

import com.mmr.core.database.entity.AccountEntity
import com.mmr.core.model.enums.AccountType
import com.mmr.feature.search.model.SearchAccountUi
import java.util.Locale

fun AccountEntity.toAccountUi(): AccountUi =
    AccountUi(
        id = id,
        name = name,
        type = type,
        typeLabel = accountTypeLabel(type),
    )

fun SearchAccountUi.toAccountUi(): AccountUi =
    AccountUi(
        id = id,
        name = name,
        type = type,
        typeLabel = accountTypeLabel(type),
    )

fun accountTypeLabel(type: AccountType): String =
    type.name.lowercase(Locale.getDefault()).replaceFirstChar {
        it.titlecase(Locale.getDefault())
    }
