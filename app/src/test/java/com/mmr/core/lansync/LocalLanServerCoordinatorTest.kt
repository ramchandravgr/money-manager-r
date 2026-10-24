package com.mmr.core.lansync

import com.mmr.core.data.preferences.LanSyncIdentityStore
import com.mmr.core.data.preferences.WebManagePreferencesStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class LocalLanServerCoordinatorTest {
    private lateinit var coordinator: LocalLanServerCoordinator
    private lateinit var identityStore: LanSyncIdentityStore
    private lateinit var webManageStore: WebManagePreferencesStore

    @Before
    fun setUp() {
        val context = RuntimeEnvironment.getApplication()
        identityStore = LanSyncIdentityStore(context)
        webManageStore = WebManagePreferencesStore(context)
        identityStore.setLanSyncEnabled(false)
        webManageStore.setWebManageEnabled(false)
        coordinator = LocalLanServerCoordinator(identityStore, webManageStore)
    }

    @Test
    fun peerSyncStarting_clearsWebManageFlag() {
        webManageStore.setWebManageEnabled(true)
        coordinator.clearWebManageBecausePeerSyncStarting()
        assertFalse(webManageStore.isWebManageEnabled())
    }

    @Test
    fun currentMode_reflectsExclusivePrefs() {
        assertEquals(LocalLanServerMode.NONE, coordinator.currentMode())
        identityStore.setLanSyncEnabled(true)
        assertEquals(LocalLanServerMode.PEER_SYNC, coordinator.currentMode())
        identityStore.setLanSyncEnabled(false)
        webManageStore.setWebManageEnabled(true)
        assertEquals(LocalLanServerMode.WEB_MANAGE, coordinator.currentMode())
    }
}
