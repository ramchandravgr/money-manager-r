package com.rrparallelstudio.miapp.feature.search

import com.rrparallelstudio.miapp.feature.search.model.SearchCategoryPick
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchSelectionFilterTest {
    @Test
    fun categoryTick_matchesThatCategory_subTick_matchesOnlyThatSub() {
        val food = SearchCategoryPick(categoryId = 1L)
        val restaurants = SearchCategoryPick(categoryId = 1L, subcategoryId = 9L)
        assertTrue(SearchSelectionFilter.matchesCategory(1L, 9L, setOf(food)))
        assertTrue(SearchSelectionFilter.matchesCategory(1L, null, setOf(food)))
        assertTrue(SearchSelectionFilter.matchesCategory(1L, 9L, setOf(restaurants)))
        assertFalse(SearchSelectionFilter.matchesCategory(1L, 10L, setOf(restaurants)))
    }

    @Test
    fun bothSides_canBeSelectedTogether() {
        val picks =
            setOf(
                SearchCategoryPick(1L, 9L),
                SearchCategoryPick(2L, 3L),
            )
        assertTrue(SearchSelectionFilter.matchesCategory(1L, 9L, picks))
        assertTrue(SearchSelectionFilter.matchesCategory(2L, 3L, picks))
        assertFalse(SearchSelectionFilter.matchesCategory(2L, 4L, picks))
    }

    @Test
    fun accountsFollowTickedCategories() {
        val rows =
            listOf(
                Triple(1L, 9L, 10L),
                Triple(1L, 10L, 11L),
                Triple(2L, 3L, 12L),
            )
        val ids =
            SearchSelectionFilter.matchingAccountIds(
                rows,
                setOf(SearchCategoryPick(1L, 9L), SearchCategoryPick(2L)),
            )
        assertEquals(setOf(10L, 12L), ids)
    }
}
