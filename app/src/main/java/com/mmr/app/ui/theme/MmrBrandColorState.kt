package com.mmr.app.ui.theme

import androidx.compose.runtime.mutableStateOf
import com.mmr.core.model.settings.MmrAppColorTheme

object MmrBrandColorState {
    val paletteState =
        mutableStateOf(MmrThemePresets.light(MmrAppColorTheme.CLASSIC_CORAL))

    var palette: MmrBrandPalette
        get() = paletteState.value
        internal set(value) {
            paletteState.value = value
        }
}
