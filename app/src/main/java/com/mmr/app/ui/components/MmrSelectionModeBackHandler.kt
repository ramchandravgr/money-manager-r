package com.mmr.app.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable

/** Clears list selection instead of leaving the screen when the user presses system back. */
@Composable
fun MmrSelectionModeBackHandler(
    enabled: Boolean,
    onExitSelectionMode: () -> Unit,
) {
    BackHandler(enabled = enabled, onBack = onExitSelectionMode)
}
