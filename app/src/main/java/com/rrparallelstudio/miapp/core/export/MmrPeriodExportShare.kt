package com.rrparallelstudio.miapp.core.export

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

object MmrPeriodExportShare {
    fun share(
        context: Context,
        file: File,
        mimeType: String,
        subject: String,
        chooserTitle: String,
    ) {
        val authority = "${context.packageName}.exportfileprovider"
        val uri = FileProvider.getUriForFile(context, authority, file)
        val send =
            Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, subject)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        context.startActivity(Intent.createChooser(send, chooserTitle))
    }
}
