package com.mmr.core.webmanage

import com.mmr.core.database.model.CategorySpending
import org.json.JSONObject
import org.junit.Assert.assertTrue
import org.junit.Test

class WebManageChartJsonBuilderTest {
    @Test
    fun pieChart_includesSeriesWhenSpendPresent() {
        val json =
            WebManageChartJsonBuilder.expensePieChartJson(
                spendings =
                    listOf(
                        CategorySpending(
                            categoryId = 1L,
                            categoryName = "Food",
                            colorHex = "#FF0000",
                            totalAmount = 100.0,
                            transactionCount = 2,
                        ),
                    ),
                tabTotal = 100.0,
            )
        val root = JSONObject(json)
        assertTrue(root.getJSONArray("series").length() > 0)
        val data = root.getJSONArray("series").getJSONObject(0).getJSONArray("data")
        assertTrue(data.length() == 1)
    }

    @Test
    fun pieChart_addsOtherSliceWhenTabTotalExceedsCategorized() {
        val json =
            WebManageChartJsonBuilder.expensePieChartJson(
                spendings =
                    listOf(
                        CategorySpending(
                            categoryId = 1L,
                            categoryName = "Food",
                            colorHex = "#FF0000",
                            totalAmount = 40.0,
                            transactionCount = 1,
                        ),
                    ),
                tabTotal = 100.0,
            )
        val root = JSONObject(json)
        val data = root.getJSONArray("series").getJSONObject(0).getJSONArray("data")
        assertTrue(data.length() == 2)
    }
}
