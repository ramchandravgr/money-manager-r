package com.rrparallelstudio.miapp.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.hypot

private data class PeriodNavTarget(
    val key: Any,
    /** +1 = later period (enter from right), −1 = earlier period (enter from left). */
    val direction: Int,
)

/**
 * Swipeable period body: finger tracking while dragging, then period change when released past threshold.
 * Uses horizontal-dominance slop so vertical lists (e.g. stats category detail) scroll without jitter.
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
    var suppressContentSlide by remember { mutableStateOf(false) }
    val density = LocalDensity.current

    LaunchedEffect(periodKey) {
        dragOffset = 0f
        suppressContentSlide = false
        gestureLocked = false
    }

    LaunchedEffect(gestureLocked) {
        if (!gestureLocked) return@LaunchedEffect
        delay(700)
        if (gestureLocked) {
            dragOffset = 0f
            suppressContentSlide = false
            gestureLocked = false
        }
    }

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
                        val touchSlop = viewConfiguration.touchSlop
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            var horizontalPeriodDrag = false
                            var accumulatedX = 0f
                            var accumulatedY = 0f

                            while (true) {
                                val event = awaitPointerEvent()
                                val change =
                                    event.changes.firstOrNull { it.id == down.id }
                                        ?: break
                                if (!change.pressed) {
                                    if (horizontalPeriodDrag) {
                                        scope.launch {
                                            finishPeriodDrag(
                                                dragOffset = { dragOffset },
                                                setDragOffset = { dragOffset = it },
                                                setNavDirection = { navDirection = it },
                                                setSuppressContentSlide = { suppressContentSlide = it },
                                                setGestureLocked = { gestureLocked = it },
                                                width = width,
                                                threshold = threshold,
                                                onPrevious = onPrevious,
                                                onNext = onNext,
                                            )
                                        }
                                    }
                                    break
                                }

                                val dx = change.positionChange().x
                                val dy = change.positionChange().y

                                if (!horizontalPeriodDrag) {
                                    accumulatedX += dx
                                    accumulatedY += dy
                                    val distance = hypot(accumulatedX, accumulatedY)
                                    if (distance > touchSlop) {
                                        if (abs(accumulatedX) < abs(accumulatedY) * 1.35f) {
                                            break
                                        }
                                        horizontalPeriodDrag = true
                                    }
                                }

                                if (horizontalPeriodDrag) {
                                    change.consume()
                                    dragOffset =
                                        (dragOffset + dx).coerceIn(-width * 0.4f, width * 0.4f)
                                }
                            }
                        }
                    },
        ) {
            AnimatedContent(
                targetState = navTarget,
                contentKey = { it.key },
                transitionSpec = {
                    if (suppressContentSlide) {
                        return@AnimatedContent EnterTransition.None togetherWith ExitTransition.None
                    }
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
            ) { _ ->
                Box(Modifier.fillMaxSize()) {
                    content()
                }
            }
        }
    }
}

private suspend fun finishPeriodDrag(
    dragOffset: () -> Float,
    setDragOffset: (Float) -> Unit,
    setNavDirection: (Int) -> Unit,
    setSuppressContentSlide: (Boolean) -> Unit,
    setGestureLocked: (Boolean) -> Unit,
    width: Float,
    threshold: Float,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    val offset = dragOffset()
    when {
        offset > threshold -> {
            setGestureLocked(true)
            setNavDirection(-1)
            setSuppressContentSlide(true)
            animate(
                initialValue = offset,
                targetValue = width,
                animationSpec = tween(140, easing = LinearEasing),
            ) { value, _ -> setDragOffset(value) }
            onPrevious()
        }
        offset < -threshold -> {
            setGestureLocked(true)
            setNavDirection(1)
            setSuppressContentSlide(true)
            animate(
                initialValue = offset,
                targetValue = -width,
                animationSpec = tween(140, easing = LinearEasing),
            ) { value, _ -> setDragOffset(value) }
            onNext()
        }
        else -> {
            val start = offset
            if (abs(start) > 0.5f) {
                animate(
                    initialValue = start,
                    targetValue = 0f,
                    animationSpec = tween(160, easing = LinearEasing),
                ) { value, _ -> setDragOffset(value) }
            } else {
                setDragOffset(0f)
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
