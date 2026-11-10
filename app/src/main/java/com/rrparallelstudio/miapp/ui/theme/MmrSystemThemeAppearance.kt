package com.rrparallelstudio.miapp.ui.theme

import android.content.Context
import android.content.res.Configuration
import com.rrparallelstudio.miapp.core.model.settings.MmrThemeMode

/** Resolves light/dark brand palettes from user preference and/or system night mode. */
object MmrSystemThemeAppearance {
    fun fromContext(context: Context): MmrThemeAppearance = fromUiMode(context.resources.configuration.uiMode)

    fun resolve(
        themeMode: MmrThemeMode,
        context: Context,
    ): MmrThemeAppearance = resolve(themeMode, isSystemDarkTheme(context))

    fun resolve(
        themeMode: MmrThemeMode,
        systemInDarkTheme: Boolean,
    ): MmrThemeAppearance =
        when (themeMode) {
            MmrThemeMode.LIGHT -> MmrThemeAppearance.LIGHT
            MmrThemeMode.DARK -> MmrThemeAppearance.DARK
            MmrThemeMode.OLED -> MmrThemeAppearance.OLED
            MmrThemeMode.SYSTEM ->
                if (systemInDarkTheme) {
                    MmrThemeAppearance.DARK
                } else {
                    MmrThemeAppearance.LIGHT
                }
        }

    fun isDarkTheme(appearance: MmrThemeAppearance): Boolean =
        appearance == MmrThemeAppearance.DARK || appearance == MmrThemeAppearance.OLED

    fun isSystemDarkTheme(context: Context): Boolean =
        fromUiMode(context.resources.configuration.uiMode) == MmrThemeAppearance.DARK

    fun fromUiMode(uiMode: Int): MmrThemeAppearance {
        val night = uiMode and Configuration.UI_MODE_NIGHT_MASK
        return if (night == Configuration.UI_MODE_NIGHT_YES) {
            MmrThemeAppearance.DARK
        } else {
            MmrThemeAppearance.LIGHT
        }
    }
}
