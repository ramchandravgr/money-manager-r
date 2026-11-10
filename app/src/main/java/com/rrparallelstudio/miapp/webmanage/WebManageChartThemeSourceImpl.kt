package com.rrparallelstudio.miapp.webmanage

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.rrparallelstudio.miapp.core.data.preferences.AppDisplaySettingsStore
import com.rrparallelstudio.miapp.core.webmanage.WebManageChartTheme
import com.rrparallelstudio.miapp.core.webmanage.WebManageChartThemeSource
import com.rrparallelstudio.miapp.core.webmanage.WebManageDisplaySnapshot
import com.rrparallelstudio.miapp.ui.theme.MmrDisplayTheme
import com.rrparallelstudio.miapp.ui.theme.MmrThemeAppearance
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WebManageChartThemeSourceImpl
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val displaySettingsStore: AppDisplaySettingsStore,
    ) : WebManageChartThemeSource {
        override fun snapshot(): WebManageDisplaySnapshot {
            val display = displaySettingsStore.snapshot()
            val appearance = MmrDisplayTheme.appearance(context, display)
            val palette = MmrDisplayTheme.palette(display, appearance)
            return WebManageDisplaySnapshot(
                chartTheme =
                    WebManageChartTheme(
                        pieBorderColor = palette.surface.toRgbHex(),
                        pieBackgroundColor = palette.surface.toRgbHex(),
                        pieTextColor = palette.reversal.toRgbHex(),
                    ),
                webAppearance = appearance.toWebKey(),
            )
        }

        private fun MmrThemeAppearance.toWebKey(): String =
            when (this) {
                MmrThemeAppearance.LIGHT -> "light"
                MmrThemeAppearance.DARK -> "dark"
                MmrThemeAppearance.OLED -> "oled"
            }

        private fun Color.toRgbHex(): String {
            val rgb = toArgb() and 0xFFFFFF
            return String.format("#%06X", rgb)
        }
    }
