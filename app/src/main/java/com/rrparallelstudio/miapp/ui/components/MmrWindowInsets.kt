package com.rrparallelstudio.miapp.ui.components

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
 * Use on inner [androidx.compose.material3.Scaffold]s hosted inside [com.rrparallelstudio.miapp.navigation.MmrMainNavigationScaffold].
 * The outer main nav scaffold does not apply status-bar insets on portrait bottom tabs; brand headers
 * ([MmrHomeTopBar], [MmrHeaderBar], etc.) call [androidx.compose.foundation.layout.statusBarsPadding] once.
 * In navigation-rail (medium+ width) mode, the rail uses Material window insets; main content gets horizontal
 * navigation-bar padding only (see [com.rrparallelstudio.miapp.navigation.MmrMainNavigationScaffold]).
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
    extraTopPadding: Dp = 12.dp,
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
                .padding(top = extraTopPadding, bottom = extraBottomPadding),
        verticalArrangement = verticalArrangement,
        content = content,
    )
}
