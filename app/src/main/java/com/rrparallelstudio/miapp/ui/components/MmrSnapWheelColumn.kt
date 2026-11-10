package com.rrparallelstudio.miapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rrparallelstudio.miapp.ui.theme.MmrBrandColors
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlin.math.roundToInt

/**
 * Single-column wheel with snap-to-row behavior (iOS-style picker column).
 */
@Composable
fun MmrSnapWheelColumn(
    items: List<String>,
    selectedIndex: Int,
    onSelectedIndexChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    itemHeight: Dp = 44.dp,
    visibleItemCount: Int = 5,
) {
    if (items.isEmpty()) return

    val density = LocalDensity.current
    val itemHeightPx = with(density) { itemHeight.roundToPx() }
    val safeIndex = selectedIndex.coerceIn(0, items.lastIndex)
    val listState =
        rememberLazyListState(
            initialFirstVisibleItemIndex = safeIndex.coerceAtLeast(0),
        )
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)

    LaunchedEffect(safeIndex, items.size) {
        val target = safeIndex.coerceIn(0, items.lastIndex)
        if (listState.firstVisibleItemIndex != target || listState.firstVisibleItemScrollOffset != 0) {
            listState.animateScrollToItem(target)
        }
    }

    LaunchedEffect(listState, items.size) {
        snapshotFlow {
            val idx = listState.firstVisibleItemIndex
            val offset = listState.firstVisibleItemScrollOffset
            if (listState.isScrollInProgress) null else snapIndex(idx, offset, itemHeightPx, items.lastIndex)
        }.distinctUntilChanged()
            .collect { snapped ->
                if (snapped != null && snapped != selectedIndex) {
                    onSelectedIndexChange(snapped)
                }
            }
    }

    Box(modifier = modifier) {
        LazyColumn(
            state = listState,
            flingBehavior = flingBehavior,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(itemHeight * visibleItemCount),
            contentPadding = PaddingValues(vertical = itemHeight * ((visibleItemCount - 1) / 2)),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            items(
                count = items.size,
                key = { items[it] },
            ) { index ->
                val selected = index == selectedIndex
                Box(
                    modifier =
                        Modifier
                            .height(itemHeight)
                            .fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = items[index],
                        fontSize = if (selected) 18.sp else 15.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        color =
                            if (selected) {
                                MmrBrandColors.Active
                            } else {
                                MmrBrandColors.TextMedium
                            },
                    )
                }
            }
        }
        Box(
            modifier =
                Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .height(itemHeight)
                    .background(MmrBrandColors.Active.copy(alpha = 0.07f)),
        )
    }
}

private fun snapIndex(
    firstVisible: Int,
    offsetPx: Int,
    itemHeightPx: Int,
    lastIndex: Int,
): Int {
    val centered = firstVisible + (offsetPx.toFloat() / itemHeightPx.toFloat()).roundToInt()
    return centered.coerceIn(0, lastIndex)
}
