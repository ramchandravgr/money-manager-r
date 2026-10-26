package com.mmr.app.ui.theme

import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.mmr.core.model.settings.MmrAmountColorScheme
import com.mmr.core.model.settings.MmrAppColorTheme
import com.mmr.core.model.settings.MmrThemeMode

@Composable
fun MoneyInboxTheme(
    appColorTheme: MmrAppColorTheme = MmrAppColorTheme.CLASSIC_CORAL,
    themeMode: MmrThemeMode = MmrThemeMode.SYSTEM,
    @Suppress("UNUSED_PARAMETER")
    amountColorScheme: MmrAmountColorScheme = MmrAmountColorScheme.SET_B,
    content: @Composable () -> Unit,
) {
    val systemDark = isSystemInDarkTheme()
    val appearance =
        remember(themeMode, systemDark) {
            MmrSystemThemeAppearance.resolve(themeMode, systemDark)
        }
    val darkTheme =
        remember(appearance) {
            MmrSystemThemeAppearance.isDarkTheme(appearance)
        }
    val palette =
        remember(appColorTheme, appearance) {
            MmrThemePresets.resolve(appColorTheme, appearance)
        }
    SideEffect {
        MmrBrandColorState.palette = palette
    }
    val activePalette by MmrBrandColorState.paletteState
    val colorScheme =
        remember(activePalette, darkTheme) {
            if (darkTheme) {
                activePalette.toMaterialDark()
            } else {
                activePalette.toMaterialLight()
            }
        }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context as? ComponentActivity ?: return@SideEffect
            val scrim = activePalette.statusBarColor().toArgb()
            activity.enableEdgeToEdge(
                statusBarStyle = SystemBarStyle.dark(scrim),
            )
            WindowCompat.getInsetsController(activity.window, view).isAppearanceLightStatusBars = false
        }
    }

    CompositionLocalProvider(
        LocalMmrAmountColorScheme provides amountColorScheme,
        LocalMmrThemeAppearance provides appearance,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = MmrShapes,
            content = content,
        )
    }
}
