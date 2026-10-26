package com.mmr.feature.transaction.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.mmr.app.ui.components.MmrContentDialog
import com.mmr.core.common.image.MmrReceiptImage

@Composable
fun TransactionReceiptViewerDialog(
    filePath: String,
    onDismiss: () -> Unit,
) {
    val bitmap =
        remember(filePath) {
            runCatching { MmrReceiptImage.decodeBitmapFile(filePath)?.asImageBitmap() }.getOrNull()
        }
    MmrContentDialog(
        title = "Receipt",
        message = null,
        onDismiss = onDismiss,
        showDismissButton = true,
        dismissLabel = "Close",
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = "Receipt full size",
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                contentScale = ContentScale.FillWidth,
            )
        }
    }
}
