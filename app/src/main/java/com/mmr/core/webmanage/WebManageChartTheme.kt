package com.mmr.core.webmanage

/** Highcharts pie chrome colors for Web Manage (hex, including `#`). */
data class WebManageChartTheme(
    val pieBorderColor: String,
    val pieBackgroundColor: String,
    val pieTextColor: String,
) {
    companion object {
        val Light =
            WebManageChartTheme(
                pieBorderColor = "#FFFFFF",
                pieBackgroundColor = "#FFFFFF",
                pieTextColor = "#1A1A2E",
            )
    }
}
