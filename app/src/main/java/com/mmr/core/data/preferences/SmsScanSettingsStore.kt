package com.mmr.core.data.preferences

import android.content.Context
import com.mmr.core.parser.scanner.SmsScanPeriod
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmsScanSettingsStore
    @Inject
    constructor(
        @ApplicationContext context: Context,
    ) {
        private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        private val _scanMonths = MutableStateFlow(readMonths())
        val scanMonths: StateFlow<Int> = _scanMonths.asStateFlow()

        fun getScanMonths(): Int = _scanMonths.value

        fun setScanMonths(months: Int) {
            val clamped = SmsScanPeriod.clampMonths(months)
            prefs.edit().putInt(KEY_SCAN_MONTHS, clamped).apply()
            _scanMonths.value = clamped
        }

        private fun readMonths(): Int =
            SmsScanPeriod.clampMonths(
                prefs.getInt(KEY_SCAN_MONTHS, SmsScanPeriod.DEFAULT_MONTHS),
            )

        private companion object {
            const val PREFS_NAME = "mmr_sms_scan_settings"
            const val KEY_SCAN_MONTHS = "scan_months"
        }
    }
