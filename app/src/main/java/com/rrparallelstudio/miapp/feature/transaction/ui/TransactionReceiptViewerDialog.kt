package com.rrparallelstudio.miapp.feature.transaction.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.rrparallelstudio.miapp.core.common.image.MmrReceiptImage
import com.rrparallelstudio.miapp.ui.components.MmrContentDialog
import com.rrparallelstudio.miapp.ui.theme.MmrBrandColors

private const val MIN_ZOOM = 1f
private const val MAX_ZOOM = 4f
private const val ZOOM_STEP = 0.35f

@Composable
fun TransactionReceiptViewerDialog(
    filePath: String,
    onDismiss: () -> Unit,
) {
    val bitmap =
        remember(filePath) {
            runCatching {
                MmrReceiptImage.decodeBitmapFileForDisplay(filePath)?.asImageBitmap()
            }.getOrNull()
        }
    MmrContentDialog(
        title = "Receipt",
        message =
            if (bitmap == null) {
                "Could not open this image."
            } else {
                "Use + and − to zoom. Drag to move when zoomed in."
            },
        onDismiss = onDismiss,
        showDismissButton = true,
        dismissLabel = "Close",
        scrollBody = false,
    ) {
        if (bitmap != null) {
            ZoomableReceiptImage(bitmap = bitmap)
        }
    }
}

@Composable
private fun ZoomableReceiptImage(bitmap: androidx.compose.ui.graphics.ImageBitmap) {
    var scale by remember(bitmap) { mutableFloatStateOf(MIN_ZOOM) }
    var offset by remember(bitmap) { mutableStateOf(Offset.Zero) }
    var viewportPx by remember(bitmap) { mutableStateOf(IntSize.Zero) }
    val aspectRatio = bitmap.width.toFloat() / bitmap.height.toFloat().coerceAtLeast(1f)

    fun fitContentSizePx(viewport: IntSize): Pair<Float, Float> {
        if (viewport.width == 0 || viewport.height == 0) return 0f to 0f
        val vw = viewport.width.toFloat()
        val vh = viewport.height.toFloat()
        return if (vw / aspectRatio <= vh) {
            vw to vw / aspectRatio
        } else {
            (vh * aspectRatio) to vh
        }
    }

    fun clampOffset(
        proposed: Offset,
        zoom: Float,
        viewport: IntSize,
    ): Offset {
        val (contentW, contentH) = fitContentSizePx(viewport)
        if (contentW <= 0f || contentH <= 0f) return Offset.Zero
        val scaledW = contentW * zoom
        val scaledH = contentH * zoom
        val maxX = ((scaledW - viewport.width) / 2f).coerceAtLeast(0f)
        val maxY = ((scaledH - viewport.height) / 2f).coerceAtLeast(0f)
        return Offset(
            x = proposed.x.coerceIn(-maxX, maxX),
            y = proposed.y.coerceIn(-maxY, maxY),
        )
    }

    fun setZoom(newScale: Float) {
        val clamped = newScale.coerceIn(MIN_ZOOM, MAX_ZOOM)
        scale = clamped
        offset =
            if (clamped <= MIN_ZOOM) {
                Offset.Zero
            } else {
                clampOffset(offset, clamped, viewportPx)
            }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = { setZoom(scale - ZOOM_STEP) },
            enabled = scale > MIN_ZOOM,
        ) {
            Icon(
                imageVector = Icons.Filled.Remove,
                contentDescription = "Zoom out",
                tint = MmrBrandColors.Active,
            )
        }
        IconButton(
            onClick = { setZoom(scale + ZOOM_STEP) },
            enabled = scale < MAX_ZOOM,
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = "Zoom in",
                tint = MmrBrandColors.Active,
            )
        }
    }

    BoxWithConstraints(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(280.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MmrBrandColors.ListBackground)
                .onSizeChanged { viewportPx = it }
                .then(
                    if (scale > MIN_ZOOM) {
                        Modifier.pointerInput(scale, viewportPx) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                offset =
                                    clampOffset(
                                        offset + dragAmount,
                                        scale,
                                        viewportPx,
                                    )
                            }
                        }
                    } else {
                        Modifier
                    },
                ),
        contentAlignment = Alignment.Center,
    ) {
        val contentW =
            if (maxWidth / aspectRatio <= maxHeight) {
                maxWidth
            } else {
                maxHeight * aspectRatio
            }
        val contentH =
            if (maxWidth / aspectRatio <= maxHeight) {
                maxWidth / aspectRatio
            } else {
                maxHeight
            }

        Image(
            bitmap = bitmap,
            contentDescription = "Receipt full size",
            modifier =
                Modifier
                    .size(contentW, contentH)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = offset.x
                        translationY = offset.y
                    },
            contentScale = ContentScale.Fit,
        )
    }
}
