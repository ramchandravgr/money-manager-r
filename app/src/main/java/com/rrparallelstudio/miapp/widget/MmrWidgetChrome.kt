package com.rrparallelstudio.miapp.widget

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.glance.unit.ColorProvider
import com.rrparallelstudio.miapp.core.data.preferences.AppDisplaySettingsSnapshot
import com.rrparallelstudio.miapp.ui.theme.MmrDisplayTheme
import com.rrparallelstudio.miapp.ui.theme.MmrThemeAppearance

/** Widget surface colors aligned with in-app Appearance (not launcher Glance blue). */
data class MmrWidgetChromeColors(
    val backgroundArgb: Int,
    val onSurfaceArgb: Int,
    val onSurfaceVariantArgb: Int,
    val separatorArgb: Int,
)

object MmrWidgetChrome {
    fun resolve(
        context: Context,
        display: AppDisplaySettingsSnapshot,
    ): MmrWidgetChromeColors {
        val appearance = MmrDisplayTheme.appearance(context, display)
        val palette = MmrDisplayTheme.palette(display, appearance)
        val background =
            when (appearance) {
                MmrThemeAppearance.LIGHT -> palette.surface
                MmrThemeAppearance.DARK,
                MmrThemeAppearance.OLED,
                -> palette.listBackground
            }
        return MmrWidgetChromeColors(
            backgroundArgb = background.toArgb(),
            onSurfaceArgb = palette.reversal.toArgb(),
            onSurfaceVariantArgb = palette.textMedium.toArgb(),
            separatorArgb = palette.borderMain.toArgb(),
        )
    }
}

fun MmrWidgetChromeColors.background(): ColorProvider = ColorProvider(Color(backgroundArgb))

fun MmrWidgetChromeColors.onSurface(): ColorProvider = ColorProvider(Color(onSurfaceArgb))

fun MmrWidgetChromeColors.onSurfaceVariant(): ColorProvider = ColorProvider(Color(onSurfaceVariantArgb))

fun MmrWidgetChromeColors.separator(): ColorProvider = ColorProvider(Color(separatorArgb))
