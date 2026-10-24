package com.mmr.core.data.preferences

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** Live SMS → pending approval pipeline (receiver + WorkManager). */
@Singleton
class SmsInboundSettingsStore
    @Inject
    constructor(
        @ApplicationContext context: Context,
    ) {
        private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        private val _notifyOnNewPending = MutableStateFlow(readNotifyEnabled(prefs))
        val notifyOnNewPending: StateFlow<Boolean> = _notifyOnNewPending.asStateFlow()

        fun isNotifyOnNewPendingEnabled(): Boolean = _notifyOnNewPending.value

        fun setNotifyOnNewPending(enabled: Boolean) {
            prefs.edit().putBoolean(KEY_NOTIFY_ON_NEW_PENDING, enabled).apply()
            _notifyOnNewPending.value = enabled
        }

        fun hasReadSmsInboundPromptBeenShown(): Boolean = prefs.getBoolean(KEY_READ_SMS_INBOUND_PROMPT_SHOWN, false)

        fun markReadSmsInboundPromptShown() {
            setReadSmsInboundPromptShown(true)
        }

        fun setReadSmsInboundPromptShown(shown: Boolean) {
            prefs.edit().putBoolean(KEY_READ_SMS_INBOUND_PROMPT_SHOWN, shown).apply()
        }

        companion object {
            private const val PREFS_NAME = "mmr_sms_inbound_settings"
            private const val KEY_NOTIFY_ON_NEW_PENDING = "notify_on_new_pending"
            private const val KEY_READ_SMS_INBOUND_PROMPT_SHOWN = "read_sms_inbound_prompt_shown"

            fun isNotifyOnNewPendingEnabled(context: Context): Boolean =
                readNotifyEnabled(
                    context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE),
                )

            private fun readNotifyEnabled(prefs: android.content.SharedPreferences): Boolean =
                prefs.getBoolean(KEY_NOTIFY_ON_NEW_PENDING, false)
        }
    }
