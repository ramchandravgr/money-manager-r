package com.mmr.app.ui.theme

import androidx.compose.ui.graphics.Color
import com.mmr.core.model.settings.MmrAmountColorScheme

/** Money Manager–style palette (header indigo, coral accent, list background). */
object MmrMmColors {
    val Header = Color(0xFF3F4671)
    val HeaderIcon = Color(0xFFFFFFFF)
    val HeaderSelection = Color(0xFF23294E)

    /** Dark tile behind launcher (foreground uses white + green strokes). */
    val HeaderBrandTile = Color(0xFF1E2445)
    val TabText = Color(0xFF9DA2BF)
    val TabTextSelected = Color(0xFFFFFFFF)
    val TabIndicator = Color(0xFFFF6255)
    val Active = Color(0xFFFF6255)
    val ListBackground = Color(0xFFF7F7FA)
    val SurfaceWhite = Color(0xFFFFFFFF)
    val BorderMain = Color(0xFFE8E8ED)

    /** MM `border_main` on Trans list row dividers (#d5d5d5). */
    val TransactionListBorder = Color(0xFFD5D5D5)
    val TextStrong = Color(0xFF4B4B4B)
    val TextMedium = Color(0xFF8E8E93)
    val TextLight = Color(0xFFB0B0B5)
    val Reversal = Color(0xFF000000)
    val NavBackground = Color(0xFFF7F7FB)
    val NavIcon = Color(0xFF6B6B75)
    val NavIconSelected = Color(0xFFFF6255)

    val ExpenseSetA = Color(0xFFD35151)
    val IncomeSetA = Color(0xFF7DA7F5)
    val ExpenseSetB = Color(0xFFEF4444)
    val IncomeSetB = Color(0xFF58A6FF)

    val ConfigBackground = Color(0xFF26282E)
    val ConfigTitle = Color(0xFFE8E8EA)
    val ConfigLabel = Color(0xFFCCCCD2)
    val ConfigValue = Color(0xFFE57373)

    fun expenseColor(scheme: MmrAmountColorScheme): Color =
        when (scheme) {
            MmrAmountColorScheme.SET_A -> ExpenseSetA
            MmrAmountColorScheme.SET_B -> ExpenseSetB
        }

    fun incomeColor(scheme: MmrAmountColorScheme): Color =
        when (scheme) {
            MmrAmountColorScheme.SET_A -> IncomeSetA
            MmrAmountColorScheme.SET_B -> IncomeSetB
        }
}
