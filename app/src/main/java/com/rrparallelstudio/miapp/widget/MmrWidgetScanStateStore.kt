package com.rrparallelstudio.miapp.widget

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MmrWidgetScanStateStore
    @Inject
    constructor(
        @ApplicationContext context: Context,
    ) {
        private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        private val scanningState = MutableStateFlow(prefs.getBoolean(KEY_SCANNING, false))

        val scanning: StateFlow<Boolean> = scanningState.asStateFlow()

        fun setScanning(scanning: Boolean) {
            prefs.edit().putBoolean(KEY_SCANNING, scanning).apply()
            scanningState.value = scanning
        }

        fun isScanning(): Boolean = scanningState.value
    }

private const val PREFS_NAME = "mmr_widget_scan_state"
private const val KEY_SCANNING = "scanning"
