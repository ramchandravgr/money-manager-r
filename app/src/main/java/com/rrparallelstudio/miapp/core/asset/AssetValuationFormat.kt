package com.rrparallelstudio.miapp.core.asset

import kotlin.math.abs

object AssetValuationFormat {
    fun percentChangeFromPrevious(
        previousAmount: Double?,
        newAmount: Double,
    ): Double? {
        if (previousAmount == null || previousAmount == 0.0) return null
        return ((newAmount - previousAmount) / previousAmount) * 100.0
    }

    fun formatPercentChange(changePercent: Double?): String? {
        if (changePercent == null) return null
        val rounded: Double =
            if (abs(changePercent) >= 100) {
                changePercent.toInt().toDouble()
            } else {
                (changePercent * 10).toInt() / 10.0
            }
        val sign = if (rounded > 0) "+" else ""
        return "$sign$rounded%"
    }
}
