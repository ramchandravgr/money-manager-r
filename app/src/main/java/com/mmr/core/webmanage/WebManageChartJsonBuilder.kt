package com.mmr.core.webmanage

import com.mmr.core.common.display.MmrMoneyFormat
import com.mmr.core.database.model.CategorySpending
import org.json.JSONArray
import org.json.JSONObject

object WebManageChartJsonBuilder {
    fun expensePieChartJson(
        spendings: List<CategorySpending>,
        tabTotal: Double,
        noDataMessage: String = "No expenses in this period",
    ): String {
        val breakdowns = buildBreakdowns(spendings, tabTotal)
        if (breakdowns.isEmpty()) {
            return emptyPie(noDataMessage)
        }
        val data = JSONArray()
        val colors = JSONArray()
        breakdowns.forEach { row ->
            data.put(
                JSONObject()
                    .put("name", row.name)
                    .put("y", row.percentage)
                    .put("amount", row.amountFormatted),
            )
            colors.put(row.colorHex)
        }
        val series = JSONArray()
        series.put(
            JSONObject()
                .put("name", " ")
                .put("type", "pie")
                .put("data", data),
        )
        return JSONObject()
            .put("pieBorderColor", "#FFFFFF")
            .put("pieBackgroundColor", "#FFFFFF")
            .put("pieTextColor", "#1A1A2E")
            .put("pieTextSize", 12)
            .put("colors", colors)
            .put("series", series)
            .put("nodata", noDataMessage)
            .toString()
    }

    private data class PieSlice(
        val name: String,
        val colorHex: String,
        val percentage: Double,
        val amountFormatted: String,
    )

    private fun buildBreakdowns(
        spendings: List<CategorySpending>,
        tabTotal: Double,
    ): List<PieSlice> {
        if (tabTotal <= 0.0 && spendings.isEmpty()) return emptyList()
        val categorizedTotal = spendings.sumOf { it.totalAmount }
        val denominator = if (tabTotal > 0) tabTotal else categorizedTotal
        if (denominator <= 0.0) return emptyList()

        val rows =
            spendings.map { spending ->
                val pct = spending.totalAmount / denominator
                PieSlice(
                    name = spending.categoryName ?: "Uncategorized",
                    colorHex = spending.colorHex ?: "#6B7280",
                    percentage = pct,
                    amountFormatted = MmrMoneyFormat.formatInrWithSpace(spending.totalAmount),
                )
            }
        val remainder = tabTotal - categorizedTotal
        if (tabTotal > 0 && remainder > 0.01) {
            val pct = remainder / denominator
            return rows +
                PieSlice(
                    name = "Other",
                    colorHex = "#9CA3AF",
                    percentage = pct,
                    amountFormatted = MmrMoneyFormat.formatInrWithSpace(remainder),
                )
        }
        return rows
    }

    private fun emptyPie(message: String): String =
        JSONObject()
            .put("pieBorderColor", "#FFFFFF")
            .put("pieBackgroundColor", "#FFFFFF")
            .put("pieTextColor", "#1A1A2E")
            .put("pieTextSize", 12)
            .put("colors", JSONArray())
            .put("series", JSONArray())
            .put("nodata", message)
            .toString()
}
