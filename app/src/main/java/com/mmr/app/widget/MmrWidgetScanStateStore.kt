package com.mmr.app.widget

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MmrWidgetScanStateStore
    @Inject
    constructor(
        @ApplicationContext context: Context,
    ) {
        private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        fun setScanning(scanning: Boolean) {
            prefs.edit().putBoolean(KEY_SCANNING, scanning).apply()
        }

        fun isScanning(): Boolean = prefs.getBoolean(KEY_SCANNING, false)
    }

private const val PREFS_NAME = "mmr_widget_scan_state"
private const val KEY_SCANNING = "scanning"
