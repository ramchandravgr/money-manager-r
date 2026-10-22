package com.mmr.feature.settings.viewmodel

import androidx.lifecycle.ViewModel
import com.mmr.core.data.preferences.SmsScanSettingsStore
import com.mmr.core.parser.scanner.SmsScanPeriod
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val smsScanSettingsStore: SmsScanSettingsStore
) : ViewModel() {

    val smsScanMonths: StateFlow<Int> = smsScanSettingsStore.scanMonths

    fun setSmsScanMonths(months: Int) {
        smsScanSettingsStore.setScanMonths(months)
    }

    val minScanMonths: Int = SmsScanPeriod.MIN_MONTHS
    val maxScanMonths: Int = SmsScanPeriod.MAX_MONTHS
}
