package com.rrparallelstudio.miapp.ui.theme

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import com.rrparallelstudio.miapp.core.model.settings.MmrAppColorTheme
import kotlin.math.min
import android.graphics.Paint as NativePaint

object MmrExpressiveScreenArt {
    fun draw(
        drawScope: DrawScope,
        theme: MmrAppColorTheme,
        palette: MmrBrandPalette,
        appearance: MmrThemeAppearance = MmrThemeAppearance.LIGHT,
    ) {
        with(drawScope) {
            when (theme.forUi()) {
                MmrAppColorTheme.CLASSIC_CORAL,
                MmrAppColorTheme.OCEAN,
                -> drawOcean(palette)
                MmrAppColorTheme.FOREST -> drawForest(palette)
                MmrAppColorTheme.SLATE -> drawSlate(palette)
                MmrAppColorTheme.ROYAL -> drawRoyal(palette)
                MmrAppColorTheme.SUNSET -> drawSunset(palette)
                MmrAppColorTheme.MIDNIGHT -> drawMidnight(palette, appearance)
                MmrAppColorTheme.ROSE_GOLD -> drawRoseGold(palette, appearance)
                MmrAppColorTheme.MINT -> drawMint(palette)
                MmrAppColorTheme.CHARCOAL_GOLD -> drawCharcoalGold(palette)
            }
        }
    }

    private fun DrawScope.drawOcean(p: MmrBrandPalette) {
        val w = size.width
        val h = size.height
        drawRect(
            brush =
                Brush.linearGradient(
                    0f to p.weekHighlight.copy(alpha = 0.85f),
                    0.45f to p.listBackground,
                    1f to p.weekHighlight.copy(alpha = 0.55f),
                    start = Offset(0f, 0f),
                    end = Offset(0f, h),
                ),
        )
        drawRect(
            brush =
                Brush.radialGradient(
                    0f to p.active.copy(alpha = 0.35f),
                    1f to Color.Transparent,
                    center = Offset(0f, 0f),
                    radius = w * 0.85f,
                ),
        )
        drawRect(
            brush =
                Brush.radialGradient(
                    0f to p.dialogAccent.copy(alpha = 0.15f),
                    1f to Color.Transparent,
                    center = Offset(w, h * 0.35f),
                    radius = w * 0.6f,
                ),
        )
        drawScrollWash(p.active.copy(alpha = 0.12f), p.header.copy(alpha = 0.06f))
    }

    private fun DrawScope.drawForest(p: MmrBrandPalette) {
        val w = size.width
        val h = size.height
        drawRect(
            brush =
                Brush.linearGradient(
                    0f to p.weekHighlight.copy(alpha = 0.7f),
                    0.5f to p.listBackground,
                    1f to p.weekHighlight.copy(alpha = 0.45f),
                    start = Offset(0f, 0f),
                    end = Offset(w * 0.3f, h),
                ),
        )
        drawRect(
            brush =
                Brush.radialGradient(
                    0f to p.active.copy(alpha = 0.22f),
                    1f to Color.Transparent,
                    center = Offset(w * 0.12f, h * 0.08f),
                    radius = w * 0.55f,
                ),
        )
        drawScrollWash(p.active.copy(alpha = 0.09f), p.header.copy(alpha = 0.05f))
        val fill = p.active.copy(alpha = 0.28f)
        val stroke = p.header.copy(alpha = 0.4f)
        drawLeaf(w * 0.12f, h * 0.22f, 1.1f, -28f, fill, stroke)
        drawLeaf(w * 0.82f, h * 0.28f, 0.85f, 18f, fill, stroke)
        drawLeaf(w * 0.4f, h * 0.42f, 1.25f, -8f, fill, stroke)
        drawLeaf(w * 0.16f, h * 0.58f, 0.75f, 38f, fill, stroke)
        drawLeaf(w * 0.88f, h * 0.54f, 0.95f, -18f, fill, stroke)
        drawLeaf(w * 0.58f, h * 0.7f, 0.9f, 12f, fill, stroke)
        drawLeaf(w * 0.24f, h * 0.82f, 1.05f, -12f, fill, stroke)
        drawLeaf(w * 0.74f, h * 0.86f, 0.7f, 28f, fill, stroke)
    }

    private fun DrawScope.drawSlate(p: MmrBrandPalette) {
        drawRect(color = p.listBackground.copy(blue = p.listBackground.blue + 0.02f))
        val step = 14f
        var y = 0f
        while (y < size.height) {
            drawLine(
                color = p.active.copy(alpha = 0.05f),
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1f,
            )
            y += step
        }
        var x = 0f
        while (x < size.width) {
            drawLine(
                color = p.active.copy(alpha = 0.05f),
                start = Offset(x, 0f),
                end = Offset(x, size.height),
                strokeWidth = 1f,
            )
            x += step
        }
        drawRect(
            brush =
                Brush.radialGradient(
                    0f to p.active.copy(alpha = 0.14f),
                    1f to Color.Transparent,
                    center = Offset(size.width * 0.85f, size.height * 0.05f),
                    radius = size.width * 0.55f,
                ),
        )
    }

    private fun DrawScope.drawRoyal(p: MmrBrandPalette) {
        val w = size.width
        val h = size.height
        drawRect(
            brush =
                Brush.verticalGradient(
                    0f to p.listBackground,
                    1f to p.weekHighlight.copy(alpha = 0.65f),
                ),
        )
        drawRect(
            brush =
                Brush.radialGradient(
                    0f to p.active.copy(alpha = 0.14f),
                    1f to Color.Transparent,
                    center = Offset(w * 0.5f, 0f),
                    radius = w * 0.8f,
                ),
        )
        var y = h * 0.22f
        while (y < h) {
            drawLine(
                color = Color(0xFFD4AF37).copy(alpha = 0.07f),
                start = Offset(0f, y),
                end = Offset(w, y),
                strokeWidth = 1f,
            )
            y += 24f
        }
        drawScrollWash(p.active.copy(alpha = 0.06f), Color(0xFFD4AF37).copy(alpha = 0.05f))
    }

    private fun DrawScope.drawSunset(p: MmrBrandPalette) {
        val w = size.width
        val h = size.height
        drawRect(
            brush =
                Brush.linearGradient(
                    0f to p.weekHighlight.copy(alpha = 0.9f),
                    0.42f to p.listBackground,
                    1f to p.weekHighlight.copy(alpha = 0.75f),
                    start = Offset(0f, 0f),
                    end = Offset(w, h),
                ),
        )
        val sunCenter = Offset(w * 0.5f, h * 0.16f)
        val sunR = min(w, h) * 0.16f
        drawCircle(
            brush =
                Brush.radialGradient(
                    0f to Color(0xFFFDE047).copy(alpha = 0.85f),
                    0.45f to Color(0xFFFBBF24).copy(alpha = 0.55f),
                    1f to Color(0xFFF97316).copy(alpha = 0f),
                    center = sunCenter,
                    radius = sunR * 2.4f,
                ),
            radius = sunR * 2.4f,
            center = sunCenter,
        )
        drawCircle(color = Color(0xFFFBBF24).copy(alpha = 0.92f), radius = sunR * 0.72f, center = sunCenter)
        drawCircle(
            color = Color(0xFFFEF9C3).copy(alpha = 0.55f),
            radius = sunR * 0.32f,
            center =
                sunCenter + Offset(-sunR * 0.18f, -sunR * 0.2f),
        )
        drawScrollWash(p.active.copy(alpha = 0.12f), p.header.copy(alpha = 0.06f))
    }

    private fun DrawScope.drawMidnight(
        p: MmrBrandPalette,
        appearance: MmrThemeAppearance,
    ) {
        val w = size.width
        val h = size.height
        val skyTop =
            if (appearance == MmrThemeAppearance.LIGHT) Color(0xFF1E293B) else Color(0xFF020617)
        val skyBottom =
            if (appearance == MmrThemeAppearance.LIGHT) Color(0xFF0F172A) else Color(0xFF000000)
        drawRect(brush = Brush.verticalGradient(0f to skyTop, 1f to skyBottom))
        drawRect(
            brush =
                Brush.radialGradient(
                    0f to p.active.copy(alpha = 0.2f),
                    1f to Color.Transparent,
                    center = Offset(w * 0.5f, 0f),
                    radius = w * 0.7f,
                ),
        )
        val moon = Offset(w * 0.82f, h * 0.34f)
        val moonR = min(w, h) * 0.07f
        drawCircle(color = Color.White.copy(alpha = 0.18f), radius = moonR * 1.45f, center = moon)
        drawCircle(color = Color(0xFFF8FAFC), radius = moonR, center = moon)
        drawCircle(
            color = Color.White.copy(alpha = 0.45f),
            radius = moonR * 0.28f,
            center =
                moon + Offset(-moonR * 0.28f, -moonR * 0.3f),
        )
        val stars =
            listOf(
                Offset(w * 0.12f, h * 0.28f),
                Offset(w * 0.28f, h * 0.22f),
                Offset(w * 0.46f, h * 0.32f),
                Offset(w * 0.18f, h * 0.42f),
                Offset(w * 0.08f, h * 0.55f),
                Offset(w * 0.34f, h * 0.6f),
                Offset(w * 0.22f, h * 0.74f),
                Offset(w * 0.48f, h * 0.8f),
                Offset(w * 0.88f, h * 0.48f),
                Offset(w * 0.92f, h * 0.62f),
                Offset(w * 0.78f, h * 0.72f),
                Offset(w * 0.14f, h * 0.88f),
            )
        stars.forEachIndexed { i, c ->
            drawSparkle(c, size = if (i % 3 == 0) 11f else 7f)
        }
    }

    private fun DrawScope.drawRoseGold(
        p: MmrBrandPalette,
        appearance: MmrThemeAppearance,
    ) {
        val w = size.width
        val h = size.height
        val isLight = appearance == MmrThemeAppearance.LIGHT
        drawRect(color = p.listBackground)
        drawRoseGoldGuilloche(p, isLight)
        drawRoseGoldMoneyLayer(p, w, h, isLight)
        drawRect(
            brush =
                Brush.radialGradient(
                    0f to p.active.copy(alpha = if (isLight) 0.42f else 0.22f),
                    1f to Color.Transparent,
                    center = Offset(w * 0.85f, 0f),
                    radius = w * 0.75f,
                ),
        )
        drawRect(
            brush =
                Brush.radialGradient(
                    0f to p.header.copy(alpha = if (isLight) 0.16f else 0.12f),
                    1f to Color.Transparent,
                    center = Offset(0f, h),
                    radius = w * 0.65f,
                ),
        )
        drawScrollWash(
            p.active.copy(alpha = if (isLight) 0.14f else 0.1f),
            p.header.copy(alpha = if (isLight) 0.06f else 0.05f),
        )
    }

    /** Tiled curves evoking security-print guilloché (see HTML mockup). */
    private fun DrawScope.drawRoseGoldGuilloche(
        p: MmrBrandPalette,
        isLight: Boolean,
    ) {
        val w = size.width
        val h = size.height
        val cell = if (isLight) 40f else 36f
        val strokeA = Stroke(width = if (isLight) 0.8f else 0.7f)
        val strokeB =
            Stroke(
                width = if (isLight) 0.6f else 0.55f,
            )
        val c1 =
            if (isLight) {
                p.header.copy(alpha = 0.06f)
            } else {
                p.active.copy(alpha = 0.06f)
            }
        val c2 = p.active.copy(alpha = if (isLight) 0.12f else 0.05f)

        var y = 0f
        while (y < h + cell) {
            var x = 0f
            while (x < w + cell) {
                // Path A: M0, cy Q cell*0.25, y0 cell*0.5, cy T cell, cy
                val cy = y + cell * 0.5f
                val pathA =
                    Path().apply {
                        moveTo(x, cy)
                        quadraticTo(x + cell * 0.25f, y, x + cell * 0.5f, cy)
                        quadraticTo(x + cell * 0.75f, y + cell, x + cell, cy)
                    }
                drawPath(pathA, color = c1, style = strokeA)

                val pathB =
                    Path().apply {
                        val cx = x + cell * 0.5f
                        moveTo(cx, y)
                        quadraticTo(x + cell, y + cell * 0.25f, cx, y + cell * 0.5f)
                        quadraticTo(x, y + cell * 0.75f, cx, y + cell)
                    }
                drawPath(pathB, color = c2, style = strokeB)
                x += cell
            }
            y += cell
        }
    }

    private fun DrawScope.drawRoseGoldMoneyLayer(
        p: MmrBrandPalette,
        w: Float,
        h: Float,
        isLight: Boolean,
    ) {
        val headerStroke = p.header.copy(alpha = if (isLight) 0.14f else 0.12f)
        val accentStroke = p.active.copy(alpha = if (isLight) 0.35f else 0.15f)
        val noteBackFill =
            Brush.linearGradient(
                0f to p.weekHighlight.copy(alpha = if (isLight) 0.45f else 0.15f),
                1f to p.active.copy(alpha = if (isLight) 0.22f else 0.1f),
                start = Offset(0f, 0f),
                end = Offset(1f, 1f),
            )
        val noteFrontFill =
            Brush.linearGradient(
                0f to p.weekHighlight.copy(alpha = if (isLight) 0.55f else 0.12f),
                1f to p.active.copy(alpha = if (isLight) 0.25f else 0.1f),
                start = Offset(0f, 0f),
                end = Offset(1f, 1f),
            )

        // Scale mockup viewBox 360×400 to this canvas
        val sx = w / 360f
        val sy = h / 400f

        fun drawNoteStack(
            originX: Float,
            originY: Float,
            pivotX: Float,
            pivotY: Float,
            rot: Float,
            noteW: Float,
            noteH: Float,
            corner: Float,
            withBand: Boolean,
            withPlaceholder: Boolean,
        ) {
            rotate(rot, pivot = Offset(pivotX, pivotY)) {
                val ox = originX
                val oy = originY
                // Back rectangle
                drawRoundRect(
                    brush = noteBackFill,
                    topLeft = Offset(ox, oy + noteH * 0.12f),
                    size = Size(noteW, noteH),
                    cornerRadius = CornerRadius(corner, corner),
                    style = Fill,
                )
                drawRoundRect(
                    color = headerStroke.copy(alpha = headerStroke.alpha * 0.85f),
                    topLeft = Offset(ox, oy + noteH * 0.12f),
                    size = Size(noteW, noteH),
                    cornerRadius = CornerRadius(corner, corner),
                    style = Stroke(width = 1f),
                )
                // Front rectangle
                drawRoundRect(
                    brush = noteFrontFill,
                    topLeft = Offset(ox + noteW * 0.025f, oy),
                    size = Size(noteW, noteH),
                    cornerRadius = CornerRadius(corner, corner),
                    style = Fill,
                )
                drawRoundRect(
                    color = headerStroke,
                    topLeft = Offset(ox + noteW * 0.025f, oy),
                    size = Size(noteW, noteH),
                    cornerRadius = CornerRadius(corner, corner),
                    style = Stroke(width = 1f),
                )
                if (withBand) {
                    val bw = noteW * 0.175f
                    val bh = noteH * 0.31f
                    drawRoundRect(
                        color = p.header.copy(alpha = if (isLight) 0.08f else 0.12f),
                        topLeft = Offset(ox + noteW * 0.44f, oy + noteH * 0.34f),
                        size = Size(bw, bh),
                        cornerRadius = CornerRadius(corner * 0.5f, corner * 0.5f),
                    )
                }
                val dashColor = p.header.copy(alpha = if (isLight) 0.1f else 0.1f)
                drawLine(
                    color = dashColor,
                    strokeWidth = 2f,
                    start = Offset(ox + noteW * 0.1f, oy + noteH * 0.69f),
                    end = Offset(ox + noteW * 0.75f, oy + noteH * 0.69f),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(3f, 4f), 0f),
                )
                if (withPlaceholder) {
                    drawRoseGoldCoinSymbol(
                        center = Offset(ox + noteW * 0.22f, oy + noteH * 0.55f),
                        text = "∗∗∗∗",
                        color = p.header.copy(alpha = if (isLight) 0.2f else 0.22f),
                        textSp = 11f,
                    )
                }
            }
        }

        // Stack 1 (left-top)
        drawNoteStack(
            originX = 20f * sx,
            originY = 48f * sy,
            pivotX = 100f * sx,
            pivotY = 84f * sy,
            rot = -8f,
            noteW = 160f * sx,
            noteH = 64f * sy,
            corner = 6f * min(sx, sy),
            withBand = true,
            withPlaceholder = false,
        )
        // Stack 2 (right-mid)
        drawNoteStack(
            originX = 200f * sx,
            originY = 120f * sy,
            pivotX = 280f * sx,
            pivotY = 156f * sy,
            rot = 14f,
            noteW = 150f * sx,
            noteH = 58f * sy,
            corner = 5f * min(sx, sy),
            withBand = false,
            withPlaceholder = true,
        )
        // Single bill (lower left)
        rotate(-4f, pivot = Offset(120f * sx, 256f * sy)) {
            val ox = 40f * sx
            val oy = 220f * sy
            val nw = 140f * sx
            val nh = 52f * sy
            val cr = 5f * min(sx, sy)
            drawRoundRect(
                color = p.weekHighlight.copy(alpha = if (isLight) 0.5f else 0.35f),
                topLeft = Offset(ox, oy + nh * 0.1f),
                size = Size(nw, nh),
                cornerRadius = CornerRadius(cr, cr),
            )
            drawRoundRect(
                color = accentStroke,
                topLeft = Offset(ox, oy + nh * 0.1f),
                size = Size(nw, nh),
                cornerRadius = CornerRadius(cr, cr),
                style = Stroke(width = 1f),
            )
        }

        // Floating micro-bills
        rotate(22f, pivot = Offset(322f * sx, 71f * sy)) {
            drawRoundRect(
                color = p.weekHighlight.copy(alpha = if (isLight) 0.55f else 0.25f),
                topLeft = Offset(300f * sx, 60f * sy),
                size = Size(44f * sx, 22f * sy),
                cornerRadius = CornerRadius(3f * min(sx, sy), 3f * min(sx, sy)),
            )
            drawRoundRect(
                color = headerStroke.copy(alpha = 0.75f),
                topLeft = Offset(300f * sx, 60f * sy),
                size = Size(44f * sx, 22f * sy),
                cornerRadius = CornerRadius(3f * min(sx, sy), 3f * min(sx, sy)),
                style = Stroke(width = 1f),
            )
        }
        rotate(-15f, pivot = Offset(28f * sx, 209f * sy)) {
            drawRoundRect(
                color = p.weekHighlight.copy(alpha = if (isLight) 0.55f else 0.22f),
                topLeft = Offset(10f * sx, 200f * sy),
                size = Size(36f * sx, 18f * sy),
                cornerRadius = CornerRadius(2.5f * min(sx, sy), 2.5f * min(sx, sy)),
            )
            drawRoundRect(
                color = headerStroke.copy(alpha = 0.65f),
                topLeft = Offset(10f * sx, 200f * sy),
                size = Size(36f * sx, 18f * sy),
                cornerRadius = CornerRadius(2.5f * min(sx, sy), 2.5f * min(sx, sy)),
                style = Stroke(width = 1f),
            )
        }

        // Coins (metal gradient + optional glyph)
        val coinMetal: (Offset, Float) -> Brush = { center, radius ->
            Brush.radialGradient(
                0f to Color(0xFFF5E6C8).copy(alpha = if (isLight) 0.85f else 0.45f),
                0.5f to Color(0xFFD4AF77).copy(alpha = if (isLight) 0.65f else 0.35f),
                1f to Color(0xFF8B7355).copy(alpha = if (isLight) 0.35f else 0.22f),
                center = center,
                radius = radius,
            )
        }
        val coinEdge = Color(0xFF8B7355).copy(alpha = if (isLight) 0.25f else 0.12f)

        fun drawCoin(
            center: Offset,
            rOuter: Float,
            symbol: String?,
        ) {
            drawCircle(brush = coinMetal(center, rOuter * 2.2f), radius = rOuter, center = center)
            drawCircle(color = coinEdge, radius = rOuter, center = center, style = Stroke(width = 1f))
            drawCircle(
                color = Color.White.copy(alpha = if (isLight) 0.35f else 0.2f),
                radius = rOuter * 0.76f,
                center = center,
                style = Stroke(width = 0.8f),
            )
            symbol?.let {
                drawRoseGoldCoinSymbol(
                    center = center,
                    text = it,
                    color = p.header.copy(alpha = if (isLight) 0.35f else 0.32f),
                    textSp = if (rOuter > 28f * min(sx, sy)) 15f else 12f,
                )
            }
        }

        drawCoin(Offset(260f * sx + 34f * sx, 260f * sy + 34f * sy), 34f * min(sx, sy), "₹")
        drawCoin(Offset(48f * sx + 22f * sx, 300f * sy + 22f * sy), 22f * min(sx, sy), "$")
        val smallCoinCenter = Offset(198f * sx, 38f * sy)
        val smallCoinR = 18f * min(sx, sy)
        drawCircle(
            brush = coinMetal(smallCoinCenter, smallCoinR * 2.5f),
            radius = smallCoinR,
            center = smallCoinCenter,
            alpha = if (isLight) 0.55f else 0.45f,
        )
    }

    private fun DrawScope.drawRoseGoldCoinSymbol(
        center: Offset,
        text: String,
        color: Color,
        textSp: Float,
    ) {
        val paint =
            NativePaint().apply {
                isAntiAlias = true
                textAlign = NativePaint.Align.CENTER
                this.color = color.toArgb()
                textSize = textSp * density
            }
        val fm = paint.fontMetrics
        val dy = -(fm.ascent + fm.descent) / 2f
        drawContext.canvas.nativeCanvas.drawText(text, center.x, center.y + dy, paint)
    }

    private fun DrawScope.drawMint(p: MmrBrandPalette) {
        val w = size.width
        drawRect(
            brush =
                Brush.verticalGradient(
                    0f to p.weekHighlight.copy(alpha = 0.85f),
                    0.3f to p.listBackground,
                    1f to p.listBackground,
                ),
        )
        var x = 48f
        while (x < w) {
            drawLine(
                color = p.active.copy(alpha = 0.07f),
                start = Offset(x, 0f),
                end = Offset(x, size.height),
                strokeWidth = 1f,
            )
            x += 48f
        }
        drawScrollWash(p.active.copy(alpha = 0.11f), p.header.copy(alpha = 0.04f))
    }

    private fun DrawScope.drawCharcoalGold(p: MmrBrandPalette) {
        drawRect(
            brush =
                Brush.verticalGradient(
                    0f to p.weekHighlight.copy(alpha = 0.45f),
                    0.4f to p.listBackground,
                    1f to p.listBackground.copy(red = p.listBackground.red - 0.02f),
                ),
        )
        drawRect(
            brush =
                Brush.radialGradient(
                    0f to p.active.copy(alpha = 0.16f),
                    1f to Color.Transparent,
                    center = Offset(size.width * 0.5f, 0f),
                    radius = size.width * 0.65f,
                ),
        )
        drawScrollWash(p.active.copy(alpha = 0.09f), p.header.copy(alpha = 0.06f))
    }

    private fun DrawScope.drawScrollWash(
        bottomAccent: Color,
        bottomSecondary: Color,
    ) {
        val h = size.height
        drawRect(
            brush =
                Brush.verticalGradient(
                    0f to Color.Transparent,
                    0.55f to Color.Transparent,
                    1f to bottomAccent,
                ),
            topLeft = Offset(0f, h * 0.45f),
            size = Size(size.width, h * 0.55f),
        )
        drawRect(
            brush =
                Brush.radialGradient(
                    0f to bottomSecondary,
                    1f to Color.Transparent,
                    center = Offset(size.width * 0.5f, h * 1.05f),
                    radius = size.width * 0.85f,
                ),
        )
    }

    private fun DrawScope.drawLeaf(
        cx: Float,
        cy: Float,
        scale: Float,
        rotationDeg: Float,
        fill: Color,
        stroke: Color,
    ) {
        rotate(rotationDeg, pivot = Offset(cx, cy)) {
            scale(scale, pivot = Offset(cx, cy)) {
                val path =
                    Path().apply {
                        moveTo(cx, cy - 22f)
                        cubicTo(cx + 14f, cy - 16f, cx + 16f, cy + 4f, cx, cy + 24f)
                        cubicTo(cx - 16f, cy + 4f, cx - 14f, cy - 16f, cx, cy - 22f)
                        close()
                    }
                drawPath(path, color = fill)
                drawPath(path, color = stroke, style = Stroke(width = 1.4f))
                drawLine(
                    color = stroke.copy(alpha = 0.7f),
                    start = Offset(cx, cy - 16f),
                    end = Offset(cx, cy + 18f),
                    strokeWidth = 1.2f,
                )
            }
        }
    }

    private fun DrawScope.drawSparkle(
        center: Offset,
        size: Float,
    ) {
        val white = Color.White
        drawCircle(color = white, radius = size * 0.14f, center = center)
        drawLine(
            color = white,
            start = Offset(center.x, center.y - size),
            end = Offset(center.x, center.y + size),
            strokeWidth = 1.6f,
        )
        drawLine(
            color = white,
            start = Offset(center.x - size, center.y),
            end = Offset(center.x + size, center.y),
            strokeWidth = 1.6f,
        )
        drawLine(
            color = white.copy(alpha = 0.55f),
            start = Offset(center.x - size * 0.62f, center.y - size * 0.62f),
            end = Offset(center.x + size * 0.62f, center.y + size * 0.62f),
            strokeWidth = 1f,
        )
        drawLine(
            color = white.copy(alpha = 0.55f),
            start = Offset(center.x + size * 0.62f, center.y - size * 0.62f),
            end = Offset(center.x - size * 0.62f, center.y + size * 0.62f),
            strokeWidth = 1f,
        )
    }
}
