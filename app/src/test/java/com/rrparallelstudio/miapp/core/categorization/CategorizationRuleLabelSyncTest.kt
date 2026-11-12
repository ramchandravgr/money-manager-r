package com.rrparallelstudio.miapp.core.categorization

import com.rrparallelstudio.miapp.core.database.entity.CategorizationRuleEntity
import com.rrparallelstudio.miapp.core.model.enums.CategorizationMatchType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CategorizationRuleLabelSyncTest {
    @Test
    fun categoryRenameRewritesCategoryLabelAndLeavesOtherRules() {
        val groceries = rule(id = 1, category = "Groceries", subcategory = "Vegetables")
        val fuel = rule(id = 2, category = "Fuel")

        val changed =
            CategorizationRuleLabelSync.categoryRenamed(
                rules = listOf(groceries, fuel),
                oldName = "groceries",
                newName = "Grocery",
                now = 50L,
            )

        assertEquals(listOf(1L), changed.map { it.id })
        assertEquals("Grocery", changed.single().categoryName)
        assertEquals("Vegetables", changed.single().subcategoryName)
        assertEquals(50L, changed.single().updatedAt)
    }

    @Test
    fun subcategoryRenameStaysInsideTheParentCategory() {
        val foodOther = rule(id = 1, category = "Food", subcategory = "Other")
        val travelOther = rule(id = 2, category = "Travel", subcategory = "Other")

        val changed =
            CategorizationRuleLabelSync.subcategoryRenamed(
                rules = listOf(foodOther, travelOther),
                categoryName = "Food",
                oldName = "other",
                newName = "Misc",
                now = 60L,
            )

        assertEquals(listOf(1L), changed.map { it.id })
        assertEquals("Misc", changed.single().subcategoryName)
        assertEquals("Food", changed.single().categoryName)
    }

    @Test
    fun removingSubcategoryClearsOnlyThatSubcategoryLabel() {
        val match = rule(id = 1, category = "Food", subcategory = "Coffee")
        val other = rule(id = 2, category = "Food", subcategory = "Dining")

        val changed =
            CategorizationRuleLabelSync.subcategoryRemoved(
                rules = listOf(match, other),
                categoryName = "Food",
                subcategoryName = "Coffee",
                now = 70L,
            )

        assertEquals(listOf(1L), changed.map { it.id })
        assertEquals("Food", changed.single().categoryName)
        assertNull(changed.single().subcategoryName)
    }

    @Test
    fun migratingCategoryRetargetsRulesAndDropsSubcategory() {
        val rule = rule(id = 4, category = "Dining", subcategory = "Cafe")

        val changed =
            CategorizationRuleLabelSync.categoryMigrated(
                rules = listOf(rule),
                oldName = "Dining",
                newName = "Food",
                now = 80L,
            )

        assertEquals("Food", changed.single().categoryName)
        assertNull(changed.single().subcategoryName)
        assertTrue(changed.single().isEnabled)
    }

    @Test
    fun removingCategoryDisablesMatchingRules() {
        val rule = rule(id = 5, category = "Dining", subcategory = "Cafe")

        val changed =
            CategorizationRuleLabelSync.categoryRemoved(
                rules = listOf(rule),
                categoryName = "Dining",
                now = 90L,
            )

        assertEquals(false, changed.single().isEnabled)
        assertEquals("Dining", changed.single().categoryName)
    }

    private fun rule(
        id: Long,
        category: String,
        subcategory: String? = null,
    ) = CategorizationRuleEntity(
        id = id,
        matchType = CategorizationMatchType.MERCHANT_OR_NOTE_CONTAINS,
        pattern = "shop",
        categoryName = category,
        subcategoryName = subcategory,
        updatedAt = 1L,
    )
}
