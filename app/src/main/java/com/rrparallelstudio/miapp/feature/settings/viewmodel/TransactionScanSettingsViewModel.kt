package com.rrparallelstudio.miapp.feature.settings.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import com.rrparallelstudio.miapp.core.data.preferences.SmsBackupFolderStore
import com.rrparallelstudio.miapp.core.data.preferences.SmsScanSettingsStore
import com.rrparallelstudio.miapp.core.parser.scanner.SmsScanPeriod
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class TransactionScanSettingsViewModel
    @Inject
    constructor(
        private val smsScanSettingsStore: SmsScanSettingsStore,
        private val smsBackupFolderStore: SmsBackupFolderStore,
    ) : ViewModel() {
        val scanMonths: StateFlow<Int> = smsScanSettingsStore.scanMonths
        val folderDisplayName: StateFlow<String?> = smsBackupFolderStore.folderDisplayName
        val folderTreeUri: StateFlow<Uri?> = smsBackupFolderStore.folderTreeUri

        val minScanMonths: Int = SmsScanPeriod.MIN_MONTHS
        val maxScanMonths: Int = SmsScanPeriod.MAX_MONTHS

        fun setScanMonths(months: Int) {
            smsScanSettingsStore.setScanMonths(months)
        }

        fun onBackupFolderTreeSelected(treeUri: Uri) {
            smsBackupFolderStore.setFolderTree(treeUri)
        }

        fun clearBackupFolder() {
            smsBackupFolderStore.clearFolder()
        }
    }
