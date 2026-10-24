package com.mmr.core.domain.model

data class AccountDeletionPreview(
    val linkedTransactionCount: Int,
    val linkedRecurringRuleCount: Int,
    val isBlocked: Boolean,
    val blockReason: String? = null,
)

enum class CategoryDeleteTransactionMode {
    /** Category has no tagged transactions (or only subcategory tags handled with category). */
    NONE,

    /** Move tagged transactions and recurring rules to another category. */
    MIGRATE,

    /** Clear category/subcategory on tagged rows. */
    UNCATEGORIZE,
}
