package com.rrparallelstudio.miapp.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.findRootCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
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

/** Y of the form's visible bottom in root coordinates, after keyboard overlap is removed. */
val LocalMmrFormContentBottomY = compositionLocalOf { 0f }

/** Bottom IME overlap for this form viewport (excludes main nav / gesture area below the scaffold). */
val LocalMmrFormImeOverlap = compositionLocalOf { 0.dp }

/** Extra scroll padding at the bottom of [MmrImeAwareScrollColumn] (included in picker fill height). */
val LocalMmrFormExtraBottomPadding = compositionLocalOf { 0.dp }

/**
 * Pads only the part of the IME that actually covers this box.
 * Full [androidx.compose.foundation.layout.imePadding] also reserves the main bottom bar and
 * gesture nav (already outside this box), which draws as an empty band above the keyboard.
 */
@Composable
fun MmrFormViewport(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    val imeBottom = WindowInsets.ime.getBottom(density)
    var overlapPx by remember { mutableIntStateOf(0) }
    var contentBottomY by remember { mutableFloatStateOf(0f) }
    val overlapDp = with(density) { overlapPx.toDp() }
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .onGloballyPositioned { coords ->
                    val root = coords.findRootCoordinates()
                    val top = coords.positionInRoot().y
                    val bottom = top + coords.size.height
                    val gapBelow = (root.size.height - bottom).toInt().coerceAtLeast(0)
                    val nextOverlap = (imeBottom - gapBelow).coerceAtLeast(0)
                    if (nextOverlap != overlapPx) overlapPx = nextOverlap
                    val nextBottom = bottom - nextOverlap
                    if (nextBottom != contentBottomY) contentBottomY = nextBottom
                },
    ) {
        androidx.compose.runtime.CompositionLocalProvider(
            LocalMmrFormContentBottomY provides contentBottomY,
            LocalMmrFormImeOverlap provides overlapDp,
        ) {
            content()
        }
    }
}

/**
 * Stretch this node so its bottom, plus [endInset], lands on [LocalMmrFormContentBottomY]
 * (the form edge directly above the Ledger bar).
 */
@Composable
fun Modifier.mmrFillDownToFormBottom(
    minHeight: Dp = 160.dp,
    endInset: Dp = 0.dp,
): Modifier {
    val targetBottom = LocalMmrFormContentBottomY.current
    val extraBottom = LocalMmrFormExtraBottomPadding.current
    val density = LocalDensity.current
    var topInRoot by remember { mutableFloatStateOf(0f) }
    val height =
        if (targetBottom <= 0f || topInRoot <= 0f) {
            minHeight
        } else {
            val minPx = with(density) { minHeight.toPx() }
            val insetPx = with(density) { (endInset - extraBottom).toPx() }
            val px = (targetBottom - topInRoot - insetPx).coerceAtLeast(minPx)
            with(density) { px.toDp() }
        }
    return this
        .onGloballyPositioned { coords ->
            val top = coords.positionInRoot().y
            if (top != topInRoot) topInRoot = top
        }.height(height)
}

@Composable
fun MmrImeAwareScrollColumn(
    modifier: Modifier = Modifier,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    horizontalPadding: Dp = 0.dp,
    extraTopPadding: Dp = 12.dp,
    extraBottomPadding: Dp = 24.dp,
    /** When false, leftover picker flings cannot scroll the form and reveal a blank band. */
    scrollEnabled: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    MmrFormViewport(modifier = modifier) {
        val imeOverlap = LocalMmrFormImeOverlap.current
        val scrollBottom = imeOverlap + extraBottomPadding
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            androidx.compose.runtime.CompositionLocalProvider(
                LocalMmrFormExtraBottomPadding provides extraBottomPadding,
            ) {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = maxHeight)
                            .verticalScroll(rememberScrollState(), enabled = scrollEnabled)
                            .padding(horizontal = horizontalPadding)
                            .padding(top = extraTopPadding, bottom = scrollBottom),
                    verticalArrangement = verticalArrangement,
                    content = content,
                )
            }
        }
    }
}
