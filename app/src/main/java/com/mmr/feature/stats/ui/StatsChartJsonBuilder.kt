package com.mmr.feature.stats.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.mmr.app.ui.theme.MmrBrandColors
import com.mmr.feature.stats.model.CategoryBreakdownUi
import com.mmr.feature.stats.model.MonthlyChartPointUi
import org.json.JSONArray
import org.json.JSONObject

internal object StatsChartJsonBuilder {
    fun pieChartJson(
        breakdowns: List<CategoryBreakdownUi>,
        noDataMessage: String,
        backgroundColor: Color = MmrBrandColors.SurfaceWhite,
        textColor: Color = MmrBrandColors.Reversal,
    ): String {
        val data = JSONArray()
        breakdowns.forEach { row ->
            data.put(
                JSONObject()
                    .put("name", row.categoryName)
                    .put("y", row.percentage.toDouble())
                    .put("amount", row.amountFormatted),
            )
        }
        val colors = JSONArray()
        breakdowns.forEach { colors.put(it.colorHex) }
        val series = JSONArray()
        series.put(
            JSONObject()
                .put("name", " ")
                .put("type", "pie")
                .put("data", data),
        )
        return JSONObject()
            .put("pieBorderColor", colorToHex(backgroundColor))
            .put("pieBackgroundColor", colorToHex(backgroundColor))
            .put("pieTextColor", colorToHex(textColor))
            .put("pieTextSize", 12)
            .put("colors", colors)
            .put("series", series)
            .put("nodata", noDataMessage)
            .toString()
    }

    fun lineChartJson(
        points: List<MonthlyChartPointUi>,
        lineColorHex: String,
        noDataMessage: String,
        valuePrefix: String = "₹",
        valueSuffix: String = "",
    ): String {
        val categories = JSONArray()
        points.forEach { categories.put(it.label) }
        val values = JSONArray()
        points.forEach { values.put(it.amount) }
        val series = JSONArray()
        series.put(
            JSONObject()
                .put("name", "Amount")
                .put("data", values),
        )
        val colors = JSONArray().put(lineColorHex)
        return JSONObject()
            .put("xAxisVisible", true)
            .put("plotBackgroundColor", colorToHex(MmrBrandColors.ListBackground))
            .put("tooltipBackgroundColor", colorToHex(MmrBrandColors.Active))
            .put("tooltipTextColor", colorToHex(MmrBrandColors.SurfaceWhite))
            .put("textMediumColor", colorToHex(MmrBrandColors.TextMedium))
            .put("baseColor", colorToHex(MmrBrandColors.SurfaceWhite))
            .put("borderColor", colorToHex(MmrBrandColors.TransactionListBorder))
            .put("categories", categories)
            .put("series", series)
            .put("colors", colors)
            .put("valuePrefix", valuePrefix)
            .put("valueSuffix", valueSuffix)
            .put("nodata", noDataMessage)
            .toString()
    }

    private fun colorToHex(color: Color): String {
        val rgb = color.toArgb() and 0xFFFFFF
        return String.format("#%06X", rgb)
    }
}
