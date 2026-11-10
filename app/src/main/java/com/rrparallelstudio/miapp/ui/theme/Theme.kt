package com.rrparallelstudio.miapp.ui.theme

import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.rrparallelstudio.miapp.core.model.settings.MmrAmountColorScheme
import com.rrparallelstudio.miapp.core.model.settings.MmrAppColorTheme
import com.rrparallelstudio.miapp.core.model.settings.MmrThemeMode

@Composable
fun MoneyInboxTheme(
    appColorTheme: MmrAppColorTheme = MmrAppColorTheme.ROSE_GOLD,
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
            MmrThemePresets.resolve(appColorTheme.forUi(), appearance)
        }
    SideEffect {
        MmrBrandColorState.palette = palette
        MmrExpressiveThemeState.appColorTheme = appColorTheme.forUi()
        MmrExpressiveThemeState.appearance = appearance
    }
    val activePalette by MmrBrandColorState.paletteState
    val colorScheme =
        remember(activePalette, darkTheme) {
            val base =
                if (darkTheme) {
                    activePalette.toMaterialDark()
                } else {
                    activePalette.toMaterialLight()
                }
            if (MmrExpressiveThemeFlags.ENABLED) {
                base.copy(background = Color.Transparent)
            } else {
                base
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
        Box(modifier = Modifier.fillMaxSize()) {
            MmrExpressiveScreenBackground(Modifier.fillMaxSize())
            MaterialTheme(
                colorScheme = colorScheme,
                typography = Typography,
                shapes = MmrShapes,
                content = content,
            )
        }
    }
}
