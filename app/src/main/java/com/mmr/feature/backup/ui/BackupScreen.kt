package com.mmr.feature.backup.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mmr.core.backup.model.BackupRestoreSummary
import com.mmr.feature.backup.model.BackupUiEvent
import com.mmr.app.ui.state.MmrErrorState
import com.mmr.app.ui.state.MmrLoadingState
import com.mmr.app.ui.state.MmrStatusBanner
import com.mmr.feature.backup.viewmodel.BackupViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen(
    onNavigateBack: () -> Unit,
    viewModel: BackupViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.onEvent(BackupUiEvent.OnExportToUri(uri))
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: SecurityException) {
                // Some providers do not support persistable permissions.
            }
            val displayName = context.contentResolver.query(
                uri,
                arrayOf(android.provider.OpenableColumns.DISPLAY_NAME),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
            viewModel.onEvent(BackupUiEvent.OnImportUriSelected(uri, displayName))
        }
    }

    if (uiState.showImportConfirmDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.onEvent(BackupUiEvent.OnDismissImportDialog) },
            title = { Text("Restore backup?") },
            text = {
                Text(
                    "This will replace all local MMR data with the contents of " +
                        "\"${uiState.pendingImportLabel ?: "the selected file"}\". " +
                        "Export a backup first if you want to keep the current data."
                )
            },
            confirmButton = {
                Button(onClick = { viewModel.onEvent(BackupUiEvent.OnConfirmImport) }) {
                    Text("Restore")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onEvent(BackupUiEvent.OnDismissImportDialog) }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Backup & Restore", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    TextButton(onClick = onNavigateBack) {
                        Text("Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                uiState.isLoading -> MmrLoadingState()

                uiState.errorMessage != null && uiState.summary == null -> {
                    MmrErrorState(
                        message = uiState.errorMessage ?: "Something went wrong.",
                        onRetry = { viewModel.onEvent(BackupUiEvent.OnRetryClick) }
                    )
                }

                else -> {
                    BackupContent(
                        summary = uiState.summary,
                        isEmpty = uiState.isEmpty,
                        isBusy = uiState.isBusy,
                        statusMessage = uiState.statusMessage,
                        errorMessage = uiState.errorMessage,
                        onDismissMessage = { viewModel.onEvent(BackupUiEvent.OnDismissMessage) },
                        onExport = {
                            val stamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.getDefault())
                                .format(Date())
                            exportLauncher.launch("mmr-backup-$stamp.json")
                        },
                        onImport = {
                            importLauncher.launch(arrayOf("application/json", "application/*"))
                        }
                    )
                }
            }

            if (uiState.isBusy) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        tonalElevation = 4.dp,
                        shadowElevation = 4.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(28.dp))
                            Text(
                                text = "Processing backup…",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BackupContent(
    summary: BackupRestoreSummary?,
    isEmpty: Boolean,
    isBusy: Boolean,
    statusMessage: String?,
    errorMessage: String?,
    onDismissMessage: () -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Offline JSON backup for your ledger, categories, accounts, loans, recurring rules, and SMS approval data.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (isEmpty) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "No local data yet",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Add transactions from Home or import an existing backup file to restore data on this device.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            summary?.let { BackupSummaryCard(summary = it) }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Export", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    text = if (isEmpty) {
                        "Export will create an empty backup file you can restore later."
                    } else {
                        "Save a JSON snapshot to your device storage or cloud folder."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(
                    onClick = onExport,
                    enabled = !isBusy,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Export backup")
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Import", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    "Choose a previously exported MMR `.json` backup file to restore.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedButton(
                    onClick = onImport,
                    enabled = !isBusy,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Import backup")
                }
            }
        }

        statusMessage?.let { message ->
            MmrStatusBanner(
                message = message,
                onDismiss = onDismissMessage,
                isError = false
            )
        }

        errorMessage?.let { message ->
            MmrStatusBanner(
                message = message,
                onDismiss = onDismissMessage
            )
        }
    }
}

@Composable
private fun BackupSummaryCard(summary: BackupRestoreSummary) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Local data ready to back up",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "${summary.totalRecords} total records",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            BackupSummaryRow("Transactions", summary.transactions)
            BackupSummaryRow("Categories", summary.categories)
            BackupSummaryRow("Accounts", summary.accounts)
            BackupSummaryRow("Loans", summary.loans)
            BackupSummaryRow("Recurring rules", summary.recurringRules)
            BackupSummaryRow("SMS / approvals", summary.smsMessageRecords + summary.transactionApprovals)
        }
    }
}

@Composable
private fun BackupSummaryRow(label: String, count: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            count.toString(),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}
