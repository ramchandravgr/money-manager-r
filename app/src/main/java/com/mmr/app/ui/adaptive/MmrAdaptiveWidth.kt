package com.mmr.app.ui.adaptive

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp

@Composable
fun Modifier.mmrAdaptiveContentWidth(): Modifier {
    val maxWidth = mmrAdaptiveMaxContentWidth()
    return if (maxWidth == Dp.Unspecified) {
        fillMaxWidth()
    } else {
        fillMaxWidth().widthIn(max = maxWidth)
    }
}

@Composable
fun MmrAdaptiveWidthBox(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Box(
            modifier = Modifier.mmrAdaptiveContentWidth(),
            content = content,
        )
    }
}
