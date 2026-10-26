package com.mmr.feature.stats.ui

import com.mmr.feature.stats.model.CategoryBreakdownUi

internal fun categoryBridgeKey(row: CategoryBreakdownUi): String =
    row.categoryId?.toString() ?: "n${row.categoryName.hashCode()}"
