package com.rrparallelstudio.miapp.feature.settings.viewmodel

import androidx.lifecycle.ViewModel
import com.rrparallelstudio.miapp.core.data.preferences.FtueStore
import com.rrparallelstudio.miapp.core.data.preferences.SmsInboundSettingsStore
import com.rrparallelstudio.miapp.core.data.preferences.SmsScanSettingsStore
import com.rrparallelstudio.miapp.core.parser.scanner.SmsScanPeriod
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel
    @Inject
    constructor(
        private val smsScanSettingsStore: SmsScanSettingsStore,
        private val smsInboundSettingsStore: SmsInboundSettingsStore,
        private val ftueStore: FtueStore,
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

        fun replayIntroduction() {
            ftueStore.requestReplay()
        }
    }
