package com.mmr.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Velocity

/**
 * Workaround for Material3 [androidx.compose.material3.ModalBottomSheet] jitter when a
 * [androidx.compose.foundation.lazy.LazyColumn] (or [androidx.compose.foundation.verticalScroll])
 * is flung upward at the scroll boundary — leftover velocity reaches the sheet drag gesture.
 *
 * Apply on the scrollable modifier chain **before** `verticalScroll` / `LazyColumn`.
 * Only consumes upward overscroll/fling so swipe-down-to-dismiss still works.
 */
@Composable
fun rememberMmrBottomSheetNestedScrollConnection(): NestedScrollConnection =
    remember {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset = if (available.y < 0f) available else Offset.Zero

            override suspend fun onPostFling(
                consumed: Velocity,
                available: Velocity,
            ): Velocity = if (available.y < 0f) available else Velocity.Zero
        }
    }
