package com.rrparallelstudio.miapp.feature.home

import com.rrparallelstudio.miapp.core.data.catalog.MmrUncategorisedCategory
import com.rrparallelstudio.miapp.core.database.entity.CategoryEntity
import com.rrparallelstudio.miapp.core.model.enums.CategoryType
import com.rrparallelstudio.miapp.feature.home.model.HomeCategoryFilterOption
import com.rrparallelstudio.miapp.feature.home.model.HomeCategoryFilterRow
import com.rrparallelstudio.miapp.feature.home.model.HomeTypeFilter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeCategoryFilterNamesTest {
    @Test
    fun allFilter_splitsUncategorisedUnderExpenseAndIncomeSections() {
        val entities =
            listOf(
                CategoryEntity(
                    id = 1,
                    name = MmrUncategorisedCategory.NAME,
                    type = CategoryType.EXPENSE,
                    colorHex = "#000000",
                ),
                CategoryEntity(
                    id = 2,
                    name = MmrUncategorisedCategory.NAME,
                    type = CategoryType.INCOME,
                    colorHex = "#000000",
                ),
            )
        val rows = categoryFilterRowsForHomeTypeFilter(entities, HomeTypeFilter.ALL)
        assertEquals(
            listOf(
                HomeCategoryFilterRow.Section("Expenses"),
                HomeCategoryFilterRow.Choice(
                    HomeCategoryFilterOption(1, MmrUncategorisedCategory.NAME),
                ),
                HomeCategoryFilterRow.Section("Income"),
                HomeCategoryFilterRow.Choice(
                    HomeCategoryFilterOption(2, MmrUncategorisedCategory.NAME),
                ),
            ),
            rows,
        )
    }

    @Test
    fun expenseFilter_usesFlatListWithoutSections() {
        val entities =
            listOf(
                CategoryEntity(
                    id = 10,
                    name = "Food",
                    type = CategoryType.EXPENSE,
                    colorHex = "#000000",
                ),
            )
        val rows = categoryFilterRowsForHomeTypeFilter(entities, HomeTypeFilter.EXPENSE)
        assertEquals(1, rows.size)
        assertTrue(rows[0] is HomeCategoryFilterRow.Choice)
    }
}
