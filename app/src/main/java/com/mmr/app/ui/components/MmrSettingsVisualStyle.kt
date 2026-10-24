package com.mmr.app.ui.components

import androidx.compose.runtime.compositionLocalOf

/**
 * Settings visual density. Sub-screens default to [Compact]; the main Settings hub stays [Standard].
 *
 * - **Standard** — bordered section bands ([MmrConfigSectionHeader]), intro panels, roomier gaps.
 * - **Compact** — small section labels, tighter padding (closer to system Settings / iOS).
 */
enum class MmrSettingsVisualStyle {
    Standard,
    Compact,
}

val LocalMmrSettingsVisualStyle =
    compositionLocalOf { MmrSettingsVisualStyle.Compact }
