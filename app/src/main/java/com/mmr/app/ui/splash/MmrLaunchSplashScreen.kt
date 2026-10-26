package com.mmr.app.ui.splash

import android.content.Context
import android.provider.Settings
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.mmr.app.ui.theme.MmrBrandColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val VIEWPORT = 108f
private val accentCoral = Color(0xFFFF6255)

private data class SplashBarSpec(
    val left: Float,
    val top: Float,
    val width: Float,
    val height: Float,
)

private val splashBars =
    listOf(
        SplashBarSpec(58f, 68f, 5f, 8f),
        SplashBarSpec(65f, 64f, 5f, 12f),
        SplashBarSpec(72f, 60f, 5f, 16f),
    )

internal fun isReduceMotionEnabled(context: Context): Boolean {
    val scale =
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        )
    return scale == 0f
}

@Composable
fun MmrLaunchSplashScreen(onFinished: () -> Unit) {
    val context = LocalContext.current
    val reduceMotion = remember { isReduceMotionEnabled(context) }
    val rotation = remember { Animatable(0f) }
    val barProgress =
        remember {
            List(splashBars.size) { Animatable(0f) }
        }
    val checkAlpha = remember { Animatable(0f) }
    val checkBlurDp = remember { Animatable(0f) }
    val density = LocalDensity.current

    LaunchedEffect(reduceMotion) {
        if (reduceMotion) {
            rotation.snapTo(0f)
            barProgress.forEach { it.snapTo(1f) }
            checkAlpha.snapTo(1f)
            checkBlurDp.snapTo(0f)
            delay(320)
            onFinished()
            return@LaunchedEffect
        }
        rotation.animateTo(
            targetValue = 360f,
            animationSpec = tween(durationMillis = 520, easing = FastOutSlowInEasing),
        )
        barProgress.forEachIndexed { index, anim ->
            launch {
                delay(index * 110L)
                anim.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 170, easing = FastOutSlowInEasing),
                )
            }
        }
        delay(splashBars.size * 110L + 200L)
        checkBlurDp.snapTo(14f)
        checkAlpha.animateTo(0.35f, tween(130))
        checkBlurDp.animateTo(5f, tween(160))
        checkAlpha.animateTo(1f, tween(220))
        checkBlurDp.animateTo(0f, tween(180))
        delay(240)
        onFinished()
    }

    val blurPx = with(density) { checkBlurDp.value.dp.toPx() }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(MmrBrandColors.Header),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier.size(220.dp),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.fillMaxWidth().fillMaxSize()) {
                val scale = size.minDimension / VIEWPORT
                val pivot = center
                rotate(degrees = rotation.value, pivot = pivot) {
                    drawLetterM(scale, Color.White)
                }
                drawBaseline(scale)
                splashBars.forEachIndexed { index, spec ->
                    drawAnimatedBar(scale, spec, barProgress[index].value)
                }
                if (checkAlpha.value > 0.01f) {
                    drawCheckBadge(scale, checkAlpha.value, blurPx)
                }
            }
        }
    }
}

private fun DrawScope.drawLetterM(
    scale: Float,
    color: Color,
) {
    val path =
        Path().apply {
            moveTo(36f * scale, 70f * scale)
            lineTo(36f * scale, 42f * scale)
            lineTo(54f * scale, 58f * scale)
            lineTo(72f * scale, 42f * scale)
            lineTo(72f * scale, 70f * scale)
        }
    drawPath(
        path = path,
        color = color,
        style =
            Stroke(
                width = 7f * scale,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
            ),
    )
}

private fun DrawScope.drawBaseline(scale: Float) {
    drawRect(
        color = accentCoral.copy(alpha = 0.55f),
        topLeft = Offset(57f * scale, 76f * scale),
        size = androidx.compose.ui.geometry.Size(22f * scale, 2f * scale),
    )
}

private fun DrawScope.drawAnimatedBar(
    scale: Float,
    spec: SplashBarSpec,
    progress: Float,
) {
    if (progress <= 0f) return
    val fullHeight = spec.height * scale
    val animatedHeight = fullHeight * progress
    val left = spec.left * scale
    val width = spec.width * scale
    val bottom = (spec.top + spec.height) * scale
    drawRect(
        color = accentCoral,
        topLeft = Offset(left, bottom - animatedHeight),
        size = androidx.compose.ui.geometry.Size(width, animatedHeight),
    )
}

/** Matches [ic_launcher_foreground] approve badge at viewport (78, 41). */
private fun DrawScope.drawCheckBadge(
    scale: Float,
    alpha: Float,
    blurPx: Float,
) {
    val center = Offset(78f * scale, 41f * scale)
    val radius = 7f * scale
    val circleColor = accentCoral.copy(alpha = alpha)
    if (blurPx > 0.5f) {
        val steps = 3
        for (i in 1..steps) {
            val spread = blurPx * (i.toFloat() / steps) * 0.35f
            drawCircle(
                color = circleColor.copy(alpha = alpha * 0.12f / i),
                radius = radius + spread,
                center = center,
            )
        }
    }
    drawCircle(color = circleColor, radius = radius, center = center)
    val check =
        Path().apply {
            moveTo(74.2f * scale, 41f * scale)
            lineTo(76.2f * scale, 43.1f * scale)
            lineTo(81.2f * scale, 38.1f * scale)
            lineTo(82.3f * scale, 39.2f * scale)
            lineTo(76.2f * scale, 45.3f * scale)
            lineTo(73.1f * scale, 42.2f * scale)
            close()
        }
    drawPath(check, Color.White.copy(alpha = alpha))
}
