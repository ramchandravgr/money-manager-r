package com.rrparallelstudio.miapp.feature.asset.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.rrparallelstudio.miapp.R
import com.rrparallelstudio.miapp.core.data.repository.AssetAttachmentRepositoryImpl
import com.rrparallelstudio.miapp.feature.asset.model.AddEditAssetUiEvent
import com.rrparallelstudio.miapp.feature.asset.viewmodel.AddEditAssetViewModel
import com.rrparallelstudio.miapp.feature.transaction.ui.TransactionReceiptSection
import com.rrparallelstudio.miapp.feature.transaction.ui.TransactionReceiptViewerDialog
import com.rrparallelstudio.miapp.feature.transaction.ui.rememberTransactionReceiptAttachLauncher
import com.rrparallelstudio.miapp.ui.components.MmrAppBarSaveIcon
import com.rrparallelstudio.miapp.ui.components.MmrImeAwareScrollColumn
import com.rrparallelstudio.miapp.ui.components.MmrNestedScaffoldInsets
import com.rrparallelstudio.miapp.ui.components.MmrPrimaryActionButton
import com.rrparallelstudio.miapp.ui.components.MmrTopAppBar
import com.rrparallelstudio.miapp.ui.components.MmrTransactionFormScreenBackground

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditAssetScreen(
    onNavigateBack: () -> Unit,
    viewModel: AddEditAssetViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val editId = uiState.assetId
    val isEdit = editId != null && editId > 0L
    var viewingPhotoPath by remember { mutableStateOf<String?>(null) }
    val launchPhotoAttach =
        rememberTransactionReceiptAttachLauncher(
            dialogTitle = stringResource(R.string.asset_attach_dialog_title),
            onPicked = { uriString ->
                viewModel.onEvent(AddEditAssetUiEvent.OnPhotoPicked(uriString))
            },
        )
    val maxPhotos = AssetAttachmentRepositoryImpl.MAX_ATTACHMENTS_PER_ASSET
    val canAddMorePhotos = uiState.photos.size < maxPhotos

    viewingPhotoPath?.let { path ->
        TransactionReceiptViewerDialog(filePath = path, onDismiss = { viewingPhotoPath = null })
    }

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) onNavigateBack()
    }

    Scaffold(
        contentWindowInsets = MmrNestedScaffoldInsets,
        topBar = {
            MmrTopAppBar(
                title = if (isEdit) "Edit asset" else "New asset",
                showBackNavigation = true,
                onNavigateBack = onNavigateBack,
                useBrandHeaderStyle = true,
                actions = {
                    MmrAppBarSaveIcon(
                        onClick = { viewModel.onEvent(AddEditAssetUiEvent.OnSaveClick) },
                        enabled = uiState.canSave && !uiState.isSaving,
                    )
                },
            )
        },
    ) { innerPadding ->
        MmrTransactionFormScreenBackground(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
        ) {
            MmrImeAwareScrollColumn(
                horizontalPadding = 16.dp,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                uiState.errorMessage?.let { Text(it) }
                AssetFormFields(uiState = uiState, onEvent = viewModel::onEvent)
                if (canAddMorePhotos) {
                    MmrPrimaryActionButton(
                        text = "Add photo",
                        onClick = launchPhotoAttach,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                TransactionReceiptSection(
                    receipts = uiState.photos,
                    onAddClick = launchPhotoAttach,
                    onRemoveSaved = { viewModel.onEvent(AddEditAssetUiEvent.OnRemovePhoto(it)) },
                    onRemovePending = { viewModel.onEvent(AddEditAssetUiEvent.OnRemovePendingPhoto(it)) },
                    canAddMore = canAddMorePhotos,
                    sectionLabel = "Photos",
                    onReceiptClick = { viewingPhotoPath = it },
                )
                MmrPrimaryActionButton(
                    text =
                        when {
                            uiState.isSaving -> "Saving…"
                            isEdit -> "Save changes"
                            else -> "Create asset"
                        },
                    onClick = { viewModel.onEvent(AddEditAssetUiEvent.OnSaveClick) },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    enabled = uiState.canSave && !uiState.isSaving,
                )
            }
        }
    }
}
