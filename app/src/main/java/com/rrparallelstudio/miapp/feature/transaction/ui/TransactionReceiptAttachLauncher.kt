package com.rrparallelstudio.miapp.feature.transaction.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.rrparallelstudio.miapp.R
import com.rrparallelstudio.miapp.receipt.MmrReceiptCaptureFiles
import com.rrparallelstudio.miapp.ui.components.MmrContentDialog
import com.rrparallelstudio.miapp.ui.components.MmrDialogOptionButton

/**
 * Shows take-photo vs gallery chooser, then returns a content [Uri] string for the host ViewModel.
 *
 * Gallery uses [Intent.ACTION_PICK] on [MediaStore.Images.Media.EXTERNAL_CONTENT_URI] so the user
 * gets a classic gallery/files flow instead of the system photo-only picker that [ACTION_GET_CONTENT]
 * often opens on recent Android versions.
 */
@Composable
fun rememberTransactionReceiptAttachLauncher(
    dialogTitle: String? = null,
    onPicked: (uriString: String) -> Unit,
): () -> Unit {
    val context = LocalContext.current
    val resolvedDialogTitle = dialogTitle ?: stringResource(R.string.receipt_attach_dialog_title)
    val galleryChooserTitle = stringResource(R.string.choose_from_gallery)
    var showChooser by remember { mutableStateOf(false) }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }

    val galleryLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult(),
            onResult = { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    val uri = result.data?.data
                    if (uri != null) {
                        onPicked(uri.toString())
                    }
                }
            },
        )

    fun launchGalleryAppChooser() {
        val galleryPick =
            Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI).apply {
                type = "image/*"
            }
        val openContentFallback =
            Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "image/*"
                addCategory(Intent.CATEGORY_OPENABLE)
            }
        val chooser =
            Intent.createChooser(galleryPick, galleryChooserTitle).apply {
                putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(openContentFallback))
            }
        galleryLauncher.launch(chooser)
    }

    val cameraLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.TakePicture(),
            onResult = { success ->
                if (success) {
                    pendingCameraUri?.let { onPicked(it.toString()) }
                }
                pendingCameraUri = null
            },
        )

    if (showChooser) {
        MmrContentDialog(
            title = resolvedDialogTitle,
            onDismiss = { showChooser = false },
            showDismissButton = true,
            dismissLabel = "Cancel",
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                MmrDialogOptionButton(
                    text = stringResource(R.string.take_photo),
                    onClick = {
                        showChooser = false
                        val uri = MmrReceiptCaptureFiles.newCameraOutputUri(context)
                        pendingCameraUri = uri
                        cameraLauncher.launch(uri)
                    },
                )
                MmrDialogOptionButton(
                    text = galleryChooserTitle,
                    onClick = {
                        showChooser = false
                        launchGalleryAppChooser()
                    },
                )
            }
        }
    }

    return {
        showChooser = true
    }
}
