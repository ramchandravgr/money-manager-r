package com.mmr.app.ui.theme

import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
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

@Composable
fun MoneyInboxTheme(
    appColorTheme: MmrAppColorTheme = MmrAppColorTheme.CLASSIC_CORAL,
    @Suppress("UNUSED_PARAMETER")
    amountColorScheme: MmrAmountColorScheme = MmrAmountColorScheme.SET_B,
    content: @Composable () -> Unit,
) {
    val palette =
        remember(appColorTheme) {
            MmrThemePresets.resolve(appColorTheme, MmrThemeAppearance.LIGHT)
        }
    SideEffect {
        MmrBrandColorState.palette = palette
    }
    val activePalette by MmrBrandColorState.paletteState
    val colorScheme = remember(activePalette) { activePalette.toMaterialLight() }

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

    CompositionLocalProvider(LocalMmrAmountColorScheme provides amountColorScheme) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = MmrShapes,
            content = content,
        )
    }
}
