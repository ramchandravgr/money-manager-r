package com.mmr.app.ui.components

import androidx.compose.foundation.layout.WindowInsets

/**
 * Use on inner [androidx.compose.material3.Scaffold]s hosted inside [com.mmr.app.navigation.MmrMainNavigationScaffold].
 * The outer scaffold does not apply status-bar insets; MM headers ([MmrMmHomeTopBar], [MmrMmHeaderBar], etc.)
 * call [androidx.compose.foundation.layout.statusBarsPadding] once so the indigo bar draws edge-to-edge.
 */
val MmrNestedScaffoldInsets: WindowInsets = WindowInsets(0, 0, 0, 0)
