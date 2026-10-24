package com.mmr.core.data.preferences

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WebManagePreferencesStore
    @Inject
    constructor(
        @ApplicationContext context: Context,
    ) {
        private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        fun isWebManageEnabled(): Boolean = prefs.getBoolean(KEY_ENABLED, false)

        fun setWebManageEnabled(enabled: Boolean) {
            prefs.edit().putBoolean(KEY_ENABLED, enabled).commit()
        }

        fun getPasswordSalt(): String? = prefs.getString(KEY_PASSWORD_SALT, null)

        fun getPasswordHash(): String? = prefs.getString(KEY_PASSWORD_HASH, null)

        fun hasAccessPassword(): Boolean = !getPasswordHash().isNullOrBlank()

        fun setAccessPassword(
            salt: String,
            hash: String,
        ) {
            prefs
                .edit()
                .putString(KEY_PASSWORD_SALT, salt)
                .putString(KEY_PASSWORD_HASH, hash)
                .commit()
        }

        fun clearAccessPassword() {
            prefs
                .edit()
                .remove(KEY_PASSWORD_SALT)
                .remove(KEY_PASSWORD_HASH)
                .commit()
        }

        private companion object {
            const val PREFS_NAME = "mmr_web_manage"
            const val KEY_ENABLED = "web_manage_enabled"
            const val KEY_PASSWORD_SALT = "web_manage_password_salt"
            const val KEY_PASSWORD_HASH = "web_manage_password_hash"
        }
    }
