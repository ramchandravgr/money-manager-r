package com.mmr.core.webmanage

/** Supplies Web Manage chart and browser appearance from the phone's display settings. */
fun interface WebManageChartThemeSource {
    fun snapshot(): WebManageDisplaySnapshot
}

data class WebManageDisplaySnapshot(
    val chartTheme: WebManageChartTheme,
    /** `light`, `dark`, or `oled` — applied to the bundled web UI via `data-mmr-theme`. */
    val webAppearance: String,
)
