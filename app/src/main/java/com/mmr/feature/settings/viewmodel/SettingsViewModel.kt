package com.mmr.feature.settings.viewmodel

import androidx.lifecycle.ViewModel
import com.mmr.core.data.preferences.SmsInboundSettingsStore
import com.mmr.core.data.preferences.SmsScanSettingsStore
import com.mmr.core.parser.scanner.SmsScanPeriod
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel
    @Inject
    constructor(
        private val smsScanSettingsStore: SmsScanSettingsStore,
        private val smsInboundSettingsStore: SmsInboundSettingsStore,
    ) : ViewModel() {
        val smsScanMonths: StateFlow<Int> = smsScanSettingsStore.scanMonths

        val smsNotifyOnNewPending: StateFlow<Boolean> = smsInboundSettingsStore.notifyOnNewPending

        fun setSmsScanMonths(months: Int) {
            smsScanSettingsStore.setScanMonths(months)
        }

        fun setSmsNotifyOnNewPending(enabled: Boolean) {
            smsInboundSettingsStore.setNotifyOnNewPending(enabled)
        }

        fun hasReadSmsInboundPromptBeenShown(): Boolean = smsInboundSettingsStore.hasReadSmsInboundPromptBeenShown()

        fun markReadSmsInboundPromptShown() {
            smsInboundSettingsStore.markReadSmsInboundPromptShown()
        }

        val minScanMonths: Int = SmsScanPeriod.MIN_MONTHS
        val maxScanMonths: Int = SmsScanPeriod.MAX_MONTHS
    }
