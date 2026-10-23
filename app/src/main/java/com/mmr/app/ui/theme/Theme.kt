package com.mmr.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val MmrLightColorScheme =
    lightColorScheme(
        primary = MmrMmColors.Active,
        onPrimary = MmrMmColors.SurfaceWhite,
        primaryContainer = MmrMmColors.Header,
        onPrimaryContainer = MmrMmColors.HeaderIcon,
        secondary = MmrMmColors.Header,
        background = MmrMmColors.ListBackground,
        onBackground = MmrMmColors.Reversal,
        surface = MmrMmColors.SurfaceWhite,
        onSurface = MmrMmColors.Reversal,
        surfaceVariant = MmrMmColors.NavBackground,
        onSurfaceVariant = MmrMmColors.TextMedium,
        outline = MmrMmColors.BorderMain,
        outlineVariant = MmrMmColors.BorderMain,
    )

private val MmrDarkColorScheme =
    darkColorScheme(
        primary = MmrMmColors.Active,
        onPrimary = MmrMmColors.SurfaceWhite,
        primaryContainer = MmrMmColors.HeaderSelection,
        background = MmrMmColors.ConfigBackground,
        onBackground = MmrMmColors.ConfigTitle,
        surface = MmrMmColors.ConfigBackground,
        onSurface = MmrMmColors.ConfigTitle,
    )

@Composable
fun MoneyManagerRTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme =
        when {
            dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                val context = LocalContext.current
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            }
            darkTheme -> MmrDarkColorScheme
            else -> MmrLightColorScheme
        }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            window.statusBarColor = MmrMmColors.Header.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = MmrShapes,
        content = content,
    )
}
