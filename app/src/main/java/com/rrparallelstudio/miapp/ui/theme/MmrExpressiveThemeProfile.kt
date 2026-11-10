package com.rrparallelstudio.miapp.ui.theme

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import com.rrparallelstudio.miapp.R
import com.rrparallelstudio.miapp.core.model.settings.MmrAppColorTheme

/**
 * Per-theme extras on top of [MmrBrandPalette]: scene fill and whether the painted
 * canvas is dark even in Light appearance (so chrome/chips/dates share one path).
 */
data class MmrExpressiveThemeProfile(
    val darkCanvasInLightAppearance: Boolean = false,
    @DrawableRes val photoFillResId: Int? = null,
)

/** Shared tokens when list chrome sits on a dark painted canvas. */
object MmrExpressiveDarkCanvasChrome {
    val onCanvas: Color = Color(0xFFF5F7FB)
    val onCanvasMuted: Color = Color(0xB8F5F7FB)
    val band: Color = Color(0xE6101824)
    val nav: Color = Color(0xF2101824)
    val navIcon: Color = Color(0xC9F5F7FB)
    val chipFill: Color = Color.White.copy(alpha = 0.10f)
    val chipBorder: Color = Color.White.copy(alpha = 0.28f)
    val chipText: Color = Color.White
}

object MmrExpressiveThemeProfiles {
    fun of(theme: MmrAppColorTheme): MmrExpressiveThemeProfile =
        when (theme.forUi()) {
            MmrAppColorTheme.OCEAN,
            MmrAppColorTheme.CLASSIC_CORAL,
            ->
                MmrExpressiveThemeProfile(
                    darkCanvasInLightAppearance = true,
                    photoFillResId = R.drawable.mmr_ocean_surface,
                )
            MmrAppColorTheme.MIDNIGHT ->
                MmrExpressiveThemeProfile(darkCanvasInLightAppearance = true)
            MmrAppColorTheme.FOREST,
            MmrAppColorTheme.SLATE,
            MmrAppColorTheme.ROYAL,
            MmrAppColorTheme.SUNSET,
            MmrAppColorTheme.ROSE_GOLD,
            MmrAppColorTheme.MINT,
            MmrAppColorTheme.CHARCOAL_GOLD,
            -> MmrExpressiveThemeProfile()
        }

    fun current(): MmrExpressiveThemeProfile = of(MmrExpressiveThemeState.appColorTheme)

    fun canvasIsDark(
        theme: MmrAppColorTheme = MmrExpressiveThemeState.appColorTheme,
        appearance: MmrThemeAppearance = MmrExpressiveThemeState.appearance,
    ): Boolean {
        if (appearance != MmrThemeAppearance.LIGHT) return true
        return of(theme).darkCanvasInLightAppearance
    }
}
