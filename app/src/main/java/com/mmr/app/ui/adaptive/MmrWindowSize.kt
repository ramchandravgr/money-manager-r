package com.mmr.app.ui.adaptive

import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp.Companion.Unspecified

val LocalMmrWindowSizeClass = staticCompositionLocalOf<WindowSizeClass> {
    error("LocalMmrWindowSizeClass not provided")
}

@Composable
fun currentMmrWidthSizeClass(): WindowWidthSizeClass =
    LocalMmrWindowSizeClass.current.widthSizeClass

fun WindowWidthSizeClass.isCompactWidth(): Boolean =
    this == WindowWidthSizeClass.Compact

@Composable
fun mmrScreenHorizontalPadding(): Dp = when (currentMmrWidthSizeClass()) {
    WindowWidthSizeClass.Compact -> 16.dp
    WindowWidthSizeClass.Medium -> 24.dp
    WindowWidthSizeClass.Expanded -> 32.dp
    else -> 16.dp
}

@Composable
fun mmrAdaptiveMaxContentWidth(): Dp = when (currentMmrWidthSizeClass()) {
    WindowWidthSizeClass.Expanded -> 960.dp
    WindowWidthSizeClass.Medium -> 840.dp
    else -> Unspecified
}
