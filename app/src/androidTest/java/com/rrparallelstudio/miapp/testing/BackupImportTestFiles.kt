package com.rrparallelstudio.miapp.testing

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

object BackupImportTestFiles {
    fun writeJsonAndGetUri(
        context: Context,
        json: String,
        fileName: String,
    ): Uri {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, fileName)
        file.writeText(json)
        return uriForFile(context, file)
    }

    fun writeArchiveAndGetUri(
        context: Context,
        archiveBytes: ByteArray,
        fileName: String,
    ): Uri {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, fileName)
        file.writeBytes(archiveBytes)
        return uriForFile(context, file)
    }

    private fun uriForFile(
        context: Context,
        file: File,
    ): Uri {
        val uri =
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.exportfileprovider",
                file,
            )
        context.grantUriPermission(context.packageName, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        return uri
    }
}
