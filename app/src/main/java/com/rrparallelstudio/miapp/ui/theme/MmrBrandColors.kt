package com.rrparallelstudio.miapp.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import com.rrparallelstudio.miapp.core.model.settings.MmrAmountColorScheme

/**
 * Semantic brand colors for the active theme. Updated from [MoneyInboxTheme] when the user
 * changes app color theme in Settings.
 */
object MmrBrandColors {
    private val p: MmrBrandPalette
        get() = MmrBrandColorState.palette

    /** Scaffold / full-screen container when expressive backgrounds are on. */
    val ScreenContainer: Color
        get() =
            if (MmrExpressiveThemeFlags.ENABLED) {
                Color.Transparent
            } else {
                p.listBackground
            }

    /** List scroll areas that should show the expressive canvas underneath. */
    val ListAreaBackground: Color
        get() = ScreenContainer

    fun canvasIsDark(): Boolean =
        if (MmrExpressiveThemeFlags.ENABLED) {
            MmrExpressiveThemeProfiles.canvasIsDark()
        } else {
            MmrExpressiveThemeState.appearance != MmrThemeAppearance.LIGHT
        }

    fun onCanvas(): Color = if (canvasIsDark()) MmrExpressiveDarkCanvasChrome.onCanvas else p.textStrong

    fun onCanvasMuted(): Color = if (canvasIsDark()) MmrExpressiveDarkCanvasChrome.onCanvasMuted else p.textLight

    /**
     * Secondary caption (footers, helper lines) on the **scroll list area** when expressive mode paints a **dark canvas**
     * behind [ScreenContainer] (transparent). Do not use for text on white [SurfaceWhite] cards — use [TextMedium] there.
     */
    fun listAreaCaptionText(): Color =
        if (MmrExpressiveThemeFlags.ENABLED && canvasIsDark()) {
            onCanvasMuted()
        } else {
            TextMedium
        }

    /**
     * Small titles / bullets on the list area (not on grouped white cards) under the same conditions as
     * [listAreaCaptionText].
     */
    fun listAreaBodyText(): Color =
        if (MmrExpressiveThemeFlags.ENABLED && canvasIsDark()) {
            onCanvas()
        } else {
            TextStrong
        }

    fun filterChipUnselectedFill(): Color =
        if (canvasIsDark()) {
            MmrExpressiveDarkCanvasChrome.chipFill
        } else {
            Color.Transparent
        }

    fun filterChipUnselectedBorder(): Color =
        if (canvasIsDark()) {
            MmrExpressiveDarkCanvasChrome.chipBorder
        } else {
            p.borderMain
        }

    fun filterChipUnselectedText(): Color =
        if (canvasIsDark()) {
            MmrExpressiveDarkCanvasChrome.chipText
        } else {
            p.textStrong
        }

    /** Frosted band for tabs, summary strip, bottom nav (expressive mode). */
    fun expressiveChromeSurface(): Color =
        if (MmrExpressiveThemeFlags.ENABLED) {
            if (canvasIsDark()) {
                MmrExpressiveDarkCanvasChrome.band
            } else {
                p.surface.copy(alpha = 0.92f)
            }
        } else {
            p.surface
        }

    fun expressiveSummaryStripBackground(): Color =
        if (MmrExpressiveThemeFlags.ENABLED) {
            lerp(
                expressiveChromeSurface(),
                p.active.copy(alpha = 0.22f),
                0.35f,
            )
        } else {
            p.surface
        }

    fun expressiveNavBackground(): Color =
        if (MmrExpressiveThemeFlags.ENABLED) {
            if (canvasIsDark()) {
                MmrExpressiveDarkCanvasChrome.nav
            } else {
                p.navBackground.copy(alpha = 0.78f)
            }
        } else {
            p.navBackground
        }

    fun summaryNetColor(): Color = if (canvasIsDark()) onCanvas() else p.reversal

    /** Bottom sheets / dialogs over expressive canvas (dark band) vs solid white elsewhere. */
    fun modalChromeSurface(): Color =
        if (MmrExpressiveThemeFlags.ENABLED && canvasIsDark()) {
            expressiveChromeSurface()
        } else {
            p.surface
        }

    /**
     * Grouped option list inside [com.rrparallelstudio.miapp.ui.components.MmrIosSettingsSingleChoiceSheet].
     * On expressive **dark** canvas, [modalChromeSurface] matches the sheet shell — lift slightly so rows read as a card.
     */
    fun settingsSheetGroupedListFill(): Color =
        when {
            MmrExpressiveThemeFlags.ENABLED && canvasIsDark() ->
                lerp(modalChromeSurface(), Color.White, 0.10f)
            canvasIsDark() ->
                // Non-expressive dark: sheet shell is [p.surface]; lift rows slightly for separation.
                lerp(p.surface, Color.White, 0.08f)
            else -> SurfaceWhite
        }

    /** Primary label on rows inside settings bottom-sheet pickers (never raw [Reversal] on dark palettes). */
    fun settingsSheetListRowText(): Color =
        when {
            canvasIsDark() && MmrExpressiveThemeFlags.ENABLED -> onCanvas()
            canvasIsDark() -> Reversal
            SurfaceWhite.mmrRelativeLuminance() >= 0.45f -> TextStrong
            else -> Reversal
        }

    fun settingsSheetListDivider(): Color =
        if (canvasIsDark()) {
            Color.White.copy(alpha = 0.14f)
        } else {
            BorderMain
        }

    /** Outline for theme swatches on dark modal chrome ([BorderMain] is too dark on frosted bands). */
    fun settingsSheetSwatchOutline(): Color =
        if (canvasIsDark()) {
            Color.White.copy(alpha = 0.32f)
        } else {
            BorderMain
        }

    /** [MmrContentDialog] / Design 12 shell body + footer band (aligned with modal chrome on dark canvas). */
    fun dialogBodySurface(): Color =
        if (canvasIsDark()) {
            modalChromeSurface()
        } else {
            p.surface
        }

    fun dialogTitleText(): Color = if (canvasIsDark()) onCanvas() else p.textStrong

    fun dialogMessageText(): Color = if (canvasIsDark()) onCanvasMuted() else p.textMedium

    /** Primary value / input text inside dialog fields (replaces light-theme [Reversal] on dark canvas). */
    fun dialogFieldValueText(): Color = if (canvasIsDark()) onCanvas() else p.reversal

    /** Secondary / placeholder text in dialog fields. */
    fun dialogFieldMutedText(): Color = if (canvasIsDark()) onCanvasMuted() else p.textMedium

    /** Picker rows and option buttons inside dialogs — light surfaces would flash on dark canvas. */
    fun dialogFieldFill(): Color =
        if (canvasIsDark()) {
            MmrExpressiveDarkCanvasChrome.chipFill
        } else {
            p.surface
        }

    fun dialogFooterDismissText(): Color = if (canvasIsDark()) onCanvasMuted() else p.textMedium

    fun expressiveFooterBandBackground(): Color =
        if (MmrExpressiveThemeFlags.ENABLED) {
            p.listBackground.copy(alpha = 0.72f)
        } else {
            p.listBackground
        }

    val Header: Color get() = p.header
    val HeaderIcon: Color get() = p.headerIcon
    val HeaderSelection: Color get() = p.headerSelection
    val HeaderBrandTile: Color get() = p.headerBrandTile
    val TabText: Color get() = p.tabText
    val TabTextSelected: Color get() = p.tabTextSelected
    val TabIndicator: Color get() = p.tabIndicator
    val Active: Color get() = p.active
    val ListBackground: Color get() = p.listBackground
    val SurfaceWhite: Color get() = p.surface

    /**
     * Primary body text on grouped field cards ([SurfaceWhite] / [MmrGroupedFormCard]).
     *
     * Use this instead of raw [Reversal]: in **light** palettes [SurfaceWhite] is near-white and [Reversal] is dark ink;
     * in **dark** palettes [SurfaceWhite] is a dark fill and [Reversal] is light ink. A few call sites sit on the light
     * card while [canvasIsDark] is true (expressive Ocean/Midnight) — they must branch explicitly; do not assume
     * [Reversal] is always dark.
     */
    fun formCardPrimaryText(): Color =
        if (SurfaceWhite.mmrRelativeLuminance() >= 0.45f) {
            TextStrong
        } else {
            Reversal
        }

    val BorderMain: Color get() = p.borderMain

    /** Selected row fill on grouped lists (safe when list and surface match, e.g. OLED). */
    val RowSelectionFill: Color get() = p.active.copy(alpha = 0.16f)

    /** Soft gray fill for settings subsection headers (between screen bg and white rows). */
    val SettingsSectionHeaderFill: Color
        get() {
            val base = p.listBackground
            val edge = p.borderMain
            val mix = 0.42f
            return Color(
                red = base.red + (edge.red - base.red) * mix,
                green = base.green + (edge.green - base.green) * mix,
                blue = base.blue + (edge.blue - base.blue) * mix,
                alpha = 1f,
            )
        }
    val TransactionListBorder: Color get() = p.transactionListBorder
    val TextStrong: Color get() = p.textStrong
    val TextMedium: Color get() = p.textMedium
    val TextLight: Color get() = p.textLight
    val Reversal: Color get() = p.reversal
    val NavBackground: Color
        get() = expressiveNavBackground()
    val NavIcon: Color
        get() =
            if (canvasIsDark()) {
                MmrExpressiveDarkCanvasChrome.navIcon
            } else {
                p.navIcon
            }
    val NavIconSelected: Color get() = p.navIconSelected
    val ExpenseSetA: Color get() = p.expenseSetA
    val IncomeSetA: Color get() = p.incomeSetA
    val ExpenseSetB: Color get() = p.expenseSetB
    val IncomeSetB: Color get() = p.incomeSetB
    val ConfigBackground: Color get() = p.configBackground
    val ConfigTitle: Color get() = p.configTitle
    val ConfigLabel: Color get() = p.configLabel
    val ConfigValue: Color get() = p.configValue
    val AmountPositive: Color get() = p.amountPositive
    val AmountNegative: Color get() = p.amountNegative
    val ApprovalApproveBackground: Color get() = p.approvalApproveBackground
    val ApprovalRejectBackground: Color get() = p.approvalRejectBackground
    val ApprovalApproveIcon: Color get() = p.approvalApproveIcon
    val ApprovalRejectIcon: Color get() = p.approvalRejectIcon
    val CalendarExpense: Color get() = p.calendarExpense
    val CalendarSaturday: Color get() = p.calendarSaturday
    val CalendarSunday: Color get() = p.calendarSunday
    val CalendarSelectedDay: Color get() = p.calendarSelectedDay
    val CalendarCellBorder: Color get() = p.calendarCellBorder
    val MonthlyIncome: Color get() = p.monthlyIncome
    val MonthlyExpense: Color get() = p.monthlyExpense
    val WeekHighlight: Color get() = p.weekHighlight
    val DialogHeader: Color get() = p.dialogHeader
    val DialogAccent: Color get() = p.dialogAccent
    val ChartLinePrimary: Color get() = p.chartLinePrimary
    val ChartPieFallback: Color get() = p.chartPieFallback
    val HomeSummaryAccent: Color get() = p.homeSummaryAccent

    fun expenseColor(scheme: MmrAmountColorScheme): Color = p.expenseColor(scheme)

    fun incomeColor(scheme: MmrAmountColorScheme): Color = p.incomeColor(scheme)
}
