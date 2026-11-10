package com.rrparallelstudio.miapp.feature.stats.ui

import com.rrparallelstudio.miapp.feature.stats.model.CategoryBreakdownUi

internal fun categoryBridgeKey(row: CategoryBreakdownUi): String =
    row.categoryId?.toString() ?: "n${row.categoryName.hashCode()}"
