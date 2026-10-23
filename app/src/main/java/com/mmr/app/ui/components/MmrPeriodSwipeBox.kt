package com.mmr.app.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.abs

private data class PeriodNavTarget(
    val key: Any,
    /** +1 = later period (enter from right), −1 = earlier period (enter from left). */
    val direction: Int,
)

/**
 * Swipeable period body: finger tracking while dragging, then one directional slide when the period changes.
 */
@Composable
fun MmrPeriodSwipeBox(
    periodKey: Any,
    enabled: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    var navDirection by remember { mutableIntStateOf(1) }
    var lastPeriodKey by remember { mutableStateOf(periodKey) }
    if (periodKey != lastPeriodKey) {
        navDirection = comparePeriodKeys(lastPeriodKey, periodKey)
        lastPeriodKey = periodKey
    }

    val navTarget =
        remember(periodKey, navDirection) {
            PeriodNavTarget(periodKey, navDirection)
        }

    var dragOffset by remember { mutableFloatStateOf(0f) }
    val scope = rememberCoroutineScope()
    var gestureLocked by remember { mutableStateOf(false) }
    val density = LocalDensity.current

    BoxWithConstraints(modifier = modifier.clipToBounds()) {
        val width = constraints.maxWidth.toFloat().coerceAtLeast(1f)
        val minThresholdPx = with(density) { 56.dp.toPx() }
        val threshold = (width * 0.22f).coerceAtLeast(minThresholdPx)

        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .graphicsLayer { translationX = dragOffset }
                    .pointerInput(enabled, width, gestureLocked, threshold) {
                        if (!enabled || gestureLocked) return@pointerInput
                        detectHorizontalDragGestures(
                            onHorizontalDrag = { _, amount ->
                                dragOffset = (dragOffset + amount).coerceIn(-width * 0.4f, width * 0.4f)
                            },
                            onDragEnd = {
                                scope.launch {
                                    when {
                                        dragOffset > threshold -> {
                                            gestureLocked = true
                                            dragOffset = 0f
                                            navDirection = -1
                                            onPrevious()
                                            gestureLocked = false
                                        }
                                        dragOffset < -threshold -> {
                                            gestureLocked = true
                                            dragOffset = 0f
                                            navDirection = 1
                                            onNext()
                                            gestureLocked = false
                                        }
                                        else -> {
                                            val start = dragOffset
                                            if (abs(start) > 0.5f) {
                                                animate(
                                                    initialValue = start,
                                                    targetValue = 0f,
                                                    animationSpec = tween(160, easing = LinearEasing),
                                                ) { value, _ -> dragOffset = value }
                                            } else {
                                                dragOffset = 0f
                                            }
                                        }
                                    }
                                }
                            },
                            onDragCancel = {
                                scope.launch {
                                    val start = dragOffset
                                    animate(
                                        initialValue = start,
                                        targetValue = 0f,
                                        animationSpec = tween(160, easing = LinearEasing),
                                    ) { value, _ -> dragOffset = value }
                                }
                            },
                        )
                    },
        ) {
            AnimatedContent(
                targetState = navTarget,
                contentKey = { it.key },
                transitionSpec = {
                    val towardNext = targetState.direction >= 0
                    slideInHorizontally(
                        animationSpec = tween(280, easing = LinearEasing),
                        initialOffsetX = { full -> if (towardNext) full else -full },
                    ) togetherWith
                        slideOutHorizontally(
                            animationSpec = tween(280, easing = LinearEasing),
                            targetOffsetX = { full -> if (towardNext) -full else full },
                        )
                },
                label = "mmrPeriodSwipe",
            ) {
                content()
            }
        }
    }
}

private fun comparePeriodKeys(
    old: Any,
    new: Any,
): Int {
    if (old is Number && new is Number) {
        return when (new.toDouble().compareTo(old.toDouble())) {
            1 -> 1
            -1 -> -1
            else -> 1
        }
    }
    return 1
}
