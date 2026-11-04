package com.mmr.app.receipt

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.util.UUID

object MmrReceiptCaptureFiles {
    private const val AUTHORITY_SUFFIX = ".exportfileprovider"

    fun newCameraOutputUri(context: Context): Uri {
        val dir = File(context.cacheDir, "pending_receipts").apply { mkdirs() }
        val file = File(dir, "capture_${UUID.randomUUID()}.jpg")
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}$AUTHORITY_SUFFIX",
            file,
        )
    }
}
