package com.rrparallelstudio.miapp.ads.firebase

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MmrAppVersionPolicyTest {
    @Test
    fun minZero_neverBlocks() {
        assertFalse(mustForceUpdateForVersion(installedVersionCode = 100, minRequired = 0L, firebaseConfigured = true))
    }

    @Test
    fun noFirebase_neverBlocks() {
        assertFalse(
            mustForceUpdateForVersion(installedVersionCode = 100, minRequired = 200L, firebaseConfigured = false),
        )
    }

    @Test
    fun belowMin_blocks() {
        assertTrue(mustForceUpdateForVersion(installedVersionCode = 901, minRequired = 902L, firebaseConfigured = true))
    }

    @Test
    fun atOrAboveMin_allows() {
        assertFalse(
            mustForceUpdateForVersion(installedVersionCode = 902, minRequired = 902L, firebaseConfigured = true),
        )
        assertFalse(
            mustForceUpdateForVersion(installedVersionCode = 903, minRequired = 902L, firebaseConfigured = true),
        )
    }
}
