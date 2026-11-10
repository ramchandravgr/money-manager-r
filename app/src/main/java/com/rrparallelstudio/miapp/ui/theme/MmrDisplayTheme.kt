package com.rrparallelstudio.miapp.ui.theme

import android.content.Context
import com.rrparallelstudio.miapp.core.data.preferences.AppDisplaySettingsSnapshot

/** Single palette/appearance resolve for widgets, Web Manage, and other non-Compose surfaces. */
object MmrDisplayTheme {
    fun appearance(
        context: Context,
        display: AppDisplaySettingsSnapshot,
    ): MmrThemeAppearance = MmrSystemThemeAppearance.resolve(display.themeMode, context)

    fun palette(
        display: AppDisplaySettingsSnapshot,
        appearance: MmrThemeAppearance,
    ): MmrBrandPalette = MmrThemePresets.resolve(display.appColorTheme.forUi(), appearance)

    fun palette(
        context: Context,
        display: AppDisplaySettingsSnapshot,
    ): MmrBrandPalette = palette(display, appearance(context, display))
}
