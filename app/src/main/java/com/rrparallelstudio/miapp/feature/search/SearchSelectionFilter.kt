package com.rrparallelstudio.miapp.feature.search

import com.rrparallelstudio.miapp.feature.search.model.SearchCategoryPick

object SearchSelectionFilter {
    fun matchesCategory(
        categoryId: Long?,
        subcategoryId: Long?,
        picks: Set<SearchCategoryPick>,
    ): Boolean {
        if (picks.isEmpty()) return true
        return picks.any { pick ->
            pick.categoryId == categoryId &&
                (pick.subcategoryId == null || pick.subcategoryId == subcategoryId)
        }
    }

    fun matchingAccountIds(
        rows: List<Triple<Long?, Long?, Long>>,
        picks: Set<SearchCategoryPick>,
    ): Set<Long> =
        rows
            .filter { (categoryId, subcategoryId, _) ->
                matchesCategory(categoryId, subcategoryId, picks)
            }.mapNotNull { it.third }
            .toSet()
}
