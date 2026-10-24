package com.mmr.core.lansync

import com.mmr.core.data.preferences.LanSyncIdentityStore
import com.mmr.core.data.preferences.WebManagePreferencesStore
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Peer device sync and Web Manage are mutually exclusive (one LAN server mode at a time).
 * Infrastructure start/stop remains in each feature's repository.
 */
enum class LocalLanServerMode {
    NONE,
    PEER_SYNC,
    WEB_MANAGE,
}

@Singleton
class LocalLanServerCoordinator
    @Inject
    constructor(
        private val identityStore: LanSyncIdentityStore,
        private val webManageStore: WebManagePreferencesStore,
    ) {
        fun currentMode(): LocalLanServerMode =
            when {
                identityStore.isLanSyncEnabled() -> LocalLanServerMode.PEER_SYNC
                webManageStore.isWebManageEnabled() -> LocalLanServerMode.WEB_MANAGE
                else -> LocalLanServerMode.NONE
            }

        /** Before peer sync starts: turn off Web Manage preference (Web Manage repo must stop its server when implemented). */
        fun clearWebManageBecausePeerSyncStarting() {
            webManageStore.setWebManageEnabled(false)
        }

        fun markWebManageActive() {
            webManageStore.setWebManageEnabled(true)
        }

        fun clearWebManage() {
            webManageStore.setWebManageEnabled(false)
        }

        /** Web Manage should call [com.mmr.core.domain.repository.LanSyncRepository.setLanSyncEnabled](false) before [markWebManageActive]. */
        fun clearPeerSyncPreference() {
            identityStore.setLanSyncEnabled(false)
        }
    }
