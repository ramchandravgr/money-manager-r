package com.mmr.app.ui.theme

import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.mmr.core.model.settings.MmrAmountColorScheme

@Immutable
data class MmrBrandPalette(
    val header: Color,
    val headerIcon: Color,
    val headerSelection: Color,
    val headerBrandTile: Color,
    val tabText: Color,
    val tabTextSelected: Color,
    val tabIndicator: Color,
    val active: Color,
    val listBackground: Color,
    val surface: Color,
    val borderMain: Color,
    val transactionListBorder: Color,
    val textStrong: Color,
    val textMedium: Color,
    val textLight: Color,
    val reversal: Color,
    val navBackground: Color,
    val navIcon: Color,
    val navIconSelected: Color,
    val expenseSetA: Color,
    val incomeSetA: Color,
    val expenseSetB: Color,
    val incomeSetB: Color,
    val expenseSetC: Color,
    val incomeSetC: Color,
    val expenseSetD: Color,
    val incomeSetD: Color,
    val expenseSetE: Color,
    val incomeSetE: Color,
    val configBackground: Color,
    val configTitle: Color,
    val configLabel: Color,
    val configValue: Color,
    val amountPositive: Color,
    val amountNegative: Color,
    val approvalApproveBackground: Color,
    val approvalRejectBackground: Color,
    val approvalApproveIcon: Color,
    val approvalRejectIcon: Color,
    val calendarExpense: Color,
    val calendarSaturday: Color,
    val calendarSunday: Color,
    val calendarSelectedDay: Color,
    val calendarCellBorder: Color,
    val monthlyIncome: Color,
    val monthlyExpense: Color,
    val weekHighlight: Color,
    val dialogHeader: Color,
    val dialogAccent: Color,
    val chartLinePrimary: Color,
    val chartPieFallback: Color,
    val homeSummaryAccent: Color,
) {
    fun expenseColor(scheme: MmrAmountColorScheme): Color =
        when (scheme) {
            MmrAmountColorScheme.SET_A -> expenseSetA
            MmrAmountColorScheme.SET_B -> expenseSetB
            MmrAmountColorScheme.SET_C -> expenseSetC
            MmrAmountColorScheme.SET_D -> expenseSetD
            MmrAmountColorScheme.SET_E -> expenseSetE
        }

    fun incomeColor(scheme: MmrAmountColorScheme): Color =
        when (scheme) {
            MmrAmountColorScheme.SET_A -> incomeSetA
            MmrAmountColorScheme.SET_B -> incomeSetB
            MmrAmountColorScheme.SET_C -> incomeSetC
            MmrAmountColorScheme.SET_D -> incomeSetD
            MmrAmountColorScheme.SET_E -> incomeSetE
        }

    fun toMaterialLight() =
        lightColorScheme(
            primary = active,
            onPrimary = headerIcon,
            primaryContainer = header,
            onPrimaryContainer = headerIcon,
            secondary = header,
            background = listBackground,
            onBackground = reversal,
            surface = surface,
            onSurface = reversal,
            surfaceVariant = navBackground,
            onSurfaceVariant = textMedium,
            outline = borderMain,
            outlineVariant = borderMain,
        )

    fun statusBarColor(): Color = header

    fun toArgbHex(color: Color): String {
        val argb = color.toArgb()
        return String.format("#%06X", argb and 0xFFFFFF)
    }
}
