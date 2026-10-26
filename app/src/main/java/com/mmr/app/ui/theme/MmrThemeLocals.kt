package com.mmr.app.ui.theme

import androidx.compose.runtime.compositionLocalOf
import com.mmr.core.model.settings.MmrAmountColorScheme

val LocalMmrAmountColorScheme =
    compositionLocalOf { MmrAmountColorScheme.SET_B }

/** Resolved light / dark / OLED appearance for the current [MoneyInboxTheme]. */
val LocalMmrThemeAppearance =
    compositionLocalOf { MmrThemeAppearance.LIGHT }
