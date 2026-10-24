package com.mmr.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Use on inner [androidx.compose.material3.Scaffold]s hosted inside [com.mmr.app.navigation.MmrMainNavigationScaffold].
 * The outer scaffold does not apply status-bar insets; brand headers ([MmrHomeTopBar], [MmrHeaderBar], etc.)
 * call [androidx.compose.foundation.layout.statusBarsPadding] once so the indigo bar draws edge-to-edge.
 */
val MmrNestedScaffoldInsets: WindowInsets = WindowInsets(0, 0, 0, 0)

/**
 * Scrollable form body that stays above the IME (required with [androidx.activity.enableEdgeToEdge]).
 * Use on transaction / recurring / loan entry and settings form scaffolds.
 */
@Composable
fun MmrImeAwareScrollColumn(
    modifier: Modifier = Modifier,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    horizontalPadding: Dp = 0.dp,
    extraBottomPadding: Dp = 24.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = horizontalPadding)
                .padding(bottom = extraBottomPadding),
        verticalArrangement = verticalArrangement,
        content = content,
    )
}
