package com.rrparallelstudio.miapp.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource

@Composable
fun MmrExpressiveScreenBackground(modifier: Modifier = Modifier) {
    if (!MmrExpressiveThemeFlags.ENABLED) {
        Box(modifier.background(MmrBrandColors.ListBackground))
        return
    }
    val theme = MmrExpressiveThemeState.appColorTheme.forUi()
    val appearance = LocalMmrThemeAppearance.current
    val palette = MmrBrandColorState.palette
    val profile = MmrExpressiveThemeProfiles.of(theme)
    Box(modifier.fillMaxSize()) {
        val photoRes = profile.photoFillResId
        if (photoRes != null) {
            Image(
                painter = painterResource(photoRes),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
            val wash =
                when (appearance) {
                    MmrThemeAppearance.LIGHT -> Color.White.copy(alpha = 0.12f)
                    MmrThemeAppearance.DARK -> Color(0xFF062A36).copy(alpha = 0.38f)
                    MmrThemeAppearance.OLED -> Color.Black.copy(alpha = 0.55f)
                }
            Box(Modifier.fillMaxSize().background(wash))
        } else {
            Canvas(Modifier.fillMaxSize()) {
                MmrExpressiveScreenArt.draw(
                    drawScope = this,
                    theme = theme,
                    palette = palette,
                    appearance = appearance,
                )
            }
        }
    }
}
