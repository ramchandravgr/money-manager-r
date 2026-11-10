package com.rrparallelstudio.miapp.feature.transaction.model

import com.rrparallelstudio.miapp.core.model.enums.AccountType
import com.rrparallelstudio.miapp.core.model.enums.TransactionType

/** Pre-select account on add expense/income/transfer (not edit/duplicate/approval). */
fun resolveInitialTransactionAccountId(
    currentSelectedId: Long?,
    accounts: List<AccountUi>,
    transactionType: TransactionType,
    defaultAccountId: Long?,
): Long? {
    if (accounts.isEmpty()) return null
    currentSelectedId?.takeIf { id -> accounts.any { it.id == id } }?.let { return it }
    defaultAccountId?.takeIf { id -> accounts.any { it.id == id } }?.let { return it }
    val pool =
        when (transactionType) {
            TransactionType.EXPENSE,
            TransactionType.INCOME,
            -> accounts.filter { it.type != AccountType.LOAN }.ifEmpty { accounts }
            TransactionType.TRANSFER -> accounts
            else -> accounts
        }
    return pool.firstOrNull()?.id
}
