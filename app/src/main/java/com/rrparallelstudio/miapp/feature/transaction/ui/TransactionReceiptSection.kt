package com.rrparallelstudio.miapp.feature.transaction.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.rrparallelstudio.miapp.core.common.image.MmrReceiptImage
import com.rrparallelstudio.miapp.feature.transaction.model.TransactionReceiptUi
import com.rrparallelstudio.miapp.ui.components.MmrTransactionFormCard
import com.rrparallelstudio.miapp.ui.components.MmrTransactionFormSectionLabel

/**
 * Receipt thumbnails below the note row (add via note row camera action).
 */
@Composable
fun TransactionReceiptSection(
    receipts: List<TransactionReceiptUi>,
    onAddClick: () -> Unit,
    onRemoveSaved: (Long) -> Unit,
    onRemovePending: (filePath: String) -> Unit,
    modifier: Modifier = Modifier,
    canAddMore: Boolean = true,
    readOnly: Boolean = false,
    sectionLabel: String = "Receipt photos",
    onReceiptClick: (filePath: String) -> Unit = {},
) {
    if (readOnly && receipts.isEmpty()) return
    if (receipts.isEmpty() && readOnly) return
    if (receipts.isEmpty()) return

    Column(modifier = modifier.fillMaxWidth()) {
        MmrTransactionFormSectionLabel(text = sectionLabel)
        MmrTransactionFormCard {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(MmrDimensReceiptPadding),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                receipts.forEach { receipt ->
                    ReceiptThumbnail(
                        filePath = receipt.filePath,
                        onClick = { onReceiptClick(receipt.filePath) },
                        onRemove =
                            if (readOnly) {
                                null
                            } else {
                                {
                                    if (receipt.id > 0L) {
                                        onRemoveSaved(receipt.id)
                                    } else {
                                        onRemovePending(receipt.filePath)
                                    }
                                }
                            },
                    )
                }
            }
        }
    }
}

private val MmrDimensReceiptPadding = 12.dp

@Composable
private fun ReceiptThumbnail(
    filePath: String?,
    onClick: () -> Unit,
    onRemove: (() -> Unit)?,
) {
    Box {
        Surface(
            modifier =
                Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onClick),
            tonalElevation = 1.dp,
        ) {
            val bitmap =
                remember(filePath) {
                    filePath?.let { path ->
                        runCatching {
                            MmrReceiptImage.decodeBitmapFileForDisplay(path, maxEdgePx = 512)?.asImageBitmap()
                        }.getOrNull()
                    }
                }
            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = "Receipt",
                    modifier = Modifier.height(72.dp).fillMaxWidth(),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Box(Modifier.size(72.dp), contentAlignment = Alignment.Center) {
                    Text("…", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        if (onRemove != null) {
            IconButton(
                onClick = onRemove,
                modifier =
                    Modifier
                        .align(Alignment.TopEnd)
                        .size(28.dp)
                        .padding(2.dp),
            ) {
                Icon(
                    Icons.Outlined.Close,
                    contentDescription = "Remove receipt",
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}
