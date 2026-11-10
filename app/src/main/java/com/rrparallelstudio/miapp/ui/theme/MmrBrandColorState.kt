package com.rrparallelstudio.miapp.ui.theme

import androidx.compose.runtime.mutableStateOf
import com.rrparallelstudio.miapp.core.model.settings.MmrAppColorTheme

object MmrBrandColorState {
    val paletteState =
        mutableStateOf(MmrThemePresets.light(MmrAppColorTheme.ROSE_GOLD))

    var palette: MmrBrandPalette
        get() = paletteState.value
        internal set(value) {
            paletteState.value = value
        }
}
