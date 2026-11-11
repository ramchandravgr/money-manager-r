package com.rrparallelstudio.miapp.core.lansync

import com.rrparallelstudio.miapp.core.data.preferences.LanSyncPreferencesStore
import com.rrparallelstudio.miapp.core.domain.repository.LanSyncRepository
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Optional foreground sync when Peer Device Sync is on and the user enabled auto sync.
 */
@Singleton
class LanSyncAutoSyncCoordinator
    @Inject
    constructor(
        private val lanSyncRepository: LanSyncRepository,
        private val preferencesStore: LanSyncPreferencesStore,
    ) {
        private var lastAttemptAtMillis: Long = 0L

        suspend fun onAppForeground() {
            if (!preferencesStore.autoSyncOnAppOpen) return
            val now = System.currentTimeMillis()
            if (now - lastAttemptAtMillis < MIN_INTERVAL_MILLIS) return
            lastAttemptAtMillis = now
            lanSyncRepository.refreshSessionState()
            delay(DISCOVERY_WARMUP_MILLIS)
            lanSyncRepository.syncActiveTrustedPeersOnAppOpen()
        }

        private companion object {
            const val DISCOVERY_WARMUP_MILLIS = 2_000L
            const val MIN_INTERVAL_MILLIS = 30_000L
        }
    }
