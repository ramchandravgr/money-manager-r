package com.rrparallelstudio.miapp.core.common.image

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

/** Decodes receipt photos and applies EXIF orientation (camera/gallery often store rotated pixels). */
object MmrReceiptImage {
    private const val JPEG_QUALITY = 92

    fun decodeBitmapFile(path: String): Bitmap? {
        val bitmap = BitmapFactory.decodeFile(path) ?: return null
        return applyExifOrientation(path, bitmap)
    }

    /** Downsampled decode for UI preview / full-screen viewer (avoids OOM on camera originals). */
    fun decodeBitmapFileForDisplay(
        path: String,
        maxEdgePx: Int = 2048,
    ): Bitmap? {
        val bounds =
            BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
        BitmapFactory.decodeFile(path, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sampleSize = 1
        while (
            bounds.outWidth / sampleSize > maxEdgePx ||
            bounds.outHeight / sampleSize > maxEdgePx
        ) {
            sampleSize *= 2
        }
        val decodeOpts =
            BitmapFactory.Options().apply {
                inSampleSize = sampleSize
            }
        val bitmap = BitmapFactory.decodeFile(path, decodeOpts) ?: return null
        return applyExifOrientation(path, bitmap)
    }

    /**
     * Re-encode as JPEG with EXIF orientation applied. When [maxBytes] is set, lowers quality
     * and scales down until the output fits (used for the 5 MB attachment cap).
     */
    fun normalizeUriBytes(
        bytes: ByteArray,
        maxBytes: Int = Int.MAX_VALUE,
    ): ByteArray {
        val bitmap = decodeBitmapBytes(bytes)
        return try {
            encodeBitmapToJpegUnderMax(bitmap, maxBytes)
        } finally {
            bitmap.recycle()
        }
    }

    fun writeNormalizedJpeg(
        bytes: ByteArray,
        dest: File,
    ) {
        val normalized = normalizeUriBytes(bytes)
        FileOutputStream(dest).use { it.write(normalized) }
    }

    private fun decodeBitmapBytes(bytes: ByteArray): Bitmap {
        val orientation =
            runCatching {
                ExifInterface(ByteArrayInputStream(bytes)).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL,
                )
            }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)
        val bitmap =
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                ?: throw IllegalArgumentException("Could not decode image.")
        return rotateBitmap(bitmap, orientation) ?: bitmap
    }

    private fun applyExifOrientation(
        path: String,
        bitmap: Bitmap,
    ): Bitmap {
        val orientation =
            runCatching {
                ExifInterface(path).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL,
                )
            }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)
        val rotated = rotateBitmap(bitmap, orientation) ?: bitmap
        if (rotated !== bitmap) {
            bitmap.recycle()
        }
        return rotated
    }

    private fun rotateBitmap(
        bitmap: Bitmap,
        orientation: Int,
    ): Bitmap? {
        val matrix =
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> Matrix().apply { postRotate(90f) }
                ExifInterface.ORIENTATION_ROTATE_180 -> Matrix().apply { postRotate(180f) }
                ExifInterface.ORIENTATION_ROTATE_270 -> Matrix().apply { postRotate(270f) }
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> Matrix().apply { postScale(-1f, 1f) }
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> Matrix().apply { postScale(1f, -1f) }
                ExifInterface.ORIENTATION_TRANSPOSE -> {
                    Matrix().apply {
                        postRotate(90f)
                        postScale(-1f, 1f)
                    }
                }
                ExifInterface.ORIENTATION_TRANSVERSE -> {
                    Matrix().apply {
                        postRotate(270f)
                        postScale(-1f, 1f)
                    }
                }
                else -> return null
            }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun encodeBitmapToJpegUnderMax(
        initial: Bitmap,
        maxBytes: Int,
    ): ByteArray {
        if (maxBytes == Int.MAX_VALUE) {
            return compressJpeg(initial, JPEG_QUALITY)
        }
        var bitmap = initial
        var ownsScaledBitmap = false
        val qualities = intArrayOf(92, 85, 75, 65, 55, 45, 35, 28)
        try {
            while (true) {
                for (quality in qualities) {
                    val encoded = compressJpeg(bitmap, quality)
                    if (encoded.size <= maxBytes) {
                        return encoded
                    }
                }
                val minEdge = minOf(bitmap.width, bitmap.height)
                if (minEdge <= 720) {
                    val last = compressJpeg(bitmap, 20)
                    if (last.size <= maxBytes) return last
                    throw IllegalArgumentException(
                        "Could not compress this image enough to stay under the size limit.",
                    )
                }
                val newW = (bitmap.width * 0.85f).toInt().coerceAtLeast(1)
                val newH = (bitmap.height * 0.85f).toInt().coerceAtLeast(1)
                val scaled = Bitmap.createScaledBitmap(bitmap, newW, newH, true)
                if (ownsScaledBitmap) {
                    bitmap.recycle()
                }
                bitmap = scaled
                ownsScaledBitmap = true
            }
        } finally {
            if (ownsScaledBitmap) {
                bitmap.recycle()
            }
        }
    }

    private fun compressJpeg(
        bitmap: Bitmap,
        quality: Int,
    ): ByteArray {
        val out = ByteArrayOutputStream()
        if (!bitmap.compress(Bitmap.CompressFormat.JPEG, quality.coerceIn(1, 100), out)) {
            throw IllegalArgumentException("Could not compress image.")
        }
        return out.toByteArray()
    }
}
