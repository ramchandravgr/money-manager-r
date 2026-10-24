package com.mmr.app.ui.theme

import androidx.compose.ui.graphics.Color
import com.mmr.core.model.settings.MmrAmountColorScheme
import com.mmr.core.model.settings.MmrAppColorTheme

enum class MmrThemeAppearance {
    LIGHT,
    DARK,
}

object MmrThemePresets {
    fun resolve(
        theme: MmrAppColorTheme,
        appearance: MmrThemeAppearance = MmrThemeAppearance.LIGHT,
    ): MmrBrandPalette =
        when (appearance) {
            MmrThemeAppearance.LIGHT -> light(theme)
            MmrThemeAppearance.DARK -> dark(theme)
        }

    fun light(theme: MmrAppColorTheme): MmrBrandPalette =
        when (theme) {
            MmrAppColorTheme.CLASSIC_CORAL -> classicCoralLight()
            MmrAppColorTheme.OCEAN -> oceanLight()
            MmrAppColorTheme.FOREST -> forestLight()
            MmrAppColorTheme.SLATE -> slateLight()
            MmrAppColorTheme.ROYAL -> royalLight()
            MmrAppColorTheme.SUNSET -> sunsetLight()
            MmrAppColorTheme.MIDNIGHT -> midnightLight()
            MmrAppColorTheme.ROSE_GOLD -> roseGoldLight()
            MmrAppColorTheme.MINT -> mintLight()
            MmrAppColorTheme.CHARCOAL_GOLD -> charcoalGoldLight()
        }

    /** Header, accent, and surface — for settings theme picker previews. */
    fun appThemePreviewColors(theme: MmrAppColorTheme): List<Color> {
        val palette = light(theme)
        return listOf(palette.header, palette.active, palette.surface)
    }

    /** Income then expense — for amount color scheme previews (uses [theme] palette). */
    fun amountSchemePreviewColors(
        theme: MmrAppColorTheme,
        scheme: MmrAmountColorScheme,
    ): List<Color> {
        val palette = light(theme)
        return listOf(palette.incomeColor(scheme), palette.expenseColor(scheme))
    }

    /** Paired dark palettes (not exposed in UI yet). */
    fun dark(theme: MmrAppColorTheme): MmrBrandPalette =
        when (theme) {
            MmrAppColorTheme.CLASSIC_CORAL -> classicCoralDark()
            MmrAppColorTheme.OCEAN -> oceanDark()
            MmrAppColorTheme.FOREST -> forestDark()
            MmrAppColorTheme.SLATE -> slateDark()
            MmrAppColorTheme.ROYAL -> royalDark()
            MmrAppColorTheme.SUNSET -> sunsetDark()
            MmrAppColorTheme.MIDNIGHT -> midnightDark()
            MmrAppColorTheme.ROSE_GOLD -> roseGoldDark()
            MmrAppColorTheme.MINT -> mintDark()
            MmrAppColorTheme.CHARCOAL_GOLD -> charcoalGoldDark()
        }

    private data class AmountPalette(
        val expenseA: Color,
        val incomeA: Color,
        val expenseB: Color,
        val incomeB: Color,
        val expenseC: Color,
        val incomeC: Color,
        val expenseD: Color,
        val incomeD: Color,
        val expenseE: Color,
        val incomeE: Color,
    )

    private fun classicCoralLight() =
        buildPalette(
            header = Color(0xFF3F4671),
            headerSelection = Color(0xFF23294E),
            headerBrandTile = Color(0xFF1E2445),
            tabText = Color(0xFF9DA2BF),
            accent = Color(0xFFFF6255),
            listBackground = Color(0xFFF7F7FA),
            navBackground = Color(0xFFFFFFFF),
            calendarExpense = Color(0xFF5B8DEF),
            monthlyIncome = Color(0xFFD35151),
            monthlyExpense = Color(0xFF5B8DEF),
            weekHighlight = Color(0xFFFFE8D6),
            dialogHeader = Color(0xFF263248),
            dialogAccent = Color(0xFFE86A4F),
            chartLine = Color(0xFF3B82F6),
            amounts =
                AmountPalette(
                    expenseA = Color(0xFFD35151),
                    incomeA = Color(0xFF0D9488),
                    expenseB = Color(0xFFEF4444),
                    incomeB = Color(0xFF059669),
                    expenseC = Color(0xFFBE123C),
                    incomeC = Color(0xFF0284C7),
                    expenseD = Color(0xFFEA580C),
                    incomeD = Color(0xFF65A30D),
                    expenseE = Color(0xFFC026D3),
                    incomeE = Color(0xFF4F46E5),
                ),
        )

    private fun oceanLight() =
        buildPalette(
            header = Color(0xFF0E4D64),
            headerSelection = Color(0xFF083848),
            headerBrandTile = Color(0xFF062A36),
            tabText = Color(0xFF8FB4C4),
            accent = Color(0xFF22D3EE),
            listBackground = Color(0xFFF4F9FB),
            navBackground = Color(0xFFFFFFFF),
            calendarExpense = Color(0xFF0891B2),
            monthlyIncome = Color(0xFF0E7490),
            monthlyExpense = Color(0xFF0369A1),
            weekHighlight = Color(0xFFCFFAFE),
            dialogHeader = Color(0xFF0E4D64),
            dialogAccent = Color(0xFF22D3EE),
            chartLine = Color(0xFF0891B2),
            amounts =
                AmountPalette(
                    expenseA = Color(0xFFC75C5C),
                    incomeA = Color(0xFF0F766E),
                    expenseB = Color(0xFFE11D48),
                    incomeB = Color(0xFF14B8A6),
                    expenseC = Color(0xFF9F1239),
                    incomeC = Color(0xFF0EA5E9),
                    expenseD = Color(0xFFD97706),
                    incomeD = Color(0xFF84CC16),
                    expenseE = Color(0xFFA21CAF),
                    incomeE = Color(0xFF4338CA),
                ),
        )

    private fun forestLight() =
        buildPalette(
            header = Color(0xFF2D4A3E),
            headerSelection = Color(0xFF1E3329),
            headerBrandTile = Color(0xFF15261E),
            tabText = Color(0xFFA3B8A8),
            accent = Color(0xFF52B788),
            listBackground = Color(0xFFF6FAF7),
            navBackground = Color(0xFFFFFFFF),
            calendarExpense = Color(0xFF15803D),
            monthlyIncome = Color(0xFF166534),
            monthlyExpense = Color(0xFF14532D),
            weekHighlight = Color(0xFFD1FAE5),
            dialogHeader = Color(0xFF2D4A3E),
            dialogAccent = Color(0xFF52B788),
            chartLine = Color(0xFF16A34A),
            amounts =
                AmountPalette(
                    expenseA = Color(0xFFB85C5C),
                    incomeA = Color(0xFF6BAF8A),
                    expenseB = Color(0xFFDC2626),
                    incomeB = Color(0xFF16A34A),
                    expenseC = Color(0xFFB91C1C),
                    incomeC = Color(0xFF0891B2),
                    expenseD = Color(0xFFCA8A04),
                    incomeD = Color(0xFF4D7C0F),
                    expenseE = Color(0xFF9333EA),
                    incomeE = Color(0xFF3730A3),
                ),
        )

    private fun slateLight() =
        buildPalette(
            header = Color(0xFF3D4F5C),
            headerSelection = Color(0xFF2A3842),
            headerBrandTile = Color(0xFF1F2A32),
            tabText = Color(0xFFA8B4BC),
            accent = Color(0xFF6366F1),
            listBackground = Color(0xFFF5F6F8),
            navBackground = Color(0xFFFFFFFF),
            calendarExpense = Color(0xFF4F46E5),
            monthlyIncome = Color(0xFF4338CA),
            monthlyExpense = Color(0xFF3730A3),
            weekHighlight = Color(0xFFE0E7FF),
            dialogHeader = Color(0xFF3D4F5C),
            dialogAccent = Color(0xFF6366F1),
            chartLine = Color(0xFF4F46E5),
            amounts =
                AmountPalette(
                    expenseA = Color(0xFFBE5C5C),
                    incomeA = Color(0xFF0D9488),
                    expenseB = Color(0xFFEF4444),
                    incomeB = Color(0xFF10B981),
                    expenseC = Color(0xFFDC2626),
                    incomeC = Color(0xFF2563EB),
                    expenseD = Color(0xFFF59E0B),
                    incomeD = Color(0xFF22C55E),
                    expenseE = Color(0xFFDB2777),
                    incomeE = Color(0xFF5B21B6),
                ),
        )

    private fun royalLight() =
        buildPalette(
            header = Color(0xFF4A3768),
            headerSelection = Color(0xFF352750),
            headerBrandTile = Color(0xFF261C3A),
            tabText = Color(0xFFB8A8CC),
            accent = Color(0xFFE879F9),
            listBackground = Color(0xFFF9F7FC),
            navBackground = Color(0xFFFFFFFF),
            calendarExpense = Color(0xFF9333EA),
            monthlyIncome = Color(0xFF7E22CE),
            monthlyExpense = Color(0xFF6B21A8),
            weekHighlight = Color(0xFFF3E8FF),
            dialogHeader = Color(0xFF4A3768),
            dialogAccent = Color(0xFFE879F9),
            chartLine = Color(0xFF9333EA),
            amounts =
                AmountPalette(
                    expenseA = Color(0xFFC45C7A),
                    incomeA = Color(0xFF9B8FD9),
                    expenseB = Color(0xFFDB2777),
                    incomeB = Color(0xFFA855F7),
                    expenseC = Color(0xFFE11D48),
                    incomeC = Color(0xFF7C3AED),
                    expenseD = Color(0xFFEA580C),
                    incomeD = Color(0xFF8B5CF6),
                    expenseE = Color(0xFFD946EF),
                    incomeE = Color(0xFF6366F1),
                ),
        )

    private fun sunsetLight() =
        buildPalette(
            header = Color(0xFF5C3D2E),
            headerSelection = Color(0xFF3D281C),
            headerBrandTile = Color(0xFF2A1A12),
            tabText = Color(0xFFD4B8A8),
            accent = Color(0xFFF97316),
            listBackground = Color(0xFFFCF8F5),
            navBackground = Color(0xFFFFFFFF),
            calendarExpense = Color(0xFFEA580C),
            monthlyIncome = Color(0xFFC2410C),
            monthlyExpense = Color(0xFF9A3412),
            weekHighlight = Color(0xFFFFEDD5),
            dialogHeader = Color(0xFF5C3D2E),
            dialogAccent = Color(0xFFF97316),
            chartLine = Color(0xFFEA580C),
            amounts =
                AmountPalette(
                    expenseA = Color(0xFFDC2626),
                    incomeA = Color(0xFF0D9488),
                    expenseB = Color(0xFFE11D48),
                    incomeB = Color(0xFF059669),
                    expenseC = Color(0xFFB45309),
                    incomeC = Color(0xFF0369A1),
                    expenseD = Color(0xFFEA580C),
                    incomeD = Color(0xFF65A30D),
                    expenseE = Color(0xFFBE185D),
                    incomeE = Color(0xFF7C2D12),
                ),
        )

    private fun midnightLight() =
        buildPalette(
            header = Color(0xFF0F172A),
            headerSelection = Color(0xFF020617),
            headerBrandTile = Color(0xFF0B1120),
            tabText = Color(0xFF94A3B8),
            accent = Color(0xFF38BDF8),
            listBackground = Color(0xFFF1F5F9),
            navBackground = Color(0xFFFFFFFF),
            calendarExpense = Color(0xFF0284C7),
            monthlyIncome = Color(0xFF0369A1),
            monthlyExpense = Color(0xFF1D4ED8),
            weekHighlight = Color(0xFFE0F2FE),
            dialogHeader = Color(0xFF0F172A),
            dialogAccent = Color(0xFF38BDF8),
            chartLine = Color(0xFF0284C7),
            amounts =
                AmountPalette(
                    expenseA = Color(0xFFF87171),
                    incomeA = Color(0xFF2DD4BF),
                    expenseB = Color(0xFFFB7185),
                    incomeB = Color(0xFF34D399),
                    expenseC = Color(0xFFF43F5E),
                    incomeC = Color(0xFF38BDF8),
                    expenseD = Color(0xFFFBBF24),
                    incomeD = Color(0xFFA3E635),
                    expenseE = Color(0xFFE879F9),
                    incomeE = Color(0xFF818CF8),
                ),
        )

    private fun roseGoldLight() =
        buildPalette(
            header = Color(0xFF6B4E5D),
            headerSelection = Color(0xFF4A3540),
            headerBrandTile = Color(0xFF352630),
            tabText = Color(0xFFD4B8C4),
            accent = Color(0xFFE8B4B8),
            listBackground = Color(0xFFFAF7F8),
            navBackground = Color(0xFFFFFFFF),
            calendarExpense = Color(0xFFDB7093),
            monthlyIncome = Color(0xFF9D6B8A),
            monthlyExpense = Color(0xFF7C4D6B),
            weekHighlight = Color(0xFFFCE7F3),
            dialogHeader = Color(0xFF6B4E5D),
            dialogAccent = Color(0xFFE8B4B8),
            chartLine = Color(0xFFDB7093),
            amounts =
                AmountPalette(
                    expenseA = Color(0xFFC45C7A),
                    incomeA = Color(0xFF6B9080),
                    expenseB = Color(0xFFE11D48),
                    incomeB = Color(0xFF10B981),
                    expenseC = Color(0xFFBE123C),
                    incomeC = Color(0xFF0E7490),
                    expenseD = Color(0xFFD97706),
                    incomeD = Color(0xFF65A30D),
                    expenseE = Color(0xFFA855F7),
                    incomeE = Color(0xFF6366F1),
                ),
        )

    private fun mintLight() =
        buildPalette(
            header = Color(0xFF1E4D47),
            headerSelection = Color(0xFF123830),
            headerBrandTile = Color(0xFF0D2924),
            tabText = Color(0xFF9FD4CC),
            accent = Color(0xFF2DD4BF),
            listBackground = Color(0xFFF0FDFA),
            navBackground = Color(0xFFFFFFFF),
            calendarExpense = Color(0xFF0F766E),
            monthlyIncome = Color(0xFF0D9488),
            monthlyExpense = Color(0xFF115E59),
            weekHighlight = Color(0xFFCCFBF1),
            dialogHeader = Color(0xFF1E4D47),
            dialogAccent = Color(0xFF2DD4BF),
            chartLine = Color(0xFF14B8A6),
            amounts =
                AmountPalette(
                    expenseA = Color(0xFFDC6B6B),
                    incomeA = Color(0xFF2DD4BF),
                    expenseB = Color(0xFFEF4444),
                    incomeB = Color(0xFF10B981),
                    expenseC = Color(0xFFE11D48),
                    incomeC = Color(0xFF06B6D4),
                    expenseD = Color(0xFFF59E0B),
                    incomeD = Color(0xFF84CC16),
                    expenseE = Color(0xFF8B5CF6),
                    incomeE = Color(0xFF0891B2),
                ),
        )

    private fun charcoalGoldLight() =
        buildPalette(
            header = Color(0xFF2D2D2D),
            headerSelection = Color(0xFF1A1A1A),
            headerBrandTile = Color(0xFF121212),
            tabText = Color(0xFFB0B0B0),
            accent = Color(0xFFD4AF37),
            listBackground = Color(0xFFF5F5F4),
            navBackground = Color(0xFFFFFFFF),
            calendarExpense = Color(0xFF78716C),
            monthlyIncome = Color(0xFFB45309),
            monthlyExpense = Color(0xFF57534E),
            weekHighlight = Color(0xFFFEF3C7),
            dialogHeader = Color(0xFF2D2D2D),
            dialogAccent = Color(0xFFD4AF37),
            chartLine = Color(0xFFA16207),
            amounts =
                AmountPalette(
                    expenseA = Color(0xFFB45309),
                    incomeA = Color(0xFF0D9488),
                    expenseB = Color(0xFFDC2626),
                    incomeB = Color(0xFF059669),
                    expenseC = Color(0xFFBE123C),
                    incomeC = Color(0xFF2563EB),
                    expenseD = Color(0xFFEA580C),
                    incomeD = Color(0xFF65A30D),
                    expenseE = Color(0xFF9333EA),
                    incomeE = Color(0xFF4F46E5),
                ),
        )

    private fun classicCoralDark() =
        classicCoralLight().let { light ->
            light.copy(
                header = Color(0xFF1A1F38),
                headerSelection = Color(0xFF12162A),
                headerBrandTile = Color(0xFF0E1220),
                listBackground = Color(0xFF12141C),
                surface = Color(0xFF1C1F2A),
                navBackground = Color(0xFF161820),
                reversal = Color(0xFFF2F2F5),
                textStrong = Color(0xFFE8E8ED),
                textMedium = Color(0xFFABABB3),
                textLight = Color(0xFF7A7A85),
                borderMain = Color(0xFF2E313C),
                transactionListBorder = Color(0xFF3A3D48),
                configBackground = Color(0xFF0E1016),
            )
        }

    private fun oceanDark() = tintDark(oceanLight())

    private fun forestDark() = tintDark(forestLight())

    private fun slateDark() = tintDark(slateLight())

    private fun royalDark() = tintDark(royalLight())

    private fun sunsetDark() = tintDark(sunsetLight())

    private fun midnightDark() = tintDark(midnightLight())

    private fun roseGoldDark() = tintDark(roseGoldLight())

    private fun mintDark() = tintDark(mintLight())

    private fun charcoalGoldDark() = tintDark(charcoalGoldLight())

    private fun tintDark(light: MmrBrandPalette): MmrBrandPalette =
        light.copy(
            header = light.header.copy(alpha = 1f).let { Color(0xFF101820) },
            headerSelection = Color(0xFF0A1218),
            headerBrandTile = Color(0xFF060C10),
            listBackground = Color(0xFF0F1418),
            surface = Color(0xFF171D22),
            navBackground = Color(0xFF12181C),
            reversal = Color(0xFFE8F4F8),
            textStrong = Color(0xFFE2E8F0),
            textMedium = Color(0xFF94A3B8),
            textLight = Color(0xFF64748B),
            borderMain = Color(0xFF2A3440),
            transactionListBorder = Color(0xFF334155),
            tabText = Color(0xFF94A3B8),
            configBackground = Color(0xFF0A0E12),
        )

    private fun buildPalette(
        header: Color,
        headerSelection: Color,
        headerBrandTile: Color,
        tabText: Color,
        accent: Color,
        listBackground: Color,
        navBackground: Color,
        calendarExpense: Color,
        monthlyIncome: Color,
        monthlyExpense: Color,
        weekHighlight: Color,
        dialogHeader: Color,
        dialogAccent: Color,
        chartLine: Color,
        amounts: AmountPalette,
    ): MmrBrandPalette {
        val surface = Color(0xFFFFFFFF)
        val textStrong = Color(0xFF4B4B4B)
        val textMedium = Color(0xFF8E8E93)
        val textLight = Color(0xFFB0B0B5)
        val positive = Color(0xFF10B981)
        val negative = Color(0xFFEF4444)
        return MmrBrandPalette(
            header = header,
            headerIcon = Color.White,
            headerSelection = headerSelection,
            headerBrandTile = headerBrandTile,
            tabText = tabText,
            tabTextSelected = Color.White,
            tabIndicator = accent,
            active = accent,
            listBackground = listBackground,
            surface = surface,
            borderMain = Color(0xFFE8E8ED),
            transactionListBorder = Color(0xFFD5D5D5),
            textStrong = textStrong,
            textMedium = textMedium,
            textLight = textLight,
            reversal = Color.Black,
            navBackground = navBackground,
            navIcon = Color(0xFF9CA3AF),
            navIconSelected = header,
            expenseSetA = amounts.expenseA,
            incomeSetA = amounts.incomeA,
            expenseSetB = amounts.expenseB,
            incomeSetB = amounts.incomeB,
            expenseSetC = amounts.expenseC,
            incomeSetC = amounts.incomeC,
            expenseSetD = amounts.expenseD,
            incomeSetD = amounts.incomeD,
            expenseSetE = amounts.expenseE,
            incomeSetE = amounts.incomeE,
            configBackground = Color(0xFF26282E),
            configTitle = Color(0xFFE8E8EA),
            configLabel = Color(0xFFCCCCD2),
            configValue = accent,
            amountPositive = positive,
            amountNegative = negative,
            approvalApproveBackground = Color(0xFF1B3D2F),
            approvalRejectBackground = Color(0xFF3D1F24),
            approvalApproveIcon = Color(0xFF22C55E),
            approvalRejectIcon = negative,
            calendarExpense = calendarExpense,
            calendarSaturday = calendarExpense,
            calendarSunday = Color(0xFFE57373),
            calendarSelectedDay = headerSelection,
            calendarCellBorder = Color(0xFFE8E8E8),
            monthlyIncome = monthlyIncome,
            monthlyExpense = monthlyExpense,
            weekHighlight = weekHighlight,
            dialogHeader = dialogHeader,
            dialogAccent = dialogAccent,
            chartLinePrimary = chartLine,
            chartPieFallback = chartLine,
            homeSummaryAccent = headerBrandTile,
        )
    }
}
