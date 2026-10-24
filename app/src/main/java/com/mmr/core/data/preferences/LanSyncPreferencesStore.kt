package com.mmr.core.data.preferences

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

enum class LanSyncPeerMode {
    ALL_TRUSTED_ON_LAN,
    SINGLE_DEVICE,
}

enum class LanSyncDuplicateEntityPolicy {
    ASK,
    MERGE_BY_NAME,
    KEEP_SEPARATE,
}

@Singleton
class LanSyncPreferencesStore
    @Inject
    constructor(
        @ApplicationContext context: Context,
    ) {
        private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        var peerMode: LanSyncPeerMode
            get() =
                when (prefs.getString(KEY_PEER_MODE, LanSyncPeerMode.ALL_TRUSTED_ON_LAN.name)) {
                    LanSyncPeerMode.SINGLE_DEVICE.name -> LanSyncPeerMode.SINGLE_DEVICE
                    else -> LanSyncPeerMode.ALL_TRUSTED_ON_LAN
                }
            set(value) {
                prefs.edit().putString(KEY_PEER_MODE, value.name).apply()
            }

        var selectedPeerDeviceId: String?
            get() = prefs.getString(KEY_SELECTED_PEER, null)?.takeIf { it.isNotBlank() }
            set(value) {
                prefs.edit().putString(KEY_SELECTED_PEER, value).apply()
            }

        var includeAttachments: Boolean
            get() = prefs.getBoolean(KEY_INCLUDE_ATTACHMENTS, false)
            set(value) {
                prefs.edit().putBoolean(KEY_INCLUDE_ATTACHMENTS, value).apply()
            }

        fun duplicatePolicy(entityKey: String): LanSyncDuplicateEntityPolicy =
            when (prefs.getString(policyKey(entityKey), LanSyncDuplicateEntityPolicy.ASK.name)) {
                LanSyncDuplicateEntityPolicy.MERGE_BY_NAME.name -> LanSyncDuplicateEntityPolicy.MERGE_BY_NAME
                LanSyncDuplicateEntityPolicy.KEEP_SEPARATE.name -> LanSyncDuplicateEntityPolicy.KEEP_SEPARATE
                else -> LanSyncDuplicateEntityPolicy.ASK
            }

        fun setDuplicatePolicy(
            entityKey: String,
            policy: LanSyncDuplicateEntityPolicy,
        ) {
            prefs.edit().putString(policyKey(entityKey), policy.name).apply()
        }

        fun duplicatePolicyEntries(): Map<String, LanSyncDuplicateEntityPolicy> =
            prefs.all
                .filterKeys { it.startsWith(DUP_POLICY_PREFIX) }
                .mapKeys { (key, _) -> key.removePrefix(DUP_POLICY_PREFIX) }
                .mapValues { (_, value) ->
                    when (value) {
                        LanSyncDuplicateEntityPolicy.MERGE_BY_NAME.name -> LanSyncDuplicateEntityPolicy.MERGE_BY_NAME
                        LanSyncDuplicateEntityPolicy.KEEP_SEPARATE.name -> LanSyncDuplicateEntityPolicy.KEEP_SEPARATE
                        else -> LanSyncDuplicateEntityPolicy.ASK
                    }
                }

        fun replaceDuplicatePolicies(entries: Map<String, LanSyncDuplicateEntityPolicy>) {
            val editor = prefs.edit()
            prefs.all.keys
                .filter { it.startsWith(DUP_POLICY_PREFIX) }
                .forEach { editor.remove(it) }
            entries.forEach { (entityKey, policy) ->
                editor.putString(policyKey(entityKey), policy.name)
            }
            editor.apply()
        }

        private fun policyKey(entityKey: String) = "$DUP_POLICY_PREFIX$entityKey"

        private companion object {
            const val DUP_POLICY_PREFIX = "dup_policy_"
            const val PREFS_NAME = "mmr_lan_sync_prefs"
            const val KEY_PEER_MODE = "peer_mode"
            const val KEY_SELECTED_PEER = "selected_peer_device_id"
            const val KEY_INCLUDE_ATTACHMENTS = "include_attachments"
        }
    }
