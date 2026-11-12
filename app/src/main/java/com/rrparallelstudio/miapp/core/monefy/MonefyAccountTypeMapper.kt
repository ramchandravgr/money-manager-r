package com.rrparallelstudio.miapp.core.monefy

import com.rrparallelstudio.miapp.core.model.enums.AccountType

internal object MonefyAccountTypeMapper {
    fun fromTitle(title: String): AccountType {
        val name = title.lowercase()
        return when {
            "cash" in name -> AccountType.CASH
            "wallet" in name -> AccountType.WALLET
            "credit" in name || "card" in name -> AccountType.CREDIT
            "debit" in name || "bank" in name || "savings" in name -> AccountType.BANK
            else -> AccountType.OTHER
        }
    }
}
