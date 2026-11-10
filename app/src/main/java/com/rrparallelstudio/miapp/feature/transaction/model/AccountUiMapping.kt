package com.rrparallelstudio.miapp.feature.transaction.model

import com.rrparallelstudio.miapp.core.database.entity.AccountEntity
import com.rrparallelstudio.miapp.core.model.enums.AccountType
import com.rrparallelstudio.miapp.feature.search.model.SearchAccountUi
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
