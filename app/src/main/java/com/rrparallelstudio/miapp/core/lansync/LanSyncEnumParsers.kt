package com.rrparallelstudio.miapp.core.lansync

import com.rrparallelstudio.miapp.core.model.enums.AccountType
import com.rrparallelstudio.miapp.core.model.enums.AssetStatus
import com.rrparallelstudio.miapp.core.model.enums.BudgetPeriodType
import com.rrparallelstudio.miapp.core.model.enums.CategorizationMatchType
import com.rrparallelstudio.miapp.core.model.enums.CategoryType
import com.rrparallelstudio.miapp.core.model.enums.LoanPaymentType
import com.rrparallelstudio.miapp.core.model.enums.LoanScheduleStatus
import com.rrparallelstudio.miapp.core.model.enums.LoanStatus
import com.rrparallelstudio.miapp.core.model.enums.LoanType
import com.rrparallelstudio.miapp.core.model.enums.NotificationRuleType
import com.rrparallelstudio.miapp.core.model.enums.RecurrenceInterval
import com.rrparallelstudio.miapp.core.model.enums.TransactionSourceType
import com.rrparallelstudio.miapp.core.model.enums.TransactionType

internal object LanSyncEnumParsers {
    fun categoryType(value: String): CategoryType = enumOrDefault(value, CategoryType.EXPENSE)

    fun accountType(value: String): AccountType = enumOrDefault(value, AccountType.OTHER)

    fun loanType(value: String): LoanType = enumOrDefault(value, LoanType.BORROWED)

    fun loanStatus(value: String): LoanStatus = enumOrDefault(value, LoanStatus.ACTIVE)

    fun loanPaymentType(value: String): LoanPaymentType = enumOrDefault(value, LoanPaymentType.MANUAL)

    fun loanScheduleStatus(value: String): LoanScheduleStatus = enumOrDefault(value, LoanScheduleStatus.PLANNED)

    fun transactionType(value: String): TransactionType = enumOrDefault(value, TransactionType.EXPENSE)

    fun transactionSourceType(value: String): TransactionSourceType = enumOrDefault(value, TransactionSourceType.MANUAL)

    fun recurrenceInterval(value: String): RecurrenceInterval = enumOrDefault(value, RecurrenceInterval.MONTHLY)

    fun budgetPeriodType(value: String): BudgetPeriodType = enumOrDefault(value, BudgetPeriodType.MONTHLY)

    fun categorizationMatchType(value: String): CategorizationMatchType =
        enumOrDefault(value, CategorizationMatchType.MERCHANT_OR_NOTE_CONTAINS)

    fun assetStatus(value: String): AssetStatus = enumOrDefault(value, AssetStatus.ACTIVE)

    fun notificationRuleType(value: String): NotificationRuleType? =
        runCatching { NotificationRuleType.valueOf(value) }.getOrNull()

    private inline fun <reified E : Enum<E>> enumOrDefault(
        value: String,
        default: E,
    ): E = runCatching { enumValueOf<E>(value) }.getOrDefault(default)
}
