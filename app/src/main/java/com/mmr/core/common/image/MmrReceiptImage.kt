package com.mmr.core.common.image

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

    fun normalizeUriBytes(bytes: ByteArray): ByteArray {
        val bitmap = decodeBitmapBytes(bytes)
        return compressJpeg(bitmap)
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
        return rotateBitmap(bitmap, orientation) ?: bitmap
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

    private fun compressJpeg(bitmap: Bitmap): ByteArray {
        val out = ByteArrayOutputStream()
        if (!bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)) {
            throw IllegalArgumentException("Could not compress image.")
        }
        return out.toByteArray()
    }
}
