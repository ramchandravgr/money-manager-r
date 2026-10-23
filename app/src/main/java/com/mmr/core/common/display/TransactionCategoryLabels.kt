package com.mmr.core.common.display

object TransactionCategoryLabels {
    fun format(
        categoryName: String?,
        subcategoryName: String?,
    ): String {
        val parent = categoryName?.trim()?.takeIf { it.isNotEmpty() } ?: "Uncategorized"
        val sub = subcategoryName?.trim()?.takeIf { it.isNotEmpty() }
        return if (sub != null) "$parent · $sub" else parent
    }
}
