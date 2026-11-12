package com.rrparallelstudio.miapp.feature.home

import com.rrparallelstudio.miapp.core.database.entity.CategoryEntity
import com.rrparallelstudio.miapp.core.model.enums.CategoryType
import com.rrparallelstudio.miapp.feature.home.model.HomeCategoryFilterOption
import com.rrparallelstudio.miapp.feature.home.model.HomeCategoryFilterRow
import com.rrparallelstudio.miapp.feature.home.model.HomeTypeFilter
import java.util.Locale

private fun List<CategoryEntity>.sortedByDisplayName(): List<CategoryEntity> =
    sortedBy { it.name.lowercase(Locale.getDefault()) }

fun categoryFilterRowsForHomeTypeFilter(
    entities: List<CategoryEntity>,
    filter: HomeTypeFilter,
): List<HomeCategoryFilterRow> =
    when (filter) {
        HomeTypeFilter.ALL -> {
            val expense = entities.filter { it.type == CategoryType.EXPENSE }.sortedByDisplayName()
            val income = entities.filter { it.type == CategoryType.INCOME }.sortedByDisplayName()
            buildList {
                if (expense.isNotEmpty()) {
                    add(HomeCategoryFilterRow.Section("Expenses"))
                    expense.forEach { entity ->
                        add(
                            HomeCategoryFilterRow.Choice(
                                HomeCategoryFilterOption(categoryId = entity.id, label = entity.name),
                            ),
                        )
                    }
                }
                if (income.isNotEmpty()) {
                    add(HomeCategoryFilterRow.Section("Income"))
                    income.forEach { entity ->
                        add(
                            HomeCategoryFilterRow.Choice(
                                HomeCategoryFilterOption(categoryId = entity.id, label = entity.name),
                            ),
                        )
                    }
                }
            }
        }
        HomeTypeFilter.EXPENSE ->
            entities
                .filter { it.type == CategoryType.EXPENSE }
                .sortedByDisplayName()
                .map { entity ->
                    HomeCategoryFilterRow.Choice(
                        HomeCategoryFilterOption(categoryId = entity.id, label = entity.name),
                    )
                }
        HomeTypeFilter.INCOME ->
            entities
                .filter { it.type == CategoryType.INCOME }
                .sortedByDisplayName()
                .map { entity ->
                    HomeCategoryFilterRow.Choice(
                        HomeCategoryFilterOption(categoryId = entity.id, label = entity.name),
                    )
                }
        HomeTypeFilter.TRANSFER -> emptyList()
    }

fun homeCategoryFilterOptionIds(rows: List<HomeCategoryFilterRow>): Set<Long> =
    rows
        .filterIsInstance<HomeCategoryFilterRow.Choice>()
        .map { it.option.categoryId }
        .toSet()
