package com.mmr.app.webmanage

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.mmr.app.ui.theme.MmrSystemThemeAppearance
import com.mmr.app.ui.theme.MmrThemeAppearance
import com.mmr.app.ui.theme.MmrThemePresets
import com.mmr.core.data.preferences.AppDisplaySettingsStore
import com.mmr.core.webmanage.WebManageChartTheme
import com.mmr.core.webmanage.WebManageChartThemeSource
import com.mmr.core.webmanage.WebManageDisplaySnapshot
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
            val appearance =
                MmrSystemThemeAppearance.resolve(display.themeMode, context)
            val palette = MmrThemePresets.resolve(display.appColorTheme, appearance)
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
