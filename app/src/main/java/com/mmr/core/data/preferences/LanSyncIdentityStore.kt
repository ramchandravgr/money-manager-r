package com.mmr.core.data.preferences

import android.content.Context
import com.mmr.core.common.sync.MmrSyncId
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LanSyncIdentityStore
    @Inject
    constructor(
        @ApplicationContext context: Context,
    ) {
        private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        private val _lanSyncEnabled = MutableStateFlow(prefs.getBoolean(KEY_ENABLED, false))
        val lanSyncEnabled: StateFlow<Boolean> = _lanSyncEnabled.asStateFlow()

        val deviceId: String
            get() {
                val existing = prefs.getString(KEY_DEVICE_ID, null)
                if (!existing.isNullOrBlank()) return existing
                val newId = MmrSyncId.newId()
                prefs.edit().putString(KEY_DEVICE_ID, newId).apply()
                return newId
            }

        val inboundAuthToken: String
            get() {
                val existing = prefs.getString(KEY_INBOUND_TOKEN, null)
                if (!existing.isNullOrBlank()) return existing
                val token = MmrSyncId.newId()
                prefs.edit().putString(KEY_INBOUND_TOKEN, token).apply()
                return token
            }

        var displayName: String
            get() =
                prefs.getString(KEY_DISPLAY_NAME, null) ?: android.os.Build.MODEL
                    .orEmpty()
                    .ifBlank { "MMR device" }
            set(value) {
                prefs.edit().putString(KEY_DISPLAY_NAME, value.trim()).apply()
            }

        fun setLanSyncEnabled(enabled: Boolean) {
            prefs.edit().putBoolean(KEY_ENABLED, enabled).apply()
            _lanSyncEnabled.value = enabled
        }

        fun isLanSyncEnabled(): Boolean = _lanSyncEnabled.value

        private companion object {
            const val PREFS_NAME = "mmr_lan_sync_identity"
            const val KEY_ENABLED = "lan_sync_enabled"
            const val KEY_DEVICE_ID = "device_id"
            const val KEY_INBOUND_TOKEN = "inbound_auth_token"
            const val KEY_DISPLAY_NAME = "display_name"
        }
    }
