package com.rrparallelstudio.miapp.feature.home.model

data class HomeCategoryFilterOption(
    val categoryId: Long,
    val label: String,
)

sealed interface HomeCategoryFilterRow {
    data class Section(
        val title: String,
    ) : HomeCategoryFilterRow

    data class Choice(
        val option: HomeCategoryFilterOption,
    ) : HomeCategoryFilterRow
}
